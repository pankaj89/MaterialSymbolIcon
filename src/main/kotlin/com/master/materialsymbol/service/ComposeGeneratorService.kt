package com.master.materialsymbol.service

import java.util.Locale

object ComposeGeneratorService {

    data class VectorPath(
        val pathData: String,
        val fillColorHex: String? = null,
        val fillAlpha: Float = 1.0f,
        val strokeColorHex: String? = null,
        val strokeAlpha: Float = 1.0f,
        val strokeWidth: Float = 1.0f,
        val strokeCap: String = "Butt",
        val strokeJoin: String = "Miter",
        val strokeMiter: Float = 4.0f,
        val fillType: String = "NonZero"
    )

    data class VectorData(
        val name: String,
        val widthDp: Float = 24f,
        val heightDp: Float = 24f,
        val viewportWidth: Float = 24f,
        val viewportHeight: Float = 24f,
        val paths: List<VectorPath> = emptyList()
    )

    /**
     * Parses Vector XML into VectorData, then generates a complete Jetpack Compose ImageVector Kotlin source file.
     */
    fun generateComposeSource(
        vectorXml: String,
        composeName: String,
        packageName: String
    ): String {
        val vectorData = parseVectorXml(vectorXml, composeName)
        return buildComposeKotlinFile(vectorData, packageName)
    }

    /**
     * Parses Android Vector Drawable XML.
     */
    fun parseVectorXml(vectorXml: String, iconName: String): VectorData {
        val vpWidth = extractFloat(vectorXml, "android:viewportWidth") ?: 24f
        val vpHeight = extractFloat(vectorXml, "android:viewportHeight") ?: 24f
        val widthDp = extractDimension(vectorXml, "android:width") ?: vpWidth
        val heightDp = extractDimension(vectorXml, "android:height") ?: vpHeight

        val pathRegex = Regex("<path\\s+([^>]*)/?>", RegexOption.DOT_MATCHES_ALL)
        val paths = mutableListOf<VectorPath>()

        for (match in pathRegex.findAll(vectorXml)) {
            val attributes = match.groupValues[1]
            val pathData = extractAttribute(attributes, "android:pathData") ?: continue
            val fillColor = extractAttribute(attributes, "android:fillColor")
            val fillAlpha = extractFloat(attributes, "android:fillAlpha") ?: 1.0f
            val strokeColor = extractAttribute(attributes, "android:strokeColor")
            val strokeAlpha = extractFloat(attributes, "android:strokeAlpha") ?: 1.0f
            val strokeWidth = extractFloat(attributes, "android:strokeWidth") ?: 1.0f
            val strokeCap = when (extractAttribute(attributes, "android:strokeLineCap")?.lowercase()) {
                "round" -> "Round"
                "square" -> "Square"
                else -> "Butt"
            }
            val strokeJoin = when (extractAttribute(attributes, "android:strokeLineJoin")?.lowercase()) {
                "round" -> "Round"
                "bevel" -> "Bevel"
                else -> "Miter"
            }
            val strokeMiter = extractFloat(attributes, "android:strokeMiterLimit") ?: 4.0f
            val fillType = if (extractAttribute(attributes, "android:fillType")?.equals("evenOdd", ignoreCase = true) == true) {
                "EvenOdd"
            } else {
                "NonZero"
            }

            paths.add(
                VectorPath(
                    pathData = pathData,
                    fillColorHex = fillColor,
                    fillAlpha = fillAlpha,
                    strokeColorHex = strokeColor,
                    strokeAlpha = strokeAlpha,
                    strokeWidth = strokeWidth,
                    strokeCap = strokeCap,
                    strokeJoin = strokeJoin,
                    strokeMiter = strokeMiter,
                    fillType = fillType
                )
            )
        }

        return VectorData(
            name = iconName,
            widthDp = widthDp,
            heightDp = heightDp,
            viewportWidth = vpWidth,
            viewportHeight = vpHeight,
            paths = paths
        )
    }

