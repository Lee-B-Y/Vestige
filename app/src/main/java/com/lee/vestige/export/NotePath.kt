package com.lee.vestige.export

import java.time.LocalDate

/**
 * Maps a date to its on-disk location and back.
 *
 * Notes live under a fixed `Vestige` base folder of the granted directory, then
 * year/month subfolders, e.g. 2026-05-31 → `Vestige/2026/05/2026-05-31.md`. Keeping
 * everything under `Vestige/` means the app never pollutes the directory the user
 * grants (e.g. Documents).
 *
 * The single place that defines the folder layout — change here to support a
 * different structure later.
 */
object NotePath {
    const val BASE = "Vestige"

    /** Subfolders from the granted tree down to the file's directory. */
    fun folders(date: LocalDate): List<String> = listOf(
        BASE,
        "%04d".format(date.year),
        "%02d".format(date.monthValue),
    )

    fun fileName(date: LocalDate): String = "$date.md" // ISO date, e.g. 2026-05-31.md

    /** Parse a file name like "2026-05-31.md" back to a date, or null if it doesn't match. */
    fun parseDate(fileName: String): LocalDate? {
        if (!fileName.endsWith(".md")) return null
        return runCatching { LocalDate.parse(fileName.removeSuffix(".md")) }.getOrNull()
    }
}
