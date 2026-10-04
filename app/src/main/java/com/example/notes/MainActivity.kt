/*
 * Copyright (c) 2026 Soham Tawde. All rights reserved.
 * Scribbly - Notes & Tasks for Android
 */

package com.example.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import com.example.notes.theme.NotesTheme
import com.example.notes.ui.components.MeshGradientBackground
import com.example.notes.ui.viewmodel.NoteViewModel

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = applicationContext as NotesApplication
        val noteViewModel: NoteViewModel by viewModels {
            NoteViewModel.Factory(application.repository, application)
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by noteViewModel.themeMode.collectAsStateWithLifecycle()
            val glassyUiEnabled by noteViewModel.glassyUiEnabled.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            NotesTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (glassyUiEnabled) {
                        MeshGradientBackground(isDark = darkTheme, modifier = Modifier.fillMaxSize())
                    }
                    
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (glassyUiEnabled) Color.Transparent else MaterialTheme.colorScheme.background
                    ) {
                        MainNavigation(viewModel = noteViewModel)
                    }
                }
            }
        }
    }
}

