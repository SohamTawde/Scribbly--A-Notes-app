package com.example.notes

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.notes.ui.screens.ArchiveScreen
import com.example.notes.ui.screens.DashboardScreen
import com.example.notes.ui.screens.EditorScreen
import com.example.notes.ui.screens.SplashScreen
import com.example.notes.ui.screens.VaultScreen
import com.example.notes.ui.viewmodel.NoteViewModel

@Composable
fun MainNavigation(viewModel: NoteViewModel) {
    val backStack = rememberNavBackStack(Splash)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
            (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) +
                fadeIn(animationSpec = tween(350))) togetherWith
            (slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(350)) +
                fadeOut(animationSpec = tween(350)))
        },
        popTransitionSpec = {
            (slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(350)) +
                fadeIn(animationSpec = tween(350))) togetherWith
            (slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) +
                fadeOut(animationSpec = tween(350)))
        },
        entryProvider = entryProvider {
            entry<Splash> {
                SplashScreen(
                    onSplashComplete = {
                        backStack.clear()
                        backStack.add(Main)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Main> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = { noteId -> backStack.add(Editor(noteId)) },
                    onQuickAction = { action -> backStack.add(Editor(0, action)) },
                    onNavigateToArchive = { backStack.add(Archive) },
                    onNavigateToSettings = { backStack.add(Settings) },
                    onNavigateToVault = { backStack.add(Vault) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Editor> { key ->
                EditorScreen(
                    noteId = key.noteId,
                    startAction = key.startAction,
                    viewModel = viewModel,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Archive> {
                ArchiveScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = { noteId -> backStack.add(Editor(noteId)) },
                    onNavigateBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Settings> {
                com.example.notes.ui.screens.SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Vault> {
                VaultScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = { noteId -> backStack.add(Editor(noteId)) },
                    onNavigateBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
