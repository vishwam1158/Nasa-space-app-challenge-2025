package com.example.farmnavigator.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.farmnavigator.R
import com.example.farmnavigator.data.NasaData
import com.example.farmnavigator.data.UiState
import com.example.farmnavigator.viewmodel.ChartDataPoint
import com.example.farmnavigator.viewmodel.FarmViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.graphicsLayer
import com.example.farmnavigator.ui.theme.Brown40
import com.example.farmnavigator.ui.theme.Brown80
import com.example.farmnavigator.ui.theme.Green40

enum class AppScreen {
    SIMULATION,
    LEARNING_HUB
}

enum class GameState {
    FARM_SETUP,
    IDLE,
    PLANTING,
    GROWING_SEASON,
    MID_SEASON_EVENT,
    CONSULTING_NASA_DATA,
    MAIN_CHALLENGE,
    PREDICTING_OUTCOME,
    HARVESTED
}

data class MidSeasonEventUI(
    val title: String,
    val message: String,
    @DrawableRes val imageRes: Int,
    val choices: @Composable () -> Unit
)

sealed class LearningTopic(val title: String, val description: String, @DrawableRes val iconRes: Int) {
    object NDVI : LearningTopic("NDVI", "Monitoring Crop Health from Space", R.drawable.ic_ndvi)
    object SoilMoisture : LearningTopic("Soil Moisture", "Knowing When to Water with SMAP", R.drawable.ic_soil_moisture)
    object LST : LearningTopic("Land Surface Temp", "Detecting Heat Stress with MODIS", R.drawable.ic_temperature)
    object GPM : LearningTopic("Precipitation", "Tracking Rain and Floods with GPM", R.drawable.ic_precipitation)
}


val regions = listOf("Gujarat, India", "Punjab, India", "Maharashtra, India", "Rajasthan, India", "California, USA", "Iowa, USA")
val seasons = listOf("Summer", "Monsoon", "Autumn", "Winter")

@Composable
fun FarmNavigatorApp(viewModel: FarmViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.SIMULATION) }
    var learningTopic by remember { mutableStateOf<LearningTopic?>(null) }


    when {
        learningTopic != null -> {
            LearningDetailScreen(topic = learningTopic!!, onClose = { learningTopic = null })
        }
        currentScreen == AppScreen.LEARNING_HUB -> {
            LearningHubScreen(
                onTopicSelected = { topic -> learningTopic = topic },
                onClose = { currentScreen = AppScreen.SIMULATION }
            )
        }
        else -> {
            SimulationScreen(viewModel = viewModel, onShowLearningHub = { currentScreen = AppScreen.LEARNING_HUB })
        }
    }
}


@Composable
fun SimulationScreen(viewModel: FarmViewModel, onShowLearningHub: () -> Unit) {
    var gameState by remember { mutableStateOf(GameState.FARM_SETUP) }
    var selectedRegion by remember { mutableStateOf(regions[0]) }
    var selectedSeason by remember { mutableStateOf(seasons[0]) }
    var showInfoPopup by remember { mutableStateOf(false) }

    val uiState by viewModel.nasaData.collectAsState()
    val workingCapital by viewModel.workingCapital.observeAsState(1000)
    val mainChallengeType by viewModel.mainChallengeType.observeAsState("none")
    val midSeasonEventType by viewModel.midSeasonEventType.observeAsState("none")

    LaunchedEffect(gameState, uiState) {
        when (gameState) {
            GameState.PLANTING -> {
                delay(2500)
                gameState = GameState.GROWING_SEASON
            }
            GameState.GROWING_SEASON -> {
                delay(3000)
                gameState = if (midSeasonEventType != "none") GameState.MID_SEASON_EVENT else GameState.CONSULTING_NASA_DATA
            }
            GameState.CONSULTING_NASA_DATA -> {
                if (uiState is UiState.Success) {
                    delay(2000)
                    if (mainChallengeType != "none") {
                        gameState = GameState.MAIN_CHALLENGE
                    } else {
                        viewModel.predictCropHealth()
                        gameState = GameState.PREDICTING_OUTCOME
                    }
                }
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
Image(
            painter = painterResource(id = R.drawable.farm_background),
            contentDescription = "Farm background",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.5f),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            TopAppBar(onInfoClick = { showInfoPopup = true }, budget = workingCapital ?: 1000)

            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                GameContent(
                    gameState = gameState,
                    viewModel = viewModel,
                    uiState = uiState,
                    selectedRegion = selectedRegion,
                    selectedSeason = selectedSeason,
                    onRegionChange = { selectedRegion = it },
                    onSeasonChange = { selectedSeason = it },
                    onStateChange = { newState -> gameState = newState },
                    onShowLearningHub = onShowLearningHub
                )
            }
        }
    }

    if (showInfoPopup) {
        MissionBriefing(onClose = { showInfoPopup = false })
    }
}

