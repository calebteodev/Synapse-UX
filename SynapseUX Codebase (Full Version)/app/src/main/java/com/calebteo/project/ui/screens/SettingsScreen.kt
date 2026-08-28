package com.calebteo.project.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.calebteo.project.data.*

import com.calebteo.project.ui.theme.ThemeColor
import com.calebteo.project.ui.theme.UnselectedColor1
import com.calebteo.project.ui.theme.UnselectedColor2
import com.calebteo.project.ui.theme.shouldUseDarkTheme
import com.calebteo.project.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed class SettingItem {

    data class Section(
        val title: String
    ) : SettingItem()

    data class Slider(
        val title: String,
        val value: Float,
        val range: ClosedFloatingPointRange<Float>,
        val steps: Int,
        val label: String,
        val onChange: (Float) -> Unit
    ) : SettingItem()

    data class Dropdown(
        val title: String,
        val options: List<String>,
        val selected: String,
        val onSelect: (String) -> Unit
    ) : SettingItem()

    data class SliderOverride(
        val title: String,
        val overrideEnabled: Boolean,
        val sliderValue: Float,
        val sliderRange: ClosedFloatingPointRange<Float>,
        val sliderSteps: Int,
        val sliderLabel: String,
        val onOverrideChange: (Boolean) -> Unit,
        val onSliderChange: (Float) -> Unit
    ) : SettingItem()

    data class DropdownOverride(
        val title: String,
        val overrideEnabled: Boolean,
        val options: List<String>,
        val selected: String,
        val dynamicBadgeText: String? = null,
        val dynamicDescriptionText: String? = null,
        val lockDynamicState: Boolean = false,
        val onOverrideChange: (Boolean) -> Unit,
        val onSelect: (String) -> Unit
    ) : SettingItem()
    data class CardSelector(
        val title: String,
        val selected: WallpaperSource,
        val onSelect: (WallpaperSource) -> Unit
    ) : SettingItem()
    data class Switch(
        val title: String,
        val subtitle: String? = null,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingItem()

    data class NavigationField(
        val title: String,
        val enabled: Boolean,
        val onClick: () -> Unit
    ) : SettingItem()
}

private enum class SettingsCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    SETTINGS("Synapse Settings", "联想设置", "Tetapan Synapse", "Ajustes de Synapse", "Parametres Synapse"),
    FREQUENTS("Frequent Apps", "常用应用程序", "Apl Kerap Digunakan", "Apps Frecuentes", "Apps Frequentes"),
    PINNED_APPS("Pinned Apps", "已固定应用程序", "Apl Disemat", "Apps Fijadas", "Apps Epinglees"),
    PINNED_SECTION("Pinned", "已固定", "Disemat", "Fijadas", "Epinglees"),
    OTHER_APPS_SECTION("Other Apps", "其他应用程序", "Apl Lain", "Otras Apps", "Autres Apps"),
    PINNED_APPS_DESCRIPTION(
        "The following apps will always appear on top of your home UI",
        "以下应用程序将始终显示在主界面顶部",
        "Apl berikut akan sentiasa muncul di bahagian atas UI utama anda",
        "Las siguientes apps siempre apareceran en la parte superior de tu interfaz principal",
        "Les applications suivantes apparaitront toujours en haut de votre interface d'accueil"
    ),
    REORDER_PINNED_APPS(
        "Reorder pinned apps by dragging",
        "拖动即可重新排序已固定应用程序",
        "Susun semula apl disemat dengan menyeret",
        "Reordena las apps fijadas arrastrandolas",
        "Reorganisez les apps epinglees en les faisant glisser"
    ),
    WALLPAPER("Wallpaper", "壁纸", "Kertas Dinding", "Fondo de Pantalla", "Fond d'ecran"),
    WIDGETS("Synapse Widgets", "联想组件", "Widget Synapse", "Widgets de Synapse", "Widgets Synapse"),
    SYNAPSE_NAVIGATION_PERMISSION_REQUIRED(
        "Turn on accessibility to re-enable Synapse Navigation.",
        "开启辅助功能以重新启用联想导航。",
        "Hidupkan kebolehaksesan untuk mengaktifkan semula Navigasi Synapse.",
        "Activa la accesibilidad para volver a habilitar Synapse Navigation.",
        "Activez l'accessibilite pour reactiver la navigation Synapse."
    ),
    LAYOUT("Home Screen Layout", "主屏幕布局", "Susun Atur Skrin Utama", "Diseno de Pantalla de Inicio", "Disposition de l'ecran d'accueil"),
    APPS_IN_TOP_BAR("Apps in Top Bar", "顶部栏中应用程序", "Apl di Bar Atas", "Apps en la Barra Superior", "Apps dans la Barre Superieure"),
    APPS_PER_GRID_ROW("Apps per Grid Row", "每行多少应用程序", "Apl Setiap Baris Grid", "Apps por Fila", "Apps par Rangee"),
    WALLPAPER_SOURCE("Wallpaper Source", "壁纸来源", "Sumber Kertas Dinding", "Origen del Fondo", "Source du Fond d'ecran"),
    WALLPAPER_THEME("Wallpaper Theme", "壁纸类别", "Tema Kertas Dinding", "Tema del Fondo", "Theme du Fond d'ecran"),
    SYSTEM_LAYOUT("Synapse Layout", "联想布局", "Susun Atur Synapse", "Diseno de Synapse", "Disposition Synapse"),
    OVERRIDE_COLOUR_THEME("Colour Theme", "界面颜色", "Tema Warna", "Tema de Color", "Theme de Couleur"),
    FONT("Font", "字体", "Fon", "Fuente", "Police"),
    VIEW_DYNAMIC_LOGIC(
        "Synapse Technical Logic",
        "技术细节，请切换到英文。",
        "Butiran teknikal, tukar ke bahasa Inggeris.",
        "Detalles tecnicos, cambia a ingles.",
        "Details techniques, passez en anglais."
    ),
    LANGUAGE("Language", "语言", "Bahasa", "Idioma", "Langue"),
    SETTINGS_UPDATED("Settings updated", "设置已更新", "Tetapan dikemas kini", "Ajustes actualizados", "Parametres mis a jour"),
    DYNAMIC_SETTINGS("Dynamic settings", "动态设置", "Tetapan dinamik", "Ajustes dinamicos", "Parametres dynamiques"),
    MANUAL_SETTINGS("Manual settings", "手动设置", "Tetapan manual", "Ajustes manuales", "Parametres manuels"),
    DYNAMIC_UPDATED("Dynamic updated", "动态已更新", "Dinamik dikemas kini", "Dinamico actualizado", "Dynamique mis a jour"),
    SYSTEM_SETTINGS_UPDATED("System settings updated", "系统设置已更新", "Tetapan sistem dikemas kini", "Configuracion del sistema actualizada", "Parametres systeme mis a jour"),
    MANUAL_SETTINGS_UPDATED("Manual settings updated", "手动设置已更新", "Tetapan manual dikemas kini", "Configuracion manual actualizada", "Parametres manuels mis a jour"),
    TECHNICAL_DETAILS("Synapse Technical Logic", "联想技术逻辑", "Logik Teknikal Synapse", "Logica Tecnica de Synapse", "Logique Technique Synapse"),
    APPEARANCE_INSIGHTS("Appearance Insights", "外观分析", "Analisis Penampilan", "Analisis de Apariencia", "Analyse de l'apparence"),
    WALLPAPER_INSIGHTS("Wallpaper Insights", "壁纸分析", "Analisis Kertas Dinding", "Analisis del Fondo", "Analyse du fond d'ecran"),
    SYSTEM_INSIGHTS("System Insights", "系统分析", "Analisis Sistem", "Analisis del Sistema", "Analyse du systeme"),
    WIDGETS_INSIGHTS("Widgets Insights", "组件分析", "Analisis Widget", "Analisis de Widgets", "Analyse des widgets"),
    SYNAPSE_NAVIGATION("Synapse Navigation", "联想导航", "Navigasi Synapse", "Navegacion Synapse", "Navigation Synapse"),
    EMAIL_SUPPORT("Email support", "发送支持邮件", "E-mel sokongan", "Correo de soporte", "E-mail de support"),
    RESET_WALLPAPER("Reset wallpaper", "重置壁纸", "Tetapkan semula kertas dinding", "Restablecer fondo", "Reinitialiser le fond d'ecran");

    fun text(language: AppLanguage): String {
        return when (language) {
            AppLanguage.ENGLISH -> english
            AppLanguage.MANDARIN -> mandarin
            AppLanguage.MALAY -> malay
            AppLanguage.SPANISH -> spanish
            AppLanguage.FRENCH -> french
        }
    }
}

