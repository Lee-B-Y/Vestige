package com.lee.vestige.data.model

/**
 * A single calendar event instance for a given day, as read from the system
 * Calendar Provider. Times are epoch milliseconds (UTC) as stored by Android.
 */
data class CalendarEvent(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean,
    val calendarName: String,
)
