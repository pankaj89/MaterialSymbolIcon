package com.master.materialsymbol.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

object ProjectStructureHelper {

    data class DetectedPaths(
        val drawablePath: String,
        val composePath: String,
        val appIconsPath: String,
        val composePackageName: String
    )

    fun autoDetectPaths(project: Project): DetectedPaths {
        val baseDir = project.guessProjectDir()
        val basePath = project.basePath ?: baseDir?.path ?: ""

        val drawableDir = findDirectory(baseDir, listOf("res", "drawable"))
            ?: findDirectory(baseDir, listOf("src", "main", "res", "drawable"))
            ?: "$basePath/app/src/main/res/drawable"

        val composeDir = findComposeDirectory(baseDir)
            ?: "$basePath/app/src/main/java/com/example/app/ui/theme/icons"

        val appIconsFile = findFileByName(baseDir, "AppIcons.kt")
            ?: "${composeDir.substringBeforeLast('/')}/AppIcons.kt"

        val detectedPackage = detectPackageName(composeDir, project)

        return DetectedPaths(
            drawablePath = drawableDir,
            composePath = composeDir,
            appIconsPath = appIconsFile,
            composePackageName = detectedPackage
        )
    }

    private fun findDirectory(root: VirtualFile?, pathSegments: List<String>): String? {
        if (root == null) return null
        var found: VirtualFile? = null
        
        VfsUtil.processFilesRecursively(root) { file ->
            if (file.isDirectory && matchesPathEnd(file, pathSegments)) {
                found = file
                return@processFilesRecursively false
            }
            true
        }
        return found?.path
    }

    private fun matchesPathEnd(file: VirtualFile, segments: List<String>): Boolean {
        var current: VirtualFile? = file
        for (i in segments.indices.reversed()) {
            if (current == null || current.name != segments[i]) return false
            current = current.parent
        }
        return true
    }

    private fun findComposeDirectory(root: VirtualFile?): String? {
        if (root == null) return null
        var candidate: VirtualFile? = null

        VfsUtil.processFilesRecursively(root) { file ->
            if (file.isDirectory) {
                val path = file.path
                if ((path.contains("ui/theme/icons") || path.contains("ui/theme") || path.contains("theme/icons"))
                    && (path.contains("src/main/java") || path.contains("src/main/kotlin"))
                ) {
                    candidate = if (path.endsWith("icons")) file else file.findChild("icons") ?: file
                    return@processFilesRecursively false
                }
            }
            true
        }
        return candidate?.path
    }

    private fun findFileByName(root: VirtualFile?, fileName: String): String? {
        if (root == null) return null
        var found: VirtualFile? = null

        VfsUtil.processFilesRecursively(root) { file ->
            if (!file.isDirectory && file.name.equals(fileName, ignoreCase = true)) {
                found = file
                return@processFilesRecursively false
            }
            true
        }
        return found?.path
    }

    fun detectPackageName(directoryPath: String, project: Project): String {
        val path = directoryPath.replace('\\', '/')
        val srcMarkerJava = "/src/main/java/"
        val srcMarkerKotlin = "/src/main/kotlin/"
        
        val relative = when {
            path.contains(srcMarkerJava) -> path.substringAfter(srcMarkerJava)
            path.contains(srcMarkerKotlin) -> path.substringAfter(srcMarkerKotlin)
            else -> {
                // Check if directory already has files with package statement
                val dir = File(directoryPath)
                if (dir.exists() && dir.isDirectory) {
                    val ktFiles = dir.listFiles { f -> f.extension == "kt" }
                    val firstPackage = ktFiles?.firstNotNullOfOrNull { file ->
                        file.useLines { lines ->
                            lines.firstOrNull { it.trim().startsWith("package ") }
                                ?.removePrefix("package ")?.trim()
                        }
                    }
                    if (!firstPackage.isNullOrBlank()) return firstPackage
                }
                "com.example.app.ui.theme.icons"
            }
        }

        return relative.replace('/', '.').trim('.')
    }
}
