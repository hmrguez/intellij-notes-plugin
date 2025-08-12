package hmrguez.fastendpointsplugin.notes

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import java.time.Instant
import java.util.UUID

@State(name = "NotesService", storages = [Storage("notes.xml")])
@Service(Service.Level.PROJECT)
class NotesService(private val project: Project) : PersistentStateComponent<NotesService.State> {
    interface NotesListener {
        fun notesChanged()
    }

    companion object {
        val AVAILABLE_TAGS: MutableList<String> = mutableListOf("todo", "mental", "giberish")
        val TOPIC: Topic<NotesListener> = Topic.create("Notes changes", NotesListener::class.java)
    }

    data class Note(
        var id: String = UUID.randomUUID().toString(),
        var content: String = "",
        var createdAtEpochSeconds: Long = Instant.now().epochSecond,
        var tags: MutableSet<String> = mutableSetOf()
    )

    class State {
        var notes: MutableList<Note> = mutableListOf()
        var availableTags: MutableList<String> = mutableListOf("todo", "mental", "giberish")
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
        // Sync available tags with the loaded state
        AVAILABLE_TAGS.clear()
        AVAILABLE_TAGS.addAll(state.availableTags)
    }

    fun allNotes(): List<Note> = state.notes.toList()

    fun addNote(content: String, tags: Set<String> = emptySet()): Note {
        val normalized = tags.map { it.lowercase() }.filter { AVAILABLE_TAGS.contains(it) }.toSet()
        val note = Note(content = content, tags = normalized.toMutableSet())
        state.notes.add(0, note) // newest first
        project.messageBus.syncPublisher(TOPIC).notesChanged()
        return note
    }

    fun updateNoteTags(id: String, tags: Set<String>) {
        val normalized = tags.map { it.lowercase() }.filter { AVAILABLE_TAGS.contains(it) }.toSet()
        state.notes.firstOrNull { it.id == id }?.let {
            it.tags = normalized.toMutableSet()
            project.messageBus.syncPublisher(TOPIC).notesChanged()
        }
    }

    fun updateNoteContent(id: String, content: String) {
        state.notes.firstOrNull { it.id == id }?.let {
            it.content = content.trim()
            project.messageBus.syncPublisher(TOPIC).notesChanged()
        }
    }

    fun deleteNote(id: String) {
        val noteIndex = state.notes.indexOfFirst { it.id == id }
        if (noteIndex != -1) {
            state.notes.removeAt(noteIndex)
            project.messageBus.syncPublisher(TOPIC).notesChanged()
        }
    }

    fun addAvailableTag(tag: String): Boolean {
        val normalized = tag.trim().lowercase()
        if (normalized.isNotEmpty() && !AVAILABLE_TAGS.contains(normalized)) {
            AVAILABLE_TAGS.add(normalized)
            state.availableTags.add(normalized)
            project.messageBus.syncPublisher(TOPIC).notesChanged()
            return true
        }
        return false
    }
}
