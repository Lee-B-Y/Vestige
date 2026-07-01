package com.lee.vestige.data.source

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.lee.vestige.data.model.HealthInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads daily health data from Health Connect (the unified, cross-vendor store).
 *
 * Health Connect produces no data itself — it only returns what other apps (Samsung
 * Health, Fitbit, Google Fit, ...) have written. Everything here degrades to `null`
 * when Health Connect is unavailable, permissions are missing, or no data exists, so it
 * never blocks writing a diary.
 */
class HealthDataSource(private val context: Context) {

    /** The read permissions this app needs. */
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun client(): HealthConnectClient? =
        if (isAvailable()) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null

    suspend fun hasAllPermissions(): Boolean {
        val client = client() ?: return false
        val granted = runCatching { client.permissionController.getGrantedPermissions() }.getOrNull()
            ?: return false
        return granted.containsAll(permissions)
    }

    suspend fun readDailyHealth(date: LocalDate): HealthInfo? = withContext(Dispatchers.IO) {
        val client = client() ?: return@withContext null
        if (!hasAllPermissions()) return@withContext null

        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
        // Previous night: covers sleep that crosses midnight into the morning of `date`.
        val sleepStart = date.minusDays(1).atTime(18, 0).atZone(zone).toInstant()
        val sleepEnd = date.atTime(12, 0).atZone(zone).toInstant()

        val steps = runCatching {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd),
                ),
            )[StepsRecord.COUNT_TOTAL]
        }.getOrNull()

        val sleepMinutes = runCatching {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(sleepStart, sleepEnd),
                ),
            )[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()
        }.getOrNull()

        val restingHr = runCatching {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(RestingHeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd),
                ),
            )[RestingHeartRateRecord.BPM_AVG]
        }.getOrNull()

        if (steps == null && sleepMinutes == null && restingHr == null) {
            null
        } else {
            HealthInfo(sleepMinutes = sleepMinutes, steps = steps, restingHeartRate = restingHr)
        }
    }
}
