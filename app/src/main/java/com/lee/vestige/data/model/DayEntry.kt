package com.lee.vestige.data.model

import java.time.LocalDate

/**
 * A full day's aggregated data: the date plus every section produced by the
 * registered data plugins, already sorted by [DaySection.order].
 */
data class DayEntry(
    val date: LocalDate,
    val sections: List<DaySection>,
)
