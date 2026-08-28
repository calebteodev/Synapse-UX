package com.calebteo.project.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.calebteo.project.ui.theme.ThemeColor
import java.util.Locale

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String?,
    default: T
): T {
    if (value == null) return default
    return enumValues<T>().firstOrNull { it.name == value.uppercase() } ?: default
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_settings"
)

enum class AppLanguage {
    ENGLISH,
    MANDARIN,
    MALAY,
    SPANISH,
    FRENCH;

    companion object {
        fun fromLocale(locale: Locale = Locale.getDefault()): AppLanguage {
            return when (locale.language.lowercase()) {
                "zh" -> MANDARIN
                "ms" -> MALAY
                "es" -> SPANISH
                "fr" -> FRENCH
                else -> ENGLISH
            }
        }
    }
}

data class LauncherSettings(
    val overrideTopBar: Boolean = true,
    val overrideGridView: Boolean = true,
    val overrideWallpaperTheme: Boolean = true,
    val overrideColorTheme: Boolean = true,
    val overrideSystemLayout: Boolean = true,
    val appGridSize: Int = 3,
    val appRowSize: Int = 4,
    val selectedAppPackages: Set<String> = emptySet(),
    val pinnedAppPackages: List<String> = emptyList(),
    val iconSize: Int = 60,
    val wallpaperSource: WallpaperSource = WallpaperSource.DEFAULT,
    val customWallpaperUri: String? = null,
    val wallpaperTheme: WallpaperCategory = WallpaperCategory.LANDSCAPES,
    val systemLayout: SystemLayout = SystemLayout.GRID_SEARCH_APPDRAWER,
    val themeColor: ThemeColor = ThemeColor.GREY,
    val appFont: AppFont = AppFont.SANS,
    val synapseNavigationEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.fromLocale(),
    val username: String = ""
)

enum class AppFont {
    SANS,
    SERIF,
    MONO,
    CURSIVE
}

enum class WallpaperSource {
    SYSTEM,
    DEFAULT,
    CUSTOM
}



enum class WallpaperCategory(
    val id: String
) {

    ABSTRACT("abstract"),
    ARTEFACTS("artefacts"),
    CYBERPUNK("cyberpunk"),
    FOOD_AND_DRINK("food_and_drink"),
    LANDSCAPES("landscapes"), // replaces NATURE
    SPACE("space"),
    URBAN("urban"),
    WILDLIFE("wildlife");

    companion object {

        val ordered = values().sortedBy { it.name }

        fun fromStoredName(name: String?): WallpaperCategory {
            return when (name?.uppercase()) {
                "ART_DECO", "RETRO", null -> LANDSCAPES
                else -> values().firstOrNull { it.name == name.uppercase() }
                    ?: LANDSCAPES
            }
        }

        fun fromLegacy(category: String): WallpaperCategory {
            return when (category.lowercase()) {
                "nature", "art_deco", "retro" -> LANDSCAPES
                else -> values().firstOrNull { it.id == category }
                    ?: LANDSCAPES
            }
        }
    }
}

enum class SystemLayout {
    MAIN_LIST, MAIN_LIST_GRID, GRID_SEARCH_LIST, WIDGETS_SEARCH_LIST, GRID_SEARCH_APPDRAWER, WIDGETS_SEARCH_APPDRAWER
}

class SettingsManager(private val context: Context) {
    private companion object {
        const val PinnedAppsDelimiter = "\u0001"
    }

    private object Keys {

        val APP_GRID_SIZE =
            intPreferencesKey("app_grid_size")
        val APP_ROW_SIZE =
            intPreferencesKey("app_row_size")
        val OVERRIDE_TOP_BAR =
            booleanPreferencesKey("override_top_bar")
        val OVERRIDE_GRID_VIEW =
            booleanPreferencesKey("override_grid_view")
        val OVERRIDE_SYSTEM_LAYOUT =
            booleanPreferencesKey("override_system_layout")
        val OVERRIDE_COLOR_THEME =
            booleanPreferencesKey("override_color_theme")
        val OVERRIDE_WALLPAPER_THEME =
            booleanPreferencesKey("override_wallpaper_theme")
        val ICON_SIZE =
            intPreferencesKey("icon_size")
        val SELECTED_APP_PACKAGES =
            stringSetPreferencesKey("selected_app_packages")
        val PINNED_APP_ORDER =
            stringPreferencesKey("pinned_app_order")
        val WALLPAPER_SOURCE =
            stringPreferencesKey("wallpaper_source")

        val CUSTOM_WALLPAPER_URI =
            stringPreferencesKey("custom_wallpaper_uri")

        val WALLPAPER_THEME =
            stringPreferencesKey("wallpaper_theme")

        val SYSTEM_LAYOUT =
            stringPreferencesKey("system_layout")
        val THEME_COLOR =
            stringPreferencesKey("theme_color")
        val APP_FONT =
            stringPreferencesKey("app_font")
        val SYNAPSE_NAVIGATION_ENABLED =
            booleanPreferencesKey("synapse_navigation_enabled")
        val LANGUAGE =
            stringPreferencesKey("language")
        val USERNAME =
            stringPreferencesKey("username")
    }