    private fun buildComposeKotlinFile(data: VectorData, packageName: String): String {
        val backingVar = "_${data.name.replaceFirstChar { it.lowercase(Locale.ROOT) }}"
        val hasPaths = data.paths.isNotEmpty()
        
        val pathsCode = StringBuilder()
        for (p in data.paths) {
            val dslCommands = convertPathDataToDsl(p.pathData)
            val fillSnippet = if (!p.fillColorHex.isNullOrBlank() && !p.fillColorHex.equals("#00000000", ignoreCase = true)) {
                val hex = formatHexColor(p.fillColorHex)
                "SolidColor(Color($hex))"
            } else {
                "SolidColor(Color(0xFF000000))"
            }

            val strokeSnippet = if (!p.strokeColorHex.isNullOrBlank()) {
                val hex = formatHexColor(p.strokeColorHex)
                "SolidColor(Color($hex))"
            } else {
                "null"
            }

            pathsCode.append("            path(\n")
            pathsCode.append("                fill = $fillSnippet,\n")
            pathsCode.append("                fillAlpha = ${p.fillAlpha}f,\n")
            pathsCode.append("                stroke = $strokeSnippet,\n")
            pathsCode.append("                strokeAlpha = ${p.strokeAlpha}f,\n")
            pathsCode.append("                strokeLineWidth = ${p.strokeWidth}f,\n")
            pathsCode.append("                strokeLineCap = StrokeCap.${p.strokeCap},\n")
            pathsCode.append("                strokeLineJoin = StrokeJoin.${p.strokeJoin},\n")
            pathsCode.append("                strokeLineMiter = ${p.strokeMiter}f,\n")
            pathsCode.append("                pathFillType = PathFillType.${p.fillType}\n")
            pathsCode.append("            ) {\n")
            pathsCode.append(dslCommands)
            pathsCode.append("            }\n")
        }

        val pkgHeader = if (packageName.isNotBlank()) "package $packageName\n\n" else ""

        return """
${pkgHeader}import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val ${data.name}: ImageVector
    get() {
        if ($backingVar != null) {
            return $backingVar!!
        }
        $backingVar = ImageVector.Builder(
            name = "${data.name}",
            defaultWidth = ${data.widthDp}.dp,
            defaultHeight = ${data.heightDp}.dp,
            viewportWidth = ${data.viewportWidth}f,
            viewportHeight = ${data.viewportHeight}f
        ).apply {
$pathsCode        }.build()
        return $backingVar!!
    }

private var $backingVar: ImageVector? = null
        """.trimIndent()
    }

    private fun formatHexColor(rawHex: String): String {
        val trimmed = rawHex.trim().removePrefix("#")
        return when (trimmed.length) {
            3 -> "0xFF${trimmed[0]}${trimmed[0]}${trimmed[1]}${trimmed[1]}${trimmed[2]}${trimmed[2]}"
            6 -> "0xFF$trimmed"
            8 -> "0x$trimmed"
            else -> "0xFF000000"
        }
    }

