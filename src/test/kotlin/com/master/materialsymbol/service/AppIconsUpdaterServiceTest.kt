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
        assertTrue("Should contain Home property", updatedCode.contains("val Home: ImageVector get() = HomeVector"))
        assertTrue("Should contain HomeVector import", updatedCode.contains("import com.example.app.ui.theme.icons.HomeVector"))
    }

    @Test
    fun testPreventDuplicatePropertyInCode() {
        val initialCode = """
            package com.example.app.ui.theme

            import androidx.compose.ui.graphics.vector.ImageVector
            import com.example.app.ui.theme.icons.HomeVector

            object AppIcons {
                val Home: ImageVector get() = HomeVector
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
        assertTrue(updatedCode.contains("val Favorite: ImageVector get() = FavoriteVector"))
        assertTrue(updatedCode.contains("import androidx.compose.ui.graphics.vector.ImageVector"))
    }
}
