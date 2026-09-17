package com.master.materialsymbol.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconsUpdaterServiceTest {

    @Test
    fun testAppendIconToCode() {
        val initialCode = """
            package com.example.app.ui.theme

            import androidx.compose.ui.graphics.vector.ImageVector

            object AppIcons {
            }
        """.trimIndent()

        val (updatedCode, updated) = AppIconsUpdaterService.appendIconToCode(
            currentText = initialCode,
            composeIconName = "Home",
            vectorPropertyName = "HomeVector",
            vectorPackageName = "com.example.app.ui.theme.icons"
        )

        assertTrue(updated)
        assertTrue("Should contain Home property with alias", updatedCode.contains("val Home: ImageVector get() = HomeIcon"))
        assertTrue("Should contain HomeVector aliased import", updatedCode.contains("import com.example.app.ui.theme.icons.HomeVector as HomeIcon"))
    }

    @Test
    fun testAppendIconToCodeWithDownloadedCamelCase() {
        val initialCode = """
            package com.securevault.ui.theme

            import androidx.compose.ui.graphics.vector.ImageVector

            object AppIcons {
            }
        """.trimIndent()

        val (updatedCode, updated) = AppIconsUpdaterService.appendIconToCode(
            currentText = initialCode,
            composeIconName = "Encrypted",
            vectorPropertyName = "encrypted",
            vectorPackageName = "com.securevault.ui.theme.icons"
        )

        assertTrue(updated)
        assertTrue("Should contain Encrypted property", updatedCode.contains("val Encrypted: ImageVector get() = EncryptedIcon"))
        assertTrue("Should contain encrypted aliased import", updatedCode.contains("import com.securevault.ui.theme.icons.encrypted as EncryptedIcon"))
    }

    @Test
    fun testFixExistingRecursiveGetter() {
        val recursiveCode = """
            package com.securevault.ui.theme

            import androidx.compose.ui.graphics.vector.ImageVector
            import com.securevault.ui.theme.icons.Notes

            object AppIcons {
                val Notes: ImageVector get() = Notes
            }
        """.trimIndent()

        val (fixedCode, updated) = AppIconsUpdaterService.appendIconToCode(
            currentText = recursiveCode,
            composeIconName = "Notes",
            vectorPropertyName = "Notes",
            vectorPackageName = "com.securevault.ui.theme.icons"
        )

        assertTrue("Should update and fix recursive code", updated)
        assertTrue("Should have aliased getter", fixedCode.contains("val Notes: ImageVector get() = NotesIcon"))
        assertFalse("Should not have self-referential recursive getter", fixedCode.contains("val Notes: ImageVector get() = Notes\n"))
        assertTrue("Should have aliased import", fixedCode.contains("import com.securevault.ui.theme.icons.Notes as NotesIcon"))
        assertFalse("Should have replaced unaliased import", fixedCode.contains("import com.securevault.ui.theme.icons.Notes\n"))
    }

    @Test
    fun testPreventDuplicatePropertyInCode() {
        val initialCode = """
            package com.example.app.ui.theme

            import androidx.compose.ui.graphics.vector.ImageVector
            import com.example.app.ui.theme.icons.HomeVector as HomeIcon

            object AppIcons {
                val Home: ImageVector get() = HomeIcon
            }
        """.trimIndent()

        val (updatedCode, updated) = AppIconsUpdaterService.appendIconToCode(
            currentText = initialCode,
            composeIconName = "Home",
            vectorPropertyName = "HomeVector",
            vectorPackageName = "com.example.app.ui.theme.icons"
        )

        assertFalse("Should not update when already present", updated)
        val occurrences = Regex("val Home: ImageVector").findAll(updatedCode).count()
        assertEquals("Should only have 1 occurrence", 1, occurrences)
    }

    @Test
    fun testAppendWhenObjectMissing() {
        val initialCode = """
            package com.example.app.ui.theme
        """.trimIndent()

        val (updatedCode, updated) = AppIconsUpdaterService.appendIconToCode(
            currentText = initialCode,
            composeIconName = "Favorite",
            vectorPropertyName = "FavoriteVector",
            vectorPackageName = "com.example.app.ui.theme.icons"
        )

        assertTrue(updated)
        assertTrue(updatedCode.contains("object AppIcons {"))
        assertTrue(updatedCode.contains("val Favorite: ImageVector get() = FavoriteIcon"))
        assertTrue(updatedCode.contains("import com.example.app.ui.theme.icons.FavoriteVector as FavoriteIcon"))
        assertTrue(updatedCode.contains("import androidx.compose.ui.graphics.vector.ImageVector"))
    }
}
