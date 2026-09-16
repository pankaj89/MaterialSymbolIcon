package com.master.materialsymbol.util

import java.net.URI
import java.util.Locale

object NamingHelper {

    /**
     * Converts raw input into a valid Android XML drawable resource name (lowercase snake_case with prefix).
     * E.g. "shopping-cart" -> "ic_shopping_cart", "arrow.up.circle" -> "ic_arrow_up_circle"
     */
    fun toXmlDrawableName(input: String, prefix: String = "ic_"): String {
        val cleaned = sanitizeSeparators(input)
        val snake = toSnakeCase(cleaned)
        val normalizedPrefix = if (prefix.endsWith("_")) prefix else "${prefix}_"
        
        val baseName = if (snake.startsWith(normalizedPrefix)) {
            snake.removePrefix(normalizedPrefix)
        } else if (snake.startsWith("ic_")) {
            snake.removePrefix("ic_")
        } else {
            snake
        }
        
        val finalName = "${normalizedPrefix}${baseName}".trim('_')
        return sanitizeIdentifier(finalName, forXml = true)
    }

    /**
     * Converts raw input into a valid Compose ImageVector identifier in PascalCase.
     * E.g. "shopping_cart" -> "ShoppingCart", "arrow.up.circle" -> "ArrowUpCircle"
     */
    fun toComposeName(input: String, suffix: String = ""): String {
        var cleaned = input.trim()
        if (cleaned.startsWith("ic_") || cleaned.startsWith("ic-")) {
            cleaned = cleaned.substring(3)
        }
        cleaned = sanitizeSeparators(cleaned)
        
        val words = cleaned.split("_").filter { it.isNotBlank() }
        val pascal = words.joinToString("") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
        
        val withSuffix = if (suffix.isNotBlank() && !pascal.endsWith(suffix)) {
            "${pascal}$suffix"
        } else {
            pascal
        }
        
        return sanitizeIdentifier(withSuffix, forXml = false)
    }

    /**
     * Attempts to extract a meaningful icon name from a URL or file path.
     */
    fun extractNameFromUrl(urlString: String): String {
        if (urlString.isBlank()) return ""
        return try {
            val uri = URI(urlString.trim())
            val path = uri.path ?: urlString.substringBefore("?")
            val segments = path.split("/").filter { it.isNotBlank() }
            
            // Check for Google fonts format: .../default/... where icon name precedes 'default'
            val defaultIdx = segments.indexOf("default")
            if (defaultIdx > 0) {
                return sanitizeSeparators(segments[defaultIdx - 1])
            }

            // Check if URL contains materialsymbols... where the following segment is the icon name
            val symbolIdx = segments.indexOfFirst { it.contains("materialsymbols", ignoreCase = true) }
            if (symbolIdx >= 0 && symbolIdx + 1 < segments.size) {
                val candidate = segments[symbolIdx + 1]
                if (candidate != "default" && !candidate.endsWith(".svg") && !candidate.endsWith(".kt")) {
                    return sanitizeSeparators(candidate)
                }
            }

            // Otherwise extract from last segment
            val lastSegment = segments.lastOrNull() ?: ""
            val baseName = lastSegment
                .removeSuffix(".kt")
                .removeSuffix(".svg")
                .removeSuffix(".xml")

            sanitizeSeparators(baseName)
        } catch (_: Exception) {
            val fallback = urlString.substringBefore("?").substringAfterLast('/')
                .removeSuffix(".kt").removeSuffix(".svg").removeSuffix(".xml")
            sanitizeSeparators(fallback)
        }
    }

    private fun sanitizeSeparators(input: String): String {
        return input
            .replace(Regex("[.\\-\\s/\\\\]+"), "_")
            .replace(Regex("[^a-zA-Z0-9_]"), "")
            .trim('_')
    }

    private fun toSnakeCase(input: String): String {
        return input
            .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .replace(Regex("[^a-zA-Z0-9_]"), "_")
            .lowercase(Locale.ROOT)
            .replace(Regex("_+"), "_")
            .trim('_')
    }

    private fun sanitizeIdentifier(input: String, forXml: Boolean): String {
        if (input.isEmpty()) return if (forXml) "ic_icon" else "Icon"
        
        var result = input
        if (result[0].isDigit()) {
            result = if (forXml) "ic_$result" else "Icon$result"
        }
        return result
    }
}
