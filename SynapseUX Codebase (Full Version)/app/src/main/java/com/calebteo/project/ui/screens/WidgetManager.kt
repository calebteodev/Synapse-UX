package com.calebteo.project.ui.screens

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.calebteo.project.data.AppLanguage
import com.calebteo.project.data.AppRepository
import com.calebteo.project.data.LauncherMetricsStore
import com.calebteo.project.data.LauncherSettings
import com.example.launcher.data.UsageStatsRepository
import com.calebteo.project.ui.theme.shouldUseDarkTheme
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import java.util.Calendar

data class WidgetApp(
    val icon: Drawable,
    val packageName: String,
    val label: String,
    val score: Int
)

@Composable
fun rememberWidgetApps(count: Int): List<WidgetApp> {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

    return remember(context, count, currentHour, currentDay) {
        val pm = context.packageManager
        val metricsStore = LauncherMetricsStore(context)
        val hourlyBucketUsage = runBlocking {
            metricsStore.getHourlyAppLaunchUsage(currentHour)
        }
        val lastWeekUsage = UsageStatsRepository(context).getLastWeekUsagePresenceMap()
        val recentlyUsedPackages = lastWeekUsage.keys
        val scoreMap = buildWidgetScoreMap(
            lastWeekUsage = lastWeekUsage,
            hourlyBucketUsage = hourlyBucketUsage
        )
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launchableApps = pm.queryIntentActivities(launcherIntent, 0)
            .filter { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                packageName in recentlyUsedPackages &&
                    packageName != context.packageName
            }
        val selectedPackages = mutableSetOf<String>()

        (0 until count).mapNotNull {
            val picked = if (launchableApps.isEmpty()) {
                null
            } else {
                pickWeightedApp(
                    apps = launchableApps,
                    scoreMap = scoreMap,
                    random = Random.Default,
                    excludedPackages = selectedPackages
                )
            }

            if (picked == null) {
                return@mapNotNull null
            }

            val appIcon = picked.loadIcon(pm)
            val packageName = picked.activityInfo.packageName
            val label = picked.loadLabel(pm)?.toString() ?: "App"

            selectedPackages += packageName

            WidgetApp(
                icon = appIcon,
                packageName = packageName,
                label = label,
                score = scoreMap[packageName] ?: 0
            )
        }
    }
}

@Composable
fun Widget(
    selectedApp: WidgetApp,
    settings: LauncherSettings
) {
    val context = LocalContext.current
    val appRepository = remember(context) { AppRepository(context) }

    val onClick: () -> Unit = {
        appRepository.launchApp(selectedApp.packageName)
    }

    WidgetBackbone(
        appIcon = selectedApp.icon,
        language = settings.language,
        username = settings.username,
        appName = selectedApp.label,
        onClick = onClick
    )
}

private fun pickWeightedApp(
    apps: List<android.content.pm.ResolveInfo>,
    scoreMap: Map<String, Int>,
    random: Random,
    excludedPackages: Set<String> = emptySet()
): android.content.pm.ResolveInfo {
    val candidateApps = apps.filterNot {
        it.activityInfo.packageName in excludedPackages
    }.ifEmpty {
        apps
    }

    val totalScore = candidateApps.sumOf { app ->
        ((scoreMap[app.activityInfo.packageName] ?: 0).coerceAtLeast(0) + 1).toDouble()
    }.coerceAtLeast(1.0)

    val weightedApps = candidateApps.map { app ->
        val score = (scoreMap[app.activityInfo.packageName] ?: 0).coerceAtLeast(0)
        app to ((score + 1).toDouble() / totalScore)
    }

    val totalWeight = weightedApps.sumOf { it.second }
    var threshold = random.nextDouble() * totalWeight

    weightedApps.forEach { (app, weight) ->
        threshold -= weight
        if (threshold <= 0.0) return app
    }

    return weightedApps.last().first
}

private fun buildWidgetScoreMap(
    lastWeekUsage: Map<String, Int>,
    hourlyBucketUsage: Map<String, Int>
): Map<String, Int> {
    val packages = lastWeekUsage.keys + hourlyBucketUsage.keys
    return packages.associateWith { packageName ->
        (lastWeekUsage[packageName] ?: 0) + (hourlyBucketUsage[packageName] ?: 0)
    }
}

@Composable
fun WidgetBackbone(
    appIcon: Drawable,
    language: AppLanguage,
    username: String,
    appName: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val greetingName = username.trim().ifBlank { widgetCopy(WidgetCopy.THERE, language) }
    val isDarkTheme = shouldUseDarkTheme(context)
    val topTextColor = if (isDarkTheme) {
        Color.White
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val bottomTextColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick)
        ,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    drawable = appIcon,
                    contentDescription = "Widget app icon",
                    modifier = Modifier.size(84.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(2f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "${widgetCopy(WidgetCopy.HELLO, language)} $greetingName,",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = topTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = widgetCopy(WidgetCopy.TIME_TO_OPEN, language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = bottomTextColor,
                    maxLines = 1
                )

                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = bottomTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private enum class WidgetCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    HELLO("Hello", "你好", "Hello", "Hola", "Bonjour"),
    TIME_TO_OPEN("time to open", "该打开了", "masa untuk buka", "hora de abrir", "il est temps d'ouvrir"),
    THERE("there", "朋友", "anda", "amigo", "ami")
}

private fun widgetCopy(copy: WidgetCopy, language: AppLanguage): String =
    when (language) {
        AppLanguage.ENGLISH -> copy.english
        AppLanguage.MANDARIN -> copy.mandarin
        AppLanguage.MALAY -> copy.malay
        AppLanguage.SPANISH -> copy.spanish
        AppLanguage.FRENCH -> copy.french
    }
