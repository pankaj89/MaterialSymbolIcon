package com.master.materialsymbol.ui.panels

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.master.materialsymbol.settings.IconImporterSettings
import com.master.materialsymbol.util.ProjectStructureHelper
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton

class SettingsPanel(private val project: Project) : JBPanel<SettingsPanel>(BorderLayout()) {

    private val drawablePathField = TextFieldWithBrowseButton()
    private val composePathField = TextFieldWithBrowseButton()
    private val appIconsPathField = TextFieldWithBrowseButton()

    private val autoAppendCheckBox = JBCheckBox("Auto-append imported icons to AppIcons.kt registry", true)
    private val composePackageField = JBTextField()
    private val xmlPrefixField = JBTextField("ic_")
    private val composeSuffixField = JBTextField()

    private val autoDetectButton = JButton("Auto-Detect from Project", AllIcons.Actions.Find)
    private val saveButton = JButton("Save Settings", AllIcons.Actions.MenuSaveall)
    private val statusLabel = JBLabel()

    init {
        border = JBUI.Borders.empty(16)
        setupLayout()
        setupListeners()
        loadSettings()
    }

    private fun setupLayout() {
        val formPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(6, 0, 6, 8)
            anchor = GridBagConstraints.WEST
        }

        // Row 0: Section header & Auto-detect button
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2
        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(JBLabel("Configure Default Export Locations & Registry", AllIcons.General.Settings, JBLabel.LEFT), BorderLayout.WEST)
            add(autoDetectButton, BorderLayout.EAST)
        }
        formPanel.add(headerPanel, gbc)
        gbc.gridwidth = 1

        // Row 1: Android XML Drawable destination path
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0
        formPanel.add(JBLabel("XML Drawables Path:"), gbc)
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0
        drawablePathField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Select XML Drawable Directory")
                .withDescription("Usually app/src/main/res/drawable")
        )
        formPanel.add(drawablePathField, gbc)

        // Row 2: Compose ImageVector destination path
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0
        formPanel.add(JBLabel("Compose Vectors Path:"), gbc)
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0
        composePathField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Select Compose Icons Directory")
                .withDescription("Usually .../ui/theme/icons")
        )
        formPanel.add(composePathField, gbc)

        // Row 3: Compose Package Name
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0
        formPanel.add(JBLabel("Compose Package:"), gbc)
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0
        composePackageField.emptyText.text = "e.g. com.example.app.ui.theme.icons"
        formPanel.add(composePackageField, gbc)

        // Row 4: Centralized AppIcons.kt file path
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0
        formPanel.add(JBLabel("AppIcons.kt Registry:"), gbc)
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0
        appIconsPathField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("kt")
                .withTitle("Select AppIcons.kt File")
                .withDescription("Central registry file for icons")
        )
        formPanel.add(appIconsPathField, gbc)

        // Row 5: Auto-append toggle
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1.0
        formPanel.add(autoAppendCheckBox, gbc)

        // Row 6: XML Prefix
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.0
        formPanel.add(JBLabel("XML Resource Prefix:"), gbc)
        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 1.0
        xmlPrefixField.emptyText.text = "ic_"
        formPanel.add(xmlPrefixField, gbc)

        // Row 7: Compose Suffix
        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0.0
        formPanel.add(JBLabel("Compose Name Suffix:"), gbc)
        gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 1.0
        composeSuffixField.emptyText.text = "e.g. Vector (leave empty for none)"
        formPanel.add(composeSuffixField, gbc)

        // Row 8: Action & Status
        gbc.gridx = 1; gbc.gridy = 8; gbc.weightx = 1.0
        val bottomPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            add(saveButton)
            add(statusLabel)
        }
        formPanel.add(bottomPanel, gbc)

        add(formPanel, BorderLayout.NORTH)
    }

    private fun setupListeners() {
        autoDetectButton.addActionListener {
            performAutoDetect()
        }

        saveButton.addActionListener {
            saveSettings()
            statusLabel.text = "Settings saved!"
            statusLabel.foreground = JBColor.GREEN
        }
    }

    private fun performAutoDetect() {
        val detected = ProjectStructureHelper.autoDetectPaths(project)
        drawablePathField.text = detected.drawablePath
        composePathField.text = detected.composePath
        appIconsPathField.text = detected.appIconsPath
        composePackageField.text = detected.composePackageName
        statusLabel.text = "Auto-detected paths from project"
        statusLabel.foreground = JBColor.GREEN
    }

    fun loadSettings() {
        val settings = IconImporterSettings.getInstance(project).state
        val defaults = ProjectStructureHelper.autoDetectPaths(project)

        drawablePathField.text = settings.drawableDestinationPath.ifBlank { defaults.drawablePath }
        composePathField.text = settings.composeDestinationPath.ifBlank { defaults.composePath }
        appIconsPathField.text = settings.appIconsFilePath.ifBlank { defaults.appIconsPath }
        composePackageField.text = settings.composePackageName.ifBlank { defaults.composePackageName }
        autoAppendCheckBox.isSelected = settings.autoAppendToAppIcons
        xmlPrefixField.text = settings.iconPrefixXml.ifBlank { "ic_" }
        composeSuffixField.text = settings.iconSuffixCompose
    }

    fun saveSettings() {
        val settings = IconImporterSettings.getInstance(project).state
        settings.drawableDestinationPath = drawablePathField.text.trim()
        settings.composeDestinationPath = composePathField.text.trim()
        settings.appIconsFilePath = appIconsPathField.text.trim()
        settings.composePackageName = composePackageField.text.trim()
        settings.autoAppendToAppIcons = autoAppendCheckBox.isSelected
        settings.iconPrefixXml = xmlPrefixField.text.trim().ifBlank { "ic_" }
        settings.iconSuffixCompose = composeSuffixField.text.trim()
    }
}
