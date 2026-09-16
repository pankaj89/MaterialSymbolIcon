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
}