@Composable
fun GameContent(
    gameState: GameState,
    viewModel: FarmViewModel,
    uiState: UiState,
    selectedRegion: String,
    selectedSeason: String,
    onRegionChange: (String) -> Unit,
    onSeasonChange: (String) -> Unit,
    onStateChange: (GameState) -> Unit,
    onShowLearningHub: () -> Unit
) {
    when (gameState) {
        GameState.FARM_SETUP -> FarmSetupScreen(
            onStart = { farmType ->
                viewModel.setFarmType(farmType)
                onStateChange(GameState.IDLE)
            },
            onShowLearningHub = onShowLearningHub
        )
        GameState.IDLE -> IdleScreen(selectedRegion, selectedSeason, onRegionChange, onSeasonChange, onPlant = {
            onStateChange(GameState.PLANTING)
            viewModel.fetchNasaData(selectedRegion, selectedSeason)
        })
        GameState.PLANTING -> StatusScreen(message = "Crops planted! Initial costs deducted. The growing season begins...")
        GameState.GROWING_SEASON -> StatusScreen(message = "Crops are growing. Monitoring satellite feeds for any changes...")
        GameState.MID_SEASON_EVENT -> MidSeasonEventScreen(viewModel = viewModel, onDecisionMade = {
            onStateChange(GameState.CONSULTING_NASA_DATA)
        })
        GameState.CONSULTING_NASA_DATA -> StatusScreen(message = "Analyzing mid-season satellite data for the main forecast...")
        GameState.MAIN_CHALLENGE -> ChallengeScreen(viewModel = viewModel, onDecisionMade = {
            viewModel.predictCropHealth()
            onStateChange(GameState.PREDICTING_OUTCOME)
        })
        GameState.PREDICTING_OUTCOME -> {
            val predictionText by viewModel.prediction.observeAsState("")
            val predictionColor by viewModel.predictionColor.observeAsState(Color.White)
            val chartData by viewModel.chartData.observeAsState(emptyList())
            PredictionScreen(chartData, predictionText, predictionColor, uiState, onHarvest = {
                viewModel.calculateFinalProfit()
                onStateChange(GameState.HARVESTED)
            })
        }
        GameState.HARVESTED -> HarvestScreen(viewModel = viewModel, onReplay = {
            viewModel.resetGame()
            onStateChange(GameState.FARM_SETUP)
        })
    }
}

