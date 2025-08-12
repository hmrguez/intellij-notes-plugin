package hmrguez.fastendpointsplugin.notes

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import java.time.Instant
import java.util.UUID

@State(name = "NotesService", storages = [Storage("notes.xml")])
@Service(Service.Level.PROJECT)
class NotesService(private val project: Project) : PersistentStateComponent<NotesService.State> {
    companion object {
        val AVAILABLE_TAGS: List<String> = listOf("todo", "mental", "giberish")
    }

    data class Note(
        var id: String = UUID.randomUUID().toString(),
        var content: String = "",
        var createdAtEpochSeconds: Long = Instant.now().epochSecond,
        var tags: MutableSet<String> = mutableSetOf()
    )

    class State {
        var notes: MutableList<Note> = mutableListOf()
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun allNotes(): List<Note> = state.notes.toList()

    fun addNote(content: String, tags: Set<String> = emptySet()): Note {
        val normalized = tags.map { it.lowercase() }.filter { AVAILABLE_TAGS.contains(it) }.toSet()
        val note = Note(content = content, tags = normalized.toMutableSet())
        state.notes.add(0, note) // newest first
        return note
    }

    fun updateNoteTags(id: String, tags: Set<String>) {
        val normalized = tags.map { it.lowercase() }.filter { AVAILABLE_TAGS.contains(it) }.toSet()
        state.notes.firstOrNull { it.id == id }?.let { it.tags = normalized.toMutableSet() }
    }
}
