package com.lee.vestige.data.source

import com.lee.vestige.data.model.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Fetches a daily weather summary from the Open-Meteo web API (free, no API key).
 *
 * Uses [HttpURLConnection] + org.json to avoid extra networking dependencies. The
 * forecast endpoint serves a window of roughly the past ~3 months to +16 days; for a
 * date outside that range (or any network error) this returns `null` and the weather
 * section is simply omitted.
 *
 * Note: this sends the coordinates to api.open-meteo.com — the only outbound network
 * call in the app.
 */
class WeatherDataSource {

    suspend fun dailyWeather(lat: Double, lon: Double, date: LocalDate): WeatherInfo? =
        withContext(Dispatchers.IO) {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto&start_date=$date&end_date=$date",
            )

            val body = runCatching {
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                try {
                    if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull() ?: return@withContext null

            parse(body)
        }

    private fun parse(body: String): WeatherInfo? = runCatching {
        val daily = JSONObject(body).optJSONObject("daily") ?: return null
        val codes = daily.optJSONArray("weather_code") ?: return null
        val max = daily.optJSONArray("temperature_2m_max") ?: return null
        val min = daily.optJSONArray("temperature_2m_min") ?: return null
        if (codes.length() == 0) return null

        WeatherInfo(
            minTempC = min.optDouble(0),
            maxTempC = max.optDouble(0),
            weatherCode = codes.optInt(0),
        )
    }.getOrNull()
}
