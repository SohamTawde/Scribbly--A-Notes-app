package com.example.notes.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notes.data.NoteRepository
import com.example.notes.data.local.NoteEntity
import com.example.notes.data.local.NoteWithTags
import com.example.notes.data.local.TagEntity
import com.example.notes.data.local.TodoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(
    private val repository: NoteRepository,
    private val application: android.app.Application? = null
) : ViewModel() {

    // Theme state persistence
    private val prefs = application?.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(prefs?.getString("theme_mode", "system") ?: "system")
    val themeMode = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs?.edit()?.putString("theme_mode", mode)?.apply()
    }

    // Glassy UI state persistence
    private val _glassyUiEnabled = MutableStateFlow(prefs?.getBoolean("glassy_ui", false) ?: false)
    val glassyUiEnabled = _glassyUiEnabled.asStateFlow()

    fun setGlassyUiEnabled(enabled: Boolean) {
        _glassyUiEnabled.value = enabled
        prefs?.edit()?.putBoolean("glassy_ui", enabled)?.apply()
    }

    // Font size scale: 0.85 = Small, 1.0 = Medium, 1.18 = Large
    private val _fontSizeScale = MutableStateFlow(prefs?.getFloat("font_size_scale", 1.0f) ?: 1.0f)
    val fontSizeScale = _fontSizeScale.asStateFlow()

    fun setFontSizeScale(scale: Float) {
        _fontSizeScale.value = scale
        prefs?.edit()?.putFloat("font_size_scale", scale)?.apply()
    }

    // Home / Search states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<TagEntity?>(null)
    val selectedTagFilter = _selectedTagFilter.asStateFlow()

    private val _isGridLayout = MutableStateFlow(true)
    val isGridLayout = _isGridLayout.asStateFlow()

    // Editor States (Active note being created/edited)
    var noteTitle by mutableStateOf("")
    var noteContent by mutableStateOf("")
    var noteColor by mutableStateOf(0) // 0 means default color
    var noteAudioPath by mutableStateOf<String?>(null)
    var noteImageUri by mutableStateOf<String?>(null)
    var noteIsLocked by mutableStateOf(false)
    var noteScribblePath by mutableStateOf<String?>(null)
    var noteSkin by mutableStateOf(0) // 0 plain, 1 lines, 2 grid, 3 dots
    var noteCreatedAt by mutableStateOf(System.currentTimeMillis())
        private set
    var notePinned by mutableStateOf(false)
        private set
    private val _noteTags = MutableStateFlow<Set<String>>(emptySet())
    val noteTags = _noteTags.asStateFlow()

    private var editingNoteId: Long = 0

    // Flows
    val notes: StateFlow<List<NoteWithTags>> = combine(
        _searchQuery,
        _selectedTagFilter,
        repository.getNotes()
    ) { query, tag, list ->
        list.filter { item ->
            val matchesQuery = item.note.title.contains(query, ignoreCase = true) ||
                    item.note.content.contains(query, ignoreCase = true)
            val matchesTag = tag == null || item.tags.any { it.name == tag.name }
            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteWithTags>> = combine(
        _searchQuery,
        repository.getArchivedNotes()
    ) { query, list ->
        list.filter { item ->
            item.note.title.contains(query, ignoreCase = true) ||
                    item.note.content.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.getTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lockedNotes: StateFlow<List<NoteWithTags>> = repository.getLockedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTagFilterSelected(tag: TagEntity?) {
        _selectedTagFilter.value = tag
    }

    fun toggleLayout() {
        _isGridLayout.value = !_isGridLayout.value
    }

    // Editor Operations
    fun loadNoteForEditing(noteId: Long) {
        if (noteId == 0L) {
            // New Note initialization
            editingNoteId = 0
            noteTitle = ""
            noteContent = ""
            noteColor = 0
            noteAudioPath = null
            noteImageUri = null
            noteIsLocked = false
            noteScribblePath = null
            noteSkin = 0
            noteCreatedAt = System.currentTimeMillis()
            notePinned = false
            _noteTags.value = emptySet()
        } else {
            viewModelScope.launch {
                val loadedNote = repository.getNoteById(noteId)
                if (loadedNote != null) {
                    editingNoteId = loadedNote.note.id
                    noteTitle = loadedNote.note.title
                    noteContent = loadedNote.note.content
                    noteColor = loadedNote.note.color
                    noteAudioPath = loadedNote.note.audioFilePath
                    noteImageUri = loadedNote.note.imageUri
                    noteIsLocked = loadedNote.note.isLocked
                    noteScribblePath = loadedNote.note.scribblePath
                    noteSkin = loadedNote.note.skin
                    noteCreatedAt = loadedNote.note.createdAt
                    notePinned = loadedNote.note.isPinned
                    _noteTags.value = loadedNote.tags.map { it.name }.toSet()
                }
            }
        }
    }

    fun toggleTagForEditingNote(tagName: String) {
        val current = _noteTags.value
        _noteTags.value = if (current.contains(tagName)) {
            current - tagName
        } else {
            current + tagName
        }
    }

    fun saveEditingNote() {
        viewModelScope.launch {
            if (noteTitle.isNotBlank() || noteContent.isNotBlank() || noteAudioPath != null || noteImageUri != null || noteScribblePath != null) {
                val note = NoteEntity(
                    id = editingNoteId,
                    title = noteTitle,
                    content = noteContent,
                    color = noteColor,
                    audioFilePath = noteAudioPath,
                    imageUri = noteImageUri,
                    isLocked = noteIsLocked,
                    scribblePath = noteScribblePath,
                    skin = noteSkin,
                    isPinned = notePinned,
                    createdAt = noteCreatedAt,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveNote(note, _noteTags.value.toList())
            }
        }
    }

    // Repository Direct Actions
    fun deleteNote(note: NoteEntity) = viewModelScope.launch {
        repository.deleteNote(note)
    }

    fun setNotePinned(noteId: Long, isPinned: Boolean) = viewModelScope.launch {
        if (noteId == editingNoteId) notePinned = isPinned
        repository.setNotePinned(noteId, isPinned)
    }

    /** Pin state for the note currently open in the editor (works for unsaved notes too). */
    fun setEditingNotePinned(isPinned: Boolean) {
        notePinned = isPinned
        if (editingNoteId != 0L) setNotePinned(editingNoteId, isPinned)
    }

    fun setNoteArchived(noteId: Long, isArchived: Boolean) = viewModelScope.launch {
        repository.setNoteArchived(noteId, isArchived)
    }

    fun createNewTag(tagName: String, color: Int) = viewModelScope.launch {
        if (tagName.isNotBlank()) {
            repository.addTag(TagEntity(tagName, color))
        }
    }

    fun deleteTag(tagName: String) = viewModelScope.launch {
        repository.deleteTag(tagName)
    }

    // Note Locking Action (toggles lock directly)
    fun setNoteLocked(noteId: Long, isLocked: Boolean) = viewModelScope.launch {
        val noteWithTags = repository.getNoteById(noteId)
        if (noteWithTags != null) {
            val updatedNote = noteWithTags.note.copy(
                isLocked = isLocked,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveNote(updatedNote, noteWithTags.tags.map { it.name })
        }
    }

    // Todo states and actions
    val todos: StateFlow<List<TodoEntity>> = repository.getTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTodo(todo: TodoEntity, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.saveTodo(todo)
        onSaved(id)
        if (todo.latitude != null && todo.longitude != null && !todo.isCompleted) {
            application?.let { ctx ->
                com.example.notes.data.GeofenceHelper(ctx).addGeofence(todo.copy(id = id))
            }
        }
    }

    fun deleteTodo(todo: TodoEntity) = viewModelScope.launch {
        repository.deleteTodo(todo)
        application?.let { ctx ->
            com.example.notes.data.GeofenceHelper(ctx).removeGeofence(todo.id)
        }
    }

    fun toggleTodoCompletion(todo: TodoEntity) = viewModelScope.launch {
        val nextCompleted = !todo.isCompleted
        repository.updateTodoCompletion(todo.id, nextCompleted)
        application?.let { ctx ->
            if (nextCompleted) {
                com.example.notes.data.GeofenceHelper(ctx).removeGeofence(todo.id)
            } else {
                com.example.notes.data.GeofenceHelper(ctx).addGeofence(todo)
            }
        }
    }

    // Geocoding Support
    var geocodingState by mutableStateOf<GeocodingResult?>(null)
        private set

    sealed class GeocodingResult {
        object Loading : GeocodingResult()
        data class Success(val address: String, val latitude: Double, val longitude: Double) : GeocodingResult()
        data class Error(val message: String) : GeocodingResult()
    }

    fun geocodeAddress(addressQuery: String) {
        if (addressQuery.isBlank()) return
        geocodingState = GeocodingResult.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(application ?: return@launch)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(addressQuery, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val addressText = address.getAddressLine(0) ?: addressQuery
                    geocodingState = GeocodingResult.Success(
                        address = addressText,
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                } else {
                    geocodingState = GeocodingResult.Error("No coordinates found for this address")
                }
            } catch (e: Exception) {
                geocodingState = GeocodingResult.Error(e.localizedMessage ?: "Geocoding failed")
            }
        }
    }

    fun clearGeocodingState() {
        geocodingState = null
    }

    // Factory
    class Factory(
        private val repository: NoteRepository,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
                return NoteViewModel(repository, application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
