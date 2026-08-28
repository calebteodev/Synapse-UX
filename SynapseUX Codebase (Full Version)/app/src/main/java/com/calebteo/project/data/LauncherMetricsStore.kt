package com.calebteo.project.data

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random


private const val DATASTORE_NAME = "launcher_metrics"
val Context.launcherMetricsDataStore by preferencesDataStore(
    name = DATASTORE_NAME
)


object LauncherMetricKeys {

    val HAS_INITIALIZED = booleanPreferencesKey("has_initialized")
    val CURRENT_INITIALIZATION_HOUR_ID =
        longPreferencesKey("current_initialization_hour_id")
    val CURRENT_HOUR_INITIALIZATION_COUNT =
        intPreferencesKey("current_hour_initialization_count")
    val TOTAL_HOURLY_INITIALIZATIONS =
        intPreferencesKey("total_hourly_initializations")
    val ACTIVE_INITIALIZATION_HOURS =
        intPreferencesKey("active_initialization_hours")

    val TOTAL_LAUNCHER_TIME_MS = longPreferencesKey("total_launcher_time_ms")
    val LAUNCHER_SESSION_COUNT = intPreferencesKey("launcher_session_count")
    val AVG_LAUNCHER_TIME_MS = longPreferencesKey("avg_launcher_time_ms")

    val TOTAL_APP_LAUNCHES = intPreferencesKey("total_app_launches")

    val ANCHOR_APPS_TIME_MS = longPreferencesKey("anchor_apps_time_ms")
    val VOLATILITY_SCORE = floatPreferencesKey("volatility_score")

    val ORGANISATION_SCORE = intPreferencesKey("organisation_score")
}

private fun hourlyAppLaunchKey(hourOfDay: Int, packageName: String) =
    intPreferencesKey("hourly_app_launch_${hourOfDay}_${packageName}")

private const val SettingsPackageName = "com.android.settings"
private const val ChromePackageName = "com.android.chrome"
private const val GmailPackageName = "com.google.android.gm"
private const val YoutubePackageName = "com.google.android.youtube"



