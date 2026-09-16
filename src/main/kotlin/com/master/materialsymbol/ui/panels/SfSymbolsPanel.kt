package com.master.materialsymbol.ui.panels

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.master.materialsymbol.model.IconDestinationType
import com.master.materialsymbol.model.IconImportRequest
import com.master.materialsymbol.service.IconExportService
import com.master.materialsymbol.service.SvgConversionService
import com.master.materialsymbol.settings.IconImporterSettings
import com.master.materialsymbol.ui.components.SvgPreviewComponent
import com.master.materialsymbol.util.NamingHelper
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.event.DocumentEvent

class SfSymbolsPanel(private val project: Project) : JBPanel<SfSymbolsPanel>(BorderLayout()) {

    private val rawSvgArea = JBTextArea(8, 50).apply {
        lineWrap = true
        wrapStyleWord = true
        emptyText.text = "Paste SVG code copied from Apple SF Symbols app here..."
    }

    private val iconNameField = JBTextField().apply {
        emptyText.text = "e.g. heart.fill, gear, bell"
    }

    private val xmlPreviewLabel = JBLabel().apply { foreground = JBColor.GRAY }
    private val composePreviewLabel = JBLabel().apply { foreground = JBColor.GRAY }
    private val sanitizeStatusLabel = JBLabel().apply { foreground = JBColor.GRAY }

    private val btnGenerateCompose = JButton("Generate ImageVector", AllIcons.Actions.Compile)
    private val btnGenerateDrawable = JButton("Generate Drawable", AllIcons.FileTypes.Xml)
    private val btnGenerateBoth = JButton("Generate Both", AllIcons.Actions.Execute)

    private val statusLabel = JBLabel()
    private val previewComponent = SvgPreviewComponent()

    init {
        border = JBUI.Borders.empty(16)
        setupLayout()
        setupListeners()
    }

    private fun setupLayout() {
        val topPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(4, 0, 4, 8)
            anchor = GridBagConstraints.WEST
        }

        // Row 0: Description banner
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2
        val bannerLabel = JBLabel("Copy an icon as SVG in Apple's SF Symbols app (Cmd+C), then paste it below:", AllIcons.General.Information, JBLabel.LEFT)
        topPanel.add(bannerLabel, gbc)
        gbc.gridwidth = 1

        // Row 1: SVG Input area
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.4
        gbc.fill = GridBagConstraints.BOTH
        val scrollPane = JBScrollPane(rawSvgArea).apply {
            preferredSize = Dimension(JBUI.scale(400), JBUI.scale(140))
        }
        topPanel.add(scrollPane, gbc)
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.gridwidth = 1
        gbc.weighty = 0.0

        // Row 2: Sanitization info
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2
        topPanel.add(sanitizeStatusLabel, gbc)
        gbc.gridwidth = 1

        // Row 3: Icon Name
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0
        topPanel.add(JBLabel("Icon Name:"), gbc)
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0
        topPanel.add(iconNameField, gbc)

        // Row 4: Name previews
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0
        val previewNamesPanel = JBPanel<JBPanel<*>>().apply {
            add(composePreviewLabel)
            add(JBLabel(" | "))
            add(xmlPreviewLabel)
        }
        topPanel.add(previewNamesPanel, gbc)

        // Row 5: Action buttons
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1.0
        val actionsPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            add(btnGenerateCompose)
            add(btnGenerateDrawable)
            add(btnGenerateBoth)
        }
        topPanel.add(actionsPanel, gbc)

        // Row 6: Status
        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 1.0
        topPanel.add(statusLabel, gbc)

        add(topPanel, BorderLayout.NORTH)

        // Center: Preview panel
        val centerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.emptyTop(12),
                JBUI.Borders.customLine(JBColor.border(), 1)
            )
            add(JBLabel("Sanitized Preview", JBLabel.CENTER).apply {
                border = JBUI.Borders.empty(4)
            }, BorderLayout.NORTH)
            add(previewComponent, BorderLayout.CENTER)
        }
        add(centerPanel, BorderLayout.CENTER)
    }

    private fun setupListeners() {
        rawSvgArea.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                onSvgChanged()
            }
        })

        iconNameField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updateNamePreviews()
            }
        })

        btnGenerateCompose.addActionListener { performExport(IconDestinationType.COMPOSE) }
        btnGenerateDrawable.addActionListener { performExport(IconDestinationType.XML) }
        btnGenerateBoth.addActionListener { performExport(IconDestinationType.BOTH) }
    }

    private fun onSvgChanged() {
        val raw = rawSvgArea.text.trim()
        if (raw.isBlank()) {
            sanitizeStatusLabel.text = ""
            previewComponent.updateSvg("")
            return
        }

        if (raw.contains("<svg")) {
            val sanitized = SvgConversionService.sanitizeSvg(raw)
            previewComponent.updateSvg(sanitized)
            val hasAppleNs = raw.contains("xmlns:sfs") || raw.contains("sfs:")
            val hasGuides = raw.contains("Guides") || raw.contains("Notes")

            val notices = mutableListOf<String>()
            if (hasAppleNs) notices.add("Stripped Apple SF namespaces")
            if (hasGuides) notices.add("Removed SF guides/notes")
            notices.add("Ready to convert")

            sanitizeStatusLabel.text = "SF Symbols: " + notices.joinToString(" • ")
            sanitizeStatusLabel.foreground = JBColor.GREEN
        } else {
            sanitizeStatusLabel.text = "Input does not appear to be a valid <svg> snippet"
            sanitizeStatusLabel.foreground = JBColor.RED
        }
    }

    private fun updateNamePreviews() {
        val raw = iconNameField.text.trim()
        val settings = IconImporterSettings.getInstance(project).state
        val xmlName = NamingHelper.toXmlDrawableName(raw, settings.iconPrefixXml)
        val composeName = NamingHelper.toComposeName(raw, settings.iconSuffixCompose)

        xmlPreviewLabel.text = "XML: $xmlName.xml"
        composePreviewLabel.text = "Compose: $composeName.kt"
    }

    private fun performExport(type: IconDestinationType) {
        val rawSvg = rawSvgArea.text.trim()
        val rawName = iconNameField.text.trim().ifBlank { "sf_symbol" }

        if (rawSvg.isBlank()) {
            statusLabel.text = "Please paste SVG code first"
            statusLabel.foreground = JBColor.RED
            return
        }

        val sanitized = SvgConversionService.sanitizeSvg(rawSvg)
        val request = IconImportRequest(
            rawIconName = rawName,
            svgContent = sanitized,
            destinationType = type
        )

        val result = IconExportService.exportIcon(project, request)
        if (result.success) {
            statusLabel.text = "Successfully generated ${type.displayName}!"
            statusLabel.foreground = JBColor.GREEN
        } else {
            statusLabel.text = "Export failed: ${result.errorMessage}"
            statusLabel.foreground = JBColor.RED
        }
    }
}