@Composable
fun MidSeasonEventScreen(viewModel: FarmViewModel, onDecisionMade: () -> Unit) {
    val midSeasonEventType by viewModel.midSeasonEventType.observeAsState("none")
    val workingCapital by viewModel.workingCapital.observeAsState(0)
    var decisionMade by remember { mutableStateOf(false) }

    val eventData = when (midSeasonEventType) {
        "pests" -> MidSeasonEventUI("Pest Activity Detected!", "High temperatures are causing a surge in pest activity. They could damage your crops.", R.drawable.bug_attack, @Composable {
            DecisionButton("Preventative Spray ($300)", 300, workingCapital ?: 0) { if (viewModel.applyDecision("pests_spray")) onDecisionMade() }
            DecisionButton("Organic Ladybugs ($150)", 150, workingCapital ?: 0) { if (viewModel.applyDecision("pests_organic")) onDecisionMade() }
            DecisionButton("Ignore Threat ($0)", 0, workingCapital ?: 0) { if (viewModel.applyDecision("pests_ignore")) onDecisionMade() }
        })
        "heatwave" -> MidSeasonEventUI("Heat Wave Approaching!", "MODIS data shows a severe temperature anomaly. This will stress the crops and rapidly decrease soil moisture.", R.drawable.heat_wave, @Composable {
            DecisionButton("Deploy Cooling Mist ($400)", 400, workingCapital ?: 0) { if (viewModel.applyDecision("heatwave_mist")) onDecisionMade() }
            DecisionButton("Hope It's Short ($0)", 0, workingCapital ?: 0) { if (viewModel.applyDecision("heatwave_ignore")) onDecisionMade() }
        })
        "pollinators" -> MidSeasonEventUI("Beneficial Pollinators Spotted!", "Conditions are perfect for pollinators like bees, which will significantly boost your final yield if protected.", R.drawable.bountiful_harvest, @Composable {
            DecisionButton("Protect Habitat ($100)", 100, workingCapital ?: 0) { if (viewModel.applyDecision("pollinators_protect")) onDecisionMade() }
            DecisionButton("Do Nothing ($0)", 0, workingCapital ?: 0) { onDecisionMade() }
        })
        else -> MidSeasonEventUI("Quiet Season", "The growing season is proceeding without any unusual events so far.", R.drawable.growing_crop, @Composable {
            Button(onClick = onDecisionMade) { Text("Continue") }
        })
    }

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = eventData.title, style = MaterialTheme.typography.headlineSmall, color = Color.Yellow, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Image(painter = painterResource(id = eventData.imageRes), contentDescription = eventData.title, modifier = Modifier.size(120.dp))
            Spacer(Modifier.height(16.dp))
            Text(text = eventData.message, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))

            if (decisionMade) {
                CircularProgressIndicator()
            } else {
                eventData.choices()
            }
        }
    }
}

@Composable
fun HarvestScreen(viewModel: FarmViewModel, onReplay: () -> Unit) {
    val revenue by viewModel.revenue.observeAsState(0)
    val expenses by viewModel.expenses.observeAsState(0)
    val profit by viewModel.profit.observeAsState(0)
    val finalCapital by viewModel.workingCapital.observeAsState(0)
    val imageRes = if (profit >= 0) R.drawable.bountiful_harvest else R.drawable.withered_crop

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("End of Season Report", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Image(painterResource(imageRes), "Harvest Result", modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp)))
            Spacer(Modifier.height(16.dp))
            FinancialReport(revenue, expenses, profit, finalCapital ?: 0)
            Spacer(Modifier.height(24.dp))
            Button(onReplay, modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)) {
                Text("Start New Season", fontSize = 20.sp)
            }
        }
    }
}


@Composable
fun TopAppBar(onInfoClick: () -> Unit, budget: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Farm Navigator", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Capital: $$budget", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD700), modifier = Modifier.padding(end = 8.dp))
            IconButton(onClick = onInfoClick) {
                Image(painter = painterResource(id = R.drawable.ic_info), contentDescription = "info")
            }
        }
    }
}

