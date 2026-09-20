package com.master.materialsymbol.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.master.materialsymbol.ui.panels.AboutUsPanel
import com.master.materialsymbol.ui.panels.MaterialSymbolPanel
import com.master.materialsymbol.ui.panels.SettingsPanel
import com.master.materialsymbol.ui.panels.SfSymbolsPanel
import com.master.materialsymbol.ui.panels.SvgImagePanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import javax.swing.Action
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.ListSelectionModel

class IconImporterDialog(private val project: Project) : DialogWrapper(project, true) {

    data class NavItem(val title: String, val icon: Icon, val cardName: String)

    private val navItems = listOf(
        NavItem("Material Symbol", AllIcons.Actions.Download, "CARD_MATERIAL"),
        NavItem("SF Symbols", AllIcons.Actions.Edit, "CARD_SF_SYMBOLS"),
        NavItem("SVG Image", AllIcons.FileTypes.Image, "CARD_SVG_IMAGE"),
        NavItem("Settings", AllIcons.General.GearPlain, "CARD_SETTINGS"),
        NavItem("About Us", AllIcons.General.Information, "CARD_ABOUT_US")
    )

    private val cardLayout = CardLayout()
    private val cardsContainer = JBPanel<JBPanel<*>>(cardLayout)

    private val materialPanel = MaterialSymbolPanel(project)
    private val sfSymbolsPanel = SfSymbolsPanel(project)
    private val svgImagePanel = SvgImagePanel(project)
    private val settingsPanel = SettingsPanel(project)
    private val aboutUsPanel = AboutUsPanel()

    private val navList = JBList(navItems).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        selectedIndex = 0
        cellRenderer = SimpleListCellRenderer.create { label, value, _ ->
            label.text = value.title
            label.icon = value.icon
            label.border = JBUI.Borders.empty(8, 14)
        }
    }

    init {
        title = "Material Symbol & Icon Importer"
        init()
    }

    override fun createCenterPanel(): JComponent {
        // Setup cards
        cardsContainer.add(materialPanel, "CARD_MATERIAL")
        cardsContainer.add(sfSymbolsPanel, "CARD_SF_SYMBOLS")
        cardsContainer.add(svgImagePanel, "CARD_SVG_IMAGE")
        cardsContainer.add(settingsPanel, "CARD_SETTINGS")
        cardsContainer.add(aboutUsPanel, "CARD_ABOUT_US")

        // Splitter
        val splitter = JBSplitter(false, 0.21f).apply {
            setHonorComponentsMinimumSize(true)
            firstComponent = JBScrollPane(navList).apply {
                minimumSize = Dimension(JBUI.scale(210), 0)
                border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 1)
            }
            secondComponent = cardsContainer
        }

        // Navigation listener
        navList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selected = navList.selectedValue
                if (selected != null) {
                    cardLayout.show(cardsContainer, selected.cardName)
                    if (selected.cardName == "CARD_SETTINGS") {
                        settingsPanel.loadSettings()
                    }
                }
            }
        }

        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            preferredSize = Dimension(JBUI.scale(1140), JBUI.scale(660))
            minimumSize = Dimension(JBUI.scale(980), JBUI.scale(540))
            add(splitter, BorderLayout.CENTER)
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction.apply { putValue(Action.NAME, "Close") })
    }

    override fun doOKAction() {
        settingsPanel.saveSettings()
        super.doOKAction()
    }
}
