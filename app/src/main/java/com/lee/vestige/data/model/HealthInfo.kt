package com.lee.vestige.data.model

/**
 * Daily health summary read from Health Connect. Every field is nullable — whatever
 * can't be read (no data / no permission) is simply omitted from the note.
 */
data class HealthInfo(
    val sleepMinutes: Long?,      // previous night's total sleep
    val steps: Long?,             // that day's step count
    val restingHeartRate: Long?,  // that day's average resting heart rate (bpm)
)
