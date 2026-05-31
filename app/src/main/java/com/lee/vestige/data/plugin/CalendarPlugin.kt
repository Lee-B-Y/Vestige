package com.lee.vestige.data.plugin

import com.lee.vestige.data.model.DaySection
import com.lee.vestige.data.source.CalendarDataSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Turns calendar events into a "Calendar" Markdown section.
 *
 * Formatting lives here (not in the renderer): timed events render as
 * `- HH:mm-HH:mm Title`, all-day events as `- 全天 Title`.
 */
class CalendarPlugin(
    private val dataSource: CalendarDataSource,
) : DataPlugin {

    override val sectionTitle: String = "Calendar"
    override val order: Int = 10

    private val zone: ZoneId = ZoneId.systemDefault()
    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override suspend fun fetch(date: LocalDate): DaySection? {
        val events = dataSource.getEventsForDay(date)
        if (events.isEmpty()) return null

        val lines = events.map { e ->
            if (e.isAllDay) {
                "- 全天 ${e.title}"
            } else {
                val start = Instant.ofEpochMilli(e.startMillis).atZone(zone).format(timeFormat)
                val end = Instant.ofEpochMilli(e.endMillis).atZone(zone).format(timeFormat)
                "- $start-$end ${e.title}"
            }
        }
        return DaySection(title = sectionTitle, order = order, lines = lines)
    }
}