class LauncherMetricsStore(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    data class SystemScoreBreakdown(
        val launcherDwellingScore: Int,
        val volatilityScore: Int,
        val anchorAppsScore: Int,
        val totalScore: Int
    )

    init {
        scope.launch {
            initializeIfNeeded()
        }
    }

    private suspend fun initializeIfNeeded() {
        context.launcherMetricsDataStore.edit { prefs ->
            if (prefs[LauncherMetricKeys.HAS_INITIALIZED] != true) {
                prefs[LauncherMetricKeys.HAS_INITIALIZED] = true
                prefs[LauncherMetricKeys.CURRENT_INITIALIZATION_HOUR_ID] = -1L
                prefs[LauncherMetricKeys.CURRENT_HOUR_INITIALIZATION_COUNT] = 0
                prefs[LauncherMetricKeys.TOTAL_HOURLY_INITIALIZATIONS] = 0
                prefs[LauncherMetricKeys.ACTIVE_INITIALIZATION_HOURS] = 0
            }
        }
    }





    suspend fun recordAppLaunch() {
        context.launcherMetricsDataStore.edit { prefs ->
            val count = prefs[LauncherMetricKeys.TOTAL_APP_LAUNCHES] ?: 0
            prefs[LauncherMetricKeys.TOTAL_APP_LAUNCHES] = count + 1
        }
    }

    suspend fun recordLauncherInitialization() {
        val currentHourId = System.currentTimeMillis() / (60 * 60 * 1000)

        context.launcherMetricsDataStore.edit { prefs ->
            val storedHourId =
                prefs[LauncherMetricKeys.CURRENT_INITIALIZATION_HOUR_ID] ?: -1L
            val currentHourCount =
                prefs[LauncherMetricKeys.CURRENT_HOUR_INITIALIZATION_COUNT] ?: 0
            var finalizedInitializationTotal =
                prefs[LauncherMetricKeys.TOTAL_HOURLY_INITIALIZATIONS] ?: 0
            var finalizedHourCount =
                prefs[LauncherMetricKeys.ACTIVE_INITIALIZATION_HOURS] ?: 0

            // When the user moves into a new real-world hour, finalize the previous
            // hour once by adding its initialization count to the long-term totals.
            if (storedHourId != -1L && storedHourId != currentHourId && currentHourCount > 0) {
                finalizedInitializationTotal += currentHourCount
                finalizedHourCount += 1
                prefs[LauncherMetricKeys.TOTAL_HOURLY_INITIALIZATIONS] =
                    finalizedInitializationTotal
                prefs[LauncherMetricKeys.ACTIVE_INITIALIZATION_HOURS] =
                    finalizedHourCount
                prefs[LauncherMetricKeys.CURRENT_HOUR_INITIALIZATION_COUNT] = 0
            }

            val refreshedCurrentHourCount =
                if (storedHourId != currentHourId) 0 else currentHourCount
            val newCurrentHourCount = refreshedCurrentHourCount + 1

            prefs[LauncherMetricKeys.CURRENT_INITIALIZATION_HOUR_ID] = currentHourId
            prefs[LauncherMetricKeys.CURRENT_HOUR_INITIALIZATION_COUNT] =
                newCurrentHourCount

            // The active current hour also counts toward the average immediately,
            // so the score reflects the user's present behavior instead of waiting
            // for the hour to end.
            val effectiveInitializationTotal =
                finalizedInitializationTotal + newCurrentHourCount
            val effectiveHourCount = finalizedHourCount + 1

            prefs[LauncherMetricKeys.VOLATILITY_SCORE] =
                calculateVolatilityFromAverageHourlyInitializations(
                    totalInitializations = effectiveInitializationTotal,
                    activeHours = effectiveHourCount
                )
        }
    }

    private fun calculateVolatilityFromAverageHourlyInitializations(
        totalInitializations: Int,
        activeHours: Int
    ): Float {
        val safeHours = activeHours.coerceAtLeast(1)
        val averageInitializationsPerHour =
            totalInitializations.coerceAtLeast(0).toFloat() / safeHours.toFloat()
        val fullVolatilityAverage = 20f
        return (averageInitializationsPerHour / fullVolatilityAverage)
            .coerceIn(0f, 1f)
    }

    /* ---------------------------------------------------
       CORE METRIC: ORGANISATION SCORE (0–120)
    --------------------------------------------------- */

    suspend fun recalculateOrganisationScoreOnStart() {
        val prefs = context.launcherMetricsDataStore.data.first()

        val avgLauncherTime =
            prefs[LauncherMetricKeys.AVG_LAUNCHER_TIME_MS] ?: 0L

        val volatility =
            prefs[LauncherMetricKeys.VOLATILITY_SCORE] ?: 0f

        val anchorAppTimesLast24Hours = getAnchorAppTimesLast24Hours()

        val componentBreakdown = buildSystemScoreBreakdown(
            avgLauncherTime = avgLauncherTime,
            volatility = volatility,
            anchorAppTimesLast24Hours = anchorAppTimesLast24Hours,
            totalScoreOverride = null
        )
        var score = componentBreakdown.totalScore

        /* ---- D) Dynamicity noise (+12 / -12) ---- */
        score += Random.nextInt(-12, 13)

        val finalScore = score.coerceIn(0, 120)

        context.launcherMetricsDataStore.edit { editPrefs ->
            editPrefs[LauncherMetricKeys.ORGANISATION_SCORE] = finalScore
        }
    }

    suspend fun getOrganisationScore(): Int {
        val prefs = context.launcherMetricsDataStore.data.first()
        return prefs[LauncherMetricKeys.ORGANISATION_SCORE] ?: 0
    }

    suspend fun getSystemScoreBreakdown(): SystemScoreBreakdown {
        val prefs = context.launcherMetricsDataStore.data.first()
        return buildSystemScoreBreakdown(
            avgLauncherTime = prefs[LauncherMetricKeys.AVG_LAUNCHER_TIME_MS] ?: 0L,
            volatility = prefs[LauncherMetricKeys.VOLATILITY_SCORE] ?: 0f,
            anchorAppTimesLast24Hours = getAnchorAppTimesLast24Hours(),
            totalScoreOverride = prefs[LauncherMetricKeys.ORGANISATION_SCORE]
        )
    }

    private fun getAnchorAppTimesLast24Hours(): Map<String, Long> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - (24L * 60L * 60L * 1000L)

        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        ).associate { usageStats ->
            usageStats.packageName to usageStats.totalTimeInForeground
        }
    }

    private fun buildSystemScoreBreakdown(
        avgLauncherTime: Long,
        volatility: Float,
        anchorAppTimesLast24Hours: Map<String, Long>,
        totalScoreOverride: Int?
    ): SystemScoreBreakdown {
        val launcherDwellingScore = when {
            avgLauncherTime <= 8_000L -> 10
            avgLauncherTime <= 18_000L -> 20
            avgLauncherTime <= 35_000L -> 30
            else -> 40
        }

        val settingsTime = anchorAppTimesLast24Hours[SettingsPackageName] ?: 0L
        val chromeTime = anchorAppTimesLast24Hours[ChromePackageName] ?: 0L
        val gmailTime = anchorAppTimesLast24Hours[GmailPackageName] ?: 0L
        val youtubeTime = anchorAppTimesLast24Hours[YoutubePackageName] ?: 0L

        val anchorAppsScore =
            (if (settingsTime < 5 * 60 * 1000L) 10 else 0) +
                (if (chromeTime < 10 * 60 * 1000L) 10 else 0) +
                (if (gmailTime < 10 * 60 * 1000L) 10 else 0) +
                (if (youtubeTime > 30 * 60 * 1000L) 10 else 0)

        val volatilityScore = when {
            volatility <= 0.2f -> 10
            volatility <= 0.4f -> 20
            volatility <= 0.6f -> 30
            else -> 40
        }

        val componentTotal =
            launcherDwellingScore + anchorAppsScore + volatilityScore

        return SystemScoreBreakdown(
            launcherDwellingScore = launcherDwellingScore,
            volatilityScore = volatilityScore,
            anchorAppsScore = anchorAppsScore,
            totalScore = totalScoreOverride ?: componentTotal
        )
    }

    suspend fun recordHourlyAppLaunch(packageName: String) {
        val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val key = hourlyAppLaunchKey(hourOfDay, packageName)

        context.launcherMetricsDataStore.edit { prefs ->
            val count = prefs[key] ?: 0
            prefs[key] = count + 1
        }
    }

    suspend fun getHourlyAppLaunchUsage(hourOfDay: Int): Map<String, Int> {
        val prefix = "hourly_app_launch_${hourOfDay}_"
        val prefs = context.launcherMetricsDataStore.data.first()

        return prefs.asMap()
            .mapNotNull { (key, value) ->
                val name = key.name
                if (!name.startsWith(prefix)) return@mapNotNull null

                val packageName = name.removePrefix(prefix)
                val count = value as? Int ?: return@mapNotNull null
                packageName to count
            }
            .toMap()
    }

}




private val Context.onboardingDataStore by preferencesDataStore("onboarding")

class OnboardingStore(private val context: Context) {

    private val KEY_DONE = booleanPreferencesKey("onboarding_done")
    private val KEY_INSTALL_TIME = longPreferencesKey("onboarding_install_time")

    private fun currentInstallTime(): Long =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .firstInstallTime

    suspend fun isFirstLaunch(): Boolean {
        val prefs = context.onboardingDataStore.data.first()
        val currentInstallTime = currentInstallTime()
        val storedInstallTime = prefs[KEY_INSTALL_TIME]

        if (storedInstallTime != currentInstallTime) {
            context.onboardingDataStore.edit {
                it[KEY_INSTALL_TIME] = currentInstallTime
                it[KEY_DONE] = false
            }
            return true
        }

        return !(prefs[KEY_DONE] ?: false)
    }

    suspend fun markCompleted() {
        context.onboardingDataStore.edit {
            it[KEY_DONE] = true
            it[KEY_INSTALL_TIME] = currentInstallTime()
        }
    }
}
