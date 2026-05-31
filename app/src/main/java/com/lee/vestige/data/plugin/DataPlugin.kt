package com.lee.vestige.data.plugin

import com.lee.vestige.data.model.DaySection
import java.time.LocalDate

/**
 * The single extension point of the whole app.
 *
 * Each data source (calendar today; weather / steps / sleep tomorrow) implements
 * this interface and is registered in [com.lee.vestige.di.AppContainer]. Adding a
 * new source means writing one new class — the export pipeline does not change.
 */
interface DataPlugin {
    /** Section heading in the Markdown output, e.g. "Calendar" / "Weather". */
    val sectionTitle: String

    /** Ordering of this section within a day's note; lower comes first. */
    val order: Int

    /**
     * Produce this plugin's section for [date], or `null` if there is nothing to
     * contribute that day (the section is then omitted entirely).
     */
    suspend fun fetch(date: LocalDate): DaySection?
}
