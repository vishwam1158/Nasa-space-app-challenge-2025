package com.example.farmnavigator.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class NasaData(
    val vegetationIndex: Double,
    val soilMoisture: Double,
    val precipitation: Double,
    val temperatureAnomaly: Double,
    val marketDemand: Double,
    val randomEventChance: Double,
    val randomEventType: String
)


sealed class UiState {
    object Loading : UiState()
    data class Success(val data: NasaData) : UiState()
    data class Error(val message: String) : UiState()
}

class NasaRepository(private val apiService: NasaApiService? = null) {

    suspend fun getFarmData(region: String, season: String): UiState = withContext(Dispatchers.Default) {
        try {
            delay(2000)

            val data = when {
                region == "Gujarat, India" && season == "Summer" -> NasaData(
                    vegetationIndex = 0.55,
                    soilMoisture = 0.40,
                    precipitation = 10.0,
                    temperatureAnomaly = 2.5,
                    marketDemand = 1.5,
                    randomEventChance = 0.7,
                    randomEventType = "heatwave"
                )
                region == "Punjab, India" && season == "Monsoon" -> NasaData(
                    vegetationIndex = 0.88,
                    soilMoisture = 0.95,
                    precipitation = 250.0,
                    temperatureAnomaly = -1.0,
                    marketDemand = 0.8,
                    randomEventChance = 0.4,
                    randomEventType = "pollinators"
                )
                region == "Iowa, USA" && season == "Summer" -> NasaData(
                    vegetationIndex = 0.70,
                    soilMoisture = 0.75,
                    precipitation = 85.0,
                    temperatureAnomaly = 1.8,
                    marketDemand = 1.2,
                    randomEventChance = 0.8,
                    randomEventType = "pests"
                )
                region == "California, USA" && season == "Summer" -> NasaData(
                    vegetationIndex = 0.65,
                    soilMoisture = 0.15,
                    precipitation = 2.0,
                    temperatureAnomaly = 3.0,
                    marketDemand = 1.4,
                    randomEventChance = 0.0,
                    randomEventType = "none"
                )
                else -> NasaData(
                    vegetationIndex = 0.78,
                    soilMoisture = 0.65,
                    precipitation = 60.0,
                    temperatureAnomaly = 0.5,
                    marketDemand = 1.0,
                    randomEventChance = 0.2,
                    randomEventType = "pollinators"
                )
            }
            UiState.Success(data)
        } catch (e: Exception) {
            UiState.Error("Data simulation failed: ${e.message}")
        }
    }
}

