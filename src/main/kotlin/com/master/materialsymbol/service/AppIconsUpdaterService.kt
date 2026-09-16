package com.master.materialsymbol.service

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.master.materialsymbol.util.ProjectStructureHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath
import java.io.File

object AppIconsUpdaterService {

    /**
     * Appends a new icon property to the AppIcons Kotlin object using IntelliJ Kotlin PSI.
     * Wrapped in WriteCommandAction for undo/redo support.
     *
     * @return true if appended successfully, false if property already exists or on failure.
     */
    fun appendIconToRegistry(
        project: Project,
        appIconsFilePath: String,
        composeIconName: String,
        vectorPropertyName: String,
        vectorPackageName: String
    ): Boolean {
        var success = false

        WriteCommandAction.runWriteCommandAction(project, "Append Icon to AppIcons", null, Runnable {
            try {
                val virtualFile = getOrCreateAppIconsFile(project, appIconsFilePath, vectorPackageName)
                    ?: return@Runnable

                val psiManager = PsiManager.getInstance(project)
                val psiFile = psiManager.findFile(virtualFile) as? KtFile ?: return@Runnable
                val factory = KtPsiFactory(project)

                // 1. Locate or create object AppIcons
                val targetObject = psiFile.declarations.filterIsInstance<KtObjectDeclaration>()
                    .find { it.name == "AppIcons" }
                    ?: psiFile.declarations.filterIsInstance<KtObjectDeclaration>().firstOrNull()
                    ?: run {
                        val newObj = factory.createObject("object AppIcons {\n}")
                        psiFile.add(newObj) as KtObjectDeclaration
                    }

                // 2. Check if property already exists
                val existingProp = targetObject.declarations.filterIsInstance<KtProperty>()
                    .find { it.name == composeIconName }
                if (existingProp != null) {
                    success = true // Already present
                    return@Runnable
                }

                // 3. Ensure imports exist
                ensureImports(psiFile, factory, vectorPackageName, vectorPropertyName)

                // 4. Create and append the new property
                val propertyCode = "val $composeIconName: ImageVector get() = $vectorPropertyName"
                val newProperty = factory.createProperty(propertyCode)

                val body = targetObject.body ?: (targetObject.add(factory.createEmptyClassBody()) as org.jetbrains.kotlin.psi.KtClassBody)
                val rBrace = body.rBrace
                if (rBrace != null) {
                    body.addBefore(newProperty, rBrace)
                    body.addBefore(factory.createNewLine(), rBrace)
                } else {
                    body.add(newProperty)
                }

                // 5. Format the modified code
                CodeStyleManager.getInstance(project).reformat(targetObject)

                // Commit document to ensure changes are synced
                PsiDocumentManager.getInstance(project).commitAllDocuments()
                success = true
            } catch (e: Exception) {
                // Fallback to safe code-level insertion if PSI fails
                try {
                    val virtualFile = getOrCreateAppIconsFile(project, appIconsFilePath, vectorPackageName)
                    if (virtualFile != null) {
                        val currentText = VfsUtil.loadText(virtualFile)
                        val (updatedText, updated) = appendIconToCode(
                            currentText = currentText,
                            composeIconName = composeIconName,
                            vectorPropertyName = vectorPropertyName,
                            vectorPackageName = vectorPackageName
                        )
                        if (updated) {
                            VfsUtil.saveText(virtualFile, updatedText)
                            success = true
                        }
                    }
                } catch (fallbackEx: Exception) {
                    fallbackEx.printStackTrace()
                }
            }
        })

        return success
    }