private fun settingsTitle(settings: LauncherSettings): String {
    val trimmedUsername = settings.username.trim()
    if (trimmedUsername.isBlank()) {
        return SettingsCopy.SETTINGS.text(settings.language)
    }

    return when (settings.language) {
        AppLanguage.ENGLISH -> "Welcome Back $trimmedUsername"
        AppLanguage.MANDARIN -> "欢迎回来 $trimmedUsername"
        AppLanguage.MALAY -> "Selamat kembali $trimmedUsername"
        AppLanguage.SPANISH -> "Bienvenido de nuevo $trimmedUsername"
        AppLanguage.FRENCH -> "Bon retour $trimmedUsername"
    }
}

private enum class SettingsAuxScreen {
    TECHNICAL_MENU,
    PINNED_APPS,
    APPEARANCE,
    WALLPAPER,
    SYSTEM,
    WIDGETS
}

private enum class SettingsOptionCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    WALLPAPER_DYNAMIC("DYNAMIC", "动态", "DINAMIK", "DINAMICO", "DYNAMIQUE"),
    WALLPAPER_LOCAL("LOCAL", "本地", "TEMPATAN", "LOCAL", "LOCAL"),
    WALLPAPER_MANUAL("MANUAL", "手动设置", "MANUAL", "MANUALES", "MANUELS"),
    WALLPAPER_DYNAMIC_DESC("System", "系统", "Sistem", "Sistema", "Systeme"),
    WALLPAPER_LOCAL_DESC("Android", "安卓", "Android", "Android", "Android"),
    WALLPAPER_MANUAL_DESC("Gallery", "相册", "Galeri", "Galeria", "Galerie"),
    THEME_ABSTRACT("Abstract", "抽象", "Abstrak", "Abstracto", "Abstrait"),
    THEME_ARTEFACTS("Artefacts", "器物", "Artefak", "Artefactos", "Artefacts"),
    THEME_CYBERPUNK("Cyberpunk", "赛博朋克", "Siberpunk", "Cyberpunk", "Cyberpunk"),
    THEME_FOOD_AND_DRINK("Food and Drink", "美食饮品", "Makanan dan Minuman", "Comida y Bebida", "Cuisine et Boissons"),
    THEME_LANDSCAPES("Landscapes", "风景", "Landskap", "Paisajes", "Paysages"),
    THEME_SPACE("Space", "太空", "Angkasa", "Espacio", "Espace"),
    THEME_URBAN("Urban", "城市", "Bandar", "Urbano", "Urbain"),
    THEME_WILDLIFE("Wildlife", "野生动物", "Hidupan Liar", "Vida Silvestre", "Faune"),
    LAYOUT_MAIN_LIST("List View", "列表视图", "Paparan Senarai", "Vista de Lista", "Vue Liste"),
    LAYOUT_MAIN_LIST_GRID("Half List-Half Grid", "半列表半网格", "Separuh Senarai-Separuh Grid", "Mitad Lista-Mitad Cuadricula", "Moitie Liste-Moitie Grille"),
    LAYOUT_GRID_SEARCH_LIST("Grid View", "网格视图", "Paparan Grid", "Vista de Cuadricula", "Vue Grille"),
    LAYOUT_WIDGETS_SEARCH_LIST("Two Widgets", "双组件", "Dua Widget", "Dos Widgets", "Deux Widgets"),
    LAYOUT_GRID_SEARCH_APPDRAWER("Grid View with App Drawer", "带应用抽屉网格视图", "Paparan Grid dengan Laci Apl", "Vista de Cuadricula con Cajon de Apps", "Vue Grille avec Tiroir d'Apps"),
    LAYOUT_WIDGETS_SEARCH_APPDRAWER("Two Widgets with App Drawer", "带应用抽屉双组件", "Dua Widget dengan Laci Apl", "Dos Widgets con Cajon de Apps", "Deux Widgets avec Tiroir d'Apps"),
    COLOR_RED("Red", "红色", "Merah", "Rojo", "Rouge"),
    COLOR_GREEN("Green", "绿色", "Hijau", "Verde", "Vert"),
    COLOR_BLUE("Blue", "蓝色", "Biru", "Azul", "Bleu"),
    COLOR_INDIGO("Indigo", "靛蓝", "Indigo", "Indigo", "Indigo"),
    COLOR_ORANGE("Orange", "橙色", "Oren", "Naranja", "Orange"),
    COLOR_YELLOW("Yellow", "黄色", "Kuning", "Amarillo", "Jaune"),
    COLOR_VIOLET("Violet", "紫罗兰", "Ungu", "Violeta", "Violet"),
    COLOR_GREY("Grey", "灰色", "Kelabu", "Gris", "Gris"),
    COLOR_MAROON("Maroon", "栗色", "Marun", "Granate", "Bordeaux"),
    COLOR_OLIVE("Olive", "橄榄色", "Zaitun", "Oliva", "Olive"),
    COLOR_CYAN("Cyan", "青色", "Sian", "Cian", "Cyan"),
    COLOR_PINK("Pink", "粉色", "Merah Jambu", "Rosa", "Rose"),
    COLOR_MAGENTA("Magenta", "品红", "Magenta", "Magenta", "Magenta"),
    COLOR_TAN("Tan", "棕褐色", "Perang Muda", "Canela", "Brun Clair"),
    COLOR_TEAL("Teal", "蓝绿色", "Teal", "Verde Azulado", "Sarcelle"),
    FONT_SANS("Sans Serif", "无衬线", "Sans Serif", "Sans Serif", "Sans Serif"),
    FONT_SERIF("Serif", "衬线", "Serif", "Serif", "Serif"),
    FONT_MONO("Monospace", "等宽", "Monospace", "Monoespaciada", "Monospace"),
    DYNAMIC_BADGE("DYNAMIC", "动态", "DINAMIK", "DINAMICO", "DYNAMIQUE"),
    TAP_TO_SWITCH_MANUAL("Tap to switch to manual", "点击切换到手动", "Ketik untuk tukar ke manual", "Toca para cambiar a manual", "Appuyez pour passer en mode manuel"),
    NO_INTERNET("NO INTERNET", "无网络", "TIADA INTERNET", "SIN INTERNET", "PAS D'INTERNET"),
    LIMITED_WALLPAPER_SELECTION(
        "Limited wallpaper selection",
        "壁纸选择受限",
        "Pilihan kertas dinding terhad",
        "Seleccion limitada de fondos",
        "Selection limitee de fonds d'ecran"
    );

    fun text(language: AppLanguage): String {
        return when (language) {
            AppLanguage.ENGLISH -> english
            AppLanguage.MANDARIN -> mandarin
            AppLanguage.MALAY -> malay
            AppLanguage.SPANISH -> spanish
            AppLanguage.FRENCH -> french
        }
    }
}

