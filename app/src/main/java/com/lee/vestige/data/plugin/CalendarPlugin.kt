package com.lee.vestige.data.plugin

import android.content.Context
import com.lee.vestige.R
import com.lee.vestige.data.model.DaySection
import com.lee.vestige.data.source.CalendarDataSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Turns calendar events into the "事件 / Events" Markdown section.
 *
 * Rendering is deliberately minimal so the diary's focus stays on the user's notes,
 * not on auxiliary data:
 * - timed event:      `- 09:00-11:00 标题`  (start-end time is necessary info, kept)
 * - all-day event:    `- 标题`              (no "全天" prefix)
 * - untitled event:   title left blank for the user to fill in later
 *   (timed → `- 09:00-11:00`, all-day → `- `)
 */
class CalendarPlugin(
    private val context: Context,
    private val dataSource: CalendarDataSource,
) : DataPlugin {

    override val sectionTitle: String get() = context.getString(R.string.section_calendar)
    override val order: Int = 20

    private val zone: ZoneId = ZoneId.systemDefault()
    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override suspend fun fetch(date: LocalDate): DaySection? {
        val events = dataSource.getEventsForDay(date)
        if (events.isEmpty()) return null

        val lines = events.map { e ->
            val title = e.title.trim()
            if (e.isAllDay) {
                // Empty title keeps a blank "- " placeholder for the user to fill in.
                "- $title"
            } else {
                val start = Instant.ofEpochMilli(e.startMillis).atZone(zone).format(timeFormat)
                val end = Instant.ofEpochMilli(e.endMillis).atZone(zone).format(timeFormat)
                if (title.isEmpty()) "- $start-$end" else "- $start-$end $title"
            }
        }
        return DaySection(title = sectionTitle, order = order, lines = lines)
    }
}
