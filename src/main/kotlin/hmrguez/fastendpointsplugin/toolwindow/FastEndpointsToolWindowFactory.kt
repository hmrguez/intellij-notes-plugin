package hmrguez.fastendpointsplugin.toolwindow

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import hmrguez.fastendpointsplugin.services.EndpointScanner
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class FastEndpointsToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val scanner = project.service<EndpointScanner>()
        val listModel = javax.swing.DefaultListModel<EndpointScanner.Endpoint>()
        val list = JBList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION

        // Custom renderer: METHOD PATH — ClassName [relative/file/path]
        list.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): java.awt.Component {
                val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is EndpointScanner.Endpoint) {
                    val rel = value.filePath
                    val method = if (value.method.isNotBlank()) value.method else "?"
                    val path = if (value.path.isNotBlank()) value.path else "<no route>"
                    text = "$method $path"
                }
                return comp
            }
        }

        fun navigateTo(ep: EndpointScanner.Endpoint?) {
            if (ep == null) return
            val vf = LocalFileSystem.getInstance().findFileByPath(ep.filePath) ?: return
            OpenFileDescriptor(project, vf, ep.line, 0).navigate(true)
        }

        // Mouse double-click to navigate
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val idx = list.locationToIndex(e.point)
                    val ep = if (idx >= 0 && idx < listModel.size()) listModel.getElementAt(idx) else null
                    navigateTo(ep)
                }
            }
        })
        // Enter key to navigate
        list.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    navigateTo(list.selectedValue)
                }
            }
        })

        fun refresh() {
            ApplicationManager.getApplication().executeOnPooledThread {
                val endpoints = scanner.scanEndpoints()
                ApplicationManager.getApplication().invokeLater {
                    listModel.removeAllElements()
                    endpoints.forEach { ep -> listModel.addElement(ep) }
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

        panel.layout = BorderLayout()
        panel.add(decorated, BorderLayout.CENTER)

        val content = ContentFactory.getInstance().createContent(panel, "FastEndpoints", false)
        toolWindow.contentManager.addContent(content)

        // Initial load
        refresh()

        // Keep a reference to update on external events from scanner
        scanner.onEndpointsChanged = { endpoints ->
            ApplicationManager.getApplication().invokeLater {
                listModel.removeAllElements()
                endpoints.forEach { ep -> listModel.addElement(ep) }
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
