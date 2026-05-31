package com.lee.vestige.export

import java.time.LocalDate

/** Outcome of a save. */
sealed interface SaveResult {
    data object Success : SaveResult
    data class Error(val message: String) : SaveResult
}

/**
 * Abstracts where day notes are read from and written to — the mirror image of
 * [com.lee.vestige.data.plugin.DataPlugin] (where data comes from).
 *
 * V1 ships only [SafNoteStore] (a user-chosen local folder via the Storage Access
 * Framework, organized into year/month subfolders). Future cloud backends — OneDrive,
 * Baidu Netdisk — implement this same interface; the rest of the app does not change.
 * The active store is produced by [com.lee.vestige.di.AppContainer.noteStoreFor].
 */
interface NoteStore {
    /** Human-readable name of this destination, for the UI. */
    val displayName: String

    /** Existing note content for [date], or `null` if no file exists yet. */
    suspend fun read(date: LocalDate): String?

    /** Create-or-overwrite the note for [date], creating year/month folders as needed. */
    suspend fun write(date: LocalDate, content: String): SaveResult
}
