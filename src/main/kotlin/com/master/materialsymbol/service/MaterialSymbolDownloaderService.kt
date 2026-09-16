package com.master.materialsymbol.service

import com.master.materialsymbol.util.NamingHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object MaterialSymbolDownloaderService {

    data class UrlValidationResult(
        val isValid: Boolean,
        val detectedIconName: String = "",
        val errorMessage: String? = null
    )

    fun validateUrl(urlString: String): UrlValidationResult {
        if (urlString.isBlank()) {
            return UrlValidationResult(isValid = false, errorMessage = "URL cannot be empty")
        }
        val trimmed = urlString.trim()
        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            return UrlValidationResult(isValid = false, errorMessage = "Malformed URL: ${e.message}")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return UrlValidationResult(isValid = false, errorMessage = "URL scheme must be http or https")
        }

        val name = NamingHelper.extractNameFromUrl(trimmed)
        return UrlValidationResult(
            isValid = true,
            detectedIconName = name
        )
    }

    /**
     * Downloads icon content from the URL.
     * If downloading from a .kt URL fails or returns HTML, attempts fallback to Google's standard SVG endpoint.
     */
    fun downloadIconContent(urlString: String): String {
        val trimmed = urlString.trim()
        val validation = validateUrl(trimmed)
        if (!validation.isValid) {
            throw IllegalArgumentException(validation.errorMessage ?: "Invalid URL")
        }

        try {
            val content = fetchUrl(trimmed)
            if (content.isNotBlank() && (content.contains("<svg") || content.contains("ImageVector") || content.contains("package "))) {
                return content
            }
        } catch (_: Exception) {
            // If primary URL failed, try fallback
        }

        // Try standard Google Fonts SVG endpoints if it was a Material Symbol
        val iconName = validation.detectedIconName
        if (iconName.isNotBlank()) {
            val fallbackUrls = listOf(
                "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/$iconName/default/24px.svg",
                "https://fonts.gstatic.com/s/i/materialsymbolsoutlined/$iconName/default/24px.svg",
                "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/$iconName/default/24px.svg",
                "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolssharp/$iconName/default/24px.svg"
            )
            for (fallback in fallbackUrls) {
                try {
                    val content = fetchUrl(fallback)
                    if (content.contains("<svg")) {
                        return content
                    }
                } catch (_: Exception) {
                    // Try next fallback
                }
            }
        }

        throw IllegalStateException("Failed to download valid SVG or Kotlin icon from $urlString")
    }

    private fun fetchUrl(urlString: String): String {
        val url = URI(urlString).toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android Studio MaterialSymbolIcon Plugin)")

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("Server returned HTTP $responseCode")
        }

        val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line).append("\n")
        }
        reader.close()
        return sb.toString().trim()
    }
}