    /**
     * Converts an SVG pathData string into indented Compose PathBuilder DSL function calls.
     */
    fun convertPathDataToDsl(pathData: String): String {
        val tokens = tokenizePathData(pathData)
        val sb = StringBuilder()
        var i = 0
        var currentCmd = ' '

        while (i < tokens.size) {
            val token = tokens[i]
            if (token.length == 1 && token[0].isLetter()) {
                currentCmd = token[0]
                i++
            }

            when (currentCmd) {
                'M' -> {
                    if (i + 1 < tokens.size) {
                        val x = tokens[i++].toFloatOrNull() ?: 0f
                        val y = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                moveTo(${fmt(x)}, ${fmt(y)})\n")
                        currentCmd = 'L' // Implicit lineTo for subsequent pairs
                    } else i++
                }
                'm' -> {
                    if (i + 1 < tokens.size) {
                        val dx = tokens[i++].toFloatOrNull() ?: 0f
                        val dy = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                moveToRelative(${fmt(dx)}, ${fmt(dy)})\n")
                        currentCmd = 'l'
                    } else i++
                }
                'L' -> {
                    if (i + 1 < tokens.size) {
                        val x = tokens[i++].toFloatOrNull() ?: 0f
                        val y = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                lineTo(${fmt(x)}, ${fmt(y)})\n")
                    } else i++
                }
                'l' -> {
                    if (i + 1 < tokens.size) {
                        val dx = tokens[i++].toFloatOrNull() ?: 0f
                        val dy = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                lineToRelative(${fmt(dx)}, ${fmt(dy)})\n")
                    } else i++
                }
                'H' -> {
                    if (i < tokens.size) {
                        val x = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                horizontalLineTo(${fmt(x)})\n")
                    }
                }
                'h' -> {
                    if (i < tokens.size) {
                        val dx = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                horizontalLineToRelative(${fmt(dx)})\n")
                    }
                }
                'V' -> {
                    if (i < tokens.size) {
                        val y = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                verticalLineTo(${fmt(y)})\n")
                    }
                }
                'v' -> {
                    if (i < tokens.size) {
                        val dy = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                verticalLineToRelative(${fmt(dy)})\n")
                    }
                }
                'C' -> {
                    if (i + 5 < tokens.size) {
                        val x1 = tokens[i++].toFloatOrNull() ?: 0f
                        val y1 = tokens[i++].toFloatOrNull() ?: 0f
                        val x2 = tokens[i++].toFloatOrNull() ?: 0f
                        val y2 = tokens[i++].toFloatOrNull() ?: 0f
                        val x3 = tokens[i++].toFloatOrNull() ?: 0f
                        val y3 = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                curveTo(${fmt(x1)}, ${fmt(y1)}, ${fmt(x2)}, ${fmt(y2)}, ${fmt(x3)}, ${fmt(y3)})\n")
                    } else i++
                }
                'c' -> {
                    if (i + 5 < tokens.size) {
                        val dx1 = tokens[i++].toFloatOrNull() ?: 0f
                        val dy1 = tokens[i++].toFloatOrNull() ?: 0f
                        val dx2 = tokens[i++].toFloatOrNull() ?: 0f
                        val dy2 = tokens[i++].toFloatOrNull() ?: 0f
                        val dx3 = tokens[i++].toFloatOrNull() ?: 0f
                        val dy3 = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                curveToRelative(${fmt(dx1)}, ${fmt(dy1)}, ${fmt(dx2)}, ${fmt(dy2)}, ${fmt(dx3)}, ${fmt(dy3)})\n")
                    } else i++
                }
                'S' -> {
                    if (i + 3 < tokens.size) {
                        val x2 = tokens[i++].toFloatOrNull() ?: 0f
                        val y2 = tokens[i++].toFloatOrNull() ?: 0f
                        val x3 = tokens[i++].toFloatOrNull() ?: 0f
                        val y3 = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                reflectiveCurveTo(${fmt(x2)}, ${fmt(y2)}, ${fmt(x3)}, ${fmt(y3)})\n")
                    } else i++
                }
                's' -> {
                    if (i + 3 < tokens.size) {
                        val dx2 = tokens[i++].toFloatOrNull() ?: 0f
                        val dy2 = tokens[i++].toFloatOrNull() ?: 0f
                        val dx3 = tokens[i++].toFloatOrNull() ?: 0f
                        val dy3 = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                reflectiveCurveToRelative(${fmt(dx2)}, ${fmt(dy2)}, ${fmt(dx3)}, ${fmt(dy3)})\n")
                    } else i++
                }
                'Q' -> {
                    if (i + 3 < tokens.size) {
                        val x1 = tokens[i++].toFloatOrNull() ?: 0f
                        val y1 = tokens[i++].toFloatOrNull() ?: 0f
                        val x2 = tokens[i++].toFloatOrNull() ?: 0f
                        val y2 = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                quadTo(${fmt(x1)}, ${fmt(y1)}, ${fmt(x2)}, ${fmt(y2)})\n")
                    } else i++
                }
                'q' -> {
                    if (i + 3 < tokens.size) {
                        val dx1 = tokens[i++].toFloatOrNull() ?: 0f
                        val dy1 = tokens[i++].toFloatOrNull() ?: 0f
                        val dx2 = tokens[i++].toFloatOrNull() ?: 0f
                        val dy2 = tokens[i++].toFloatOrNull() ?: 0f
                        sb.append("                quadToRelative(${fmt(dx1)}, ${fmt(dy1)}, ${fmt(dx2)}, ${fmt(dy2)})\n")
                    } else i++
                }
                'Z', 'z' -> {
                    sb.append("                close()\n")
                }
                else -> {
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun fmt(value: Float): String {
        return if (value % 1.0f == 0.0f) {
            "${value.toInt()}.0f"
        } else {
            "${value}f"
        }
    }

    private fun tokenizePathData(pathData: String): List<String> {
        val tokens = mutableListOf<String>()
        val regex = Regex("([a-zA-Z]|[-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)")
        for (m in regex.findAll(pathData)) {
            val token = m.value.trim()
            if (token.isNotBlank()) {
                tokens.add(token)
            }
        }
        return tokens
    }

    private fun extractAttribute(xml: String, attrName: String): String? {
        val regex = Regex("""$attrName\s*=\s*"([^"]*)"""")
        return regex.find(xml)?.groupValues?.get(1)
    }

    private fun extractFloat(xml: String, attrName: String): Float? {
        return extractAttribute(xml, attrName)?.toFloatOrNull()
    }

    private fun extractDimension(xml: String, attrName: String): Float? {
        val raw = extractAttribute(xml, attrName) ?: return null
        return raw.replace(Regex("[^0-9.]"), "").toFloatOrNull()
    }
}
