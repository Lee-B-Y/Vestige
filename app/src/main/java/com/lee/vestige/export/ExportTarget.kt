package com.lee.vestige.export

/** Outcome of an export attempt. */
sealed interface ExportResult {
    data class Success(val fileName: String) : ExportResult
    /** File already exists and `overwrite` was false — ask the user before clobbering. */
    data class AlreadyExists(val fileName: String) : ExportResult
    data class Error(val message: String) : ExportResult
}

/**
 * Abstracts "where the Markdown is written to". The mirror image of
 * [com.lee.vestige.data.plugin.DataPlugin] (which abstracts where data comes from).
 *
 * V1 ships only [SafExportTarget] (a user-chosen local folder via the Storage Access
 * Framework). Future cloud backends — OneDrive, Baidu Netdisk — implement this same
 * interface; the export pipeline and UI do not change. The chosen target is produced
 * by [com.lee.vestige.di.AppContainer.exportTargetFor].
 */
interface ExportTarget {
    /** Human-readable name of this destination, for the UI. */
    val displayName: String

    /**
     * Write [content] to [fileName] at this target.
     *
     * Create-once semantics: if the file already exists and [overwrite] is false,
     * return [ExportResult.AlreadyExists] without touching it.
     */
    suspend fun export(fileName: String, content: String, overwrite: Boolean): ExportResult
}
