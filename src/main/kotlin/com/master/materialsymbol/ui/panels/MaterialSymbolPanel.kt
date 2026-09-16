package com.master.materialsymbol.ui.panels

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.master.materialsymbol.model.IconDestinationType
import com.master.materialsymbol.model.IconImportRequest
import com.master.materialsymbol.service.IconExportService
import com.master.materialsymbol.service.MaterialSymbolDownloaderService
import com.master.materialsymbol.settings.IconImporterSettings
import com.master.materialsymbol.ui.components.SvgPreviewComponent
import com.master.materialsymbol.util.NamingHelper
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JProgressBar
import javax.swing.event.DocumentEvent

class MaterialSymbolPanel(private val project: Project) : JBPanel<MaterialSymbolPanel>(BorderLayout()) {

    private val urlField = JBTextField().apply {
        emptyText.text = "Paste Google Material Symbols URL (e.g. https://fonts.gstatic.com/.../home.kt?var=...)"
    }
    private val validationLabel = JBLabel()
    private val iconNameField = JBTextField()
    private val xmlNamePreviewLabel = JBLabel().apply { foreground = JBColor.GRAY }
    private val composeNamePreviewLabel = JBLabel().apply { foreground = JBColor.GRAY }

    private val btnGenerateCompose = JButton("Generate ImageVector", AllIcons.Actions.Compile)
    private val btnGenerateDrawable = JButton("Generate Drawable", AllIcons.FileTypes.Xml)
    private val btnGenerateBoth = JButton("Generate Both", AllIcons.Actions.Execute)

    private val progressBar = JProgressBar().apply {
        isIndeterminate = true
        isVisible = false
    }
    private val statusLabel = JBLabel()
    private val previewComponent = SvgPreviewComponent()

    private var downloadedContent: String? = null
    private var lastDownloadedUrl: String = ""

    init {
        border = JBUI.Borders.empty(16)
        setupLayout()
        setupListeners()
    }

    private fun setupLayout() {
        val formPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(4, 0, 4, 8)
            anchor = GridBagConstraints.WEST
        }

        // Row 0: URL input
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        formPanel.add(JBLabel("Symbol URL:"), gbc)
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0
        formPanel.add(urlField, gbc)

        // Row 1: URL validation status
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0
        formPanel.add(validationLabel, gbc)

        // Row 2: Icon Name
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0
        formPanel.add(JBLabel("Icon Name:"), gbc)
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0
        formPanel.add(iconNameField, gbc)

        // Row 3: Name previews
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0
        val previewNamesPanel = JBPanel<JBPanel<*>>().apply {
            add(composeNamePreviewLabel)
            add(JBLabel(" | "))
            add(xmlNamePreviewLabel)
        }
        formPanel.add(previewNamesPanel, gbc)