@Composable
fun FarmSetupScreen(onStart: (String) -> Unit, onShowLearningHub: () -> Unit) {
    var selectedFarm by remember { mutableStateOf("Small-Scale") }
    val budget = if (selectedFarm == "Industrial") 2500 else 1000

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LearningHubCard(onClick = onShowLearningHub)

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Welcome, Navigator!", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FarmTypeButton("Small-Scale", selectedFarm == "Small-Scale") { selectedFarm = it }
                    FarmTypeButton("Industrial", selectedFarm == "Industrial") { selectedFarm = it }
                }
                Spacer(Modifier.height(16.dp))
                Text("Starting Capital: $$budget", fontWeight = FontWeight.SemiBold, color = Color.Yellow)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { onStart(selectedFarm) }, modifier = Modifier.height(50.dp)) {
                    Text("Start Simulation", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun LearningHubCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors =  CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚀 Learning Hub 🚀", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(8.dp))
            Text(
                "Get mission-ready! Learn about the NASA data that will guide your decisions.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }

    }
}


@Composable
fun FarmTypeButton(type: String, isSelected: Boolean, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(type) },
        colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
        modifier = Modifier.width(150.dp)
    ) {
        Text(type)
    }
}

@Composable
fun IdleScreen(selectedRegion: String, selectedSeason: String, onRegionChange: (String) -> Unit, onSeasonChange: (String) -> Unit, onPlant: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Select Location & Season", color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Dropdown("Region", regions, selectedRegion, onRegionChange)
                Dropdown("Season", seasons, selectedSeason, onSeasonChange)
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = onPlant, modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)) {
                Text("Plant Crop", fontSize = 20.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(label: String, items: List<String>, selectedItem: String, onSelectionChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color.LightGray) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded, /*tint = Color.LightGray*/) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.LightGray
            ),
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onSelectionChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatusScreen(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.growing_crop), "Growing Crop", modifier = Modifier.size(200.dp))
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator(color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text(message, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ChallengeScreen(viewModel: FarmViewModel, onDecisionMade: () -> Unit) {
    val challengeType by viewModel.mainChallengeType.observeAsState("none")
    val workingCapital by viewModel.workingCapital.observeAsState(0)
    var decisionMade by remember { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("!!! URGENT DATA ALERT !!!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(viewModel.getChallengeMessage(challengeType), color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            if (decisionMade) {
                Text("Decision Applied! Predicting final outcome...", color = Color.Yellow, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
            } else {
                val onDecision: () -> Unit = { decisionMade = true; onDecisionMade() }
                when (challengeType) {
                    "drought" -> DroughtDecisionButtons(viewModel, workingCapital ?: 0, onDecision)
                    "pests" -> PestDecisionButtons(viewModel, workingCapital ?: 0, onDecision)
                    "flooding" -> FloodingDecisionButtons(viewModel, workingCapital ?: 0, onDecision)
                    "wildfire_risk" -> WildfireDecisionButtons(viewModel, workingCapital ?: 0, onDecision)
                }
            }
        }
    }
}

@Composable
fun DroughtDecisionButtons(vm: FarmViewModel, budget: Int, onDecision: () -> Unit) {
    DecisionButton("Invest in Advanced Irrigation ($400)", 400, budget) { if (vm.applyDecision("irrigation_invest")) onDecision() }
    DecisionButton("Low-Cost Water Rations ($150)", 150, budget) { if (vm.applyDecision("irrigation_low")) onDecision() }
    DecisionButton("Ignore, Hope for Rain ($0)", 0, budget) { if (vm.applyDecision("irrigation_ignore")) onDecision() }
}

@Composable
fun PestDecisionButtons(vm: FarmViewModel, budget: Int, onDecision: () -> Unit) {
    DecisionButton("Chemical Treatment ($350)", 350, budget) { if (vm.applyDecision("fertilizer_chemical")) onDecision() }
    DecisionButton("Organic Pest Control ($200)", 200, budget) { if (vm.applyDecision("fertilizer_organic")) onDecision() }
    DecisionButton("Do Nothing, Risk Yield ($0)", 0, budget) { if (vm.applyDecision("pest_ignore")) onDecision() }
}

@Composable
fun FloodingDecisionButtons(vm: FarmViewModel, budget: Int, onDecision: () -> Unit) {
    DecisionButton("Activate Emergency Drainage ($500)", 500, budget) { if (vm.applyDecision("drainage_system")) onDecision() }
    DecisionButton("Wait and See ($0)", 0, budget) { if (vm.applyDecision("flood_ignore")) onDecision() }
}

@Composable
fun WildfireDecisionButtons(vm: FarmViewModel, budget: Int, onDecision: () -> Unit) {
    DecisionButton("Create Firebreak ($600)", 600, budget) { if (vm.applyDecision("fire_prevention")) onDecision() }
    DecisionButton("Take the Risk ($0)", 0, budget) { if (vm.applyDecision("fire_ignore")) onDecision() }
}

@Composable
fun DecisionButton(label: String, cost: Int, budget: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = budget >= cost,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 4.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label)
    }
}


