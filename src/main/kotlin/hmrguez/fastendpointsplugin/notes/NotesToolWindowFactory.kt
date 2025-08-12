package hmrguez.fastendpointsplugin.notes

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.application.ApplicationManager
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
                return if (value is NotesService.Note) {
                    NoteCellPanel(value, isSelected, cellHasFocus)
                } else {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                }
            }
        }

        fun refresh() {
            listModel.removeAllElements()
            notesService.allNotes().forEach { listModel.addElement(it) }
        }

        val panel = JPanel(BorderLayout())
        val scroll = JBScrollPane(list)

        fun parseTags(input: String?): Set<String> {
            if (input == null) return emptySet()
            return input.split(',')
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSet()
                .filter { NotesService.AVAILABLE_TAGS.contains(it) }
                .toSet()
        }


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
            .setEditAction {
                val selected = list.selectedValue ?: return@setEditAction
                showTagEditDialog(project, selected) { newTags ->
                    notesService.updateNoteTags(selected.id, newTags)
                    refresh()
                }
            }
            .setEditActionUpdater { list.selectedValue != null }
            .disableRemoveAction()
            .createPanel()

        panel.add(decorated, BorderLayout.CENTER)

        val content = ContentFactory.getInstance().createContent(panel, null, false)

        // Create a Disposable for this tool window content and subscribe to updates
        val disposable: Disposable = Disposer.newDisposable("NotesToolWindowDisposable")
        content.setDisposer(disposable)
        val connection = project.messageBus.connect(disposable)
        connection.subscribe(NotesService.TOPIC, object : NotesService.NotesListener {
            override fun notesChanged() {
                // Ensure UI updates happen on EDT
                ApplicationManager.getApplication().invokeLater { refresh() }
            }
        })

        toolWindow.contentManager.addContent(content)

        refresh()
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    private fun showTagEditDialog(project: Project, note: NotesService.Note, onTagsChanged: (Set<String>) -> Unit) {
        val notesService = project.service<NotesService>()
        val tagEditor = TagEditor(
            availableTags = NotesService.AVAILABLE_TAGS.toMutableList(),
            selectedTags = note.tags.toMutableSet(),
            onNewTagCreated = { newTag ->
                // Add the new tag to the global available tags list
                notesService.addAvailableTag(newTag)
            }
        )

        val dialog = object : com.intellij.openapi.ui.DialogWrapper(project) {
            init {
                title = "Edit Tags for Note"
                init()
            }

            override fun createCenterPanel(): javax.swing.JComponent {
                val panel = JPanel(BorderLayout()).apply {
                    add(tagEditor, BorderLayout.CENTER)
                    preferredSize = java.awt.Dimension(450, 200)
                }
                return panel
            }
        }

        if (dialog.showAndGet()) {
            onTagsChanged(tagEditor.getSelectedTags())
        }
    }
}
