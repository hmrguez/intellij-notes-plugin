package hmrguez.fastendpointsplugin.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.ui.ToolbarDecorator
import hmrguez.fastendpointsplugin.services.EndpointScanner
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class FastEndpointsToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val scanner = project.service<EndpointScanner>()
        val listModel = javax.swing.DefaultListModel<String>()
        val list = JBList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION

        fun refresh() {
            ApplicationManager.getApplication().executeOnPooledThread {
                val endpoints = scanner.scanEndpoints()
                ApplicationManager.getApplication().invokeLater {
                    listModel.removeAllElements()
                    endpoints.forEach { ep -> listModel.addElement("${'$'}{ep.className} [${'$'}{ep.filePath}]") }
                }
            }
        }

        val panel = JPanel()
        val scroll = JBScrollPane(list)
        val decorated = ToolbarDecorator.createDecorator(list)
            .setAddAction { refresh() } // Use + as Refresh for simplicity
            .setRemoveActionUpdater { false }
            .disableRemoveAction()
            .createPanel()

        panel.layout = java.awt.BorderLayout()
        panel.add(decorated, java.awt.BorderLayout.CENTER)

        val content = ContentFactory.getInstance().createContent(panel, "FastEndpoints", false)
        toolWindow.contentManager.addContent(content)

        // Initial load
        refresh()

        // Keep a reference to update on external events from scanner
        scanner.onEndpointsChanged = { endpoints ->
            ApplicationManager.getApplication().invokeLater {
                listModel.removeAllElements()
                endpoints.forEach { ep -> listModel.addElement("${'$'}{ep.className} [${'$'}{ep.filePath}]") }
            }
        }

        // Also auto-refresh after builds
        val connection = project.messageBus.connect()
        connection.subscribe(com.intellij.task.ProjectTaskListener.TOPIC, object : com.intellij.task.ProjectTaskListener {
            override fun finished(result: com.intellij.task.ProjectTaskManager.Result) {
                refresh()
            }
        })
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
