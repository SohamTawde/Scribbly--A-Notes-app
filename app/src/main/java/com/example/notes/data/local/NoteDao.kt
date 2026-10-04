package com.example.notes.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isLocked = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesFlow(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotesFlow(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isLocked = 1 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getLockedNotesFlow(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): NoteWithTags?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity): Int

    @Update
    suspend fun updateNote(note: NoteEntity): Int

    @Query("UPDATE notes SET isPinned = :isPinned, updatedAt = :timestamp WHERE id = :noteId")
    suspend fun setNotePinned(noteId: Long, isPinned: Boolean, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE notes SET isArchived = :isArchived, isPinned = 0, updatedAt = :timestamp WHERE id = :noteId")
    suspend fun setNoteArchived(noteId: Long, isArchived: Boolean, timestamp: Long = System.currentTimeMillis()): Int

    // Tag Operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE name = :tagName")
    suspend fun deleteTag(tagName: String): Int

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getTagsFlow(): Flow<List<TagEntity>>

    // Cross Reference Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(ref: NoteTagCrossRef): Long

    @Delete
    suspend fun deleteNoteTagCrossRef(ref: NoteTagCrossRef): Int

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun clearNoteTags(noteId: Long): Int

    // Search and Filters
    @Transaction
    @Query("""
        SELECT * FROM notes 
        WHERE isArchived = 0 
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') 
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT n.* FROM notes n
        INNER JOIN note_tag_cross_ref ref ON n.id = ref.noteId
        WHERE n.isArchived = 0 
          AND ref.tagName = :tagName
          AND (n.title LIKE '%' || :query || '%' OR n.content LIKE '%' || :query || '%')
        ORDER BY n.isPinned DESC, n.updatedAt DESC
    """)
    fun searchNotesWithTag(query: String, tagName: String): Flow<List<NoteWithTags>>
}