        // Row 4: Action buttons
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0
        val actionsPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            add(btnGenerateCompose)
            add(btnGenerateDrawable)
            add(btnGenerateBoth)
        }
        formPanel.add(actionsPanel, gbc)

        // Row 5: Status & Progress
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1.0
        val statusPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(progressBar)
            add(statusLabel)
        }
        formPanel.add(statusPanel, gbc)

        // Main content assembly
        add(formPanel, BorderLayout.NORTH)

        // Center: Preview panel with border
        val centerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.emptyTop(12),
                JBUI.Borders.customLine(JBColor.border(), 1)
            )
            add(JBLabel("Icon Preview", JBLabel.CENTER).apply {
                border = JBUI.Borders.empty(4)
            }, BorderLayout.NORTH)
            add(previewComponent, BorderLayout.CENTER)
        }
        add(centerPanel, BorderLayout.CENTER)
    }

    private fun setupListeners() {
        urlField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                onUrlChanged()
            }
        })

        iconNameField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updateNamePreviews()
            }
        })

        btnGenerateCompose.addActionListener { performDownloadAndSave(IconDestinationType.COMPOSE) }
        btnGenerateDrawable.addActionListener { performDownloadAndSave(IconDestinationType.XML) }
        btnGenerateBoth.addActionListener { performDownloadAndSave(IconDestinationType.BOTH) }
    }

    private fun onUrlChanged() {
        val url = urlField.text.trim()
        if (url != lastDownloadedUrl) {
            downloadedContent = null
        }
        val result = MaterialSymbolDownloaderService.validateUrl(url)
        if (result.isValid) {
            validationLabel.text = "Valid URL (Detected: ${result.detectedIconName})"
            validationLabel.icon = AllIcons.General.InspectionsOK
            validationLabel.foreground = JBColor.GREEN

            if (iconNameField.text.isBlank() || iconNameField.text == lastAutoName) {
                lastAutoName = result.detectedIconName
                iconNameField.text = result.detectedIconName
            }
        } else {
            validationLabel.text = result.errorMessage ?: "Invalid URL"
            validationLabel.icon = AllIcons.General.Warning
            validationLabel.foreground = JBColor.RED
        }
        updateNamePreviews()
    }

    private var lastAutoName: String = ""

    private fun updateNamePreviews() {
        val raw = iconNameField.text.trim()
        val settings = IconImporterSettings.getInstance(project).state
        val xmlName = NamingHelper.toXmlDrawableName(raw, settings.iconPrefixXml)
        val composeName = NamingHelper.toComposeName(raw, settings.iconSuffixCompose)

        xmlNamePreviewLabel.text = "XML: $xmlName.xml"
        composeNamePreviewLabel.text = "Compose: $composeName.kt"
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnGenerateCompose.isEnabled = enabled
        btnGenerateDrawable.isEnabled = enabled
        btnGenerateBoth.isEnabled = enabled
    }

    private fun performDownloadAndSave(destinationType: IconDestinationType) {
        val url = urlField.text.trim()
        val rawName = iconNameField.text.trim().ifBlank { NamingHelper.extractNameFromUrl(url) }

        if (url.isBlank()) {
            statusLabel.text = "Please enter a URL"
            statusLabel.foreground = JBColor.RED
            return
        }

        // Fast path: If already downloaded and URL has not changed, export immediately
        val cached = downloadedContent
        if (cached != null && url == lastDownloadedUrl) {
            val request = IconImportRequest(
                rawIconName = rawName,
                svgContent = cached,
                destinationType = destinationType,
                sourceUrl = url
            )
            val result = IconExportService.exportIcon(project, request)
            if (result.success) {
                statusLabel.text = "Successfully generated ${destinationType.displayName}!"
                statusLabel.foreground = JBColor.GREEN
            } else {
                statusLabel.text = "Export failed: ${result.errorMessage}"
                statusLabel.foreground = JBColor.RED
            }
            return
        }

        setButtonsEnabled(false)
        progressBar.isVisible = true
        statusLabel.text = "Downloading..."
        statusLabel.foreground = JBColor.foreground()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Downloading Material Symbol", true) {
            private var content: String? = null
            private var errorMsg: String? = null

            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Fetching $url..."
                    content = MaterialSymbolDownloaderService.downloadIconContent(url)
                } catch (e: Exception) {
                    errorMsg = e.message
                }
            }

            override fun onSuccess() {
                setButtonsEnabled(true)
                progressBar.isVisible = false

                val downloaded = content
                if (downloaded != null && downloaded.isNotBlank()) {
                    downloadedContent = downloaded
                    lastDownloadedUrl = url
                    previewComponent.updateSvg(downloaded)

                    // Execute Export
                    val request = IconImportRequest(
                        rawIconName = rawName,
                        svgContent = downloaded,
                        destinationType = destinationType,
                        sourceUrl = url
                    )

                    val result = IconExportService.exportIcon(project, request)
                    if (result.success) {
                        statusLabel.text = "Successfully generated ${destinationType.displayName}!"
                        statusLabel.foreground = JBColor.GREEN
                    } else {
                        statusLabel.text = "Export failed: ${result.errorMessage}"
                        statusLabel.foreground = JBColor.RED
                    }
                } else {
                    statusLabel.text = "Error: ${errorMsg ?: "Failed to download icon"}"
                    statusLabel.foreground = JBColor.RED
                }
            }

            override fun onCancel() {
                setButtonsEnabled(true)
                progressBar.isVisible = false
                statusLabel.text = "Cancelled"
                statusLabel.foreground = JBColor.RED
            }
        })
    }
}
