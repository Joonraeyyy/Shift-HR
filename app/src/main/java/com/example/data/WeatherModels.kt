package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val name: String,
    val main: MainData,
    val weather: List<WeatherDescription>,
    val wind: WindData
)

@JsonClass(generateAdapter = true)
data class MainData(
    val temp: Double,
    @Json(name = "feels_like") val feelsLike: Double,
    @Json(name = "temp_min") val tempMin: Double,
    @Json(name = "temp_max") val tempMax: Double,
    val humidity: Int,
    val pressure: Int
)

@JsonClass(generateAdapter = true)
data class WeatherDescription(
    val main: String,
    val description: String,
    val icon: String
)

@JsonClass(generateAdapter = true)
data class WindData(
    val speed: Double
)

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    val list: List<ForecastItem>,
    val city: CityData
)

@JsonClass(generateAdapter = true)
data class ForecastItem(
    val dt: Long,
    val main: MainData,
    val weather: List<WeatherDescription>,
    @Json(name = "dt_txt") val dtTxt: String
)

@JsonClass(generateAdapter = true)
data class CityData(
    val name: String,
    val country: String
)