private fun wallpaperThemeLabel(theme: WallpaperCategory, language: AppLanguage): String =
    when (theme) {
        WallpaperCategory.ABSTRACT -> SettingsOptionCopy.THEME_ABSTRACT.text(language)
        WallpaperCategory.ARTEFACTS -> SettingsOptionCopy.THEME_ARTEFACTS.text(language)
        WallpaperCategory.CYBERPUNK -> SettingsOptionCopy.THEME_CYBERPUNK.text(language)
        WallpaperCategory.FOOD_AND_DRINK -> SettingsOptionCopy.THEME_FOOD_AND_DRINK.text(language)
        WallpaperCategory.LANDSCAPES -> SettingsOptionCopy.THEME_LANDSCAPES.text(language)
        WallpaperCategory.SPACE -> SettingsOptionCopy.THEME_SPACE.text(language)
        WallpaperCategory.URBAN -> SettingsOptionCopy.THEME_URBAN.text(language)
        WallpaperCategory.WILDLIFE -> SettingsOptionCopy.THEME_WILDLIFE.text(language)
    }

private fun systemLayoutLabel(layout: SystemLayout, language: AppLanguage): String =
    when (layout) {
        SystemLayout.MAIN_LIST -> SettingsOptionCopy.LAYOUT_MAIN_LIST.text(language)
        SystemLayout.MAIN_LIST_GRID -> SettingsOptionCopy.LAYOUT_MAIN_LIST_GRID.text(language)
        SystemLayout.GRID_SEARCH_LIST -> SettingsOptionCopy.LAYOUT_GRID_SEARCH_LIST.text(language)
        SystemLayout.WIDGETS_SEARCH_LIST -> SettingsOptionCopy.LAYOUT_WIDGETS_SEARCH_LIST.text(language)
        SystemLayout.GRID_SEARCH_APPDRAWER -> SettingsOptionCopy.LAYOUT_GRID_SEARCH_APPDRAWER.text(language)
        SystemLayout.WIDGETS_SEARCH_APPDRAWER -> SettingsOptionCopy.LAYOUT_WIDGETS_SEARCH_APPDRAWER.text(language)
    }