@Composable
fun PredictionScreen(chartData: List<ChartDataPoint>, predictionText: String, predictionColor: Color, uiState: UiState, onHarvest: () -> Unit) {
    val data = (uiState as? UiState.Success)?.data ?: return

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DATA ANALYSIS COMPLETE", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF03A9F4))
            Spacer(Modifier.height(16.dp))
            Text("Soil Moisture Trends (Simulated)", color = Color.White, fontWeight = FontWeight.SemiBold)
            DataVisualization(chartData)
            Spacer(Modifier.height(16.dp))
            DataDisplay(data)
            Spacer(Modifier.height(24.dp))
            Text("PREDICTED OUTCOME:", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(predictionText, color = predictionColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(24.dp))
            Button(onHarvest, modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) {
                Text("Proceed to Harvest", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun DataDisplay(data: NasaData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("NDVI (Vegetation Index): ${"%.2f".format(data.vegetationIndex)}", color = Color.White)
        Text("Soil Moisture: ${"%.1f".format(data.soilMoisture * 100)}%", color = Color.White)
        Text("Precipitation: ${"%.1f".format(data.precipitation)} mm", color = Color.White)
        Text("Temp Anomaly: +${"%.1f".format(data.temperatureAnomaly)}°C", color = if (data.temperatureAnomaly > 1.5) Color.Red else Color.White)
    }
}

@Composable
fun FinancialReport(revenue: Int, expenses: Int, profit: Int, finalCapital: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FinancialRow("Total Revenue:", "$$revenue", Color(0xFF4CAF50))
        FinancialRow("Total Expenses:", "$$expenses", Color(0xFFF44336))
        FinancialRow(if (profit >= 0) "Season Profit:" else "Season Loss:", "$${abs(profit)}", if (profit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336))
        Divider(color = Color.Gray, thickness = 2.dp, modifier = Modifier.padding(vertical = 4.dp))
        FinancialRow("Final Capital:", "$$finalCapital", Color.Yellow, isBold = true)
    }
}

@Composable
fun FinancialRow(label: String, value: String, valueColor: Color, isBold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, fontSize = if(isBold) 18.sp else 16.sp)
        Text(value, color = valueColor, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, fontSize = if(isBold) 18.sp else 16.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionBriefing(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("Mission Briefing", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                actions = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                })
        }) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFFF0F4F8))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BriefingSection(
                    "NDVI: Checking Plant Health", stringResource(R.string.ndvi_desc),
                    "Case Study: Locust Swarms", stringResource(R.string.case_study_locusts), R.drawable.case_study_locusts
                )
                BriefingSection(
                    "Soil Moisture: Are Plants Thirsty?", stringResource(R.string.soil_moisture_desc),
                    "Case Study: California Drought", stringResource(R.string.case_study_drought), R.drawable.withered_crop_drought_consequence
                )
            }
        }
    }
}

