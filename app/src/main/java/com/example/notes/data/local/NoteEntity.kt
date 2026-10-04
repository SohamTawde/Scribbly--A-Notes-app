package com.example.notes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val color: Int, // ARGB Color integer
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val audioFilePath: String? = null,
    val imageUri: String? = null,
    val isLocked: Boolean = false,
    val scribblePath: String? = null,
    // Page skin: 0 = plain, 1 = lines, 2 = grid, 3 = dots
    @ColumnInfo(defaultValue = "0") val skin: Int = 0
)
