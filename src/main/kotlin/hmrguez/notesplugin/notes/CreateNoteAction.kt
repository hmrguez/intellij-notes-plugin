package hmrguez.fastendpointsplugin.notes

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages

class CreateNoteAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val notesService = project.service<NotesService>()

        // Ask for note content
        val text = Messages.showMultilineInputDialog(
            project,
            "Enter your note:",
            "New Note",
            "",
            null,
            null
        )
        if (text.isNullOrBlank()) return

        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // Create note with no tags; tags can be edited later in the tool window
        notesService.addNote(trimmed)
    }
}
