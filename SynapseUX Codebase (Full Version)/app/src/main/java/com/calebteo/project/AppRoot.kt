package com.calebteo.project

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calebteo.project.data.AppLanguage
import com.calebteo.project.data.LauncherMetricsStore
import com.calebteo.project.data.LauncherSettings
import com.calebteo.project.data.OnboardingStore
import com.calebteo.project.data.SettingsManager
import com.calebteo.project.data.SystemLayout
import com.calebteo.project.data.WallpaperSessionCache
import com.calebteo.project.data.WallpaperSource
import com.calebteo.project.data.hasInternetConnection
import com.calebteo.project.ui.screens.AppDrawer
import com.calebteo.project.ui.screens.AppearanceDataAnalyScreen
import com.calebteo.project.ui.screens.HomeScreenFullListOneWidget
import com.calebteo.project.ui.screens.HomeScreenHalfListOneWidget
import com.calebteo.project.ui.screens.HomeScreenOneWidget
import com.calebteo.project.ui.screens.HomeScreenTwoWidgets
import com.calebteo.project.ui.screens.ListAppDrawer
import com.calebteo.project.ui.screens.MenuScreen
import com.calebteo.project.ui.screens.SettingsScreen
import com.calebteo.project.ui.screens.SystemDataAnalyScreen
import com.calebteo.project.ui.screens.TechnicalDetailsScreen
import com.calebteo.project.ui.screens.WidgetDataAnalyScreen
import com.calebteo.project.ui.screens.WallpaperDataAnalyScreen
import com.calebteo.project.ui.theme.ThemeColorCache
import com.calebteo.project.ui.theme.Blue
import com.calebteo.project.ui.theme.BlueDark
import com.calebteo.project.ui.theme.BlueLight
import com.calebteo.project.ui.theme.DarkBackground
import com.calebteo.project.ui.theme.DarkSurface
import com.calebteo.project.ui.theme.LightBackground
import com.calebteo.project.ui.theme.LightSurface
import com.calebteo.project.ui.theme.shouldUseDarkTheme
import com.calebteo.project.viewmodel.LauncherViewModel
import com.example.launcher.data.NextBarAppSelector
import com.example.launcher.data.TopBarAppSelector
import com.example.launcher.data.UsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var deepLinkData = mutableStateOf<Uri?>(null)

private enum class LauncherRoute {
    HOME,
    MENU,
    SETTINGS,
    TECHNICAL_MENU,
    TECHNICAL_APPEARANCE,
    TECHNICAL_WALLPAPER,
    TECHNICAL_SYSTEM,
    TECHNICAL_WIDGETS
}

