/*
 * Copyright (c) 2026 Soham Tawde. All rights reserved.
 * Scribbly - Notes & Tasks for Android
 */

package com.example.notes.data

import com.example.notes.data.local.NoteDao
import com.example.notes.data.local.NoteEntity
import com.example.notes.data.local.NoteTagCrossRef
import com.example.notes.data.local.NoteWithTags
import com.example.notes.data.local.TagEntity
import com.example.notes.data.local.TodoDao
import com.example.notes.data.local.TodoEntity
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(): Flow<List<NoteWithTags>>
    fun getArchivedNotes(): Flow<List<NoteWithTags>>
    fun getLockedNotes(): Flow<List<NoteWithTags>>
    fun getTags(): Flow<List<TagEntity>>
    suspend fun getNoteById(id: Long): NoteWithTags?
    suspend fun saveNote(note: NoteEntity, tagNames: List<String>): Long
    suspend fun deleteNote(note: NoteEntity): Int
    suspend fun setNotePinned(id: Long, isPinned: Boolean): Int
    suspend fun setNoteArchived(id: Long, isArchived: Boolean): Int
    suspend fun addTag(tag: TagEntity): Long
    suspend fun deleteTag(tagName: String): Int
    fun searchNotes(query: String, tagName: String? = null): Flow<List<NoteWithTags>>

    // Todo operations
    fun getTodos(): Flow<List<TodoEntity>>
    suspend fun getTodoById(id: Long): TodoEntity?
    suspend fun getActiveTodosWithLocation(): List<TodoEntity>
    suspend fun saveTodo(todo: TodoEntity): Long
    suspend fun deleteTodo(todo: TodoEntity): Int
    suspend fun updateTodoCompletion(id: Long, isCompleted: Boolean): Int
}

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val todoDao: TodoDao
) : NoteRepository {
    override fun getNotes(): Flow<List<NoteWithTags>> = noteDao.getNotesFlow()
    override fun getArchivedNotes(): Flow<List<NoteWithTags>> = noteDao.getArchivedNotesFlow()
    override fun getLockedNotes(): Flow<List<NoteWithTags>> = noteDao.getLockedNotesFlow()
    override fun getTags(): Flow<List<TagEntity>> = noteDao.getTagsFlow()
    override suspend fun getNoteById(id: Long): NoteWithTags? = noteDao.getNoteById(id)

    override suspend fun saveNote(note: NoteEntity, tagNames: List<String>): Long {
        val noteId = noteDao.insertNote(note)
        noteDao.clearNoteTags(noteId)
        for (tagName in tagNames) {
            noteDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagName))
        }
        return noteId
    }

    override suspend fun deleteNote(note: NoteEntity): Int = noteDao.deleteNote(note)
    override suspend fun setNotePinned(id: Long, isPinned: Boolean): Int = noteDao.setNotePinned(id, isPinned)
    override suspend fun setNoteArchived(id: Long, isArchived: Boolean): Int = noteDao.setNoteArchived(id, isArchived)
    override suspend fun addTag(tag: TagEntity): Long = noteDao.insertTag(tag)
    override suspend fun deleteTag(tagName: String): Int = noteDao.deleteTag(tagName)

    override fun searchNotes(query: String, tagName: String?): Flow<List<NoteWithTags>> {
        return if (tagName.isNullOrEmpty()) {
            noteDao.searchNotes(query)
        } else {
            noteDao.searchNotesWithTag(query, tagName)
        }
    }

    // Todo operations implementations
    override fun getTodos(): Flow<List<TodoEntity>> = todoDao.getTodosFlow()
    override suspend fun getTodoById(id: Long): TodoEntity? = todoDao.getTodoById(id)
    override suspend fun getActiveTodosWithLocation(): List<TodoEntity> = todoDao.getActiveTodosWithLocation()
    override suspend fun saveTodo(todo: TodoEntity): Long = todoDao.insertTodo(todo)
    override suspend fun deleteTodo(todo: TodoEntity): Int = todoDao.deleteTodo(todo)
    override suspend fun updateTodoCompletion(id: Long, isCompleted: Boolean): Int = todoDao.updateTodoCompletion(id, isCompleted)
}
