package com.lee.vestige.data.model

/**
 * Daily weather summary for one day at one location, as returned by Open-Meteo.
 * [weatherCode] is a WMO code; temperatures are in degrees Celsius.
 */
data class WeatherInfo(
    val minTempC: Double,
    val maxTempC: Double,
    val weatherCode: Int,
)
