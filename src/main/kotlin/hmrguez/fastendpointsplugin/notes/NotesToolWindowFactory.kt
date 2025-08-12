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

        fun badgeHtml(tag: String): String {
            val key = tag.lowercase()
            val color = when (key) {
                "todo" -> "#0969da"
                "mental" -> "#8250df"
                "giberish" -> "#9a6700"
                else -> "#57606a"
            }
            // Use simple, Swing-safe HTML without CSS: colored text in brackets + non-breaking space
            return "<font color='${color}'>[${tag}]</font>&nbsp;"
        }

        list.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): java.awt.Component {
                val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is NotesService.Note) {
                    val firstLine = value.content.lines().firstOrNull()?.take(80) ?: "(empty)"
                    // Stable tag order like GitHub (by available set), then any others alphabetically
                    val order = NotesService.AVAILABLE_TAGS.withIndex().associate { it.value to it.index }
                    val sortedTags = value.tags.sortedWith(compareBy({ order[it] ?: Int.MAX_VALUE }, { it }))
                    val tags = sortedTags.joinToString(separator = "") { badgeHtml(it) }
                    val html = """
                        <html>
                          <b>${firstLine}</b><br/>
                          ${tags}
                        </html>
                    """.trimIndent()
                    text = html
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
                        val tagsStr = Messages.showInputDialog(
                            project,
                            "Enter tags (comma separated). Allowed: ${NotesService.AVAILABLE_TAGS.joinToString(", ")}",
                            "Tags",
                            Messages.getQuestionIcon(),
                            "",
                            null
                        )
                        val tags = parseTags(tagsStr)
                        notesService.addNote(trimmed, tags)
                        refresh()
                    }
                }
            }
            .setEditAction {
                val selected = list.selectedValue ?: return@setEditAction
                val current = selected.tags.joinToString(", ")
                val tagsStr = Messages.showInputDialog(
                    project,
                    "Edit tags (comma separated). Allowed: ${NotesService.AVAILABLE_TAGS.joinToString(", ")}",
                    "Edit Tags",
                    Messages.getQuestionIcon(),
                    current,
                    null
                )
                if (tagsStr != null) {
                    val tags = parseTags(tagsStr)
                    notesService.updateNoteTags(selected.id, tags)
                    refresh()
                }
            }
            .setEditActionUpdater { list.selectedValue != null }
            .disableRemoveAction()
            .createPanel()

        panel.add(decorated, BorderLayout.CENTER)

        val content = ContentFactory.getInstance().createContent(panel, "Notes", false)

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
}
