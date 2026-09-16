package com.master.materialsymbol

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.master.materialsymbol.ui.IconImporterDialog
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JButton

class MyToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = MaterialSymbolToolWindow(project)
        val content = ContentFactory.getInstance().createContent(toolWindowPanel.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    class MaterialSymbolToolWindow(private val project: Project) {
        private val content = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(16)

            val innerPanel = JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                add(JBLabel("MaterialSymbolIcon", AllIcons.Actions.Download, JBLabel.LEFT).apply {
                    font = font.deriveFont(16.0f)
                    border = JBUI.Borders.emptyBottom(8)
                })

                add(JBLabel("Download and convert Material Symbols, SF Symbols, and SVG images for Compose and XML.").apply {
                    border = JBUI.Borders.emptyBottom(16)
                })

                val buttonPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                    add(JButton("Open Icon Importer...", AllIcons.Actions.Download).apply {
                        addActionListener {
                            IconImporterDialog(project).show()
                        }
                    })
                }
                add(buttonPanel)
            }

            add(innerPanel, BorderLayout.NORTH)
        }

        fun getContent(): JBPanel<JBPanel<*>> = content
    }
}
