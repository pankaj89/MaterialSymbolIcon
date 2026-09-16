package com.master.materialsymbol.service

import com.android.ide.common.vectordrawable.Svg2Vector
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Files
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object SvgConversionService {

    /**
     * Sanitizes an SVG string (especially Apple SF Symbols SVGs) and converts it to Android Vector Drawable XML.
     */
    fun convertSvgToVectorXml(rawSvg: String): Pair<String, String?> {
        val sanitizedSvg = sanitizeSvg(rawSvg)
        
        var warningMessage: String? = null
        val tempSvg = Files.createTempFile("material_symbol_", ".svg")
        try {
            Files.writeString(tempSvg, sanitizedSvg)
            val outputStream = ByteArrayOutputStream()
            
            val resultWarning = Svg2Vector.parseSvgToXml(tempSvg, outputStream)
            val xmlOutput = outputStream.toString(Charsets.UTF_8)

            if (!resultWarning.isNullOrBlank()) {
                warningMessage = resultWarning
            }

            if (xmlOutput.isNotBlank() && xmlOutput.contains("<vector")) {
                return Pair(xmlOutput, warningMessage)
            } else {
                // Fallback to our own SVG to Vector generator if Svg2Vector output is empty
                val fallbackXml = fallbackSvgToVectorXml(sanitizedSvg)
                return Pair(fallbackXml, warningMessage ?: "Generated via fallback converter")
            }
        } catch (e: Exception) {
            val fallbackXml = fallbackSvgToVectorXml(sanitizedSvg)
            return Pair(fallbackXml, "Svg2Vector notice: ${e.message}. Used fallback converter.")
        } finally {
            Files.deleteIfExists(tempSvg)
        }
    }

    /**
     * Cleans Apple SF Symbols SVG quirks:
     * - Removes Guides, Notes, non-visible layers
     * - Selects the 'Regular' weight layer if multi-weight layers exist
     * - Strips Apple-specific namespaces and attributes
     * - Normalizes inline style attributes into XML presentation attributes
     * - Ensures valid viewBox and dimensions
     */
    fun sanitizeSvg(rawSvg: String): String {
        var svg = rawSvg.trim()
        
        // Remove XML declaration or DOCTYPE if present for cleaner parsing
        svg = svg.replace(Regex("<\\?xml[^>]*\\?>"), "")
            .replace(Regex("<!DOCTYPE[^>]*>"), "")
            .trim()

        if (!svg.startsWith("<svg") && svg.contains("<svg")) {
            svg = svg.substring(svg.indexOf("<svg"))
        }

        return try {
            val dbf = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isValidating = false
                setFeature("http://xml.org/sax/features/namespaces", false)
                setFeature("http://xml.org/sax/features/validation", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            }
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(InputSource(StringReader(svg)))
            val root = doc.documentElement

            cleanNode(root)
            normalizeDimensions(root)

            val tf = TransformerFactory.newInstance()
            val transformer = tf.newTransformer().apply {
                setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
                setOutputProperty(OutputKeys.INDENT, "yes")
            }
            val writer = StringWriter()
            transformer.transform(DOMSource(doc), StreamResult(writer))
            regexCleanSvg(writer.toString())
        } catch (_: Exception) {
            // Fallback regex cleaning if DOM parsing encounters malformed XML
            regexCleanSvg(svg)
        }
    }

    private fun cleanNode(node: Node) {
        if (node is Element) {
            cleanElementAttributes(node)
        }
        val childNodes = node.childNodes
        var i = childNodes.length - 1
        while (i >= 0) {
            val child = childNodes.item(i)
            if (child is Element) {
                val id = child.getAttribute("id").lowercase()
                val display = child.getAttribute("display").lowercase()
                val visibility = child.getAttribute("visibility").lowercase()
                val tagName = child.tagName.lowercase()

                // Remove guide layers, notes, and invisible layers
                if (id == "guides" || id == "notes" || id.contains("guideline") ||
                    display == "none" || visibility == "hidden" ||
                    tagName == "metadata" || tagName == "sfs"
                ) {
                    node.removeChild(child)
                    i--
                    continue
                }

                // If SF Symbols has multiple weight layers, prefer Regular / first non-guide layer
                if (id.endsWith("-layer") && !id.contains("regular") && !id.contains("default")) {
                    val hasRegularSibling = hasSiblingWithId(node, "regular")
                    if (hasRegularSibling) {
                        node.removeChild(child)
                        i--
                        continue
                    }
                }

                cleanElementAttributes(child)
                normalizeStyleAttribute(child)

                // Recurse into children
                cleanNode(child)
            }
            i--
        }
    }

    private fun cleanElementAttributes(element: Element) {
        val attributes = element.attributes
        val attrsToRemove = mutableListOf<String>()
        for (a in 0 until attributes.length) {
            val attr = attributes.item(a)
            val attrName = attr.nodeName
            if (attrName.startsWith("sfs:") || attrName.startsWith("xmlns:sfs") ||
                attrName == "data-name" || attrName == "sketch:type"
            ) {
                attrsToRemove.add(attrName)
            }
        }
        for (attrName in attrsToRemove) {
            element.removeAttribute(attrName)
        }
    }

    private fun hasSiblingWithId(parent: Node, needle: String): Boolean {
        val children = parent.childNodes
        for (j in 0 until children.length) {
            val item = children.item(j)
            if (item is Element && item.getAttribute("id").lowercase().contains(needle)) {
                return true
            }
        }
        return false
    }

    private fun normalizeStyleAttribute(element: Element) {
        val style = element.getAttribute("style")
        if (style.isNotBlank()) {
            val declarations = style.split(";").map { it.trim() }.filter { it.isNotBlank() }
            for (decl in declarations) {
                val parts = decl.split(":")
                if (parts.size == 2) {
                    val key = parts[0].trim().lowercase()
                    val value = parts[1].trim()
                    if (key in listOf("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "fill-rule", "opacity", "fill-opacity")) {
                        if (!element.hasAttribute(key)) {
                            element.setAttribute(key, value)
                        }
                    }
                }
            }
            element.removeAttribute("style")
        }
    }

    private fun normalizeDimensions(root: Element) {
        val viewBox = root.getAttribute("viewBox")
        if (viewBox.isNotBlank()) {
            val parts = viewBox.trim().split(Regex("[\\s,]+"))
            if (parts.size == 4) {
                val w = parts[2]
                val h = parts[3]
                if (!root.hasAttribute("width") || root.getAttribute("width").isBlank()) {
                    root.setAttribute("width", "${w}px")
                }
                if (!root.hasAttribute("height") || root.getAttribute("height").isBlank()) {
                    root.setAttribute("height", "${h}px")
                }
            }
        } else if (root.hasAttribute("width") && root.hasAttribute("height")) {
            val w = root.getAttribute("width").replace(Regex("[^0-9.]"), "")
            val h = root.getAttribute("height").replace(Regex("[^0-9.]"), "")
            if (w.isNotBlank() && h.isNotBlank()) {
                root.setAttribute("viewBox", "0 0 $w $h")
            }
        }
    }

    private fun regexCleanSvg(svg: String): String {
        return svg
            .replace(Regex("<g\\s+[^>]*id=[\"']Guides[\"'][^>]*>.*?</g>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<g\\s+[^>]*id=[\"']Notes[\"'][^>]*>.*?</g>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("sfs:[a-zA-Z0-9_-]+=\"[^\"]*\""), "")
            .replace(Regex("xmlns:sfs=\"[^\"]*\""), "")
    }

    /**
     * Fallback converter that transforms SVG elements into Android Vector Drawable XML directly.
     */
    fun fallbackSvgToVectorXml(svg: String): String {
        val viewBoxMatch = Regex("viewBox=[\"']([0-9.\\s,-]+)[\"']").find(svg)
        var vpWidth = "24"
        var vpHeight = "24"
        if (viewBoxMatch != null) {
            val coords = viewBoxMatch.groupValues[1].trim().split(Regex("[\\s,]+"))
            if (coords.size == 4) {
                vpWidth = coords[2]
                vpHeight = coords[3]
            }
        }

        val pathMatches = Regex("<path\\s+([^>]*)/?>").findAll(svg)
        val pathsBuilder = StringBuilder()

        for (match in pathMatches) {
            val attributes = match.groupValues[1]
            val dMatch = Regex("d=[\"']([^\"']+)[\"']").find(attributes)
            if (dMatch != null) {
                val pathData = dMatch.groupValues[1]
                val fillMatch = Regex("fill=[\"']([^\"']+)[\"']").find(attributes)
                val fillColor = fillMatch?.groupValues?.get(1)?.let { parseColor(it) } ?: "#FF000000"
                val fillTypeMatch = Regex("fill-rule=[\"']evenodd[\"']", RegexOption.IGNORE_CASE).find(attributes)
                val fillTypeAttr = if (fillTypeMatch != null) " android:fillType=\"evenOdd\"" else ""

                pathsBuilder.append("    <path\n")
                pathsBuilder.append("        android:fillColor=\"$fillColor\"\n")
                if (fillTypeAttr.isNotBlank()) {
                    pathsBuilder.append("       $fillTypeAttr\n")
                }
                pathsBuilder.append("        android:pathData=\"$pathData\" />\n")
            }
        }

        return """
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="${vpWidth}dp"
    android:height="${vpHeight}dp"
    android:viewportWidth="$vpWidth"
    android:viewportHeight="$vpHeight">
$pathsBuilder</vector>
        """.trimIndent()
    }

    private fun parseColor(colorStr: String): String {
        val trimmed = colorStr.trim()
        return when {
            trimmed.startsWith("#") -> {
                when (trimmed.length) {
                    4 -> "#FF${trimmed[1]}${trimmed[1]}${trimmed[2]}${trimmed[2]}${trimmed[3]}${trimmed[3]}"
                    7 -> "#FF${trimmed.substring(1)}"
                    9 -> trimmed
                    else -> "#FF000000"
                }
            }
            trimmed.equals("none", ignoreCase = true) -> "#00000000"
            trimmed.equals("black", ignoreCase = true) -> "#FF000000"
            trimmed.equals("white", ignoreCase = true) -> "#FFFFFFFF"
            else -> "#FF000000"
        }
    }
}
