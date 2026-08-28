package com.calebteo.project.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.calebteo.project.data.LauncherSettings
import com.calebteo.project.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.pow
import kotlin.math.sqrt

// REDm3lk
private val DarkRedScheme = darkColorScheme(
    primary = Red,
    secondary = RedDark,
    tertiary = RedLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightRedScheme = lightColorScheme(
    primary = Red,
    secondary = RedDark,
    tertiary = RedLight,
    background = LightBackground,
    surface = LightSurface
)

// GREEN
private val DarkGreenScheme = darkColorScheme(
    primary = Green,
    secondary = GreenDark,
    tertiary = GreenLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightGreenScheme = lightColorScheme(
    primary = Green,
    secondary = GreenDark,
    tertiary = GreenLight,
    background = LightBackground,
    surface = LightSurface
)

// BLUE
private val DarkBlueScheme = darkColorScheme(
    primary = Blue,
    secondary = BlueDark,
    tertiary = BlueLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightBlueScheme = lightColorScheme(
    primary = Blue,
    secondary = BlueDark,
    tertiary = BlueLight,
    background = LightBackground,
    surface = LightSurface
)

// INDIGO
private val DarkIndigoScheme = darkColorScheme(
    primary = Indigo,
    secondary = IndigoDark,
    tertiary = IndigoLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightIndigoScheme = lightColorScheme(
    primary = Indigo,
    secondary = IndigoDark,
    tertiary = IndigoLight,
    background = LightBackground,
    surface = LightSurface
)

// ORANGE
private val DarkOrangeScheme = darkColorScheme(
    primary = Orange,
    secondary = OrangeDark,
    tertiary = OrangeLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightOrangeScheme = lightColorScheme(
    primary = Orange,
    secondary = OrangeDark,
    tertiary = OrangeLight,
    background = LightBackground,
    surface = LightSurface
)

// YELLOW
private val DarkYellowScheme = darkColorScheme(
    primary = Yellow,
    secondary = YellowDark,
    tertiary = YellowLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightYellowScheme = lightColorScheme(
    primary = Yellow,
    secondary = YellowDark,
    tertiary = YellowLight,
    background = LightBackground,
    surface = LightSurface
)

// VIOLET
private val DarkVioletScheme = darkColorScheme(
    primary = Violet,
    secondary = VioletDark,
    tertiary = VioletLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightVioletScheme = lightColorScheme(
    primary = Violet,
    secondary = VioletDark,
    tertiary = VioletLight,
    background = LightBackground,
    surface = LightSurface
)

// GREY
private val DarkGreyScheme = darkColorScheme(
    primary = Grey,
    secondary = GreyDark,
    tertiary = GreyLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightGreyScheme = lightColorScheme(
    primary = Grey,
    secondary = GreyDark,
    tertiary = GreyLight,
    background = LightBackground,
    surface = LightSurface
)

// MAROON
private val DarkMaroonScheme = darkColorScheme(
    primary = Maroon,
    secondary = MaroonDark,
    tertiary = MaroonLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightMaroonScheme = lightColorScheme(
    primary = Maroon,
    secondary = MaroonDark,
    tertiary = MaroonLight,
    background = LightBackground,
    surface = LightSurface
)



// OLIVE
private val DarkOliveScheme = darkColorScheme(
    primary = Olive,
    secondary = OliveDark,
    tertiary = OliveLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightOliveScheme = lightColorScheme(
    primary = Olive,
    secondary = OliveDark,
    tertiary = OliveLight,
    background = LightBackground,
    surface = LightSurface
)

// CYAN
private val DarkCyanScheme = darkColorScheme(
    primary = Cyan,
    secondary = CyanDark,
    tertiary = CyanLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightCyanScheme = lightColorScheme(
    primary = Cyan,
    secondary = CyanDark,
    tertiary = CyanLight,
    background = LightBackground,
    surface = LightSurface
)

// PINK
private val DarkPinkScheme = darkColorScheme(
    primary = Pink,
    secondary = PinkDark,
    tertiary = PinkLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightPinkScheme = lightColorScheme(
    primary = Pink,
    secondary = PinkDark,
    tertiary = PinkLight,
    background = LightBackground,
    surface = LightSurface
)

// MAGENTA
private val DarkMagentaScheme = darkColorScheme(
    primary = Magenta,
    secondary = MagentaDark,
    tertiary = MagentaLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightMagentaScheme = lightColorScheme(
    primary = Magenta,
    secondary = MagentaDark,
    tertiary = MagentaLight,
    background = LightBackground,
    surface = LightSurface
)

// TAN
private val DarkTanScheme = darkColorScheme(
    primary = Tan,
    secondary = TanDark,
    tertiary = TanLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightTanScheme = lightColorScheme(
    primary = Tan,
    secondary = TanDark,
    tertiary = TanLight,
    background = LightBackground,
    surface = LightSurface
)

// TEAL
private val DarkTealScheme = darkColorScheme(
    primary = Teal,
    secondary = TealDark,
    tertiary = TealLight,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightTealScheme = lightColorScheme(
    primary = Teal,
    secondary = TealDark,
    tertiary = TealLight,
    background = LightBackground,
    surface = LightSurface
)

object ThemeColorCache {
    var cached by mutableStateOf(ThemeColor.BLUE)
}

/* ---------------------------------------------------
   DataStore
--------------------------------------------------- */

private const val DATASTORE_NAME = "launcher_color_scores"

val Context.colorScoreDataStore by preferencesDataStore(
    name = DATASTORE_NAME
)

/* ---------------------------------------------------
   Theme Colors
--------------------------------------------------- */

enum class ThemeColor {
    RED, GREEN, BLUE, INDIGO, ORANGE, YELLOW, VIOLET,
    GREY, MAROON, OLIVE, CYAN, PINK, MAGENTA, TAN, TEAL,

}

/* ---------------------------------------------------
   DataStore Keys
--------------------------------------------------- */

object ColorScoreKeys {

    val RED = intPreferencesKey("score_red")
    val GREEN = intPreferencesKey("score_green")
    val BLUE = intPreferencesKey("score_blue")
    val INDIGO = intPreferencesKey("score_indigo")
    val ORANGE = intPreferencesKey("score_orange")
    val YELLOW = intPreferencesKey("score_yellow")
    val VIOLET = intPreferencesKey("score_violet")

    val GREY = intPreferencesKey("score_grey")
    val MAROON = intPreferencesKey("score_maroon")
    val BLACK = intPreferencesKey("score_black")
    val OLIVE = intPreferencesKey("score_olive")
    val CYAN = intPreferencesKey("score_cyan")
    val PINK = intPreferencesKey("score_pink")
    val MAGENTA = intPreferencesKey("score_magenta")
    val TAN = intPreferencesKey("score_tan")
    val TEAL = intPreferencesKey("score_teal")
    val SYSTEM = intPreferencesKey("system")
    fun keyFor(color: ThemeColor): Preferences.Key<Int> {
        return when (color) {
            ThemeColor.RED -> RED
            ThemeColor.GREEN -> GREEN
            ThemeColor.BLUE -> BLUE
            ThemeColor.INDIGO -> INDIGO
            ThemeColor.ORANGE -> ORANGE
            ThemeColor.YELLOW -> YELLOW
            ThemeColor.VIOLET -> VIOLET
            ThemeColor.GREY -> GREY
            ThemeColor.MAROON -> MAROON
            ThemeColor.OLIVE -> OLIVE
            ThemeColor.CYAN -> CYAN
            ThemeColor.PINK -> PINK
            ThemeColor.MAGENTA -> MAGENTA
            ThemeColor.TAN -> TAN
            ThemeColor.TEAL -> TEAL
        }
    }
}

/* ---------------------------------------------------
   Reference RGB values
--------------------------------------------------- */

private val referenceColors = mapOf(
    ThemeColor.RED to intArrayOf(229, 57, 53),
    ThemeColor.GREEN to intArrayOf(67, 160, 71),
    ThemeColor.BLUE to intArrayOf(30, 136, 229),
    ThemeColor.INDIGO to intArrayOf(94, 53, 177),
    ThemeColor.ORANGE to intArrayOf(251, 140, 0),
    ThemeColor.YELLOW to intArrayOf(253, 216, 53),
    ThemeColor.VIOLET to intArrayOf(142, 36, 170),

    ThemeColor.GREY to intArrayOf(158, 158, 158),
    ThemeColor.MAROON to intArrayOf(128, 0, 0),
    ThemeColor.OLIVE to intArrayOf(128, 128, 0),
    ThemeColor.CYAN to intArrayOf(0, 172, 193),
    ThemeColor.PINK to intArrayOf(236, 64, 122),
    ThemeColor.MAGENTA to intArrayOf(216, 27, 96),
    ThemeColor.TAN to intArrayOf(210, 180, 140),
    ThemeColor.TEAL to intArrayOf(0, 137, 123)
)

/* ---------------------------------------------------
   Extract dominant wallpaper colour
--------------------------------------------------- */
suspend fun initializeBestThemeColor(context: Context) {

    val prefs = context.colorScoreDataStore.data.first()
    val bestColor = ThemeColor.values().maxByOrNull {
        prefs[ColorScoreKeys.keyFor(it)] ?: 0
    }

    ThemeColorCache.cached =
        if (bestColor == null || (prefs[ColorScoreKeys.keyFor(bestColor)] ?: 0) <= 0) {
            ThemeColor.BLUE
        } else {
            bestColor
        }
}
fun getAverageColor(bitmap: Bitmap): IntArray {

    var r = 0L
    var g = 0L
    var b = 0L

    val width = bitmap.width
    val height = bitmap.height
    val size = width * height

    for (x in 0 until width step 20) {
        for (y in 0 until height step 20) {
            val pixel = bitmap.getPixel(x, y)
            r += AndroidColor.red(pixel)
            g += AndroidColor.green(pixel)
            b += AndroidColor.blue(pixel)
        }
    }

    val samples = (width / 20) * (height / 20)

    return intArrayOf(
        (r / samples).toInt(),
        (g / samples).toInt(),
        (b / samples).toInt()
    )
}

/* ---------------------------------------------------
   Color similarity (1–100)
--------------------------------------------------- */

fun colorSimilarity(a: IntArray, b: IntArray): Int {

    val distance = sqrt(
        (a[0] - b[0]).toDouble().pow(2) +
                (a[1] - b[1]).toDouble().pow(2) +
                (a[2] - b[2]).toDouble().pow(2)
    )

    val maxDistance = sqrt((255.0.pow(2)) * 3)

    val similarity = (1 - distance / maxDistance) * 100

    return similarity.toInt().coerceIn(1, 100)
}

/* ---------------------------------------------------
   Update scores from wallpaper
--------------------------------------------------- */
suspend fun loadBitmapFromUrl(url: String): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeStream(URL(url).openStream())
        } catch (e: Exception) {
            null
        }
    }
suspend fun updateColorScores(
    context: Context,
    bitmap: Bitmap
) {

    // Downscale for speed
    val small = Bitmap.createScaledBitmap(bitmap, 50, 50, true)

    var r = 0L
    var g = 0L
    var b = 0L
    var count = 0

    for (x in 0 until small.width step 5) {
        for (y in 0 until small.height step 5) {

            val pixel = small.getPixel(x, y)

            r += AndroidColor.red(pixel)
            g += AndroidColor.green(pixel)
            b += AndroidColor.blue(pixel)

            count++
        }
    }

    val avgColor = intArrayOf(
        (r / count).toInt(),
        (g / count).toInt(),
        (b / count).toInt()
    )

    var bestTheme = ThemeColor.BLUE
    var bestScore = 0

    context.colorScoreDataStore.edit { prefs ->

        referenceColors.forEach { (theme, refRGB) ->

            val score = colorSimilarity(avgColor, refRGB)
            val key = ColorScoreKeys.keyFor(theme)

            // overwrite instead of accumulate
            prefs[key] = score

            if (score > bestScore) {
                bestScore = score
                bestTheme = theme
            }
        }

        prefs[intPreferencesKey("best_theme")] = bestTheme.ordinal
    }

    // update runtime cache immediately
    ThemeColorCache.cached = bestTheme
}
fun getBestThemeColor(): ThemeColor {
    return ThemeColorCache.cached
}
fun shouldUseDarkTheme(context: Context): Boolean {

    val uiMode =
    context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK

    if (uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) return true
    if (uiMode == android.content.res.Configuration.UI_MODE_NIGHT_NO) return false

    // fallback: time of day
    val hour = java.util.Calendar.getInstance()
        .get(java.util.Calendar.HOUR_OF_DAY)

    return hour < 6 || hour >= 18
}
@Composable
fun generalTheme(
    content: @Composable () -> Unit
) {

    val context = LocalContext.current

    val settingsManager = remember {
        SettingsManager(context)
    }

    val settings by settingsManager
        .settingsFlow
        .collectAsState(initial = LauncherSettings())

    val dark = shouldUseDarkTheme(context)

    val themeColor =
        if (settings.overrideColorTheme)
            getBestThemeColor()
        else
            settings.themeColor

    val colorScheme = when (themeColor) {

        ThemeColor.RED -> if (dark) DarkRedScheme else LightRedScheme
        ThemeColor.GREEN -> if (dark) DarkGreenScheme else LightGreenScheme
        ThemeColor.BLUE -> if (dark) DarkBlueScheme else LightBlueScheme
        ThemeColor.INDIGO -> if (dark) DarkIndigoScheme else LightIndigoScheme
        ThemeColor.ORANGE -> if (dark) DarkOrangeScheme else LightOrangeScheme
        ThemeColor.YELLOW -> if (dark) DarkYellowScheme else LightYellowScheme
        ThemeColor.VIOLET -> if (dark) DarkVioletScheme else LightVioletScheme
        ThemeColor.GREY -> if (dark) DarkGreyScheme else LightGreyScheme
        ThemeColor.MAROON -> if (dark) DarkMaroonScheme else LightMaroonScheme
        ThemeColor.OLIVE -> if (dark) DarkOliveScheme else LightOliveScheme
        ThemeColor.CYAN -> if (dark) DarkCyanScheme else LightCyanScheme
        ThemeColor.PINK -> if (dark) DarkPinkScheme else LightPinkScheme
        ThemeColor.MAGENTA -> if (dark) DarkMagentaScheme else LightMagentaScheme
        ThemeColor.TAN -> if (dark) DarkTanScheme else LightTanScheme
        ThemeColor.TEAL -> if (dark) DarkTealScheme else LightTealScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyForFont(settings.appFont),
        content = content
    )
}
