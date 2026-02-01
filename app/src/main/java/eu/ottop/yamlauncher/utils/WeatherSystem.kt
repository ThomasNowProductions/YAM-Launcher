package eu.ottop.yamlauncher.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import eu.ottop.yamlauncher.MainActivity
import eu.ottop.yamlauncher.R
import eu.ottop.yamlauncher.settings.SharedPreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class WeatherSystem(private val context: Context) {

    private val sharedPreferenceManager = SharedPreferenceManager(context)
    private val stringUtils = StringUtils()
    private val logger = Logger.getInstance(context)

    suspend fun setGpsLocation(activity: MainActivity) {

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                LocationManager.NETWORK_PROVIDER
            } else {
                LocationManager.GPS_PROVIDER
            }

            locationManager.getCurrentLocation(
                provider,
                null,
                ContextCompat.getMainExecutor(context)
            )

            { location: Location? ->
                if (location != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        sharedPreferenceManager.setWeatherLocation(
                            "latitude=${latitude}&longitude=${longitude}",
                            context.getString(R.string.latest_location)
                        )
                        activity.updateWeatherText()
                    }

                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        activity.updateWeatherText()
                    }
                }
            }
        } catch(_: Exception) {
            return
        }
    }

    // Run within Dispatchers.IO from the outside (doesn't seem to refresh properly otherwise)
    fun getSearchedLocations(searchTerm: String?) : MutableList<Map<String, String>> {
        val foundLocations = mutableListOf<Map<String, String>>()

        val trimmedSearchTerm = searchTerm?.trim().orEmpty()
        if (trimmedSearchTerm.length < 2) return foundLocations

        val encodedSearchTerm = URLEncoder.encode(trimmedSearchTerm, "UTF-8")
        val language = Locale.getDefault().language.takeIf { it.isNotBlank() } ?: "en"
        val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encodedSearchTerm&count=50&language=$language&format=json")
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 8000
            try {
                val stream = if (responseCode in 200..299) inputStream else errorStream
                if (stream == null) return foundLocations

                stream.bufferedReader().use {
                    val response = it.readText()
                    val jsonObject = JSONObject(response)
                    val resultArray = jsonObject.optJSONArray("results") ?: return foundLocations

                    for (i in 0 until resultArray.length()) {
                        val resultObject: JSONObject = resultArray.getJSONObject(i)

                        val latitude = resultObject.optDouble("latitude", Double.NaN)
                        val longitude = resultObject.optDouble("longitude", Double.NaN)
                        if (latitude.isNaN() || longitude.isNaN()) continue

                        foundLocations.add(mapOf(
                            "name" to resultObject.optString("name"),
                            "latitude" to latitude.toString(),
                            "longitude" to longitude.toString(),
                            "country" to resultObject.optString("country", resultObject.optString("country_code","")),
                            "region" to stringUtils.addEndTextIfNotEmpty(resultObject.optString("admin2", resultObject.optString("admin1",resultObject.optString("admin3",""))), ", ")
                        ))
                    }
                }
            }catch (e: Exception){
                logger.e("WeatherSystem", "Error searching locations for '$trimmedSearchTerm'", e)
            }
        }
        return foundLocations
    }

    // Run with Dispatchers.IO from the outside
    fun getTemp() : String {

        val tempUnits = sharedPreferenceManager.getTempUnits()
        var currentWeather = ""

        val location = sharedPreferenceManager.getWeatherLocation()

        if (location != null) {
            if (location.isNotEmpty()) {
                val url =
                    URL("https://api.open-meteo.com/v1/forecast?$location&temperature_unit=${tempUnits}&current=temperature_2m,weather_code")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 8000

                    try {
                        val stream = if (responseCode in 200..299) inputStream else errorStream
                        if (stream == null) return@with

                        stream.bufferedReader().use {
                            val response = it.readText()

                            val jsonObject = JSONObject(response)

                            val currentData = jsonObject.optJSONObject("current") ?: return@use

                            var weatherType = ""

                            when (currentData.optInt("weather_code")) {
                                0, 1 -> {
                                    weatherType = "☀\uFE0E" // Sunny
                                }

                                2, 3, 45, 48 -> {
                                    weatherType = "☁\uFE0E" // Sunny
                                }

                                51, 53, 55, 56, 57, 61, 63, 65, 67, 80, 81, 82 -> {
                                    weatherType = "☂\uFE0E" // Rain
                                }

                                71, 73, 75, 77, 85, 86 -> {
                                    weatherType = "❄\uFE0E" // Snow
                                }

                                95, 96, 99 -> {
                                    weatherType = "⛈\uFE0E" // Thunder
                                }

                            }

                            val temperature = currentData.optInt("temperature_2m", Int.MIN_VALUE)
                            if (temperature != Int.MIN_VALUE) {
                                currentWeather = "$weatherType $temperature"
                            }

                        }

                    } catch(e: Exception) {
                        logger.e("WeatherSystem", "Error fetching weather data", e)
                    }
                }}
        }

        return when (tempUnits) {
            "celsius" -> {
                stringUtils.addEndTextIfNotEmpty(currentWeather, "°C")
            }

            "fahrenheit" -> {
                stringUtils.addEndTextIfNotEmpty(currentWeather, "°F")
            }

            else -> {
                ""
            }
        }

    }
}
