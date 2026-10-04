package com.example.notes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String,
    val color: Int // ARGB color
)
