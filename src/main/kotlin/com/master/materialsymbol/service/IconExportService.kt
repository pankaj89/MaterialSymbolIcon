package com.master.materialsymbol.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.master.materialsymbol.model.IconImportRequest
import com.master.materialsymbol.model.IconImportResult
import com.master.materialsymbol.settings.IconImporterSettings
import com.master.materialsymbol.util.NamingHelper
import com.master.materialsymbol.util.ProjectStructureHelper
import java.io.File

object IconExportService {

    fun exportIcon(project: Project, request: IconImportRequest): IconImportResult {
        val settings = IconImporterSettings.getInstance(project).state

        // Determine destination paths
        val defaults = ProjectStructureHelper.autoDetectPaths(project)
        val drawableDir = settings.drawableDestinationPath.ifBlank { defaults.drawablePath }
        val composeDir = settings.composeDestinationPath.ifBlank { defaults.composePath }
        val appIconsPath = settings.appIconsFilePath.ifBlank { defaults.appIconsPath }
        val composePackage = settings.composePackageName.ifBlank {
            ProjectStructureHelper.detectPackageName(composeDir, project)
        }

        val xmlName = request.customXmlName?.ifBlank { null }
            ?: NamingHelper.toXmlDrawableName(request.rawIconName, settings.iconPrefixXml)

        val composeName = request.customComposeName?.ifBlank { null }
            ?: NamingHelper.toComposeName(request.rawIconName, settings.iconSuffixCompose)

        var exportedXmlPath: String? = null
        var exportedComposePath: String? = null
        var appendedToRegistry = false
        var warningOrError: String? = null

        try {
            val isKotlin = ComposeGeneratorService.isKotlinComposeSource(request.svgContent)
            var vectorXml = ""
            val composeCode: String
            val vectorPropName: String

            if (isKotlin) {
                // Preserve the downloaded Kotlin file as it is, adjusting package to composePackage
                composeCode = ComposeGeneratorService.prepareKotlinSource(request.svgContent, composePackage)
                vectorPropName = ComposeGeneratorService.extractVectorPropertyName(composeCode) ?: composeName

                // If XML Drawable was also requested, attempt to fetch fallback SVG for XML generation
                if (request.destinationType.isXml) {
                    val svgFallback = MaterialSymbolDownloaderService.downloadSvgForIconName(request.rawIconName)
                    if (svgFallback != null) {
                        val (xml, warning) = SvgConversionService.convertSvgToVectorXml(svgFallback)
                        vectorXml = xml
                        if (warning != null) warningOrError = warning
                    } else {
                        warningOrError = "XML Drawable could not be created from Kotlin source URL"
                    }
                }
            } else {
                // 1. Convert SVG to Vector Drawable XML
                val (xml, warning) = SvgConversionService.convertSvgToVectorXml(request.svgContent)
                vectorXml = xml
                if (warning != null) {
                    warningOrError = warning
                }

                composeCode = ComposeGeneratorService.generateComposeSource(
                    vectorXml = vectorXml,
                    composeName = composeName,
                    packageName = composePackage
                )
                vectorPropName = composeName
            }

            WriteCommandAction.runWriteCommandAction(project, "Export Icon Files", null, Runnable {
                val lfs = LocalFileSystem.getInstance()

                // Save XML Drawable if requested
                if (request.destinationType.isXml && vectorXml.isNotBlank()) {
                    val dirFile = File(drawableDir)
                    if (!dirFile.exists()) dirFile.mkdirs()

                    val targetFile = File(dirFile, "$xmlName.xml")
                    targetFile.writeText(vectorXml, Charsets.UTF_8)
                    exportedXmlPath = targetFile.absolutePath

                    val vFile = lfs.refreshAndFindFileByIoFile(targetFile)
                    vFile?.refresh(false, false)
                }

                // Save Compose ImageVector if requested
                if (request.destinationType.isCompose) {
                    val dirFile = File(composeDir)
                    if (!dirFile.exists()) dirFile.mkdirs()

                    val targetFile = File(dirFile, "$composeName.kt")
                    targetFile.writeText(composeCode, Charsets.UTF_8)
                    exportedComposePath = targetFile.absolutePath

                    val vFile = lfs.refreshAndFindFileByIoFile(targetFile)
                    vFile?.refresh(false, false)
                }
            })

            // Auto-append to AppIcons.kt if enabled and Compose export was performed
            if (request.destinationType.isCompose && settings.autoAppendToAppIcons) {
                appendedToRegistry = AppIconsUpdaterService.appendIconToRegistry(
                    project = project,
                    appIconsFilePath = appIconsPath,
                    composeIconName = composeName,
                    vectorPropertyName = vectorPropName,
                    vectorPackageName = composePackage
                )
            }

            // Refresh VFS
            LocalFileSystem.getInstance().findFileByPath(drawableDir)?.refresh(true, true)
            LocalFileSystem.getInstance().findFileByPath(composeDir)?.refresh(true, true)

            showNotification(
                project = project,
                title = "Icon Imported Successfully",
                message = buildSuccessMessage(xmlName, composeName, request, appendedToRegistry),
                type = NotificationType.INFORMATION
            )

            return IconImportResult(
                success = true,
                xmlFilePath = exportedXmlPath,
                composeFilePath = exportedComposePath,
                appendedToAppIcons = appendedToRegistry,
                appIconsProperty = composeName,
                errorMessage = warningOrError
            )
        } catch (e: Exception) {
            val error = e.message ?: "Unknown export error"
            showNotification(
                project = project,
                title = "Icon Import Failed",
                message = error,
                type = NotificationType.ERROR
            )
            return IconImportResult(
                success = false,
                errorMessage = error
            )
        }
    }

    private fun buildSuccessMessage(
        xmlName: String,
        composeName: String,
        request: IconImportRequest,
        appended: Boolean
    ): String {
        val parts = mutableListOf<String>()
        if (request.destinationType.isXml) parts.add("XML: $xmlName.xml")
        if (request.destinationType.isCompose) parts.add("Compose: $composeName.kt")
        if (appended) parts.add("Registered in AppIcons.$composeName")
        return parts.joinToString("\n")
    }

    fun showNotification(project: Project, title: String, message: String, type: NotificationType) {
        try {
            val group = NotificationGroupManager.getInstance().getNotificationGroup("MaterialSymbolIcon")
                ?: NotificationGroupManager.getInstance().getNotificationGroup("General")
            group?.createNotification(title, message, type)?.notify(project)
        } catch (_: Exception) {
            // Notification group might not be registered, fallback silently
        }
    }
}
