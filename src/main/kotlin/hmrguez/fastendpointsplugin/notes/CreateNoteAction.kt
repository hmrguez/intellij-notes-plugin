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

        // Ask for tags (optional)
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
    }

    private fun parseTags(input: String?): Set<String> {
        if (input.isNullOrBlank()) return emptySet()
        return input.split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
            .filter { NotesService.AVAILABLE_TAGS.contains(it) }
            .toSet()
    }
}
