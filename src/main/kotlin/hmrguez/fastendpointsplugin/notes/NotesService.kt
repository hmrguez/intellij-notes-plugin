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
    data class Note(
        var id: String = UUID.randomUUID().toString(),
        var content: String = "",
        var createdAtEpochSeconds: Long = Instant.now().epochSecond
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

    fun addNote(content: String): Note {
        val note = Note(content = content)
        state.notes.add(0, note) // newest first
        return note
    }
}
