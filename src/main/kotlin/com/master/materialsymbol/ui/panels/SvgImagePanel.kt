package com.master.materialsymbol.ui.panels

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
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
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import javax.swing.JButton
import javax.swing.event.DocumentEvent

class SvgImagePanel(private val project: Project) : JBPanel<SvgImagePanel>(BorderLayout()) {

    private val filePicker = TextFieldWithBrowseButton()
    private val dropZoneLabel = JBLabel("Or Drag & Drop an .svg file here", AllIcons.General.ArrowDown, JBLabel.CENTER).apply {
        border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.border(), 1),
            JBUI.Borders.empty(12)
        )
        foreground = JBColor.GRAY
    }

    private val iconNameField = JBTextField()
    private val xmlPreviewLabel = JBLabel().apply { foreground = JBColor.GRAY }
    private val composePreviewLabel = JBLabel().apply { foreground = JBColor.GRAY }

    private val btnGenerateCompose = JButton("Generate ImageVector", AllIcons.Actions.Compile)
    private val btnGenerateDrawable = JButton("Generate Drawable", AllIcons.FileTypes.Xml)
    private val btnGenerateBoth = JButton("Generate Both", AllIcons.Actions.Execute)

    private val statusLabel = JBLabel()
    private val previewComponent = SvgPreviewComponent()

    private var loadedSvgContent: String? = null

    init {
        border = JBUI.Borders.empty(16)
        setupLayout()
        setupListeners()
        setupDragAndDrop()
    }

    private fun setupLayout() {
        val formPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(4, 0, 4, 8)
            anchor = GridBagConstraints.WEST
        }

        // Row 0: File picker
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        formPanel.add(JBLabel("SVG File:"), gbc)
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0
        filePicker.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("svg")
                .withTitle("Select SVG File")
                .withDescription("Choose an SVG image to convert")
        )
        formPanel.add(filePicker, gbc)

        // Row 1: Drop zone
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0
        formPanel.add(dropZoneLabel, gbc)

        // Row 2: Icon Name
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0
        formPanel.add(JBLabel("Icon Name:"), gbc)
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0
        formPanel.add(iconNameField, gbc)

        // Row 3: Name previews
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0
        val previewNamesPanel = JBPanel<JBPanel<*>>().apply {
            add(composePreviewLabel)
            add(JBLabel(" | "))
            add(xmlPreviewLabel)
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

        // Row 5: Status
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1.0
        formPanel.add(statusLabel, gbc)

        add(formPanel, BorderLayout.NORTH)

        // Center: Preview panel
        val centerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.emptyTop(12),
                JBUI.Borders.customLine(JBColor.border(), 1)
            )
            add(JBLabel("SVG Preview", JBLabel.CENTER).apply {
                border = JBUI.Borders.empty(4)
            }, BorderLayout.NORTH)
            add(previewComponent, BorderLayout.CENTER)
        }
        add(centerPanel, BorderLayout.CENTER)
    }

    private fun setupListeners() {
        filePicker.textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                loadFile(filePicker.text.trim())
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

    private fun setupDragAndDrop() {
        val dropTargetListener = object : DropTargetAdapter() {
            override fun drop(dtde: DropTargetDropEvent) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY)
                    val transferable = dtde.transferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @Suppress("UNCHECKED_CAST")
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                        val svgFile = files?.firstOrNull { it.name.endsWith(".svg", ignoreCase = true) }
                        if (svgFile != null) {
                            filePicker.text = svgFile.absolutePath
                            loadFile(svgFile.absolutePath)
                            dtde.dropComplete(true)
                            return
                        }
                    }
                    dtde.dropComplete(false)
                } catch (_: Exception) {
                    dtde.dropComplete(false)
                }
            }
        }
        DropTarget(dropZoneLabel, dropTargetListener)
        DropTarget(this, dropTargetListener)
    }

    private fun loadFile(path: String) {
        if (path.isBlank()) {
            loadedSvgContent = null
            previewComponent.updateSvg("")
            return
        }

        val file = File(path)
        if (file.exists() && file.isFile) {
            try {
                val content = file.readText(Charsets.UTF_8)
                loadedSvgContent = content
                previewComponent.updateSvg(content)

                val rawName = file.nameWithoutExtension
                if (iconNameField.text.isBlank() || iconNameField.text == lastAutoName) {
                    lastAutoName = rawName
                    iconNameField.text = rawName
                }
                statusLabel.text = "Loaded: ${file.name}"
                statusLabel.foreground = JBColor.GREEN
            } catch (e: Exception) {
                statusLabel.text = "Error reading file: ${e.message}"
                statusLabel.foreground = JBColor.RED
            }
        } else {
            statusLabel.text = "File not found"
            statusLabel.foreground = JBColor.RED
        }
        updateNamePreviews()
    }

    private var lastAutoName: String = ""

    private fun updateNamePreviews() {
        val raw = iconNameField.text.trim()
        val settings = IconImporterSettings.getInstance(project).state
        val xmlName = NamingHelper.toXmlDrawableName(raw, settings.iconPrefixXml)
        val composeName = NamingHelper.toComposeName(raw, settings.iconSuffixCompose)

        xmlPreviewLabel.text = "XML: $xmlName.xml"
        composePreviewLabel.text = "Compose: $composeName.kt"
    }

    private fun performExport(type: IconDestinationType) {
        val content = loadedSvgContent
        val rawName = iconNameField.text.trim().ifBlank { "custom_icon" }

        if (content.isNullOrBlank()) {
            statusLabel.text = "Please select or drop an SVG file first"
            statusLabel.foreground = JBColor.RED
            return
        }

        val sanitized = SvgConversionService.sanitizeSvg(content)
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