@Composable
fun BriefingSection(title: String, description: String, caseStudyTitle: String, caseStudyText: String, @DrawableRes imageRes: Int) {
    Card(elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = Brown40)
            Spacer(Modifier.height(16.dp))
            Image(painterResource(imageRes), title, modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(16.dp))
            Text(caseStudyTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Brown40)
            Spacer(Modifier.height(4.dp))
            Text(caseStudyText, style = MaterialTheme.typography.bodyMedium, color = Brown40)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningHubScreen(onTopicSelected: (LearningTopic) -> Unit, onClose: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learning Hub", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Simulation", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        val topics = listOf(LearningTopic.NDVI, LearningTopic.SoilMoisture, LearningTopic.LST, LearningTopic.GPM)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF0F4F8))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(topics.size) { index ->
                val topic = topics[index]
                LearningTopicCard(
                    topic = topic,
                    onClick = { onTopicSelected(topic) }
                )
            }
        }
    }
}

@Composable
fun LearningTopicCard(topic: LearningTopic, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Brown80)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(id = topic.iconRes), contentDescription = "Image", modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray), contentScale = ContentScale.Crop)

           /* Icon(
                painter = painterResource(id = topic.iconRes),
                contentDescription = topic.title,
                modifier = Modifier.size(40.dp), // Slightly smaller icon
                tint = MaterialTheme.colorScheme.primary
            )*/
            Spacer(Modifier.height(8.dp))
            Text(
                topic.title,
                style = MaterialTheme.typography.titleMedium,
                color = Brown40,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                topic.description,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Brown40,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LearningDetailScreen(topic: LearningTopic, onClose: () -> Unit) {
    val slides = remember(topic) { getSlidesForTopic(topic) }
    val pagerState = rememberPagerState(pageCount = { slides.size })

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(topic.title, color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Hub", tint = Color.White)
                        }
                    }
                )
            },
            bottomBar = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(slides.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(12.dp)
                        )
                    }
                }
            }
        ) { padding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) { page ->
                val slide = slides[page]
                LearningSlideContent(slide = slide)
            }
        }
    }
}

@Composable
fun LearningSlideContent(slide: LearningSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(slide.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(id = slide.imageRes),
            contentDescription = slide.title,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray),
            contentScale = ContentScale.None
        )
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(slide.content, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Justify)

      /*  slide.videoPrompt?.let {
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.height(8.dp))
            VideoPlayerPlaceholder()
        }*/
    }
}

data class LearningSlide(
    val title: String,
    val content: String,
    @DrawableRes val imageRes: Int
)

