package com.example.notes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Splash : NavKey
@Serializable data object Main : NavKey
/** startAction: "" (plain), "voice", "image", "draw" — lets quick actions open the editor in a mode. */
@Serializable data class Editor(val noteId: Long = 0, val startAction: String = "") : NavKey
@Serializable data object Archive : NavKey
@Serializable data object Settings : NavKey
@Serializable data object Vault : NavKey

