/*
 * Copyright (c) 2026 Soham Tawde. All rights reserved.
 * Scribbly - Notes & Tasks for Android
 */

package com.example.notes

import android.app.Application
import com.example.notes.data.NoteRepository
import com.example.notes.data.NoteRepositoryImpl
import com.example.notes.data.local.NoteDatabase

class NotesApplication : Application() {
    val database by lazy { NoteDatabase.getDatabase(this) }
    val repository by lazy { NoteRepositoryImpl(database.noteDao(), database.todoDao()) }
}
