package com.lee.vestige.data.source

import android.content.Context
import android.provider.CalendarContract
import com.lee.vestige.data.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads calendar event instances for a single day from the system Calendar Provider.
 *
 * Queries [CalendarContract.Instances] (not `Events`) so that recurring events are
 * expanded into concrete occurrences within the day window. Any calendar exposed
 * through the system provider — Google, local, Samsung — is read uniformly.
 */
class CalendarDataSource(private val context: Context) {

    suspend fun getEventsForDay(date: LocalDate): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            // Instances URI carries the [start, end] window as path segments;
            // the provider expands recurrence rules for us.
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(dayStart.toString())
                .appendPath(dayEnd.toString())
                .build()

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            )

            val events = mutableListOf<CalendarEvent>()
            // If READ_CALENDAR is somehow missing, the provider throws SecurityException;
            // treat that as "no events" so the pipeline degrades gracefully.
            runCatching {
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC",
                )?.use { c ->
                    val iTitle = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                    val iBegin = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                    val iEnd = c.getColumnIndexOrThrow(CalendarContract.Instances.END)
                    val iAllDay = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                    val iCal = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                    while (c.moveToNext()) {
                        val rawTitle = c.getString(iTitle)
                        events += CalendarEvent(
                            title = if (rawTitle.isNullOrBlank()) "(无标题)" else rawTitle,
                            startMillis = c.getLong(iBegin),
                            endMillis = c.getLong(iEnd),
                            isAllDay = c.getInt(iAllDay) == 1,
                            calendarName = c.getString(iCal).orEmpty(),
                        )
                    }
                }
            }
            events
        }
}