fun getSlidesForTopic(topic: LearningTopic): List<LearningSlide> {
    return when (topic) {
        LearningTopic.NDVI -> listOf(
            LearningSlide(
                title = "What is NDVI?",
                content = "NDVI (Normalized Difference Vegetation Index) is a simple indicator that uses satellite imagery to measure the health of vegetation. Healthy plants reflect more near-infrared light and absorb more visible light. NDVI gives a score from -1 to +1, where higher values mean healthier, greener vegetation.",
                imageRes = R.drawable.cs_1_1
            ),
            LearningSlide(
                title = "How NASA Measures It",
                content = "NASA's Terra and Aqua satellites carry the MODIS instrument, which scans the entire Earth every 1-2 days. Landsat satellites provide even more detailed images. They capture different bands of light, and scientists process this data to create the NDVI maps that farmers use to monitor crop health over large areas.",
                imageRes = R.drawable.cs_1_2
            ),
            LearningSlide(
                title = "Case Study: Locust Swarms in Gujarat",
                content = "In 2019-2020, Gujarat faced one of the worst locust attacks in decades. By analyzing near real-time NDVI data, authorities could identify areas where vegetation was suddenly disappearing, indicating where the swarms were feeding. This allowed them to predict the locusts' path and direct control efforts more effectively, saving countless farms from devastation.",
                imageRes = R.drawable.cs_1_3
            )
        )
        LearningTopic.SoilMoisture -> listOf(
            LearningSlide(
                title = "What is Soil Moisture?",
                content = "Soil moisture is the water stored in the spaces between soil particles. It is a critical component for plant growth, as it helps transport nutrients from the soil to the plant's roots. Too little water can lead to drought stress and crop failure, while too much can lead to root rot and flooding.",
                imageRes = R.drawable.cs_2_1
            ),
            LearningSlide(
                title = "NASA's SMAP Mission",
                content = "NASA's SMAP (Soil Moisture Active Passive) satellite was launched in 2015 to create high-resolution global maps of soil moisture. It uses a radiometer to measure natural microwave emissions from the soil, which vary depending on the amount of water present. This provides farmers with a crucial tool for managing irrigation more efficiently.",
                imageRes = R.drawable.cs_2_2
            ),
            LearningSlide(
                title = "Case Study: Conserving Water in Punjab",
                content = "The states of Punjab and Haryana are India's breadbasket, but they rely heavily on groundwater for irrigation, leading to rapidly depleting aquifers. SMAP data helps farmers in this region with 'precision irrigation.' By knowing the exact moisture level of their soil, they can avoid overwatering, saving massive amounts of water and electricity, and ensuring the long-term sustainability of their farms.",
                imageRes = R.drawable.cs_2_3
            )
        )
        LearningTopic.LST -> listOf(
            LearningSlide(
                title = "What is Land Surface Temperature?",
                content = "Land Surface Temperature (LST) is exactly what it sounds like: the temperature of the ground. It is different from the air temperature reported in weather forecasts. LST is a vital sign for our planet and a key indicator of plant stress. Just like humans get a fever when sick, plants get hotter when they are stressed, especially from a lack of water.",
                imageRes = R.drawable.cs_3_1
            ),
            LearningSlide(
                title = "Detecting 'Plant Fever' with MODIS",
                content = "The MODIS instrument aboard NASA's Terra and Aqua satellites measures thermal infrared radiation—the heat—emitted from the Earth's surface. This allows scientists to create detailed LST maps. A high LST during a growing season is an early warning sign that crops are under heat and water stress, often days before the plants show visible signs of wilting.",
                imageRes = R.drawable.cs_3_2
            ),
            LearningSlide(
                title = "Case Study: Drought Early Warning in Central India",
                content = "Regions like Vidarbha and Marathwada in Central India are highly prone to drought and heatwaves. By monitoring LST data, disaster management agencies can identify areas where ground temperatures are abnormally high. This serves as an early warning for impending drought, allowing for the timely implementation of relief measures, water conservation policies, and support for farmers before a full-blown crisis develops.",
                imageRes = R.drawable.cs_3_3
            )
        )
        LearningTopic.GPM -> listOf(
            LearningSlide(
                title = "Why Track Global Precipitation?",
                content = "Precipitation—rain, snow, and hail—is the lifeblood of our planet, but it can also be a destructive force. Knowing where, when, and how much it's precipitating is essential for everything from agriculture and freshwater management to forecasting floods and landslides.",
                imageRes = R.drawable.cs_4_1
            ),
            LearningSlide(
                title = "The GPM Mission",
                content = "The Global Precipitation Measurement (GPM) mission is an international constellation of satellites with the GPM Core Observatory at its heart. This advanced observatory uses a special radar and microwave imager to measure the size of precipitation particles from space. This helps scientists understand the structure of storms and improve weather forecasts.",
                imageRes = R.drawable.cs_4_2
            ),
            LearningSlide(
                title = "Case Study: Monsoon Flood Warnings in Assam",
                content = "Assam, with the mighty Brahmaputra river, faces annual, devastating floods during the monsoon season. The GPM mission provides critical data on the intensity of rainfall in the river's vast catchment area. This information is fed into hydrological models to predict the river's flow and issue accurate, timely flood warnings, giving communities precious hours or days to evacuate and protect their lives and livestock.",
                imageRes = R.drawable.cs_4_3
            )
        )
    }
}

