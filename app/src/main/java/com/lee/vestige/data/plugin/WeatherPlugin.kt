package com.lee.vestige.data.plugin

import android.content.Context
import com.lee.vestige.R
import com.lee.vestige.data.model.DaySection
import com.lee.vestige.data.source.LocationProvider
import com.lee.vestige.data.source.WeatherDataSource
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Produces the "天气 / Weather" section, e.g. `- 晴 18°C ~ 26°C`.
 *
 * Returns `null` (section omitted) whenever weather can't be determined: location
 * permission denied, no cached location, no network, or the date is outside the API's
 * supported range. Ordered before events so the day note reads weather → events → notes.
 */
class WeatherPlugin(
    private val context: Context,
    private val locationProvider: LocationProvider,
    private val weatherDataSource: WeatherDataSource,
) : DataPlugin {

    override val sectionTitle: String get() = context.getString(R.string.section_weather)
    override val order: Int = 10

    override suspend fun fetch(date: LocalDate): DaySection? {
        val (lat, lon) = locationProvider.lastKnownLocation() ?: return null
        val weather = weatherDataSource.dailyWeather(lat, lon, date) ?: return null

        val condition = context.getString(conditionRes(weather.weatherCode))
        val line = "- $condition ${weather.minTempC.roundToInt()}°C ~ ${weather.maxTempC.roundToInt()}°C"
        return DaySection(title = sectionTitle, order = order, lines = listOf(line))
    }

    /** Maps a WMO weather code to a localized condition string resource. */
    private fun conditionRes(code: Int): Int = when (code) {
        0 -> R.string.weather_clear
        1, 2, 3 -> R.string.weather_cloudy
        45, 48 -> R.string.weather_fog
        51, 53, 55, 56, 57 -> R.string.weather_drizzle
        61, 63, 65, 66, 67 -> R.string.weather_rain
        71, 73, 75, 77, 85, 86 -> R.string.weather_snow
        80, 81, 82 -> R.string.weather_showers
        95, 96, 99 -> R.string.weather_thunderstorm
        else -> R.string.weather_unknown
    }
}
