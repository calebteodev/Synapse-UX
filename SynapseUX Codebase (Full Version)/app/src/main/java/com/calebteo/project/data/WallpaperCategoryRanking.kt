package com.calebteo.project.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlin.math.pow
import kotlin.random.Random

object WallpaperCategoryRanking {

    private fun key(category: WallpaperCategory) =
        intPreferencesKey("wallpaper_category_score_${category.id}")


    suspend fun adjustScore(
        context: Context,
        category: WallpaperCategory,
        delta: Int
    ) {

        context.wallpaperDataStore.edit { prefs ->

            val current = prefs[key(category)] ?: 10

            val newScore = (current + delta).coerceIn(1, 30)

            prefs[key(category)] = newScore
        }
    }

    suspend fun getAllScores(
        context: Context
    ): Map<WallpaperCategory, Int> {

        val prefs = context.wallpaperDataStore.data.first()

        return WallpaperCategory.values().associateWith {
            prefs[key(it)] ?: 10
        }
    }
}


object WallpaperSelectionEngine {

    suspend fun chooseCategory(
        context: Context,
        settings: LauncherSettings
    ): WallpaperCategory {
        if (!settings.overrideWallpaperTheme) {
            return settings.wallpaperTheme
        }
        val scores = WallpaperCategoryRanking.getAllScores(context)

        // weighted pool using score directly
        val weightedPool = scores.flatMap { (category, score) ->
            List(score) { category }
        }

        if (weightedPool.isEmpty())
            return WallpaperCategory.LANDSCAPES

        return weightedPool.random()
    }


    suspend fun chooseWallpaper(
        settings: LauncherSettings,
        context: Context,
        wallpapers: List<Wallpaper>
    ): Wallpaper {

        val category = chooseCategory( context,settings)

        val filtered = wallpapers.filter {
            WallpaperCategory.fromLegacy(it.category) == category
        }

        if (filtered.isEmpty())
            return wallpapers.random()

        return filtered.random()
    }

}


suspend fun shouldAskFeedback(
    context: Context
): Boolean {

    val scores = WallpaperCategoryRanking.getAllScores(context)
        .values
        .map { it.toDouble() }

    val mean = scores.average()

    val variance = scores
        .map { (it - mean).pow(2) }
        .average()

    val maxVariance = 210.25

    val confidence = (variance / maxVariance)
        .coerceIn(0.0, 1.0)

    val probability = (0.20 - 0.16 * confidence.pow(2.0))
        .coerceIn(0.04, 0.20)

    return Random.nextDouble() < probability
}