private fun openManageWriteSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_WRITE_SETTINGS,
        Uri.parse("package:${context.packageName}")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: LauncherViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val onboardingPagerState = rememberPagerState(pageCount = { 4 })

    val usageRepo = remember { UsageStatsRepository(context) }
    val metricsStore = remember { LauncherMetricsStore(context) }
    val onboardingStore = remember { OnboardingStore(context) }
    val settings by viewModel.settings.collectAsState()
    val currentCachedThemeColor = ThemeColorCache.cached

    var score by remember { mutableStateOf(0) }
    var showOnboarding by remember { mutableStateOf(false) }
    var hasUsageAccess by remember { mutableStateOf(false) }
    var isLauncherDefaultState by remember { mutableStateOf(false) }
    var hasOverviewAccessibility by remember { mutableStateOf(false) }
    var hasWriteSettingsAccess by remember { mutableStateOf(false) }
    var openSettingsTrigger by rememberSaveable { mutableStateOf(false) }
    var shouldHideFab by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showOnboarding = onboardingStore.isFirstLaunch()
        hasUsageAccess = hasUsagePermission(context)
        isLauncherDefaultState = isDefaultLauncher(context)
        hasOverviewAccessibility = isOverviewAccessibilityEnabled(context)
        hasWriteSettingsAccess = Settings.System.canWrite(context)
    }

    LaunchedEffect(
        settings.wallpaperSource,
        settings.wallpaperTheme,
        settings.overrideWallpaperTheme
    ) {
        if (settings.wallpaperSource == WallpaperSource.DEFAULT) {
            if (hasInternetConnection(context)) {
                WallpaperSessionCache.getOrInitializeDefaultWallpaper(
                    context = context,
                    settings = settings
                )
            } else {
                WallpaperSessionCache.getOrInitializeOfflineWallpaper(context)
            }
        }
    }

    LaunchedEffect(settings.synapseNavigationEnabled) {
        OverviewAccessibilityService.setNavigationVisible(
            context = context,
            visible = settings.synapseNavigationEnabled
        )
    }

    LaunchedEffect(
        settings.overrideColorTheme,
        settings.themeColor,
        currentCachedThemeColor
    ) {
        OverviewAccessibilityService.refreshNavigationStyle()
    }

    LaunchedEffect(Unit) {
        while (true) {
            deliverDueReminders(context)
            delay(15_000)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = hasUsagePermission(context)
                isLauncherDefaultState = isDefaultLauncher(context)
                hasOverviewAccessibility = isOverviewAccessibilityEnabled(context)
                hasWriteSettingsAccess = Settings.System.canWrite(context)
                val usageMap = usageRepo.getUsageCountMap()
                viewModel.updateUsageStats(usageMap)

                kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                    // Treat each launcher resume as a fresh initialization event
                    // for the current hour bucket before recalculating the score.
                    metricsStore.recordLauncherInitialization()
                    metricsStore.recalculateOrganisationScoreOnStart()
                    score = metricsStore.getOrganisationScore()
                    if (!showOnboarding && hasUsageAccess && isLauncherDefaultState) {
                        maybeSendReviewPromptNotification(context, settings.language)
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val shouldShowAccessGate = !showOnboarding &&
        (
            !hasUsageAccess ||
                !isLauncherDefaultState ||
                !hasWriteSettingsAccess ||
                (settings.synapseNavigationEnabled && !hasOverviewAccessibility)
        )

    Box(modifier = Modifier.fillMaxSize()) {
        if (!shouldShowAccessGate) {
            LauncherApp(
                deepLinkData = deepLinkData.value,
                viewModel = viewModel,
                metrics = score,
                suppressWallpaperDialogs = showOnboarding,
                onOpenSettingsfromFAB = openSettingsTrigger,
                onSettingsHandled = { openSettingsTrigger = false },
                onFabVisibilityChanged = { shouldHideFab = it },
                hasOverviewAccessibility = hasOverviewAccessibility
            )
        }

        if (!showOnboarding && !shouldShowAccessGate && !shouldHideFab) {
            FloatingActionButton(
                onClick = { openSettingsTrigger = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.secondary

            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",

                )
            }
        }

        if (showOnboarding) {
            OnboardingScreen(
                pagerState = onboardingPagerState,
                initialLanguage = settings.language,
                initialUsername = settings.username,
                onLanguageSelected = viewModel::updateLanguage,
                onUsernameChanged = viewModel::updateUsername,
                onApplyDynamicDefaults = {
                    viewModel.updateOverrideTopBar(true)
                    viewModel.updateOverrideGridView(true)
                    viewModel.updateOverrideSystemLayout(true)
                    viewModel.updateOverrideColorTheme(true)
                    viewModel.updateOverrideWallpaperTheme(true)
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        viewModel.updateWallpaperSource(WallpaperSource.DEFAULT)
                    }
                },
                onFinish = {
                    showOnboarding = false
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        onboardingStore.markCompleted()
                    }
                }
            )
        }

        if (shouldShowAccessGate) {
            AccessSetupScreen(
                language = settings.language,
                hasUsageAccess = hasUsageAccess,
                isDefaultLauncher = isLauncherDefaultState,
                hasOverviewAccessibility = hasOverviewAccessibility,
                hasWriteSettingsAccess = hasWriteSettingsAccess,
                onSetDefaultLauncher = { openLauncherSettings(context) },
                onOpenUsageAccessSettings = { openUsageAccessSettings(context) },
                onOpenAccessibilitySettings = { openOverviewAccessibilitySettings(context) },
                onOpenWriteSettings = { openManageWriteSettings(context) },
                onContinueWithoutAccessibility = {
                    scope.launch {
                        viewModel.updateSynapseNavigationEnabled(false)
                    }
                }
            )
        }

    }
}

