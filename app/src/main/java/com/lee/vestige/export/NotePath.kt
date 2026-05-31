package com.lee.vestige.export

import java.time.LocalDate

/**
 * Maps a date to its on-disk location: year/month subfolders + file name,
 * e.g. 2026-05-31 → folders ["2026", "05"], file "2026-05-31.md".
 *
 * The single place that defines the folder layout — change here to support a
 * different structure later.
 */
object NotePath {
    fun folders(date: LocalDate): List<String> = listOf(
        "%04d".format(date.year),
        "%02d".format(date.monthValue),
    )

    fun fileName(date: LocalDate): String = "$date.md" // ISO date, e.g. 2026-05-31.md
}
