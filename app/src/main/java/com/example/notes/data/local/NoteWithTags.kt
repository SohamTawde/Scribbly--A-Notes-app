package com.example.notes.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class NoteWithTags(
    @Embedded val note: NoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "name",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagName"
        )
    )
    val tags: List<TagEntity>
)