@Composable
fun OnboardingScreen(
    pagerState: PagerState,
    initialLanguage: AppLanguage,
    initialUsername: String,
    onLanguageSelected: (AppLanguage) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onApplyDynamicDefaults: () -> Unit,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedLanguage by rememberSaveable { mutableStateOf(initialLanguage) }
    var username by rememberSaveable { mutableStateOf(initialUsername) }
    val isDarkTheme = shouldUseDarkTheme(context)
    val onboardingColorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = Blue,
            secondary = BlueDark,
            tertiary = BlueLight,
            background = DarkBackground,
            surface = DarkSurface
        )
    } else {
        lightColorScheme(
            primary = Blue,
            secondary = BlueDark,
            tertiary = BlueLight,
            background = LightBackground,
            surface = LightSurface
        )
    }
    val canContinue = pagerState.currentPage < pagerState.pageCount - 1 || username.trim().isNotEmpty()

    MaterialTheme(colorScheme = onboardingColorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val onboardingPages = remember { onboardingPageData() }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> OnboardingPage(
                            entries = onboardingPages[0],
                            icon = Icons.Default.AppShortcut
                        )

                        1 -> OnboardingPage(
                            entries = onboardingPages[1],
                            icon = Icons.Default.Home
                        )

                        2 -> OnboardingPage(
                            entries = onboardingPages[2],
                            icon = Icons.Default.Upcoming
                        )

                        3 -> OnboardingSetupPage(
                            selectedLanguage = selectedLanguage,
                            username = username,
                            onLanguageSelected = { selectedLanguage = it },
                            onUsernameChanged = { username = it }
                        )
                    }
                }

                PageIndicators(pagerState)

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (pagerState.currentPage < pagerState.pageCount - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onLanguageSelected(selectedLanguage)
                            onUsernameChanged(username.trim())
                            onApplyDynamicDefaults()
                            onFinish()
                        }
                    },
                    enabled = canContinue,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        disabledContentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < pagerState.pageCount - 1) {
                            OnboardingLabel.NEXT.text(selectedLanguage)
                        } else {
                            OnboardingLabel.SAVE_AND_START.text(selectedLanguage)
                        },
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSetupPage(
    selectedLanguage: AppLanguage,
    username: String,
    onLanguageSelected: (AppLanguage) -> Unit,
    onUsernameChanged: (String) -> Unit
) {
    var languageExpanded by remember { mutableStateOf(false) }
    val textColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingTextBlock(entries = onboardingSetupEntries(), textColor = textColor)

        Spacer(Modifier.height(24.dp))

        ExposedDropdownMenuBox(
            expanded = languageExpanded,
            onExpandedChange = { languageExpanded = !languageExpanded }
        ) {
            OutlinedTextField(
                value = onboardingLanguageLabel(selectedLanguage),
                onValueChange = {},
                readOnly = true,
                label = { Text(OnboardingLabel.LANGUAGE.text(selectedLanguage)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            DropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false }
            ) {
                AppLanguage.values().forEach { language ->
                    DropdownMenuItem(
                        text = { Text(onboardingLanguageLabel(language)) },
                        onClick = {
                            onLanguageSelected(language)
                            languageExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChanged,
            label = { Text(OnboardingLabel.USERNAME.text(selectedLanguage)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            )
        )
    }
}

private fun onboardingLanguageLabel(language: AppLanguage): String =
    when (language) {
        AppLanguage.ENGLISH -> "English 🇬🇧"
        AppLanguage.MANDARIN -> "Mandarin 🇨🇳"
        AppLanguage.MALAY -> "Malay 🇲🇾"
        AppLanguage.SPANISH -> "Spanish 🇪🇸"
        AppLanguage.FRENCH -> "French 🇫🇷"
    }

@Composable
private fun OnboardingPage(
    entries: List<OnboardingEntry>,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val textColor = MaterialTheme.colorScheme.onBackground

        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        OnboardingTextBlock(entries = entries, textColor = textColor)
    }
}

private data class OnboardingEntry(
    val title: String,
    val text: String
)

@Composable
private fun OnboardingTextBlock(
    entries: List<OnboardingEntry>,
    textColor: Color
) {
    entries.forEachIndexed { index, entry ->
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = textColor
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = entry.text,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = textColor
        )

        if (index != entries.lastIndex) {
            Spacer(Modifier.height(14.dp))
        }
    }
}

private enum class OnboardingLabel(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    NEXT("Next", "下一步", "Seterusnya", "Siguiente", "Suivant"),
    SAVE_AND_START(
        "Save and Start",
        "保存并开始",
        "Simpan dan mula",
        "Guardar e iniciar",
        "Enregistrer et commencer"
    ),
    LANGUAGE("Language", "语言", "Bahasa", "Idioma", "Langue"),
    USERNAME("Username", "用户名", "Nama pengguna", "Nombre de usuario", "Nom d'utilisateur");

    fun text(language: AppLanguage): String =
        when (language) {
            AppLanguage.ENGLISH -> english
            AppLanguage.MANDARIN -> mandarin
            AppLanguage.MALAY -> malay
            AppLanguage.SPANISH -> spanish
            AppLanguage.FRENCH -> french
        }
}

private enum class AccessGateCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    BEFORE_YOU_START(
        "One Last Step...",
        "最后一步……",
        "Satu Langkah Terakhir...",
        "Un ultimo paso...",
        "Une derniere etape..."
    ),
    SUBHEADING(
        "Please make Synapse UX your default launcher and grant it usage data access to allow us to deliver our adaptive UI.",
        "请将联想用户体验设为你的默认启动器，并授予它使用数据访问权限，以便我们提供自适应界面。",
        "Sila jadikan Synapse UX pelancar lalai anda dan benarkan akses data penggunaan supaya kami dapat menyampaikan UI adaptif kami.",
        "Haz de Synapse UX tu lanzador predeterminado y concedele acceso a datos de uso para que podamos ofrecer nuestra interfaz adaptativa.",
        "Veuillez definir Synapse UX comme lanceur par defaut et lui accorder l'acces aux donnees d'utilisation afin que nous puissions offrir notre interface adaptative."
    ),
    DEFAULT_LAUNCHER(
        "Default launcher",
        "默认启动器",
        "Pelancar lalai",
        "Lanzador predeterminado",
        "Lanceur par defaut"
    ),
    DEFAULT_LAUNCHER_DESC(
        "Set us as your default launcher!",
        "把联想用户体验设为你的默认启动器！",
        "Tetapkan kami sebagai pelancar lalai anda!",
        "Configuranos como tu lanzador predeterminado!",
        "Definissez-nous comme votre lanceur par defaut !"
    ),
    USAGE_ACCESS(
        "Usage access",
        "使用情况访问",
        "Akses penggunaan",
        "Acceso de uso",
        "Acces d'utilisation"
    ),
    USAGE_ACCESS_DESC(
        "Allow us to customize your interface to your preferences!",
        "允许我们按你的偏好自定义你的界面！",
        "Benarkan kami menyesuaikan antara muka anda mengikut pilihan anda!",
        "Permitenos personalizar tu interfaz segun tus preferencias!",
        "Permettez-nous de personnaliser votre interface selon vos preferences !"
    ),
    GRANTED("Granted", "已授予", "Diberikan", "Concedido", "Accorde"),
    SET_DEFAULT_LAUNCHER(
        "Set default launcher",
        "设置默认启动器",
        "Tetapkan pelancar lalai",
        "Establecer lanzador predeterminado",
        "Definir le lanceur par defaut"
    ),
    OPEN_USAGE_ACCESS(
        "Open usage access",
        "打开使用情况访问",
        "Buka akses penggunaan",
        "Abrir acceso de uso",
        "Ouvrir l'acces d'utilisation"
    ),
    APP_SWITCHER_ACCESS(
        "Accessibility",
        "辅助功能",
        "Kebolehaksesan",
        "Accesibilidad",
        "Accessibilite"
    ),
    APP_SWITCHER_ACCESS_DESC(
        "Head to Installed Apps/Synapse UX and allow full control, so that we are able to launch our home button!",
        "前往 已安装的应用程序/Synapse UX 并允许完全控制，这样我们就能够启动我们的主页按钮！",
        "Pergi ke Apl Dipasang/Synapse UX dan benarkan kawalan penuh supaya kami boleh melancarkan butang laman utama kami!",
        "Ve a Apps instaladas/Synapse UX y permite el control total para que podamos lanzar nuestro boton de inicio.",
        "Allez dans Applications installees/Synapse UX et autorisez le controle total afin que nous puissions lancer notre bouton d'accueil !"
    ),
    ENABLE_ACCESSIBILITY(
        "Enable accessibility",
        "启用辅助功能",
        "Dayakan kebolehaksesan",
        "Activar accesibilidad",
        "Activer l'accessibilite"
    ),
    SYSTEM_SETTINGS(
        "System Settings",
        "系统设置",
        "Tetapan Sistem",
        "Configuracion del sistema",
        "Parametres systeme"
    ),
    SYSTEM_SETTINGS_DESC(
        "allow us to moderate your home settings for you!",
        "允许我们为你调整你的主屏幕设置！",
        "benarkan kami mengurus tetapan rumah anda untuk anda!",
        "permitenos ajustar la configuracion de tu inicio por ti!",
        "permettez-nous de moderer vos parametres d'accueil pour vous !"
    ),
    OPEN_SYSTEM_SETTINGS(
        "Open system settings",
        "打开系统设置",
        "Buka tetapan sistem",
        "Abrir configuracion del sistema",
        "Ouvrir les parametres systeme"
    ),
    CONTINUE_WITHOUT_NAVIGATION(
        "Continue without Synapse Navigation",
        "在没有联想导航的情况下继续",
        "Teruskan tanpa Navigasi Synapse",
        "Continuar sin Synapse Navigation",
        "Continuer sans la navigation Synapse"
    ),
    CONTINUE_WITHOUT_NAVIGATION_DESC(
        "You can keep using Synapse UX without this permission. Synapse Navigation will stay off until you enable accessibility from Settings.",
        "你仍然可以在没有此权限的情况下继续使用联想用户体验。联想导航将保持关闭，直到你在设置中启用辅助功能。",
        "Anda masih boleh menggunakan Synapse UX tanpa kebenaran ini. Navigasi Synapse akan kekal dimatikan sehingga anda mengaktifkan kebolehaksesan dalam Tetapan.",
        "Puedes seguir usando Synapse UX sin este permiso. Synapse Navigation permanecera desactivado hasta que actives la accesibilidad desde Ajustes.",
        "Vous pouvez continuer a utiliser Synapse UX sans cette autorisation. La navigation Synapse restera desactivee jusqu'a ce que vous activiez l'accessibilite dans les parametres."
    ),
    DONE("Done", "完成", "Selesai", "Hecho", "Termine"),
    READY("Ready", "就绪", "Sedia", "Listo", "Pret");

    fun text(language: AppLanguage): String =
        when (language) {
            AppLanguage.ENGLISH -> english
            AppLanguage.MANDARIN -> mandarin
            AppLanguage.MALAY -> malay
            AppLanguage.SPANISH -> spanish
            AppLanguage.FRENCH -> french
        }
}

private fun onboardingSetupEntries(): List<OnboardingEntry> = listOf(
    OnboardingEntry("Final Setup", "Select your language and username."),
    OnboardingEntry("最后设置", "选择你的语言和用户名。"),
    OnboardingEntry("Persediaan akhir", "Pilih bahasa dan nama pengguna anda."),
    OnboardingEntry("Configuracion final", "Selecciona tu idioma y nombre de usuario."),
    OnboardingEntry("Configuration finale", "Selectionnez votre langue et votre nom d'utilisateur.")
)

private fun onboardingPageData(): List<List<OnboardingEntry>> = listOf(
    listOf(
        OnboardingEntry("Welcome to Synapse UX", "A fully integrated, dynamic experience."),
        OnboardingEntry("欢迎来到 联想用户体验", "完整整合的动态体验。"),
        OnboardingEntry("Selamat datang ke Synapse UX", "Pengalaman dinamik yang bersepadu sepenuhnya."),
        OnboardingEntry("Bienvenido a Synapse UX", "Una experiencia dinamica e integrada por completo."),
        OnboardingEntry("Bienvenue sur Synapse UX", "Une experience entierement integree et dynamique.")
    ),
    listOf(
        OnboardingEntry("A Brand New Home UI", "Your most used apps first, within your reach."),
        OnboardingEntry("全新的主屏幕界面", "最常用的应用优先显示，触手可及。"),
        OnboardingEntry("UI skrin utama yang serba baharu", "Apl yang paling kerap digunakan didahulukan, dalam jangkauan anda."),
        OnboardingEntry("Una nueva interfaz de inicio", "Tus apps mas usadas primero, siempre a tu alcance."),
        OnboardingEntry("Une toute nouvelle interface d'accueil", "Vos applis les plus utilisees d'abord, a portee de main.")
    ),
    listOf(
        OnboardingEntry("As You Wish (Without You Asking)", "Dynamic widgets and wallpapers, tailored to your needs."),
        OnboardingEntry("如你所愿，无需开口", "动态组件和壁纸，按你的需求而变。"),
        OnboardingEntry("Seperti yang anda mahukan, tanpa perlu meminta", "Widget dan kertas dinding dinamik, disesuaikan dengan keperluan anda."),
        OnboardingEntry("Como lo quieres, sin pedirlo", "Widgets y fondos dinamicos, adaptados a tus necesidades."),
        OnboardingEntry("Comme vous le voulez, sans le demander", "Widgets et fonds d'ecran dynamiques, adaptes a vos besoins.")
    )
)

@Composable
fun PageIndicators(pagerState: PagerState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pagerState.pageCount) { page ->
            val color =
                if (pagerState.currentPage == page) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Gray
                }

            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(10.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
private fun AccessSetupScreen(
    language: AppLanguage,
    hasUsageAccess: Boolean,
    isDefaultLauncher: Boolean,
    hasOverviewAccessibility: Boolean,
    hasWriteSettingsAccess: Boolean,
    onSetDefaultLauncher: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onContinueWithoutAccessibility: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val subheadingColor =
        if (shouldUseDarkTheme(context)) Color.White else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = AccessGateCopy.BEFORE_YOU_START.text(language),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = AccessGateCopy.SUBHEADING.text(language),
                style = MaterialTheme.typography.bodyLarge,
                color = subheadingColor
            )

            AccessRequirementCard(
                title = AccessGateCopy.DEFAULT_LAUNCHER.text(language),
                description = if (isDefaultLauncher) {
                    AccessGateCopy.GRANTED.text(language)
                } else {
                    AccessGateCopy.DEFAULT_LAUNCHER_DESC.text(language)
                },
                complete = isDefaultLauncher,
                readyLabel = AccessGateCopy.READY.text(language),
                primaryActionLabel = if (isDefaultLauncher) {
                    AccessGateCopy.DONE.text(language)
                } else {
                    AccessGateCopy.SET_DEFAULT_LAUNCHER.text(language)
                },
                onPrimaryAction = if (isDefaultLauncher) null else onSetDefaultLauncher
            )

            AccessRequirementCard(
                title = AccessGateCopy.USAGE_ACCESS.text(language),
                description = if (hasUsageAccess) {
                    AccessGateCopy.GRANTED.text(language)
                } else {
                    AccessGateCopy.USAGE_ACCESS_DESC.text(language)
                },
                complete = hasUsageAccess,
                readyLabel = AccessGateCopy.READY.text(language),
                primaryActionLabel = if (hasUsageAccess) {
                    AccessGateCopy.DONE.text(language)
                } else {
                    AccessGateCopy.OPEN_USAGE_ACCESS.text(language)
                },
                onPrimaryAction = if (hasUsageAccess) null else onOpenUsageAccessSettings
            )

            AccessRequirementCard(
                title = AccessGateCopy.APP_SWITCHER_ACCESS.text(language),
                description = if (hasOverviewAccessibility) {
                    AccessGateCopy.GRANTED.text(language)
                } else {
                    AccessGateCopy.APP_SWITCHER_ACCESS_DESC.text(language)
                },
                complete = hasOverviewAccessibility,
                readyLabel = AccessGateCopy.READY.text(language),
                primaryActionLabel = if (hasOverviewAccessibility) {
                    AccessGateCopy.DONE.text(language)
                } else {
                    AccessGateCopy.ENABLE_ACCESSIBILITY.text(language)
                },
                onPrimaryAction = if (hasOverviewAccessibility) null else onOpenAccessibilitySettings,
                secondaryActionLabel = if (hasOverviewAccessibility) {
                    null
                } else {
                    AccessGateCopy.CONTINUE_WITHOUT_NAVIGATION.text(language)
                },
                onSecondaryAction = if (hasOverviewAccessibility) null else onContinueWithoutAccessibility
            )

            AccessRequirementCard(
                title = AccessGateCopy.SYSTEM_SETTINGS.text(language),
                description = if (hasWriteSettingsAccess) {
                    AccessGateCopy.GRANTED.text(language)
                } else {
                    AccessGateCopy.SYSTEM_SETTINGS_DESC.text(language)
                },
                complete = hasWriteSettingsAccess,
                readyLabel = AccessGateCopy.READY.text(language),
                primaryActionLabel = if (hasWriteSettingsAccess) {
                    AccessGateCopy.DONE.text(language)
                } else {
                    AccessGateCopy.OPEN_SYSTEM_SETTINGS.text(language)
                },
                onPrimaryAction = if (hasWriteSettingsAccess) null else onOpenWriteSettings
            )
        }
    }
}