    val settingsFlow: Flow<LauncherSettings> =
        context.dataStore.data.map { prefs ->
            val selectedPackages =
                prefs[Keys.SELECTED_APP_PACKAGES] ?: emptySet()
            val storedPinnedOrder =
                prefs[Keys.PINNED_APP_ORDER]
                    ?.split(PinnedAppsDelimiter)
                    ?.filter { it.isNotBlank() }
                    ?.distinct()
                    ?: emptyList()
            val pinnedPackages = if (storedPinnedOrder.isEmpty()) {
                selectedPackages.sorted()
            } else {
                buildList {
                    addAll(storedPinnedOrder.filter { it in selectedPackages })
                    addAll(selectedPackages.filter { it !in storedPinnedOrder }.sorted())
                }
            }

            LauncherSettings(
                appGridSize =
                    prefs[Keys.APP_GRID_SIZE] ?: 3,
                appRowSize =
                    prefs[Keys.APP_ROW_SIZE] ?: 4,
                overrideTopBar =
                    prefs[Keys.OVERRIDE_TOP_BAR] ?: true,
                overrideGridView =
                    prefs[Keys.OVERRIDE_GRID_VIEW] ?: true,
                overrideSystemLayout =
                    prefs[Keys.OVERRIDE_SYSTEM_LAYOUT] ?: true,
                overrideColorTheme =
                    prefs[Keys.OVERRIDE_COLOR_THEME] ?: true,
                overrideWallpaperTheme =
                    prefs[Keys.OVERRIDE_WALLPAPER_THEME] ?: true,
                selectedAppPackages =
                    pinnedPackages.toSet(),
                pinnedAppPackages =
                    pinnedPackages,
                iconSize =
                    prefs[Keys.ICON_SIZE] ?: 60,

                wallpaperSource =
                    enumValueOrDefault(
                        prefs[Keys.WALLPAPER_SOURCE],
                        WallpaperSource.DEFAULT
                    ),

                customWallpaperUri =
                    prefs[Keys.CUSTOM_WALLPAPER_URI],

                wallpaperTheme =
                    WallpaperCategory.fromStoredName(
                        prefs[Keys.WALLPAPER_THEME]
                    ),

                systemLayout =
                    enumValueOrDefault(
                        prefs[Keys.SYSTEM_LAYOUT],
                        SystemLayout.GRID_SEARCH_APPDRAWER
                    ),
                themeColor =
                    enumValueOrDefault(
                        prefs[Keys.THEME_COLOR],
                        ThemeColor.BLUE
                    ),
                appFont =
                    enumValueOrDefault(
                        prefs[Keys.APP_FONT],
                        AppFont.SANS
                    ),
                synapseNavigationEnabled =
                    prefs[Keys.SYNAPSE_NAVIGATION_ENABLED] ?: true,
                language =
                    enumValueOrDefault(
                        prefs[Keys.LANGUAGE],
                        AppLanguage.fromLocale()
                    ),
                username =
                    prefs[Keys.USERNAME] ?: ""
            )
        }

    suspend fun updateAppGridSize(size: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_GRID_SIZE] = size
        }
    }
    suspend fun updateAppRowSize(size: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_ROW_SIZE] = size
        }
    }

    suspend fun updateIconSize(size: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ICON_SIZE] = size
        }
    }

    suspend fun updateWallpaperSource(source: WallpaperSource) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WALLPAPER_SOURCE] = source.name
        }
    }

    suspend fun updateCustomWallpaperUri(uri: String?) {
        context.dataStore.edit { prefs ->

            if (uri != null) {
                prefs[Keys.CUSTOM_WALLPAPER_URI] = uri
            } else {
                prefs.remove(Keys.CUSTOM_WALLPAPER_URI)
            }
        }
    }

    suspend fun updateWallpaperTheme(theme: WallpaperCategory) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WALLPAPER_THEME] = theme.name
        }
    }

    suspend fun updateSystemLayout(layout: SystemLayout) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYSTEM_LAYOUT] = layout.name
        }
    }
    suspend fun updateSelectedAppPackages(packages: Set<String>) {
        updatePinnedApps(packages.sorted())
    }

    suspend fun updatePinnedApps(packages: List<String>) {
        val uniquePackages = packages.distinct()
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_APP_PACKAGES] = uniquePackages.toSet()
            prefs[Keys.PINNED_APP_ORDER] =
                uniquePackages.joinToString(PinnedAppsDelimiter)
        }
    }
    suspend fun updateThemeColor(color: ThemeColor) {

        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_COLOR] = color.name
        }
    }
    suspend fun updateAppFont(font: AppFont) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_FONT] = font.name
        }
    }
    suspend fun updateSynapseNavigationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYNAPSE_NAVIGATION_ENABLED] = enabled
        }
    }
    suspend fun updateOverrideTopBar(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERRIDE_TOP_BAR] = enabled
        }
    }
    suspend fun updateOverrideGridView(enabled2: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERRIDE_GRID_VIEW] = enabled2
        }
    }
    suspend fun updateOverrideWallpaperTheme(enabled2: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERRIDE_WALLPAPER_THEME] = enabled2
        }
    }
    suspend fun updateOverrideSystemLayout(enabled2: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERRIDE_SYSTEM_LAYOUT] = enabled2
        }
    }
    suspend fun updateOverrideColorTheme(enabled2: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERRIDE_COLOR_THEME] = enabled2
        }
    }
    suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.name
        }
    }

    suspend fun updateUsername(username: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USERNAME] = username
        }
    }
}
