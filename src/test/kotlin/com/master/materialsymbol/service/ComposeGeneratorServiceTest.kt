package com.master.materialsymbol.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeGeneratorServiceTest {

    @Test
    fun testConvertPathDataToDsl() {
        val pathData = "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"
        val dsl = ComposeGeneratorService.convertPathDataToDsl(pathData)

        assertTrue(dsl.contains("moveTo(10.0f, 20.0f)"))
        assertTrue(dsl.contains("verticalLineToRelative(-6.0f)"))
        assertTrue(dsl.contains("horizontalLineToRelative(4.0f)"))
        assertTrue(dsl.contains("verticalLineToRelative(6.0f)"))
        assertTrue(dsl.contains("lineTo(12.0f, 3.0f)"))
        assertTrue(dsl.contains("lineTo(2.0f, 12.0f)"))
        assertTrue(dsl.contains("close()"))
    }

    @Test
    fun testGenerateComposeSource() {
        val vectorXml = """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24">
                <path
                    android:fillColor="#FF000000"
                    android:pathData="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>
            </vector>
        """.trimIndent()

        val composeCode = ComposeGeneratorService.generateComposeSource(
            vectorXml = vectorXml,
            composeName = "Home",
            packageName = "com.example.app.ui.theme.icons"
        )

        assertTrue(composeCode.contains("package com.example.app.ui.theme.icons"))
        assertTrue(composeCode.contains("import androidx.compose.ui.graphics.vector.ImageVector"))
        assertTrue(composeCode.contains("public val Home: ImageVector"))
        assertTrue(composeCode.contains("private var _home: ImageVector? = null"))
        assertTrue(composeCode.contains("defaultWidth = 24.0.dp"))
        assertTrue(composeCode.contains("viewportWidth = 24.0f"))
        assertTrue(composeCode.contains("moveTo(10.0f, 20.0f)"))
        assertTrue(composeCode.contains("close()"))
    }

    @Test
    fun testParseVectorXml() {
        val vectorXml = """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="48dp"
                android:height="48dp"
                android:viewportWidth="48"
                android:viewportHeight="48">
                <path
                    android:fillColor="#FFFF0000"
                    android:pathData="M0 0 H48 V48 H0 Z"/>
            </vector>
        """.trimIndent()

        val parsed = ComposeGeneratorService.parseVectorXml(vectorXml, "TestIcon")
        assertEquals("TestIcon", parsed.name)
        assertEquals(48f, parsed.viewportWidth)
        assertEquals(48f, parsed.viewportHeight)
        assertEquals(1, parsed.paths.size)
        assertEquals("#FFFF0000", parsed.paths[0].fillColorHex)
        assertEquals("M0 0 H48 V48 H0 Z", parsed.paths[0].pathData)
    }

    @Test
    fun testIsKotlinComposeSource() {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24"><path d="M0 0h24v24H0z"/></svg>"""
        val vectorXml = """<vector xmlns:android="http://schemas.android.com/apk/res/android"><path android:pathData="M0 0"/></vector>"""
        val composeKt = """
            @Suppress("CheckReturnValue")
            public val encrypted: ImageVector
                get() {
                    return ImageVector.Builder("encrypted", 24.dp, 24.dp, 24f, 24f).build()
                }
        """.trimIndent()

        org.junit.Assert.assertFalse(ComposeGeneratorService.isKotlinComposeSource(svg))
        org.junit.Assert.assertFalse(ComposeGeneratorService.isKotlinComposeSource(vectorXml))
        assertTrue(ComposeGeneratorService.isKotlinComposeSource(composeKt))
    }

    @Test
    fun testExtractVectorPropertyName() {
        val code1 = """
            @Suppress("CheckReturnValue")
            public val encrypted: ImageVector
                get() = ...
        """.trimIndent()
        assertEquals("encrypted", ComposeGeneratorService.extractVectorPropertyName(code1))

        val code2 = """
            public val Encrypted: ImageVector
                get() = ...
        """.trimIndent()
        assertEquals("Encrypted", ComposeGeneratorService.extractVectorPropertyName(code2))

        val code3 = """
            val notes: ImageVector get() = ...
        """.trimIndent()
        assertEquals("notes", ComposeGeneratorService.extractVectorPropertyName(code3))
    }

    @Test
    fun testPrepareKotlinSource() {
        val originalNoPackage = """
            @Suppress("CheckReturnValue")
            public val encrypted: ImageVector
        """.trimIndent()

        val prepared = ComposeGeneratorService.prepareKotlinSource(originalNoPackage, "com.securevault.ui.theme.icons")
        assertTrue(prepared.startsWith("package com.securevault.ui.theme.icons\n\n"))
        assertTrue(prepared.contains("public val encrypted: ImageVector"))

        val originalWithPackage = """
            package androidx.compose.material.icons
            
            public val home: ImageVector
        """.trimIndent()
        val preparedReplaced = ComposeGeneratorService.prepareKotlinSource(originalWithPackage, "com.app.icons")
        assertTrue(preparedReplaced.startsWith("package com.app.icons"))
        org.junit.Assert.assertFalse(preparedReplaced.contains("androidx.compose.material.icons"))
    }
}
