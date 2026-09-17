package com.master.materialsymbol.ui.panels

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Desktop
import java.awt.FlowLayout
import java.awt.Font
import java.net.URI
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton

class AboutUsPanel : JBPanel<AboutUsPanel>(BorderLayout()) {

    private val portfolioUrl = "https://pankaj89.vercel.app/"

    init {
        border = JBUI.Borders.empty()
        setupContent()
    }

    private fun setupContent() {
        val rootContainer = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(20, 24)
        }

        // Header Section
        val headerPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(16)

            val titleRow = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                alignmentX = Component.LEFT_ALIGNMENT
                add(JBLabel(AllIcons.Toolwindows.ToolWindowPalette))
                add(JBLabel("Material Symbol & Icon Importer").apply {
                    font = font.deriveFont(Font.BOLD, 18f)
                })
                add(JBLabel("v1.0.2").apply {
                    font = font.deriveFont(Font.PLAIN, 12f)
                    foreground = JBColor.GRAY
                    border = JBUI.Borders.compound(
                        BorderFactory.createLineBorder(JBColor.border(), 1, true),
                        JBUI.Borders.empty(2, 6)
                    )
                })
            }
            add(titleRow)
            add(Box.createVerticalStrut(JBUI.scale(6)))

            val subtitleLabel = JBLabel("Unified vector icon automation for Jetpack Compose ImageVector and Android XML Drawables.").apply {
                font = font.deriveFont(Font.PLAIN, 13f)
                foreground = JBColor.GRAY
                alignmentX = Component.LEFT_ALIGNMENT
            }
            add(subtitleLabel)
        }
        rootContainer.add(headerPanel)

        // Developer Profile Card
        val developerCard = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.compound(
                BorderFactory.createLineBorder(JBColor.border(), 1, true),
                JBUI.Borders.empty(16)
            )

            val devContent = JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                val tagLabel = JBLabel("CREATOR & DEVELOPER").apply {
                    font = font.deriveFont(Font.BOLD, 11f)
                    foreground = JBColor.namedColor("Label.infoForeground", JBColor(0x5E6A75, 0x8C9BA5))
                    alignmentX = Component.LEFT_ALIGNMENT
                }
                add(tagLabel)
                add(Box.createVerticalStrut(JBUI.scale(4)))

                val nameLabel = JBLabel("Pankaj Sharma").apply {
                    font = font.deriveFont(Font.BOLD, 17f)
                    alignmentX = Component.LEFT_ALIGNMENT
                }
                add(nameLabel)
                add(Box.createVerticalStrut(JBUI.scale(4)))

                val descLabel = JBLabel("Android & Kotlin Tooling Specialist | Jetpack Compose Architecture").apply {
                    font = font.deriveFont(Font.PLAIN, 13f)
                    foreground = JBColor.GRAY
                    alignmentX = Component.LEFT_ALIGNMENT
                }
                add(descLabel)
                add(Box.createVerticalStrut(JBUI.scale(12)))

                // Link & Button Row
                val linkRow = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 12, 0)).apply {
                    alignmentX = Component.LEFT_ALIGNMENT

                    val openPortfolioBtn = JButton("Visit Portfolio", AllIcons.General.Web).apply {
                        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        addActionListener { openUrl(portfolioUrl) }
                    }
                    add(openPortfolioBtn)

                    val actionLink = ActionLink(portfolioUrl) {
                        openUrl(portfolioUrl)
                    }.apply {
                        font = font.deriveFont(Font.PLAIN, 13f)
                        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    }
                    add(actionLink)
                }
                add(linkRow)
            }
            add(devContent, BorderLayout.CENTER)
        }
        rootContainer.add(developerCard)
        rootContainer.add(Box.createVerticalStrut(JBUI.scale(16)))

        // Capabilities Card
        val capabilitiesCard = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.compound(
                BorderFactory.createLineBorder(JBColor.border(), 1, true),
                JBUI.Borders.empty(16)
            )

            val capContent = JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                val capTitle = JBLabel("KEY CAPABILITIES").apply {
                    font = font.deriveFont(Font.BOLD, 11f)
                    foreground = JBColor.namedColor("Label.infoForeground", JBColor(0x5E6A75, 0x8C9BA5))
                    alignmentX = Component.LEFT_ALIGNMENT
                }
                add(capTitle)
                add(Box.createVerticalStrut(JBUI.scale(10)))

                val features = listOf(
                    Pair(AllIcons.Actions.Download, "Google Material Symbols: Direct URL parsing, fast download, and instant vector conversion."),
                    Pair(AllIcons.Actions.Edit, "Apple SF Symbols: Automatic sanitization, namespace stripping, and viewBox normalization from clipboard."),
                    Pair(AllIcons.FileTypes.Image, "Custom SVG Import: Drag-and-drop or file browser support with live interactive preview."),
                    Pair(AllIcons.Actions.Compile, "Jetpack Compose ImageVector: Direct generation of idiomatic Kotlin vector definitions."),
                    Pair(AllIcons.FileTypes.Xml, "Android Vector Drawables: Clean XML generation ready for resource compilation."),
                    Pair(AllIcons.General.InspectionsOK, "Centralized AppIcons Registry: Automatic PSI update to your project's AppIcons.kt.")
                )

                for (feature in features) {
                    val row = JBPanel<JBPanel<*>>(BorderLayout(10, 0)).apply {
                        alignmentX = Component.LEFT_ALIGNMENT
                        border = JBUI.Borders.empty(3, 0)
                        add(JBLabel(feature.first), BorderLayout.WEST)
                        add(JBLabel(feature.second).apply {
                            font = font.deriveFont(Font.PLAIN, 13f)
                        }, BorderLayout.CENTER)
                    }
                    add(row)
                }
            }
            add(capContent, BorderLayout.CENTER)
        }
        rootContainer.add(capabilitiesCard)
        rootContainer.add(Box.createVerticalStrut(JBUI.scale(20)))

        // Footer note
        val footerLabel = JBLabel("Crafted with care by Pankaj Sharma • All rights reserved").apply {
            font = font.deriveFont(Font.PLAIN, 12f)
            foreground = JBColor.GRAY
            alignmentX = Component.LEFT_ALIGNMENT
        }
        rootContainer.add(footerLabel)

        val scrollPane = JBScrollPane(rootContainer).apply {
            border = JBUI.Borders.empty()
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                return
            }
        } catch (_: Throwable) {
            // Fall back to OS-specific command
        }

        try {
            val os = System.getProperty("os.name", "").lowercase()
            when {
                os.contains("mac") -> ProcessBuilder("open", url).start()
                os.contains("win") -> ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start()
                else -> ProcessBuilder("xdg-open", url).start()
            }
        } catch (_: Throwable) {
            // Ignore if external browser cannot be launched
        }
    }
}
