package com.example.notes.data.local

import androidx.room.Entity

@Entity(tableName = "note_tag_cross_ref", primaryKeys = ["noteId", "tagName"])
data class NoteTagCrossRef(
    val noteId: Long,
    val tagName: String
)
