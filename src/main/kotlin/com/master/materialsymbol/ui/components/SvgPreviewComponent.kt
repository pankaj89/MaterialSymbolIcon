package com.master.materialsymbol.ui.components

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import javax.swing.JPanel

class SvgPreviewComponent : JPanel() {

    private var currentPath: Path2D.Float? = null
    private var viewportWidth: Float = 24f
    private var viewportHeight: Float = 24f

    init {
        preferredSize = Dimension(JBUI.scale(120), JBUI.scale(120))
        minimumSize = Dimension(JBUI.scale(80), JBUI.scale(80))
        background = JBColor.PanelBackground
        border = JBUI.Borders.customLine(JBColor.border(), 1)
    }

    fun updateSvg(svgOrXml: String) {
        if (svgOrXml.isBlank()) {
            currentPath = null
            repaint()
            return
        }

        try {
            // Extract viewBox or viewport
            val vpMatch = Regex("viewportWidth=[\"']([0-9.]+)[\"']").find(svgOrXml)
                ?: Regex("viewBox=[\"'][0-9.\\s,-]+\\s+([0-9.]+)\\s+([0-9.]+)[\"']").find(svgOrXml)
            if (vpMatch != null && vpMatch.groupValues.size >= 2) {
                viewportWidth = vpMatch.groupValues[1].toFloatOrNull() ?: 24f
                viewportHeight = if (vpMatch.groupValues.size >= 3) {
                    vpMatch.groupValues[2].toFloatOrNull() ?: viewportWidth
                } else viewportWidth
            }

            // Extract all pathData or d attributes
            val dRegex = Regex("""(?:android:pathData|d)\s*=\s*"([^"]*)"""")
            val combinedPath = Path2D.Float()
            var found = false

            for (match in dRegex.findAll(svgOrXml)) {
                val d = match.groupValues[1]
                val path = parseSvgPath(d)
                combinedPath.append(path, false)
                found = true
            }

            currentPath = if (found) combinedPath else null
        } catch (_: Exception) {
            currentPath = null
        }
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as? Graphics2D ?: return
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val path = currentPath
        if (path == null) {
            g2.color = JBColor.GRAY
            val msg = "No Preview"
            val fm = g2.fontMetrics
            val x = (width - fm.stringWidth(msg)) / 2
            val y = (height - fm.height) / 2 + fm.ascent
            g2.drawString(msg, x, y)
            return
        }

        val padding = JBUI.scale(16)
        val drawWidth = width - (padding * 2)
        val drawHeight = height - (padding * 2)

        if (drawWidth <= 0 || drawHeight <= 0) return

        val scaleX = drawWidth.toDouble() / viewportWidth.toDouble()
        val scaleY = drawHeight.toDouble() / viewportHeight.toDouble()
        val scale = minOf(scaleX, scaleY)

        val xOffset = padding + (drawWidth - (viewportWidth * scale)) / 2.0
        val yOffset = padding + (drawHeight - (viewportHeight * scale)) / 2.0

        val transform = AffineTransform().apply {
            translate(xOffset, yOffset)
            scale(scale, scale)
        }

        val transformedPath = path.createTransformedShape(transform)
        g2.color = JBColor.foreground()
        g2.fill(transformedPath)
    }

    private fun parseSvgPath(d: String): Path2D.Float {
        val path = Path2D.Float()
        val tokens = tokenize(d)
        var i = 0
        var cmd = ' '

        while (i < tokens.size) {
            val token = tokens[i]
            if (token.length == 1 && token[0].isLetter()) {
                cmd = token[0]
                i++
            }

            when (cmd) {
                'M' -> {
                    if (i + 1 < tokens.size) {
                        path.moveTo(tokens[i++].toFloatOrNull() ?: 0f, tokens[i++].toFloatOrNull() ?: 0f)
                        cmd = 'L'
                    } else i++
                }
                'm' -> {
                    if (i + 1 < tokens.size) {
                        val cx = path.currentPoint?.x?.toFloat() ?: 0f
                        val cy = path.currentPoint?.y?.toFloat() ?: 0f
                        path.moveTo(cx + (tokens[i++].toFloatOrNull() ?: 0f), cy + (tokens[i++].toFloatOrNull() ?: 0f))
                        cmd = 'l'
                    } else i++
                }
                'L' -> {
                    if (i + 1 < tokens.size) {
                        path.lineTo(tokens[i++].toFloatOrNull() ?: 0f, tokens[i++].toFloatOrNull() ?: 0f)
                    } else i++
                }
                'l' -> {
                    if (i + 1 < tokens.size) {
                        val cx = path.currentPoint?.x?.toFloat() ?: 0f
                        val cy = path.currentPoint?.y?.toFloat() ?: 0f
                        path.lineTo(cx + (tokens[i++].toFloatOrNull() ?: 0f), cy + (tokens[i++].toFloatOrNull() ?: 0f))
                    } else i++
                }
                'H' -> {
                    if (i < tokens.size) {
                        val cy = path.currentPoint?.y?.toFloat() ?: 0f
                        path.lineTo(tokens[i++].toFloatOrNull() ?: 0f, cy)
                    }
                }
                'h' -> {
                    if (i < tokens.size) {
                        val cx = path.currentPoint?.x?.toFloat() ?: 0f
                        val cy = path.currentPoint?.y?.toFloat() ?: 0f
                        path.lineTo(cx + (tokens[i++].toFloatOrNull() ?: 0f), cy)
                    }
                }
                'V' -> {
                    if (i < tokens.size) {
                        val cx = path.currentPoint?.x?.toFloat() ?: 0f
                        path.lineTo(cx, tokens[i++].toFloatOrNull() ?: 0f)
                    }
                }
                'v' -> {
                    if (i < tokens.size) {
                        val cx = path.currentPoint?.x?.toFloat() ?: 0f
                        val cy = path.currentPoint?.y?.toFloat() ?: 0f
                        path.lineTo(cx, cy + (tokens[i++].toFloatOrNull() ?: 0f))
                    }
                }
                'C' -> {
                    if (i + 5 < tokens.size) {
                        path.curveTo(
                            tokens[i++].toFloatOrNull() ?: 0f, tokens[i++].toFloatOrNull() ?: 0f,
                            tokens[i++].toFloatOrNull() ?: 0f, tokens[i++].toFloatOrNull() ?: 0f,
                            tokens[i++].toFloatOrNull() ?: 0f, tokens[i++].toFloatOrNull() ?: 0f
                        )
                    } else i++
                }
                'c' -> {
                    if (i + 5 < tokens.size) {
                        val cx = path.currentPoint?.x?.toFloat() ?: 0f
                        val cy = path.currentPoint?.y?.toFloat() ?: 0f
                        path.curveTo(
                            cx + (tokens[i++].toFloatOrNull() ?: 0f), cy + (tokens[i++].toFloatOrNull() ?: 0f),
                            cx + (tokens[i++].toFloatOrNull() ?: 0f), cy + (tokens[i++].toFloatOrNull() ?: 0f),
                            cx + (tokens[i++].toFloatOrNull() ?: 0f), cy + (tokens[i++].toFloatOrNull() ?: 0f)
                        )
                    } else i++
                }
                'Z', 'z' -> {
                    path.closePath()
                }
                else -> i++
            }
        }
        return path
    }

    private fun tokenize(d: String): List<String> {
        val list = mutableListOf<String>()
        val regex = Regex("([a-zA-Z]|[-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)")
        for (m in regex.findAll(d)) {
            val token = m.value.trim()
            if (token.isNotBlank()) list.add(token)
        }
        return list
    }
}
