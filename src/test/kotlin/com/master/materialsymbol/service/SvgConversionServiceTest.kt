package com.master.materialsymbol.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SvgConversionServiceTest {

    @Test
    fun testAppleSfSymbolsSanitization() {
        val appleSvg = """
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:sfs="http://schemas.apple.com/symbols/2021" viewBox="0 0 100 100">
                <g id="Guides">
                    <path d="M0 0 L100 100" />
                </g>
                <g id="Notes">
                    <text>Some note</text>
                </g>
                <g id="Regular" sfs:layer="monochrome" style="fill: #FF5722;">
                    <path d="M 10 10 L 90 10 L 90 90 Z" sfs:weight="regular" />
                </g>
            </svg>
        """.trimIndent()

        val sanitized = SvgConversionService.sanitizeSvg(appleSvg)

        // Verify guides and notes removed
        assertFalse("Guides should be removed", sanitized.contains("id=\"Guides\""))
        assertFalse("Notes should be removed", sanitized.contains("id=\"Notes\""))

        // Verify Apple namespace and attributes removed
        assertFalse("sfs namespace should be removed", sanitized.contains("xmlns:sfs"))
        assertFalse("sfs:layer attribute should be removed", sanitized.contains("sfs:layer"))
        assertFalse("sfs:weight attribute should be removed", sanitized.contains("sfs:weight"))

        // Verify path preserved
        assertTrue("Path should be preserved", sanitized.contains("M 10 10 L 90 10 L 90 90 Z"))
    }

    @Test
    fun testConvertSvgToVectorXml() {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">
                <path fill="#000000" d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>
            </svg>
        """.trimIndent()

        val (vectorXml, _) = SvgConversionService.convertSvgToVectorXml(svg)

        assertTrue("Should contain <vector", vectorXml.contains("<vector"))
        assertTrue("Should contain pathData", vectorXml.contains("android:pathData"))
        assertTrue("Should preserve path data", vectorXml.contains("M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z") || vectorXml.contains("M10,20"))
    }

    @Test
    fun testFallbackSvgToVectorXml() {
        val svg = """
            <svg viewBox="0 0 48 48">
                <path d="M0 0 H48 V48 H0 Z" fill="#FF0000" />
            </svg>
        """.trimIndent()

        val fallbackXml = SvgConversionService.fallbackSvgToVectorXml(svg)

        assertTrue(fallbackXml.contains("android:viewportWidth=\"48\""))
        assertTrue(fallbackXml.contains("android:viewportHeight=\"48\""))
        assertTrue(fallbackXml.contains("android:fillColor=\"#FFFF0000\""))
        assertTrue(fallbackXml.contains("android:pathData=\"M0 0 H48 V48 H0 Z\""))
    }
}
