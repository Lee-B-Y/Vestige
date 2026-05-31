package com.lee.vestige.domain

import com.lee.vestige.data.model.DayEntry
import com.lee.vestige.data.plugin.DataPlugin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

/**
 * Runs every registered [DataPlugin] for a date and assembles a [DayEntry].
 *
 * Plugins are fetched concurrently (cheap for the single V1 calendar plugin, useful
 * once network-backed sources like weather are added). Sections returning `null`
 * are dropped; the rest are ordered by [DataPlugin.order].
 */
class DayAggregator(
    private val plugins: List<DataPlugin>,
) {
    suspend fun aggregate(date: LocalDate): DayEntry = coroutineScope {
        val sections = plugins
            .map { plugin -> async { plugin.fetch(date) } }
            .awaitAll()
            .filterNotNull()
            .sortedBy { it.order }
        DayEntry(date = date, sections = sections)
    }
}
