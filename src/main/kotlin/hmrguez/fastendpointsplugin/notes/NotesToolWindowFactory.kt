package hmrguez.fastendpointsplugin.notes

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.components.service
import java.awt.BorderLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class NotesToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val notesService = project.service<NotesService>()

        val listModel = javax.swing.DefaultListModel<NotesService.Note>()
        val list = JBList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION

        list.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): java.awt.Component {
                val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is NotesService.Note) {
                    val preview = value.content.lines().firstOrNull()?.take(80) ?: "(empty)"
                    text = preview
                }
                return comp
            }
        }

        fun refresh() {
            listModel.removeAllElements()
            notesService.allNotes().forEach { listModel.addElement(it) }
        }

        val panel = JPanel(BorderLayout())
        val scroll = JBScrollPane(list)

        val decorated = ToolbarDecorator.createDecorator(list)
            .setAddAction {
                val text = Messages.showMultilineInputDialog(
                    project,
                    "Enter your note:",
                    "New Note",
                    "",
                    null,
                    null
                )
                if (text != null) {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        notesService.addNote(trimmed)
                        refresh()
                    }
                }
            }
            .disableRemoveAction()
            .createPanel()

        panel.add(decorated, BorderLayout.CENTER)

        val content = ContentFactory.getInstance().createContent(panel, "Notes", false)
        toolWindow.contentManager.addContent(content)

        refresh()
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