private fun themeColorLabel(color: ThemeColor, language: AppLanguage): String =
    when (color) {
        ThemeColor.RED -> SettingsOptionCopy.COLOR_RED.text(language)
        ThemeColor.GREEN -> SettingsOptionCopy.COLOR_GREEN.text(language)
        ThemeColor.BLUE -> SettingsOptionCopy.COLOR_BLUE.text(language)
        ThemeColor.INDIGO -> SettingsOptionCopy.COLOR_INDIGO.text(language)
        ThemeColor.ORANGE -> SettingsOptionCopy.COLOR_ORANGE.text(language)
        ThemeColor.YELLOW -> SettingsOptionCopy.COLOR_YELLOW.text(language)
        ThemeColor.VIOLET -> SettingsOptionCopy.COLOR_VIOLET.text(language)
        ThemeColor.GREY -> SettingsOptionCopy.COLOR_GREY.text(language)
        ThemeColor.MAROON -> SettingsOptionCopy.COLOR_MAROON.text(language)
        ThemeColor.OLIVE -> SettingsOptionCopy.COLOR_OLIVE.text(language)
        ThemeColor.CYAN -> SettingsOptionCopy.COLOR_CYAN.text(language)
        ThemeColor.PINK -> SettingsOptionCopy.COLOR_PINK.text(language)
        ThemeColor.MAGENTA -> SettingsOptionCopy.COLOR_MAGENTA.text(language)
        ThemeColor.TAN -> SettingsOptionCopy.COLOR_TAN.text(language)
        ThemeColor.TEAL -> SettingsOptionCopy.COLOR_TEAL.text(language)
    }

private fun appFontLabel(font: AppFont, language: AppLanguage): String =
    when (font) {
        AppFont.SANS -> SettingsOptionCopy.FONT_SANS.text(language)
        AppFont.SERIF -> SettingsOptionCopy.FONT_SERIF.text(language)
        AppFont.MONO -> SettingsOptionCopy.FONT_MONO.text(language)
        AppFont.CURSIVE -> SettingsOptionCopy.FONT_SANS.text(language)
    }

private fun languageLabel(language: AppLanguage): String =
    when (language) {
        AppLanguage.ENGLISH -> "English 🇬🇧"
        AppLanguage.MANDARIN -> "Mandarin 🇨🇳"
        AppLanguage.MALAY -> "Malay 🇲🇾"
        AppLanguage.SPANISH -> "Spanish 🇪🇸"
        AppLanguage.FRENCH -> "French 🇫🇷"
    }