@Composable
private fun AccessRequirementCard(
    title: String,
    description: String,
    complete: Boolean,
    readyLabel: String,
    primaryActionLabel: String,
    onPrimaryAction: (() -> Unit)?,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onPrimaryAction?.invoke() },
                    enabled = onPrimaryAction != null,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White
                    )
                ) {
                    Text(primaryActionLabel, color = Color.White)
                }

                if (complete) {
                    Text(
                        text = readyLabel,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            if (secondaryActionLabel != null && onSecondaryAction != null) {
                OutlinedButton(onClick = onSecondaryAction) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
fun LauncherApp(
    deepLinkData: Uri? = null,
    viewModel: LauncherViewModel = viewModel(),
    metrics: Int,
    suppressWallpaperDialogs: Boolean = false,
    onOpenSettingsfromFAB: Boolean,
    onSettingsHandled: () -> Unit,
    onFabVisibilityChanged: (Boolean) -> Unit,
    hasOverviewAccessibility: Boolean
) {
    val context = LocalContext.current
    var isAppDrawerOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var technicalRoute by remember { mutableStateOf<LauncherRoute?>(null) }

    val apps by viewModel.filteredApps.collectAsState()
    val usageMap by viewModel.usageCountMap.collectAsState()

    val settingsManager = remember { SettingsManager(context) }
    val settings by settingsManager
        .settingsFlow
        .collectAsState(initial = LauncherSettings())

    var topAppBarScore = settings.appGridSize
    if (settings.overrideTopBar){topAppBarScore = TopBarAppSelector.determineTopBarCount(
        apps,
        usageMap)}
    var nextAppBarScore = settings.appRowSize
    if (settings.overrideGridView){nextAppBarScore = NextBarAppSelector.determineNextBarCount(
        apps)}
    val scope = rememberCoroutineScope()
    var parsemetrics = metrics
    if (!settings.overrideSystemLayout){
        when (settings.systemLayout){
            SystemLayout.MAIN_LIST -> parsemetrics=0
            SystemLayout.MAIN_LIST_GRID -> parsemetrics=20
            SystemLayout.GRID_SEARCH_LIST -> parsemetrics=40
            SystemLayout.WIDGETS_SEARCH_LIST -> parsemetrics=60
            SystemLayout.GRID_SEARCH_APPDRAWER -> parsemetrics=80
            SystemLayout.WIDGETS_SEARCH_APPDRAWER -> parsemetrics=100


        }
    }

    LaunchedEffect(onOpenSettingsfromFAB) {
        if (onOpenSettingsfromFAB) {
            isSettingsOpen = true
            onSettingsHandled()
        }
    }

    LaunchedEffect(deepLinkData) {
        deepLinkData?.let { uri ->
            if (uri.host == "settings") {
                isSettingsOpen = true
            }
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == OverviewAccessibilityService.ACTION_OVERLAY_HOME_PRESSED) {
                    val isAlreadyOnHomeScreen =
                        !isAppDrawerOpen && !isSettingsOpen && !isMenuOpen && technicalRoute == null
                    if (isAlreadyOnHomeScreen) {
                        return
                    }
                    isAppDrawerOpen = false
                    isSettingsOpen = false
                    isMenuOpen = false
                    technicalRoute = null
                    viewModel.updateSearchQuery("")
                } else if (intent?.action == OverviewAccessibilityService.ACTION_OVERLAY_MENU_PRESSED) {
                    val isAlreadyOnMenuScreen =
                        isMenuOpen && technicalRoute == null && !isSettingsOpen && !isAppDrawerOpen
                    if (isAlreadyOnMenuScreen) {
                        return
                    }
                    val shouldCloseMenu =
                        isMenuOpen && technicalRoute == null && !isSettingsOpen && !isAppDrawerOpen
                    if (shouldCloseMenu) {
                        isMenuOpen = false
                    } else {
                        isAppDrawerOpen = false
                        isSettingsOpen = false
                        technicalRoute = null
                        isMenuOpen = true
                    }
                    viewModel.updateSearchQuery("")
                }
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter().apply {
                    addAction(OverviewAccessibilityService.ACTION_OVERLAY_HOME_PRESSED)
                    addAction(OverviewAccessibilityService.ACTION_OVERLAY_MENU_PRESSED)
                },
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter().apply {
                    addAction(OverviewAccessibilityService.ACTION_OVERLAY_HOME_PRESSED)
                    addAction(OverviewAccessibilityService.ACTION_OVERLAY_MENU_PRESSED)
                }
            )
        }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val route = when {
            technicalRoute != null -> technicalRoute!!
            isMenuOpen -> LauncherRoute.MENU
            isSettingsOpen -> LauncherRoute.SETTINGS
            else -> LauncherRoute.HOME
        }

        LaunchedEffect(route) {
            onFabVisibilityChanged(
                route == LauncherRoute.MENU || route == LauncherRoute.SETTINGS
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = route,
                modifier = Modifier
                    .fillMaxSize(),
                label = "launcher-route-transition",
                transitionSpec = {
                    when {
                        targetState == LauncherRoute.SETTINGS ->
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300)
                            ).togetherWith(ExitTransition.None)
                        targetState == LauncherRoute.MENU ->
                            fadeIn(animationSpec = tween(260))
                                .togetherWith(fadeOut(animationSpec = tween(220)))
                        initialState == LauncherRoute.MENU && targetState == LauncherRoute.HOME ->
                            fadeIn(animationSpec = tween(260))
                                .togetherWith(fadeOut(animationSpec = tween(220)))

                        else ->
                            EnterTransition.None.togetherWith(ExitTransition.None)
                    }
                }
            ) { currentRoute ->
                when (currentRoute) {
                    LauncherRoute.MENU -> {
                        MenuScreen(settings = settings)
                    }
                    LauncherRoute.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onBack = { isSettingsOpen = false },
                        launcherViewModel = viewModel,
                        onUpdateAppGridSize = { scope.launch { viewModel.updateAppGridSize(it) } },
                        onUpdateAppRowSize = { scope.launch { viewModel.updateAppRowSize(it) } },
                        onUpdateIconSize = { scope.launch { viewModel.updateIconSize(it) } },
                        onUpdateWallpaperSource = { scope.launch { viewModel.updateWallpaperSource(it) } },
                        onUpdateCustomWallpaperUri = { scope.launch { viewModel.updateCustomWallpaperUri(it) } },
                        onUpdateWallpaperTheme = { scope.launch { viewModel.updateWallpaperTheme(it) } },
                        onUpdateSystemLayout = { scope.launch { viewModel.updateSystemLayout(it) } },
                        onUpdateOverrideTopBar = { scope.launch { viewModel.updateOverrideTopBar(it) } },
                        onUpdateOverrideGridView = { scope.launch { viewModel.updateOverrideGridView(it) } },
                        onUpdateThemeColor = { scope.launch { viewModel.updateThemeColor(it) } },
                        onUpdateOverrideThemeColor = { scope.launch { viewModel.updateOverrideColorTheme(it) } },
                        onUpdateAppFont = { scope.launch { viewModel.updateAppFont(it) } },
                        onUpdateOverrideSystemLayout = { scope.launch { viewModel.updateOverrideSystemLayout(it) } },
                        onUpdateOverrideWallpaperTheme = { scope.launch { viewModel.updateOverrideWallpaperTheme(it) } },
                        onUpdateSynapseNavigationEnabled = { scope.launch { viewModel.updateSynapseNavigationEnabled(it) } },
                        onUpdateLanguage = { scope.launch { viewModel.updateLanguage(it) } },
                        hasOverviewAccessibility = hasOverviewAccessibility,
                        onOpenAccessibilitySettings = { openOverviewAccessibilitySettings(context) }
                    )
                    }

                    LauncherRoute.TECHNICAL_MENU -> {
                        TechnicalDetailsScreen(
                            language = settings.language,
                            onBack = { technicalRoute = null },
                            onOpenAppearance = {
                                technicalRoute = LauncherRoute.TECHNICAL_APPEARANCE
                            },
                            onOpenWallpaper = {
                                technicalRoute = LauncherRoute.TECHNICAL_WALLPAPER
                            },
                            onOpenSystem = {
                                technicalRoute = LauncherRoute.TECHNICAL_SYSTEM
                            },
                            onOpenWidgets = {
                                technicalRoute = LauncherRoute.TECHNICAL_WIDGETS
                            }
                        )
                    }

                    LauncherRoute.TECHNICAL_APPEARANCE -> {
                        AppearanceDataAnalyScreen(
                            onBack = { technicalRoute = LauncherRoute.TECHNICAL_MENU },
                            apps = apps,
                            usageCountMap = usageMap
                        )
                    }

                    LauncherRoute.TECHNICAL_WALLPAPER -> {
                        WallpaperDataAnalyScreen(
                            onBack = { technicalRoute = LauncherRoute.TECHNICAL_MENU }
                        )
                    }

                    LauncherRoute.TECHNICAL_SYSTEM -> {
                        SystemDataAnalyScreen(
                            onBack = { technicalRoute = LauncherRoute.TECHNICAL_MENU }
                        )
                    }

                    LauncherRoute.TECHNICAL_WIDGETS -> {
                        WidgetDataAnalyScreen(
                            onBack = { technicalRoute = LauncherRoute.TECHNICAL_MENU }
                        )
                    }

                    LauncherRoute.HOME -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when {
                                parsemetrics <= 19 -> {
                                    HomeScreenFullListOneWidget(
                                        viewModel = viewModel,
                                        settings = settings,
                                        suppressWallpaperDialogs = suppressWallpaperDialogs,
                                        onAppDrawerOpen = { isAppDrawerOpen = true },
                                        onSettingsOpen = { isSettingsOpen = true },
                                        onTechnicalOpen = {
                                            technicalRoute = LauncherRoute.TECHNICAL_MENU
                                        },
                                        appsintopbar = topAppBarScore
                                    )
                                }

                            parsemetrics <= 39 -> {
                                HomeScreenHalfListOneWidget(
                                    viewModel = viewModel,
                                    settings = settings,
                                    suppressWallpaperDialogs = suppressWallpaperDialogs,
                                    onAppDrawerOpen = { isAppDrawerOpen = true },
                                    onSettingsOpen = { isSettingsOpen = true },
                                    onTechnicalOpen = {
                                        technicalRoute = LauncherRoute.TECHNICAL_MENU
                                    },
                                    appsintopbar = topAppBarScore,
                                    appsinnextbar = nextAppBarScore
                                )
                            }

                            parsemetrics <= 59 -> {
                                HomeScreenOneWidget(
                                    viewModel = viewModel,
                                    settings = settings,
                                    suppressWallpaperDialogs = suppressWallpaperDialogs,
                                    onAppDrawerOpen = { isAppDrawerOpen = true },
                                    onSettingsOpen = { isSettingsOpen = true },
                                    onTechnicalOpen = {
                                        technicalRoute = LauncherRoute.TECHNICAL_MENU
                                    },
                                    appsintopbar = topAppBarScore,
                                    appsinnextbar = nextAppBarScore
                                )
                            }

                            parsemetrics <= 79 -> {
                                HomeScreenTwoWidgets(
                                    viewModel = viewModel,
                                    settings = settings,
                                    suppressWallpaperDialogs = suppressWallpaperDialogs,
                                    onAppDrawerOpen = { isAppDrawerOpen = true },
                                    onSettingsOpen = { isSettingsOpen = true },
                                    onTechnicalOpen = {
                                        technicalRoute = LauncherRoute.TECHNICAL_MENU
                                    },
                                    appsintopbar = topAppBarScore
                                )
                            }

                            parsemetrics <= 99 -> {
                                HomeScreenOneWidget(
                                    viewModel = viewModel,
                                    settings = settings,
                                    suppressWallpaperDialogs = suppressWallpaperDialogs,
                                    onAppDrawerOpen = { isAppDrawerOpen = true },
                                    onSettingsOpen = { isSettingsOpen = true },
                                    onTechnicalOpen = {
                                        technicalRoute = LauncherRoute.TECHNICAL_MENU
                                    },
                                    appsintopbar = topAppBarScore,
                                    appsinnextbar = nextAppBarScore
                                )

                                if (isAppDrawerOpen) {
                                    AppDrawer(
                                        viewModel = viewModel,
                                        onDismiss = { isAppDrawerOpen = false }
                                    )
                                }
                            }

                            else -> {
                                HomeScreenTwoWidgets(
                                    viewModel = viewModel,
                                    settings = settings,
                                    suppressWallpaperDialogs = suppressWallpaperDialogs,
                                    onAppDrawerOpen = { isAppDrawerOpen = true },
                                    onSettingsOpen = { isSettingsOpen = true },
                                    onTechnicalOpen = {
                                        technicalRoute = LauncherRoute.TECHNICAL_MENU
                                    },
                                    appsintopbar = topAppBarScore
                                )

                                if (isAppDrawerOpen) {
                                    AppDrawer(
                                        viewModel = viewModel,
                                        onDismiss = { isAppDrawerOpen = false }
                                    )
                                }
                            }
                        }

                            AnimatedVisibility(
                                visible = isAppDrawerOpen && parsemetrics < 80,
                                enter = fadeIn(animationSpec = tween(1300)),
                                exit = fadeOut(animationSpec = tween(1300))
                            ) {
                                ListAppDrawer(
                                    viewModel = viewModel,
                                    suppressWallpaperDialogs = suppressWallpaperDialogs,
                                    onDismiss = { isAppDrawerOpen = false }
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}
