package com.calebteo.project.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import com.calebteo.project.R
import com.calebteo.project.data.AppInfo
import com.calebteo.project.data.AppRepository
import com.calebteo.project.data.LauncherMetricsStore
import com.calebteo.project.data.WallpaperCategory
import com.calebteo.project.data.WallpaperCategoryRanking
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceDataAnalyScreen(
    onBack: () -> Unit,
    apps: List<AppInfo>,
    usageCountMap: Map<String, Int>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frequent Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Synapse Logic",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Image(
                painter = painterResource(R.drawable.analysing_frequents),
                contentScale = ContentScale.FillWidth,
                contentDescription = null
            )
            Text(
                text = "Apps in Top Bar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "     The launcher analyses your usage patterns to identify if there is a set of your top apps that you use twice as much as other apps, and puts them to the top.",
                style = MaterialTheme.typography.bodyLarge
            )
            // Spacer(Modifier.height(2.dp))
            Text(
                text = "     If you have more than four apps among your most frequented, your top three will be displayed in the top bar.",
                style = MaterialTheme.typography.bodyLarge
            )
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Apps per Grid Row",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "     For each grid row in layouts with grids, we will include between 3 and 6 apps, whichever is closer to 5% of total apps. ",
                style = MaterialTheme.typography.bodyLarge
            )
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
            AppUsageBarChart(
                apps = apps,
                usageCountMap = usageCountMap,
                minUsage = 0,
                minimumVisibleApps = apps.size
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperDataAnalyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val wallpaperScores by produceState<Map<WallpaperCategory, Int>?>(
        initialValue = null,
        key1 = context
    ) {
        value = WallpaperCategoryRanking.getAllScores(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallpaper") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Synapse Logic",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Image(
                painter = painterResource(R.drawable.analysing_frequents_4),
                contentScale = ContentScale.FillWidth,
                contentDescription = null
            )
            Text(
                text = "Wallpaper Selection",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "     Occasionally, we will ask you if you like a certain wallpaper. This will contribute to the our understanding of your perception of that category.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "     We will recommend you wallpapers from your favourite categories more frequently. When we're more sure about your preferences, we'll prompt you about your favourite wallpapers less!",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "     Fun Fact: We have around 80 wallpapers in our online repository, and we are constantly adding more!.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
            wallpaperScores?.let { scores ->
                WallpaperPreferenceBarChart(scores = scores)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WallpaperPreferenceBarChart(
    scores: Map<WallpaperCategory, Int>
) {
    val rankedCategories = remember(scores) {
        scores.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value.coerceIn(0, 30) }
    }
    val confirmationScore = remember(scores) {
        wallpaperConfirmationPromptPercent(scores)
    }
    val isDarkTheme = isSystemInDarkTheme()
    val confirmationBackground = if (isDarkTheme) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val confirmationTextColor = if (isDarkTheme) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "Wallpaper Preferences",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rankedCategories.forEach { (category, score) ->
                MetricBreakdownBar(
                    label = formatWallpaperCategoryName(category),
                    score = score,
                    maxScore = 30
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = confirmationBackground,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val workingscore = (100 - confirmationScore)
                Text(
                    text = "Confirmation Score $workingscore%",
                    style = MaterialTheme.typography.titleSmall,
                    color = confirmationTextColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Percentage of wallpapers where we'll prompt your preferences is $confirmationScore% ",
                    style = MaterialTheme.typography.bodySmall,
                    color = confirmationTextColor
                )
            }
        }
    }
}

private fun formatWallpaperCategoryName(category: WallpaperCategory): String {
    return category.id
        .split("_")
        .joinToString(" ") { part ->
            part.replaceFirstChar { it.uppercase() }
        }
}

private fun wallpaperConfirmationPromptPercent(
    scores: Map<WallpaperCategory, Int>
): Int {
    val values = scores.values.map { it.toDouble() }
    val mean = values.average()
    val variance = values
        .map { (it - mean).pow(2) }
        .average()
    val maxVariance = 210.25
    val confidence = (variance / maxVariance)
        .coerceIn(0.0, 1.0)
    val probability = (0.20 - 0.16 * confidence.pow(2.0))
        .coerceIn(0.04, 0.20)
    return (probability * 100).toInt()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemDataAnalyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val metricsStore = remember(context) { LauncherMetricsStore(context) }
    val scoreBreakdown by produceState<LauncherMetricsStore.SystemScoreBreakdown?>(
        initialValue = null,
        key1 = metricsStore
    ) {
        value = metricsStore.getSystemScoreBreakdown()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Layout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Synapse Logic",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Image(
                painter = painterResource(R.drawable.analysing_frequents_2),
                contentScale = ContentScale.FillWidth,
                contentDescription = null
            )
            Text(
                text = "Synapse Layout",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "The system layout is determined by a 'spontaneity score' (120 being maximum spontaneity).",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "     One third of the score is determined by launcher dwell time. A higher dwell time indicates a higher need for spontaneity, and we will prioritize grid layouts which put your most used apps within closer reach!",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "     Another third is determined by 'volatility'. A larger number of launcher launches (switching apps) will lead to a higher spontaneity score.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "     The last is determined by time spent on anchor apps. There is also minor random variation of the score to allow the layout to augment.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
            scoreBreakdown?.let { breakdown ->
                SpontaneityScoreSection(breakdown = breakdown)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetDataAnalyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appRepository = remember(context) { AppRepository(context) }
    val metricsStore = remember(context) { LauncherMetricsStore(context) }
    val currentHour = remember {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }
    val currentHourUsage by produceState<Map<String, Int>?>(initialValue = null, key1 = context) {
        value = metricsStore.getHourlyAppLaunchUsage(currentHour)
    }
    val installedApps by produceState<List<AppInfo>>(initialValue = emptyList(), key1 = context) {
        value = appRepository.getInstalledApps()
    }
    val currentHourLabel = remember {
        val calendar = Calendar.getInstance()
        val startHour = calendar.get(Calendar.HOUR_OF_DAY)
        val endHour = (startHour + 1) % 24
        String.format("%02d:00-%02d:00", startHour, endHour)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Synapse Logic",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Image(
                painter = painterResource(R.drawable.analysing_frequents_3),
                contentScale = ContentScale.FillWidth,
                contentDescription = null
            )
            Text(
                text = "Widget ",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "     The widgets we push are determined by your usage. Each opening of an app is tracked by the launcher, and we will use it to match the widgets to your daily habits.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "     The usage data is scoped by hour of day. Hence, widgets are suited for temporal recommendations rather than lifetime.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Current-Hour App Usage ($currentHourLabel)",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "     If you have newly installed this app, it will take some time to match widget recommendations to your usage fully. Bear with us!",
                style = MaterialTheme.typography.bodyLarge
            )
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
            currentHourUsage?.let { usageMap ->
                AppUsageBarChart(
                    apps = installedApps,
                    usageCountMap = usageMap,
                    minimumVisibleApps = installedApps.size,
                    minUsage = 0,
                    title = "App Usage (current hour)",
                    valueFormatter = ::formatUsageCount
                )
            }

        }
    }
}

@Composable
private fun SpontaneityScoreSection(
    breakdown: LauncherMetricsStore.SystemScoreBreakdown
) {
    val totalScore = breakdown.totalScore.coerceIn(0, 120)
    val totalPercent = ((totalScore / 120f) * 100).toInt()

    Text(
        text = "Spontaneity Score",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )

    MetricBreakdownBar(
        label = "Launcher Dwelling",
        score = breakdown.launcherDwellingScore,
        maxScore = 40
    )
    MetricBreakdownBar(
        label = "Volatility",
        score = breakdown.volatilityScore,
        maxScore = 40
    )
    MetricBreakdownBar(
        label = "Anchor Apps",
        score = breakdown.anchorAppsScore,
        maxScore = 40
    )


    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "List View",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Grid Views",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Text(
        text = "$totalScore/120 ($totalPercent%)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun MetricBreakdownBar(
    label: String,
    score: Int,
    maxScore: Int
) {
    val clampedScore = score.coerceIn(0, maxScore)
    val fillFraction = clampedScore / maxScore.toFloat()
    val remainderColor =
        if (isSystemInDarkTheme()) Color.White else Color.Black

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$clampedScore/$maxScore",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(remainderColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun AppUsageBarChart(
    apps: List<AppInfo>,
    usageCountMap: Map<String, Int>,
    minimumVisibleApps: Int,
    minUsage: Int = 10 * 60 * 1000,
    title: String = "App Usage",
    valueFormatter: (Int) -> String = ::formatUsageTime,
    modifier: Modifier = Modifier
) {
    val rankedApps = remember(apps, usageCountMap) {
        apps
            .filter { (usageCountMap[it.packageName] ?: 0) >= minUsage }
            .sortedByDescending { usageCountMap[it.packageName] ?: 0 }
    }

    val visibleApps = remember(rankedApps, apps, usageCountMap, minimumVisibleApps) {
        if (rankedApps.isNotEmpty() || minimumVisibleApps <= 0) {
            rankedApps
        } else {
            apps
                .sortedByDescending { usageCountMap[it.packageName] ?: 0 }
                .take(minimumVisibleApps)
        }
    }

    val maxUsage = visibleApps
        .maxOfOrNull { usageCountMap[it.packageName] ?: 0 }
        ?.coerceAtLeast(1)
        ?: 1

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {

            visibleApps.forEach { app ->

                val usage = (usageCountMap[app.packageName] ?: 0).coerceAtLeast(0)
                val barFraction = usage.toFloat() / maxUsage.toFloat()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    app.icon?.let {
                        AppIcon(
                            drawable = it,
                            contentDescription = app.label,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width((200.dp * barFraction).coerceAtLeast(12.dp))
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = valueFormatter(usage),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun formatUsageTime(timeMs: Int): String {
    val totalMinutes = timeMs / 1000 / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

private fun formatUsageCount(count: Int): String {
    return when (count) {
        1 -> "1 use"
        else -> "$count uses"
    }
}