private fun wallpaperSourceUpdatedText(
    source: WallpaperSource,
    language: AppLanguage
): String =
    when (source) {
        WallpaperSource.DEFAULT -> SettingsCopy.MANUAL_SETTINGS_UPDATED.text(language)
        WallpaperSource.SYSTEM -> SettingsCopy.MANUAL_SETTINGS_UPDATED.text(language)
        WallpaperSource.CUSTOM -> SettingsCopy.MANUAL_SETTINGS_UPDATED.text(language)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    onBack: () -> Unit,
    onUpdateAppGridSize: (Int) -> Unit,
    onUpdateAppRowSize: (Int) -> Unit,
    launcherViewModel: LauncherViewModel,
    onUpdateIconSize: (Int) -> Unit,
    onUpdateWallpaperSource: (WallpaperSource) -> Unit,
    onUpdateCustomWallpaperUri: (String?) -> Unit,
    onUpdateOverrideWallpaperTheme: (Boolean) -> Unit,
    onUpdateWallpaperTheme: (WallpaperCategory) -> Unit,
    onUpdateOverrideTopBar: (Boolean) -> Unit,
    onUpdateOverrideGridView: (Boolean) -> Unit,
    onUpdateOverrideSystemLayout: (Boolean) -> Unit,
    onUpdateSystemLayout: (SystemLayout) -> Unit,
    onUpdateOverrideThemeColor: (Boolean) -> Unit,
    onUpdateThemeColor: (ThemeColor) -> Unit,
    onUpdateAppFont: (AppFont) -> Unit,
    onUpdateSynapseNavigationEnabled: (Boolean) -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    hasOverviewAccessibility: Boolean,
    onOpenAccessibilitySettings: () -> Unit
) {

    val context = LocalContext.current
    val isOffline = !hasInternetConnection(context)
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentAuxScreen by remember { mutableStateOf<SettingsAuxScreen?>(null) }
    val apps by launcherViewModel.filteredApps.collectAsState()
    val installedApps by launcherViewModel.installedApps.collectAsState()
    val usageCountMap by launcherViewModel.usageCountMap.collectAsState()
    val sortedInstalledApps = remember(installedApps, settings.pinnedAppPackages) {
        val pinnedOrder = settings.pinnedAppPackages.withIndex().associate { it.value to it.index }
        installedApps.sortedWith(
            compareBy<AppInfo> { pinnedOrder[it.packageName] ?: Int.MAX_VALUE }
                .thenBy { it.label.lowercase() }
        )
    }
    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                onUpdateCustomWallpaperUri(it.toString())
                onUpdateWallpaperSource(WallpaperSource.CUSTOM)
            }
        }

    fun showSettingsUpdatedSnackbar() {
        scope.launch {
            snackbarHostState.showSnackbar(
                SettingsCopy.SETTINGS_UPDATED.text(settings.language)
            )
        }
    }

    fun showSettingsModeToast(enabled: Boolean) {
        val message = if (enabled) {
            SettingsCopy.DYNAMIC_SETTINGS.text(settings.language)
        } else {
            SettingsCopy.MANUAL_SETTINGS.text(settings.language)
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun togglePinnedApp(packageName: String, pinned: Boolean) {
        val updatedPinnedApps = settings.pinnedAppPackages.toMutableList().apply {
            if (pinned) {
                if (packageName !in this) add(packageName)
            } else {
                remove(packageName)
            }
        }

        scope.launch {
            launcherViewModel.updatePinnedApps(updatedPinnedApps)
            snackbarHostState.showSnackbar(
                SettingsCopy.SETTINGS_UPDATED.text(settings.language)
            )
        }
    }

    val settingsItems = listOf(

        SettingItem.Section(SettingsCopy.FREQUENTS.text(settings.language)),

        SettingItem.SliderOverride(
            title = SettingsCopy.APPS_IN_TOP_BAR.text(settings.language),
            overrideEnabled = settings.overrideTopBar,
            sliderValue = settings.appGridSize.toFloat(),
            sliderRange = 1f..4f,
            sliderSteps = 3,
            sliderLabel = settings.appGridSize.toString(),
            onOverrideChange = {
                onUpdateOverrideTopBar(it)
                showSettingsModeToast(it)
            },
            onSliderChange = {
                onUpdateAppGridSize(it.toInt())
                showSettingsUpdatedSnackbar()
            }
        ),

        SettingItem.SliderOverride(
            title = SettingsCopy.APPS_PER_GRID_ROW.text(settings.language),
            overrideEnabled = settings.overrideGridView,
            sliderValue = settings.appRowSize.toFloat(),
            sliderRange = 3f..6f,
            sliderSteps = 3,
            sliderLabel = settings.appRowSize.toString(),
            onOverrideChange = {
                onUpdateOverrideGridView(it)
                showSettingsModeToast(it)
            },
            onSliderChange = {
                onUpdateAppRowSize(it.toInt())
                showSettingsUpdatedSnackbar()
            }
        ),

        SettingItem.NavigationField(
            title = SettingsCopy.PINNED_APPS.text(settings.language),
            enabled = true,
            onClick = { currentAuxScreen = SettingsAuxScreen.PINNED_APPS }
        ),

        SettingItem.NavigationField(
            title = SettingsCopy.VIEW_DYNAMIC_LOGIC.text(settings.language),
            enabled = settings.language == AppLanguage.ENGLISH,
            onClick = { currentAuxScreen = SettingsAuxScreen.APPEARANCE }
        ),
        SettingItem.Section(SettingsCopy.SYSTEM_LAYOUT.text(settings.language)),

        SettingItem.DropdownOverride(
            title = SettingsCopy.LAYOUT.text(settings.language),
            overrideEnabled = settings.overrideSystemLayout,
            options = SystemLayout.values().map { systemLayoutLabel(it, settings.language) },
            selected = systemLayoutLabel(settings.systemLayout, settings.language),
            onOverrideChange = {
                onUpdateOverrideSystemLayout(it)
                showSettingsModeToast(it)
            },
            onSelect = {
                val layout = SystemLayout.values().first { layout ->
                    systemLayoutLabel(layout, settings.language) == it
                }
                onUpdateSystemLayout(layout)
                showSettingsUpdatedSnackbar()
            }
        ),

        SettingItem.NavigationField(
            title = SettingsCopy.VIEW_DYNAMIC_LOGIC.text(settings.language),
            enabled = settings.language == AppLanguage.ENGLISH,
            onClick = { currentAuxScreen = SettingsAuxScreen.SYSTEM }
        ),
        SettingItem.Section(SettingsCopy.WALLPAPER.text(settings.language)),

        SettingItem.CardSelector(
            title = SettingsCopy.WALLPAPER_SOURCE.text(settings.language),
            selected = settings.wallpaperSource,
            onSelect = { source ->

                if (source == WallpaperSource.CUSTOM) {
                                        imagePickerLauncher.launch(arrayOf("image/*"))
                } else {
                    onUpdateWallpaperSource(source)
                    if (source == WallpaperSource.SYSTEM) {
                        onUpdateThemeColor(ThemeColor.BLUE)
                    }
                }

                scope.launch {
                    snackbarHostState.showSnackbar(
                        wallpaperSourceUpdatedText(source, settings.language)
                    )
                }
            }
        ),

        SettingItem.DropdownOverride(
            title = SettingsCopy.WALLPAPER_THEME.text(settings.language),
            overrideEnabled = settings.overrideWallpaperTheme,
            options = WallpaperCategory.values().map { wallpaperThemeLabel(it, settings.language) },
            selected = wallpaperThemeLabel(settings.wallpaperTheme, settings.language),
            dynamicBadgeText = if (isOffline) {
                SettingsOptionCopy.NO_INTERNET.text(settings.language)
            } else {
                null
            },
            dynamicDescriptionText = if (isOffline) {
                SettingsOptionCopy.LIMITED_WALLPAPER_SELECTION.text(settings.language)
            } else {
                null
            },
            lockDynamicState = isOffline,
            onOverrideChange = {
                onUpdateOverrideWallpaperTheme(it)
                showSettingsModeToast(it)
            },
            onSelect = {
                val theme = WallpaperCategory.values().first { theme ->
                    wallpaperThemeLabel(theme, settings.language) == it
                }
                onUpdateWallpaperTheme(theme)
                showSettingsUpdatedSnackbar()
            }
        ),
        SettingItem.DropdownOverride(
            title = SettingsCopy.OVERRIDE_COLOUR_THEME.text(settings.language),
            overrideEnabled = settings.overrideColorTheme,
            options = ThemeColor.values().map { themeColorLabel(it, settings.language) },
            selected = themeColorLabel(settings.themeColor, settings.language),
            onOverrideChange = {
                onUpdateOverrideThemeColor(it)
                showSettingsModeToast(it)
            },
            onSelect = {
                val color = ThemeColor.values().first { color ->
                    themeColorLabel(color, settings.language) == it
                }
                onUpdateThemeColor(color)
                showSettingsUpdatedSnackbar()
            }
        ),
        SettingItem.Dropdown(
            title = SettingsCopy.FONT.text(settings.language),
            options = listOf(AppFont.SANS, AppFont.SERIF, AppFont.MONO)
                .map { appFontLabel(it, settings.language) },
            selected = appFontLabel(settings.appFont, settings.language),
            onSelect = {
                val font = listOf(AppFont.SANS, AppFont.SERIF, AppFont.MONO).first { appFont ->
                    appFontLabel(appFont, settings.language) == it
                }
                onUpdateAppFont(font)
                showSettingsUpdatedSnackbar()
            }
        ),

        SettingItem.NavigationField(
            title = SettingsCopy.VIEW_DYNAMIC_LOGIC.text(settings.language),
            enabled = settings.language == AppLanguage.ENGLISH,
            onClick = { currentAuxScreen = SettingsAuxScreen.WALLPAPER }
        ),

        SettingItem.Section(SettingsCopy.WIDGETS.text(settings.language)),
        SettingItem.Switch(
            title = SettingsCopy.SYNAPSE_NAVIGATION.text(settings.language),
            subtitle = if (!hasOverviewAccessibility && !settings.synapseNavigationEnabled) {
                SettingsCopy.SYNAPSE_NAVIGATION_PERMISSION_REQUIRED.text(settings.language)
            } else {
                null
            },
            checked = settings.synapseNavigationEnabled,
            onCheckedChange = {
                if (it && !hasOverviewAccessibility) {
                    onOpenAccessibilitySettings()
                } else {
                    onUpdateSynapseNavigationEnabled(it)
                }
                showSettingsUpdatedSnackbar()
            }
        ),
        SettingItem.NavigationField(
            title = SettingsCopy.VIEW_DYNAMIC_LOGIC.text(settings.language),
            enabled = settings.language == AppLanguage.ENGLISH,
            onClick = { currentAuxScreen = SettingsAuxScreen.WIDGETS }
        ),

        SettingItem.Section(""),

        SettingItem.Dropdown(
            title = SettingsCopy.LANGUAGE.text(settings.language),
            options = AppLanguage.values().map { languageLabel(it) },
            selected = languageLabel(settings.language),
            onSelect = { selected ->
                val language = AppLanguage.values().first { languageLabel(it) == selected }
                onUpdateLanguage(language)
                showSettingsUpdatedSnackbar()
            }
        )
    )

    val sections = buildList {
        var currentTitle: String? = null
        var currentItems = mutableListOf<SettingItem>()

        settingsItems.forEach { item ->
            when (item) {
                is SettingItem.Section -> {
                    if (currentTitle != null) {
                        add(currentTitle!! to currentItems.toList())
                    }
                    currentTitle = item.title
                    currentItems = mutableListOf()
                }
                else -> currentItems.add(item)
            }
        }

        if (currentTitle != null) {
            add(currentTitle!! to currentItems.toList())
        }
    }

    if (currentAuxScreen != null) {
        when (currentAuxScreen!!) {
            SettingsAuxScreen.TECHNICAL_MENU -> {
                TechnicalDetailsScreen(
                    language = settings.language,
                    onBack = { currentAuxScreen = null },
                    onOpenAppearance = { currentAuxScreen = SettingsAuxScreen.APPEARANCE },
                    onOpenWallpaper = { currentAuxScreen = SettingsAuxScreen.WALLPAPER },
                    onOpenSystem = { currentAuxScreen = SettingsAuxScreen.SYSTEM },
                    onOpenWidgets = { currentAuxScreen = SettingsAuxScreen.WIDGETS }
                )
            }
            SettingsAuxScreen.PINNED_APPS ->
                PinnedAppsScreen(
                    language = settings.language,
                    apps = sortedInstalledApps,
                    pinnedPackages = settings.pinnedAppPackages,
                    onBack = { currentAuxScreen = null },
                    onTogglePinnedApp = ::togglePinnedApp,
                    onReorderPinnedApps = { reorderedPackages ->
                        scope.launch {
                            launcherViewModel.updatePinnedApps(reorderedPackages)
                            snackbarHostState.showSnackbar(
                                SettingsCopy.SETTINGS_UPDATED.text(settings.language)
                            )
                        }
                    }
                )
            SettingsAuxScreen.APPEARANCE ->
                AppearanceDataAnalyScreen(
                    onBack = { currentAuxScreen = null },
                    apps = apps ,
                    usageCountMap = usageCountMap,
                )
            SettingsAuxScreen.WALLPAPER ->
                WallpaperDataAnalyScreen(
                    onBack = { currentAuxScreen = null }
                )
            SettingsAuxScreen.SYSTEM ->
                SystemDataAnalyScreen(
                    onBack = { currentAuxScreen = null }
                )
            SettingsAuxScreen.WIDGETS ->
                WidgetDataAnalyScreen(
                    onBack = { currentAuxScreen = null }
                )
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },

        topBar = {

            TopAppBar(
                title = { Text("Settings") },

                navigationIcon = {

                    IconButton(onClick = onBack) {

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
            )
        }

    ) { paddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
        ) {
            sections.forEach { (title, items) ->
                renderSection(
                    sectionTitle = title,
                    sectionItems = items,
                    language = settings.language
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalDetailsScreen(
    language: AppLanguage,
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onOpenSystem: () -> Unit,
    onOpenWidgets: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(SettingsCopy.TECHNICAL_DETAILS.text(language)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DynamicLogicEntryField(
                title = SettingsCopy.APPEARANCE_INSIGHTS.text(language),
                enabled = language == AppLanguage.ENGLISH,
                onClick = onOpenAppearance
            )
            DynamicLogicEntryField(
                title = SettingsCopy.WALLPAPER_INSIGHTS.text(language),
                enabled = language == AppLanguage.ENGLISH,
                onClick = onOpenWallpaper
            )
            DynamicLogicEntryField(
                title = SettingsCopy.SYSTEM_INSIGHTS.text(language),
                enabled = language == AppLanguage.ENGLISH,
                onClick = onOpenSystem
            )
            DynamicLogicEntryField(
                title = SettingsCopy.WIDGETS_INSIGHTS.text(language),
                enabled = language == AppLanguage.ENGLISH,
                onClick = onOpenWidgets
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinnedAppsScreen(
    language: AppLanguage,
    apps: List<AppInfo>,
    pinnedPackages: List<String>,
    onBack: () -> Unit,
    onTogglePinnedApp: (String, Boolean) -> Unit,
    onReorderPinnedApps: (List<String>) -> Unit
) {
    val scrollState = rememberScrollState()
    var orderedPinnedPackages by remember(pinnedPackages) {
        mutableStateOf(pinnedPackages)
    }
    fun updateLocalPinnedPackages(updatedPackages: List<String>) {
        orderedPinnedPackages = updatedPackages
    }
    val pinnedApps = remember(apps, orderedPinnedPackages) {
        val appsByPackage = apps.associateBy { it.packageName }
        orderedPinnedPackages.mapNotNull { packageName -> appsByPackage[packageName] }
    }
    val otherApps = remember(apps, orderedPinnedPackages) {
        apps.filter { it.packageName !in orderedPinnedPackages }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(SettingsCopy.PINNED_APPS.text(language)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = SettingsCopy.PINNED_APPS_DESCRIPTION.text(language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = SettingsCopy.REORDER_PINNED_APPS.text(language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(16.dp))

                    SectionHeader(SettingsCopy.PINNED_SECTION.text(language))
                    Spacer(Modifier.height(8.dp))

                    pinnedApps.forEach { app ->
                        DraggablePinnedAppRow(
                            app = app,
                            checked = true,
                            itemCount = pinnedApps.size,
                            index = pinnedApps.indexOf(app),
                            onCheckedChange = { pinned ->
                                if (!pinned) {
                                    updateLocalPinnedPackages(
                                        orderedPinnedPackages.filterNot { it == app.packageName }
                                    )
                                }
                                onTogglePinnedApp(app.packageName, pinned)
                            },
                            onMove = { fromIndex, toIndex ->
                                val reorderedPackages = orderedPinnedPackages.toMutableList().apply {
                                    add(toIndex, removeAt(fromIndex))
                                }
                                updateLocalPinnedPackages(reorderedPackages)
                                onReorderPinnedApps(reorderedPackages)
                            }
                        )
                    }

                    if (pinnedApps.isNotEmpty() && otherApps.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                    }

                    SectionHeader(SettingsCopy.OTHER_APPS_SECTION.text(language))
                    Spacer(Modifier.height(8.dp))

                    otherApps.forEach { app ->
                        SwitchSetting(
                            title = app.label,
                            subtitle = app.packageName,
                            checked = false,
                            onCheckedChange = { pinned ->
                                if (pinned) {
                                    updateLocalPinnedPackages(
                                        orderedPinnedPackages + app.packageName
                                    )
                                }
                                onTogglePinnedApp(app.packageName, pinned)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggablePinnedAppRow(
    app: AppInfo,
    checked: Boolean,
    itemCount: Int,
    index: Int,
    onCheckedChange: (Boolean) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 72.dp.toPx() }
    var dragOffset by remember(app.packageName) { mutableStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(x = 0, y = dragOffset.roundToInt()) }
            .pointerInput(app.packageName, index, itemCount) {
                detectDragGesturesAfterLongPress(
                    onDragEnd = {
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        dragOffset = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount.y

                    when {
                        dragOffset > rowHeightPx / 2f && index < itemCount - 1 -> {
                            onMove(index, index + 1)
                            dragOffset -= rowHeightPx
                        }
                        dragOffset < -rowHeightPx / 2f && index > 0 -> {
                            onMove(index, index - 1)
                            dragOffset += rowHeightPx
                        }
                    }
                }
            }
    ) {
        SwitchSetting(
            title = app.label,
            subtitle = app.packageName,
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun renderSection(
    sectionTitle: String,
    sectionItems: List<SettingItem>,
    language: AppLanguage
) {
    val showTitle = sectionTitle.isNotBlank()

    OutlinedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            if (showTitle) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(12.dp))
            }

            sectionItems.forEach { item ->

                when (item) {

                    is SettingItem.Slider ->
                        SliderSetting(
                            title = item.title,
                            value = item.value,
                            valueRange = item.range,
                            steps = item.steps,
                            valueLabel = item.label,
                            onValueChange = item.onChange
                        )

                    is SettingItem.Dropdown ->
                        DropdownSetting(
                            title = item.title,
                            options = item.options,
                            selectedOption = item.selected,
                            onOptionSelected = item.onSelect
                        )

                    is SettingItem.SliderOverride ->
                        TopBarOverrideSlider(
                            text = item.title,
                            language = language,
                            overrideEnabled = item.overrideEnabled,
                            sliderValue = item.sliderValue,
                            sliderRange = item.sliderRange,
                            sliderSteps = item.sliderSteps,
                            sliderLabel = item.sliderLabel,
                            onOverrideChange = item.onOverrideChange,
                            onSliderChange = item.onSliderChange
                        )

                    is SettingItem.DropdownOverride ->
                        WallpaperThemeOverrideDropdown(
                            text = item.title,
                            language = language,
                            overrideEnabled = item.overrideEnabled,
                            options = item.options,
                            selectedOption = item.selected,
                            dynamicBadgeText = item.dynamicBadgeText,
                            dynamicDescriptionText = item.dynamicDescriptionText,
                            lockDynamicState = item.lockDynamicState,
                            onOverrideChange = item.onOverrideChange,
                            onOptionSelected = item.onSelect
                        )

                    is SettingItem.CardSelector ->
                        CardSetting(
                            title = item.title,
                            selectedSource = item.selected,
                            onSourceSelected = item.onSelect,
                            language = language
                        )

                    is SettingItem.Switch ->
                        SwitchSetting(
                            title = item.title,
                            subtitle = item.subtitle,
                            checked = item.checked,
                            onCheckedChange = item.onCheckedChange
                        )

                    is SettingItem.NavigationField ->
                    {Spacer(Modifier.height(12.dp))
                        DynamicLogicEntryField(
                            title = item.title,
                            enabled = item.enabled,
                            onClick = item.onClick
                        )}
                    is SettingItem.Section ->
                        SectionHeader(item.title)
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DynamicLogicEntryField(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                },
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SwitchSetting(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SliderSetting(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSetting(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun TopBarOverrideSlider(
    text: String,
    language: AppLanguage,
    overrideEnabled: Boolean,
    sliderValue: Float,
    sliderRange: ClosedFloatingPointRange<Float>,
    sliderSteps: Int,
    sliderLabel: String,
    onOverrideChange: (Boolean) -> Unit,
    onSliderChange: (Float) -> Unit
) {

    val offset by animateDpAsState(
        targetValue = if (overrideEnabled) 0.dp else 400.dp,
        animationSpec = tween(400)
    )

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.weight(1f))

            val alpha by animateFloatAsState(
                targetValue = if (overrideEnabled) 0f else 1f,
                animationSpec = tween(300)
            )

            Switch(
                checked = overrideEnabled,
                onCheckedChange = onOverrideChange,
                modifier = Modifier.alpha(alpha)
            )
        }

        Spacer(Modifier.height(8.dp))


        Box {

            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                valueRange = sliderRange,
                steps = sliderSteps,
                enabled = !overrideEnabled,
                modifier = Modifier.alpha(
                    if (overrideEnabled) 0f else 1f
                )
            )

            val offset by animateDpAsState(
                targetValue = if (overrideEnabled) 0.dp else 400.dp,
                animationSpec = tween(400)
            )

            Surface(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = offset)
                    .clickable(enabled = overrideEnabled) {
                        onOverrideChange(false)
                    },
                color = MaterialTheme.colorScheme.tertiary,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 6.dp
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = SettingsOptionCopy.DYNAMIC_BADGE.text(language),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = SettingsOptionCopy.TAP_TO_SWITCH_MANUAL.text(language),
                            color=MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        }

        if (!overrideEnabled) {
            Text(
                text = sliderLabel,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperThemeOverrideDropdown(
    text: String,
    language: AppLanguage,
    overrideEnabled: Boolean,
    options: List<String>,
    selectedOption: String,
    dynamicBadgeText: String?,
    dynamicDescriptionText: String?,
    lockDynamicState: Boolean,
    onOverrideChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    val offset by animateDpAsState(
        targetValue = if (overrideEnabled || lockDynamicState) 0.dp else 400.dp,
        animationSpec = tween(400)
    )
    val dynamicCardClickable = dynamicBadgeText == null && dynamicDescriptionText == null

    Column {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.weight(1f))

            val alpha by animateFloatAsState(
                targetValue = if (overrideEnabled) 0f else 1f,
                animationSpec = tween(300)
            )

            Switch(
                checked = if (lockDynamicState) true else overrideEnabled,
                onCheckedChange = onOverrideChange,
                enabled = !lockDynamicState,
                modifier = Modifier.alpha(alpha)
            )
        }

        Spacer(Modifier.height(8.dp))

        Box {

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    if (!overrideEnabled && !lockDynamicState) expanded = it
                }
            ) {

                OutlinedTextField(
                    value = selectedOption,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .alpha(if (overrideEnabled || lockDynamicState) 0f else 1f)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = offset)
                    .clickable(
                        enabled = overrideEnabled && dynamicCardClickable && !lockDynamicState
                    ) {
                        onOverrideChange(false)
                    },
                color = MaterialTheme.colorScheme.tertiary,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 6.dp
            ) {

                Box(contentAlignment = Alignment.Center) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Text(
                            text = dynamicBadgeText
                                ?: SettingsOptionCopy.DYNAMIC_BADGE.text(language),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = dynamicDescriptionText
                                ?: SettingsOptionCopy.TAP_TO_SWITCH_MANUAL.text(language),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun CardSetting(
    title: String,
    selectedSource: WallpaperSource,
    onSourceSelected: (WallpaperSource) -> Unit,
    language: AppLanguage
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardOption(
                label = SettingsOptionCopy.WALLPAPER_DYNAMIC.text(language),
                description = SettingsOptionCopy.WALLPAPER_DYNAMIC_DESC.text(language),
                isSelected = selectedSource == WallpaperSource.DEFAULT,
                onClick = { onSourceSelected(WallpaperSource.DEFAULT) },
                modifier = Modifier.weight(1f)
            )

            CardOption(
                label = SettingsOptionCopy.WALLPAPER_LOCAL.text(language),
                description = SettingsOptionCopy.WALLPAPER_LOCAL_DESC.text(language),
                isSelected = selectedSource == WallpaperSource.SYSTEM,
                onClick = { onSourceSelected(WallpaperSource.SYSTEM) },
                modifier = Modifier.weight(1f)
            )

            CardOption(
                label = SettingsOptionCopy.WALLPAPER_MANUAL.text(language),
                description = SettingsOptionCopy.WALLPAPER_MANUAL_DESC.text(language),
                isSelected = selectedSource == WallpaperSource.CUSTOM,
                onClick = { onSourceSelected(WallpaperSource.CUSTOM) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
@Composable
fun CardOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val containerColor =
        if (isSelected)
            MaterialTheme.colorScheme.tertiary
        else{
            if(shouldUseDarkTheme(context)){
                UnselectedColor2
            }else{
                UnselectedColor1
            }}


    val contentColor =
        if (isSelected)
            MaterialTheme.colorScheme.secondary
        else{
            if(shouldUseDarkTheme(context)){
                UnselectedColor1
            }else{
                UnselectedColor2
            }}


    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = if (isSelected) 3.dp else 0.dp
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}
