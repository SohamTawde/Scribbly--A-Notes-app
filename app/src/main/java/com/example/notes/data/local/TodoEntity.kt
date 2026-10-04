package com.example.notes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
