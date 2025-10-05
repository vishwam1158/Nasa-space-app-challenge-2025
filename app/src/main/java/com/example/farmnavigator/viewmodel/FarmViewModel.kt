package com.example.farmnavigator.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmnavigator.data.NasaData
import com.example.farmnavigator.data.NasaRepository
import com.example.farmnavigator.data.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class ChartDataPoint(val day: Int, val value: Double)

class FarmViewModel(private val repository: NasaRepository = NasaRepository()) : ViewModel() {

    private val _nasaData = MutableStateFlow<UiState>(UiState.Loading)
    val nasaData: StateFlow<UiState> = _nasaData

    private val _prediction = MutableLiveData<String>()
    val prediction: LiveData<String> = _prediction

    private val _predictionColor = MutableLiveData<Color>()
    val predictionColor: LiveData<Color> = _predictionColor

    private val _mainChallengeType = MutableLiveData("none")
    val mainChallengeType: LiveData<String> = _mainChallengeType

    private val _midSeasonEventType = MutableLiveData("none")
    val midSeasonEventType: LiveData<String> = _midSeasonEventType

    private val _chartData = MutableLiveData<List<ChartDataPoint>>()
    val chartData: LiveData<List<ChartDataPoint>> = _chartData

    private val _workingCapital = MutableLiveData(1000)
    val workingCapital: LiveData<Int> = _workingCapital
    private val _expenses = MutableLiveData(0)
    val expenses: LiveData<Int> = _expenses
    private val _revenue = MutableLiveData(0)
    val revenue: LiveData<Int> = _revenue
    private val _profit = MutableLiveData(0)
    val profit: LiveData<Int> = _profit

    private var initialBudget = 1000
    private val plantingCost = 200

    fun fetchNasaData(region: String, season: String) {
        _nasaData.value = UiState.Loading
        _expenses.value = plantingCost
        _workingCapital.value = initialBudget - plantingCost

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.getFarmData(region, season) }
            _nasaData.value = result

