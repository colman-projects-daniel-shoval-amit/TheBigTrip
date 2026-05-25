package com.dsa.thebigtrip.data.weather

import com.dsa.thebigtrip.data.post.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeatherRepository {

    private val cache = mutableMapOf<String, String>()

    companion object {
        val shared = WeatherRepository()
    }

    suspend fun getWeatherSummary(post: Post): String {
        val latitude = post.latitude ?: return "Weather unavailable"
        val longitude = post.longitude ?: return "Weather unavailable"
        if (post.createdAt <= 0) return "Weather unavailable"

        val postDate = Instant.ofEpochMilli(post.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val cacheKey = "${latitude},${longitude},${postDate}"

        cache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            val summary = fetchWeatherSummary(latitude, longitude, postDate)
            cache[cacheKey] = summary
            summary
        }
    }

    private fun fetchWeatherSummary(latitude: Double, longitude: Double, postDate: LocalDate): String {
        val date = postDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endpoint = getEndpointForDate(postDate)
        val url = URL(
            "$endpoint?latitude=$latitude&longitude=$longitude" +
                    "&start_date=$date&end_date=$date" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto"
        )

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
        }

        return try {
            if (connection.responseCode !in 200..299) {
                return "Weather unavailable"
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            parseWeatherSummary(response, postDate)
        } finally {
            connection.disconnect()
        }
    }

    private fun getEndpointForDate(postDate: LocalDate): String {
        val today = LocalDate.now()

        return if (postDate.isBefore(today.minusDays(5))) {
            "https://archive-api.open-meteo.com/v1/archive"
        } else {
            "https://api.open-meteo.com/v1/forecast"
        }
    }

    private fun parseWeatherSummary(response: String, postDate: LocalDate): String {
        val daily = JSONObject(response).getJSONObject("daily")
        val code = daily.getJSONArray("weather_code").optInt(0)
        val maxTemp = daily.getJSONArray("temperature_2m_max").optDouble(0)
        val minTemp = daily.getJSONArray("temperature_2m_min").optDouble(0)
        val dateLabel = postDate.format(DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()))

        return String.format(
            Locale.getDefault(),
            "Weather %s: %s, %.0fC / %.0fC",
            dateLabel,
            describeWeatherCode(code),
            maxTemp,
            minTemp
        )
    }

    private fun describeWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Fog"
            51, 53, 55, 56, 57 -> "Drizzle"
            61, 63, 65, 66, 67 -> "Rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Showers"
            85, 86 -> "Snow showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Weather"
        }
    }
}
