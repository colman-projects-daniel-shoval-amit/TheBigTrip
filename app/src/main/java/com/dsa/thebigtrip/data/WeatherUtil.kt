package com.dsa.thebigtrip.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object WeatherUtil {

    suspend fun fetchWeather(lat: Double, lon: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon&current_weather=true"
                val json = URL(url).readText()
                val current = JSONObject(json).getJSONObject("current_weather")
                val tempC = current.getDouble("temperature").toInt()
                val code = current.getInt("weathercode")
                val label = weatherLabel(code)
                "$label · ${tempC}°C"
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun weatherLabel(code: Int): String {
        return when (code) {
            0 -> "☀️ Clear"
            1, 2, 3 -> "🌤️ Partly cloudy"
            45, 48 -> "🌫️ Foggy"
            51, 53, 55 -> "🌦️ Drizzle"
            61, 63, 65 -> "🌧️ Rainy"
            71, 73, 75 -> "❄️ Snowy"
            80, 81, 82 -> "🌧️ Showers"
            95, 96, 99 -> "⛈️ Thunderstorm"
            else -> "🌡️ Weather"
        }
    }
}