            if (result is UiState.Success) {
                _mainChallengeType.value = determineMainChallenge(result.data)
                _chartData.value = generateChartData(result.data)
                if (Random.nextDouble() < result.data.randomEventChance) {
                    _midSeasonEventType.value = result.data.randomEventType
                }
            }
        }
    }

    fun predictCropHealth() {
        val data = (nasaData.value as? UiState.Success)?.data ?: return
        val healthScore = (data.vegetationIndex * 0.5) + (data.soilMoisture * 0.5) - (data.temperatureAnomaly * 0.05)

        val (message, color) = when {
            healthScore >= 0.70 -> Pair("Excellent yield predicted! Your use of data led to a healthy crop.", Color(0xFF4CAF50))
            healthScore >= 0.45 -> Pair("A respectable yield is likely. You successfully navigated some challenges.", Color(0xFFFFC107))
            else -> Pair("Crop health is critical. A significant loss is almost certain due to environmental stress.", Color(0xFFF44336))
        }
        _prediction.value = message
        _predictionColor.value = color
    }

    private fun determineMainChallenge(data: NasaData): String = when {
        data.soilMoisture < 0.20 && data.precipitation < 10.0 && data.temperatureAnomaly > 2.0 -> "wildfire_risk"
        data.soilMoisture < 0.40 && data.precipitation < 20.0 -> "drought"
        data.vegetationIndex < 0.75 && data.temperatureAnomaly > 1.5 -> "pests"
        data.soilMoisture > 0.9 && data.precipitation > 150.0 -> "flooding"
        else -> "none"
    }

    fun getChallengeMessage(challengeType: String): String = when (challengeType) {
        "drought" -> "CRITICAL DROUGHT ALERT: SMAP data shows soil moisture is dangerously low, and GPM confirms a lack of rain. Crop failure is imminent without intervention."
        "pests" -> "PEST INFESTATION WARNING: MODIS temperature data shows a high heat anomaly, creating ideal breeding conditions for pests. NDVI values are starting to drop."
        "flooding" -> "FLOOD EMERGENCY: GPM satellites report extreme rainfall, and SMAP data confirms the ground is oversaturated. Immediate action is required to prevent total crop loss."
        "wildfire_risk" -> "IMMINENT FIRE THREAT: A combination of critically low moisture (SMAP), extreme heat (MODIS), and low precipitation (GPM) has put your farm at extreme risk. VIIRS satellite systems are monitoring fire hotspots in your region."
        else -> "Conditions appear stable. Monitor data for any changes."
    }

    fun applyDecision(decision: String): Boolean {
        val currentCapital = _workingCapital.value ?: 0
        val data = (nasaData.value as? UiState.Success)?.data ?: return false

        val (cost, healthEffect, moistureEffect, tempEffect) = getDecisionEffects(decision)

        if (currentCapital < cost) return false

        _workingCapital.value = currentCapital - cost
        _expenses.value = (_expenses.value ?: 0) + cost

        val updatedData = data.copy(
            vegetationIndex = (data.vegetationIndex + healthEffect).coerceIn(0.0, 1.0),
            soilMoisture = (data.soilMoisture + moistureEffect).coerceIn(0.0, 1.0),
            temperatureAnomaly = (data.temperatureAnomaly + tempEffect).coerceAtLeast(0.0)
        )
        _nasaData.value = UiState.Success(updatedData)
        return true
    }

    private fun getDecisionEffects(decision: String): Quadruple<Int, Double, Double, Double> {
        return when (decision) {
            "pests_spray" -> Quadruple(300, 0.25, 0.0, 0.0)
            "pests_organic" -> Quadruple(150, 0.15, 0.0, 0.0)
            "pests_ignore" -> Quadruple(0, -0.5, 0.0, 0.0) // Harsher penalty
            "heatwave_mist" -> Quadruple(400, 0.1, 0.15, -1.0)
            "heatwave_ignore" -> Quadruple(0, -0.25, -0.1, 0.0)
            "pollinators_protect" -> Quadruple(100, 0.2, 0.0, 0.0)
            "irrigation_invest" -> Quadruple(400, 0.05, 0.30, -0.5)
            "irrigation_low" -> Quadruple(150, 0.0, 0.15, -0.2)
            "irrigation_ignore" -> Quadruple(0, -0.1, -0.25, 0.0)
            "fertilizer_chemical" -> Quadruple(350, 0.35, 0.0, 0.0)
            "fertilizer_organic" -> Quadruple(200, 0.20, 0.0, 0.0)
            "pest_ignore" -> Quadruple(0, -0.5, 0.0, 0.0)
            "drainage_system" -> Quadruple(500, 0.1, -0.40, 0.0)
            "flood_ignore" -> Quadruple(0, -1.0, 0.0, 0.0) // Total loss
            "fire_prevention" -> Quadruple(600, 0.0, 0.0, 0.0)
            "fire_ignore" -> Quadruple(0, -1.0, -1.0, 0.0) // Total loss
            else -> Quadruple(0, 0.0, 0.0, 0.0)
        }
    }

    fun calculateFinalProfit() {
        val data = (nasaData.value as? UiState.Success)?.data ?: return
        val healthFactor = (data.vegetationIndex * data.soilMoisture - (data.temperatureAnomaly * 0.1)).coerceAtLeast(0.0)

        val baseRevenue = initialBudget * 2.0
        val marketVolatility = Random.nextDouble(0.8, 1.2)

        val calculatedRevenue = (baseRevenue * healthFactor * data.marketDemand * marketVolatility).toInt()
        _revenue.value = calculatedRevenue

        val finalProfit = calculatedRevenue - (_expenses.value ?: 0)
        _profit.value = finalProfit

        _workingCapital.value = initialBudget + finalProfit
    }

    private fun generateChartData(currentData: NasaData): List<ChartDataPoint> {
        val days = 7
        val chartPoints = mutableListOf<ChartDataPoint>()
        val random = Random(System.currentTimeMillis())
        for (i in 1..days) {
            val fluctuation = random.nextDouble() * 0.2 - 0.1
            val value = if (i == days) currentData.soilMoisture
            else (currentData.soilMoisture - 0.15 + fluctuation).coerceIn(0.0, 1.0)
            chartPoints.add(ChartDataPoint(i, value))
        }
        return chartPoints
    }

    fun resetGame() {
        _prediction.value = ""
        _predictionColor.value = Color.White
        _mainChallengeType.value = "none"
        _midSeasonEventType.value = "none"
        _nasaData.value = UiState.Loading
        _chartData.value = emptyList()
        _workingCapital.value = initialBudget
        _expenses.value = 0
        _revenue.value = 0
        _profit.value = 0
    }

    fun setFarmType(type: String) {
        initialBudget = if (type == "Industrial") 2500 else 1000
        _workingCapital.value = initialBudget
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

