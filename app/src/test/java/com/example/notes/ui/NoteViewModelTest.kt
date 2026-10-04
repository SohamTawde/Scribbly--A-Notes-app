package com.example.notes.ui

import com.example.notes.data.NoteRepository
import com.example.notes.data.local.NoteEntity
import com.example.notes.data.local.NoteWithTags
import com.example.notes.data.local.TagEntity
import com.example.notes.data.local.TodoEntity
import com.example.notes.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun noteViewModel_initialState_isCorrect() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = NoteViewModel(repository)

        assertEquals("", viewModel.searchQuery.value)
        assertEquals(null, viewModel.selectedTagFilter.value)
        assertTrue(viewModel.isGridLayout.value)
    }

    @Test
    fun noteViewModel_onSearchQueryChanged_updatesQuery() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = NoteViewModel(repository)

        viewModel.onSearchQueryChanged("Meeting")
        assertEquals("Meeting", viewModel.searchQuery.value)
    }

    @Test
    fun noteViewModel_loadNoteForEditing_loadsCorrectDetails() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = NoteViewModel(repository)

        // Insert a dummy note
        val note = NoteEntity(id = 1, title = "Design doc", content = "Write specifications", color = 2)
        repository.saveNote(note, listOf("Work"))

        testScheduler.advanceUntilIdle()

        viewModel.loadNoteForEditing(1)
        testScheduler.advanceUntilIdle()

        assertEquals("Design doc", viewModel.noteTitle)
        assertEquals("Write specifications", viewModel.noteContent)
        assertEquals(2, viewModel.noteColor)
    }
}

class FakeNoteRepository : NoteRepository {
    private val notesFlow = MutableStateFlow<List<NoteWithTags>>(emptyList())
    private val tagsFlow = MutableStateFlow<List<TagEntity>>(emptyList())
    private val todosFlow = MutableStateFlow<List<TodoEntity>>(emptyList())

    override fun getNotes(): Flow<List<NoteWithTags>> = notesFlow
    override fun getArchivedNotes(): Flow<List<NoteWithTags>> = notesFlow // simple mock
    override fun getLockedNotes(): Flow<List<NoteWithTags>> = MutableStateFlow(notesFlow.value.filter { it.note.isLocked })
    override fun getTags(): Flow<List<TagEntity>> = tagsFlow

    override suspend fun getNoteById(id: Long): NoteWithTags? {
        return notesFlow.value.find { it.note.id == id }
    }

    override suspend fun saveNote(note: NoteEntity, tagNames: List<String>): Long {
        val targetId = if (note.id == 0L) (notesFlow.value.size + 1).toLong() else note.id
        val updatedNote = note.copy(id = targetId)
        val tagsList = tagNames.map { TagEntity(it, 0) }
        val noteWithTags = NoteWithTags(updatedNote, tagsList)
        
        notesFlow.value = notesFlow.value.filter { it.note.id != targetId } + noteWithTags
        return targetId
    }

    override suspend fun deleteNote(note: NoteEntity): Int {
        val originalSize = notesFlow.value.size
        notesFlow.value = notesFlow.value.filter { it.note.id != note.id }
        return originalSize - notesFlow.value.size
    }

    override suspend fun setNotePinned(id: Long, isPinned: Boolean): Int {
        var updatedCount = 0
        notesFlow.value = notesFlow.value.map { 
            if (it.note.id == id) {
                updatedCount++
                NoteWithTags(it.note.copy(isPinned = isPinned), it.tags)
            } else it
        }
        return updatedCount
    }

    override suspend fun setNoteArchived(id: Long, isArchived: Boolean): Int {
        var updatedCount = 0
        notesFlow.value = notesFlow.value.map {
            if (it.note.id == id) {
                updatedCount++
                NoteWithTags(it.note.copy(isArchived = isArchived), it.tags)
            } else it
        }
        return updatedCount
    }

    override suspend fun addTag(tag: TagEntity): Long {
        tagsFlow.value = tagsFlow.value + tag
        return 1L
    }

    override suspend fun deleteTag(tagName: String): Int {
        val originalSize = tagsFlow.value.size
        tagsFlow.value = tagsFlow.value.filter { it.name != tagName }
        return originalSize - tagsFlow.value.size
    }

    override fun searchNotes(query: String, tagName: String?): Flow<List<NoteWithTags>> = notesFlow

    // Mocks for Todo operations
    override fun getTodos(): Flow<List<TodoEntity>> = todosFlow
    override suspend fun getTodoById(id: Long): TodoEntity? = todosFlow.value.find { it.id == id }
    override suspend fun getActiveTodosWithLocation(): List<TodoEntity> = todosFlow.value.filter { !it.isCompleted && it.latitude != null }
    override suspend fun saveTodo(todo: TodoEntity): Long {
        val targetId = if (todo.id == 0L) (todosFlow.value.size + 1).toLong() else todo.id
        val updatedTodo = todo.copy(id = targetId)
        todosFlow.value = todosFlow.value.filter { it.id != targetId } + updatedTodo
        return targetId
    }
    override suspend fun deleteTodo(todo: TodoEntity): Int {
        val originalSize = todosFlow.value.size
        todosFlow.value = todosFlow.value.filter { it.id != todo.id }
        return originalSize - todosFlow.value.size
    }
    override suspend fun updateTodoCompletion(id: Long, isCompleted: Boolean): Int {
        var updatedCount = 0
        todosFlow.value = todosFlow.value.map {
            if (it.id == id) {
                updatedCount++
                it.copy(isCompleted = isCompleted)
            } else it
        }
        return updatedCount
    }
}
