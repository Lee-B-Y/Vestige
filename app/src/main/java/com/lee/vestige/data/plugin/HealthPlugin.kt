package com.lee.vestige.data.plugin

import android.content.Context
import com.lee.vestige.R
import com.lee.vestige.data.model.DaySection
import com.lee.vestige.data.source.HealthDataSource
import java.time.LocalDate

/**
 * Produces the "健康 / Health" section, e.g.
 * ```
 * - 睡眠 7 小时 20 分
 * - 步数 8,432
 * - 静息心率 58 bpm
 * ```
 * Ordered between weather (10) and events (20). Returns `null` (section omitted) when
 * Health Connect is unavailable, not connected, or has no data for the day.
 */
class HealthPlugin(
    private val context: Context,
    private val healthDataSource: HealthDataSource,
) : DataPlugin {

    override val key: String = "health"
    override val sectionTitle: String get() = context.getString(R.string.section_health)
    override val order: Int = 15

    override suspend fun fetch(date: LocalDate): DaySection? {
        val info = healthDataSource.readDailyHealth(date) ?: return null

        val lines = buildList {
            info.sleepMinutes?.let {
                add("- ${context.getString(R.string.health_sleep)} ${formatDuration(it)}")
            }
            info.steps?.let {
                add("- ${context.getString(R.string.health_steps)} ${"%,d".format(it)}")
            }
            info.restingHeartRate?.let {
                add("- ${context.getString(R.string.health_resting_hr)} $it bpm")
            }
        }
        if (lines.isEmpty()) return null
        return DaySection(key = key, title = sectionTitle, order = order, lines = lines)
    }

    private fun formatDuration(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) {
            context.getString(R.string.health_duration_hm, h, m)
        } else {
            context.getString(R.string.health_duration_m, m)
        }
    }
}
