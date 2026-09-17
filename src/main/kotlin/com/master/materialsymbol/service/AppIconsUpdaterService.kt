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
import org.jetbrains.kotlin.name.Name
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

                val alias = if (composeIconName.endsWith("Icon")) "${composeIconName}Vector" else "${composeIconName}Icon"

                // 2. Check if property already exists
                val existingProp = targetObject.declarations.filterIsInstance<KtProperty>()
                    .find { it.name == composeIconName }
                if (existingProp != null) {
                    val getterExpr = existingProp.getter?.bodyExpression?.text?.trim()
                    if (getterExpr == composeIconName) {
                        // Fix recursive getter
                        val fixedProperty = factory.createProperty("val $composeIconName: ImageVector get() = $alias")
                        existingProp.replace(fixedProperty)
                        ensureImports(psiFile, factory, vectorPackageName, vectorPropertyName, alias)
                        CodeStyleManager.getInstance(project).reformat(targetObject)
                        PsiDocumentManager.getInstance(project).commitAllDocuments()
                    }
                    success = true // Already present
                    return@Runnable
                }

                // 3. Ensure imports exist
                ensureImports(psiFile, factory, vectorPackageName, vectorPropertyName, alias)

                // 4. Create and append the new property
                val propertyCode = "val $composeIconName: ImageVector get() = $alias"
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
        val alias = if (composeIconName.endsWith("Icon")) "${composeIconName}Vector" else "${composeIconName}Icon"
        val vectorFq = if (vectorPackageName.isNotBlank()) "$vectorPackageName.$vectorPropertyName" else vectorPropertyName
        val vectorImport = "import $vectorFq as $alias"
        val propertyRegex = Regex("""val\s+$composeIconName\s*:\s*ImageVector""")

        var code = currentText

        // Check if property is already present
        if (propertyRegex.containsMatchIn(code)) {
            // Check if it's the recursive bug: val Foo: ImageVector get() = Foo
            val recursiveRegex = Regex("""val\s+$composeIconName\s*:\s*ImageVector\s+get\(\)\s*=\s*$composeIconName\b""")
            if (recursiveRegex.containsMatchIn(code)) {
                code = code.replace(recursiveRegex, "val $composeIconName: ImageVector get() = $alias")
                code = ensureAliasedImportInCode(code, vectorFq, vectorImport)
                return Pair(code, true)
            }
            return Pair(currentText, false) // Already present and not recursive
        }

        val requiredImport = "import androidx.compose.ui.graphics.vector.ImageVector"
        if (!code.contains(requiredImport)) {
            val pkgMatch = Regex("""package\s+[^\n]+""").find(code)
            code = if (pkgMatch != null) {
                code.replaceRange(pkgMatch.range.last + 1, pkgMatch.range.last + 1, "\n\n$requiredImport")
            } else {
                "$requiredImport\n\n$code"
            }
        }

        code = ensureAliasedImportInCode(code, vectorFq, vectorImport)

        val newPropCode = "    val $composeIconName: ImageVector get() = $alias\n"
        val objectRegex = Regex("""object\s+AppIcons[^{]*\{""", RegexOption.MULTILINE)
        val objectMatch = objectRegex.find(code)
            ?: Regex("""object\s+\w+[^{]*\{""", RegexOption.MULTILINE).find(code)

        if (objectMatch != null) {
            val closingBraceIndex = findMatchingClosingBrace(code, objectMatch.range.last)
            if (closingBraceIndex != -1) {
                code = code.substring(0, closingBraceIndex) + newPropCode + code.substring(closingBraceIndex)
                return Pair(code, true)
            }
        }

        // If no object AppIcons exists, append it
        val newObjectCode = """

object AppIcons {
    val $composeIconName: ImageVector get() = $alias
}
        """.trimIndent()
        code = "$code\n\n$newObjectCode"
        return Pair(code, true)
    }

    private fun ensureAliasedImportInCode(code: String, vectorFq: String, vectorImport: String): String {
        if (code.contains(vectorImport)) return code

        val unaliasedRegex = Regex("""import\s+${Regex.escape(vectorFq)}\s*(?:\r?\n|$)""")
        if (unaliasedRegex.containsMatchIn(code)) {
            return code.replace(unaliasedRegex, "$vectorImport\n")
        }

        val lastImport = Regex("""import\s+[^\n]+""").findAll(code).lastOrNull()
        return if (lastImport != null) {
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
        vectorPropertyName: String,
        alias: String
    ) {
        val imports = psiFile.importDirectives

        // Import ImageVector if not present
        val hasImageVector = imports.any {
            val path = it.importPath?.pathStr
            path == "androidx.compose.ui.graphics.vector.ImageVector" || path == "androidx.compose.ui.graphics.vector.*"
        }
        if (!hasImageVector) {
            val importDirective = factory.createImportDirective(
                ImportPath(FqName("androidx.compose.ui.graphics.vector.ImageVector"), false)
            )
            val importList = psiFile.importList
            if (importList != null) {
                importList.add(importDirective)
            } else {
                psiFile.add(importDirective)
            }
        }

        // Import vector property with alias
        val fullVectorFq = if (vectorPackageName.isNotBlank()) "$vectorPackageName.$vectorPropertyName" else vectorPropertyName
        val alreadyImported = imports.any {
            it.importedFqName?.asString() == fullVectorFq && it.aliasName == alias
        }

        if (!alreadyImported) {
            val unaliasedExisting = imports.find {
                it.importedFqName?.asString() == fullVectorFq && it.aliasName == null
            }

            val importDirective = factory.createImportDirective(
                ImportPath(FqName(fullVectorFq), false, Name.identifier(alias))
            )

            if (unaliasedExisting != null) {
                unaliasedExisting.replace(importDirective)
            } else {
                val importList = psiFile.importList
                if (importList != null) {
                    importList.add(importDirective)
                } else {
                    psiFile.add(importDirective)
                }
            }
        }
    }
}
