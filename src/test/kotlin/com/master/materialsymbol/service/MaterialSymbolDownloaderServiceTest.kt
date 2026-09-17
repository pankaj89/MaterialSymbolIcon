package com.master.materialsymbol.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialSymbolDownloaderServiceTest {

    @Test
    fun testValidateUrl() {
        val validUrl = "https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/home.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50"
        val result = MaterialSymbolDownloaderService.validateUrl(validUrl)
        assertTrue(result.isValid)
        assertEquals("home", result.detectedIconName)

        val emptyResult = MaterialSymbolDownloaderService.validateUrl("")
        assertFalse(emptyResult.isValid)

        val ftpResult = MaterialSymbolDownloaderService.validateUrl("ftp://example.com/icon.svg")
        assertFalse(ftpResult.isValid)
    }

    @Test
    fun testDownloadEncryptedKt() {
        val encryptedUrl = "https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/encrypted.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50"
        val content = MaterialSymbolDownloaderService.downloadIconContent(encryptedUrl)

        assertTrue("Content should contain ImageVector", content.contains("ImageVector"))
        assertTrue("Content should contain encrypted val declaration", content.contains("val encrypted: ImageVector"))
        assertTrue("Content should contain @Suppress", content.contains("""@Suppress("CheckReturnValue")"""))
        assertTrue("Content should contain viewportWidth = 24f", content.contains("viewportWidth = 24f"))
        assertTrue("Content should contain quadTo or path commands", content.contains("quadTo") || content.contains("moveTo"))

        // Also verify ComposeGeneratorService recognizes it as Kotlin Compose source
        assertTrue(ComposeGeneratorService.isKotlinComposeSource(content))
        assertEquals("encrypted", ComposeGeneratorService.extractVectorPropertyName(content))
    }
}