    /**
     * Safely appends an icon declaration to Kotlin source code without duplicates.
     */
    fun appendIconToCode(
        currentText: String,
        composeIconName: String,
        vectorPropertyName: String,
        vectorPackageName: String
    ): Pair<String, Boolean> {
        val propertyRegex = Regex("""val\s+$composeIconName\s*:\s*ImageVector""")
        if (propertyRegex.containsMatchIn(currentText)) {
            return Pair(currentText, false) // Already present
        }

        var code = currentText
        val requiredImport = "import androidx.compose.ui.graphics.vector.ImageVector"
        if (!code.contains(requiredImport)) {
            val pkgMatch = Regex("""package\s+[^\n]+""").find(code)
            code = if (pkgMatch != null) {
                code.replaceRange(pkgMatch.range.last + 1, pkgMatch.range.last + 1, "\n\n$requiredImport")
            } else {
                "$requiredImport\n\n$code"
            }
        }

        if (vectorPackageName.isNotBlank()) {
            val vectorImport = "import $vectorPackageName.$vectorPropertyName"
            if (!code.contains(vectorImport) && !code.contains("import $vectorPackageName.*")) {
                val lastImport = Regex("""import\s+[^\n]+""").findAll(code).lastOrNull()
                code = if (lastImport != null) {
                    code.replaceRange(lastImport.range.last + 1, lastImport.range.last + 1, "\n$vectorImport")
                } else {
                    val pkgMatch = Regex("""package\s+[^\n]+""").find(code)
                    if (pkgMatch != null) {
                        code.replaceRange(pkgMatch.range.last + 1, pkgMatch.range.last + 1, "\n\n$vectorImport")
                    } else {
                        "$vectorImport\n$code"
                    }
                }
            }
        }

        val objectRegex = Regex("""object\s+AppIcons[^{]*\{""", RegexOption.MULTILINE)
        val objectMatch = objectRegex.find(code)
            ?: Regex("""object\s+\w+[^{]*\{""", RegexOption.MULTILINE).find(code)

        if (objectMatch != null) {
            val objStartIndex = objectMatch.range.first
            val closingBraceIndex = findMatchingClosingBrace(code, objectMatch.range.last)
            if (closingBraceIndex != -1) {
                val newPropCode = "    val $composeIconName: ImageVector get() = $vectorPropertyName\n"
                code = code.substring(0, closingBraceIndex) + newPropCode + code.substring(closingBraceIndex)
                return Pair(code, true)
            }
        }

        // If no object AppIcons exists, append it
        val newObjectCode = """

object AppIcons {
    val $composeIconName: ImageVector get() = $vectorPropertyName
}
        """.trimIndent()
        code = "$code\n\n$newObjectCode"
        return Pair(code, true)
    }

    private fun findMatchingClosingBrace(text: String, openBraceIndex: Int): Int {
        var depth = 1
        for (i in (openBraceIndex + 1) until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun getOrCreateAppIconsFile(
        project: Project,
        filePath: String,
        vectorPackageName: String
    ): VirtualFile? {
        val file = File(filePath)
        val lfs = LocalFileSystem.getInstance()

        if (file.exists()) {
            return lfs.refreshAndFindFileByIoFile(file)
        }

        // Create parent directories if needed
        val parentDir = file.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }

        val parentVirtualDir = lfs.refreshAndFindFileByIoFile(parentDir ?: return null)
            ?: return null

        val appIconsPackage = ProjectStructureHelper.detectPackageName(parentVirtualDir.path, project)
            .ifBlank { vectorPackageName.substringBeforeLast('.', "com.example.app.ui.theme") }

        val starterContent = """
package $appIconsPackage

import androidx.compose.ui.graphics.vector.ImageVector

object AppIcons {
}
        """.trimIndent()

        val createdFile = parentVirtualDir.createChildData(this, file.name)
        VfsUtil.saveText(createdFile, starterContent)
        return createdFile
    }

    private fun ensureImports(
        psiFile: KtFile,
        factory: KtPsiFactory,
        vectorPackageName: String,
        vectorPropertyName: String
    ) {
        val currentPackage = psiFile.packageFqName.asString()
        val imports = psiFile.importDirectives.mapNotNull { it.importPath?.pathStr }

        // Import ImageVector if not present
        if (!imports.contains("androidx.compose.ui.graphics.vector.ImageVector") &&
            !imports.contains("androidx.compose.ui.graphics.vector.*")
        ) {
            val importDirective = factory.createImportDirective(
                ImportPath(FqName("androidx.compose.ui.graphics.vector.ImageVector"), false)
            )
            psiFile.importList?.add(importDirective)
        }

        // Import vector property if in a different package
        if (vectorPackageName.isNotBlank() && vectorPackageName != currentPackage) {
            val fullVectorFq = "$vectorPackageName.$vectorPropertyName"
            val wildcardFq = "$vectorPackageName.*"
            if (!imports.contains(fullVectorFq) && !imports.contains(wildcardFq)) {
                val importDirective = factory.createImportDirective(
                    ImportPath(FqName(fullVectorFq), false)
                )
                psiFile.importList?.add(importDirective)
            }
        }
    }
}
