package com.calebteo.project.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calebteo.project.data.LauncherMetricsStore

import com.calebteo.project.data.AppLanguage
import com.calebteo.project.data.AppInfo
import com.calebteo.project.data.LauncherSettings
import com.calebteo.project.data.Wallpaper
import com.calebteo.project.data.WallpaperCategory
import com.calebteo.project.data.WallpaperSessionCache
import com.calebteo.project.data.WallpaperSource
import com.calebteo.project.data.hasInternetConnection
import com.calebteo.project.sendDebugReviewPromptNotification
import com.calebteo.project.ui.theme.UnselectedColor1
import com.calebteo.project.ui.theme.UnselectedColor2
import com.calebteo.project.ui.theme.getBestThemeColor
import com.calebteo.project.ui.theme.shouldUseDarkTheme
import com.calebteo.project.viewmodel.LauncherViewModel
import com.example.launcher.data.TopBarAppSelector

import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.collections.drop


    //generaly max 2-4 or else size is similar to normal or too big, can adjust dynamically
//make sure to reinitialize the screen whenevr this var is mutated! ok?
@Composable
fun HomeScreenOneWidget(
    viewModel: LauncherViewModel,
    settings: LauncherSettings,
    suppressWallpaperDialogs: Boolean = false,
    onAppDrawerOpen: () -> Unit,
    onSettingsOpen: () -> Unit,
    onTechnicalOpen: () -> Unit,
    appsintopbar: Int,
    appsinnextbar: Int
) {
    val installedApps by viewModel.filteredApps.collectAsState()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppActionDialog by remember { mutableStateOf(false) }
    val widgetApps = rememberWidgetApps(count = 1)
    val widgetPackages = remember(widgetApps) {
        widgetApps.map { it.packageName }.toSet()
    }
    val availableApps = remember(installedApps, widgetPackages) {
        installedApps.filterNot { it.packageName in widgetPackages }
    }
    val mostUsedApps = remember(availableApps, appsintopbar) {
        availableApps.take(appsintopbar)
    }

    val homeApps = remember(availableApps, appsintopbar, appsinnextbar) {
        availableApps.drop(appsintopbar)
            .take(appsinnextbar * 2)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackground(
            settings = settings,
            suppressFeedbackDialog = suppressWallpaperDialogs
        )

        MainTopActions(
            settings = settings,
            viewModel = viewModel,
            onTechnicalOpen = onTechnicalOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))


            GreetingChip(settings.language)

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                mostUsedApps.forEach { app ->
                    AppIcon(
                        drawable = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size((240/maxOf(3,appsintopbar)).dp) // ⬅️ expanded
                            .combinedClickable(
                                onClick = { viewModel.launchApp(app.packageName) },
                                onLongClick = {
                                    selectedApp = app
                                    showAppActionDialog = true
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = dynamicUiCopy(DynamicUiCopy.YOUR_MOST_USED, settings.language),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(32.dp))
            widgetApps.firstOrNull()?.let { widgetApp ->
                Widget(
                    selectedApp = widgetApp,
                    settings = settings
                )

                Spacer(Modifier.height(24.dp))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(appsinnextbar),
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(homeApps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.combinedClickable(
                            onClick = { viewModel.launchApp(app.packageName) },
                            onLongClick = {
                                selectedApp = app
                                showAppActionDialog = true
                            }
                        )
                    ) {
                        AppIcon(
                            drawable = app.icon,
                            contentDescription = app.label,
                            modifier = Modifier.size((224/appsinnextbar).dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            /* ---------- MORE APPS ---------- */
            Row(
                modifier = Modifier
                    .clickable { onAppDrawerOpen() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = dynamicUiCopy(DynamicUiCopy.MORE_APPS, settings.language),
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dynamicUiCopy(DynamicUiCopy.MORE_APPS, settings.language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "© caleb teo, 2026",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        HomeAppActionDialog(
            selectedApp = selectedApp,
            showDialog = showAppActionDialog,
            language = settings.language,
            viewModel = viewModel,
            onDismiss = {
                showAppActionDialog = false
                selectedApp = null
            }
        )
    }
}
@Composable
fun HomeScreenTwoWidgets(
    viewModel: LauncherViewModel,
    settings: LauncherSettings,
    suppressWallpaperDialogs: Boolean = false,
    onAppDrawerOpen: () -> Unit,
    onSettingsOpen: () -> Unit,
    onTechnicalOpen: () -> Unit,
    appsintopbar: Int
) {

    val installedApps by viewModel.filteredApps.collectAsState()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppActionDialog by remember { mutableStateOf(false) }
    val widgetApps = rememberWidgetApps(count = 2)
    val widgetPackages = remember(widgetApps) {
        widgetApps.map { it.packageName }.toSet()
    }
    val availableApps = remember(installedApps, widgetPackages) {
        installedApps.filterNot { it.packageName in widgetPackages }
    }

    val mostUsedApps = remember(availableApps, appsintopbar) {
        availableApps.take(appsintopbar)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackground(
            settings = settings,
            suppressFeedbackDialog = suppressWallpaperDialogs
        )

        MainTopActions(
            settings = settings,
            viewModel = viewModel,
            onTechnicalOpen = onTechnicalOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))


            GreetingChip(settings.language)

            Spacer(Modifier.height(24.dp))


            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                mostUsedApps.forEach { app ->
                    AppIcon(
                        drawable = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size((240/maxOf(appsintopbar,3)).dp)
                            .combinedClickable(
                                onClick = { viewModel.launchApp(app.packageName) },
                                onLongClick = {
                                    selectedApp = app
                                    showAppActionDialog = true
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = dynamicUiCopy(DynamicUiCopy.YOUR_MOST_USED, settings.language),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(24.dp))


            widgetApps.getOrNull(0)?.let { widgetApp ->
                Widget(
                    selectedApp = widgetApp,
                    settings = settings
                )

                if (widgetApps.size > 1) {
                    Spacer(Modifier.height(16.dp))
                } else {
                    Spacer(Modifier.height(24.dp))
                }
            }

            widgetApps.getOrNull(1)?.let { widgetApp ->
                Widget(
                    selectedApp = widgetApp,
                    settings = settings
                )

                Spacer(Modifier.height(24.dp))
            }


            Row(
                modifier = Modifier
                    .clickable { onAppDrawerOpen() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = dynamicUiCopy(DynamicUiCopy.MORE_APPS, settings.language),
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dynamicUiCopy(DynamicUiCopy.MORE_APPS, settings.language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "© caleb teo, 2026",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        HomeAppActionDialog(
            selectedApp = selectedApp,
            showDialog = showAppActionDialog,
            language = settings.language,
            viewModel = viewModel,
            onDismiss = {
                showAppActionDialog = false
                selectedApp = null
            }
        )
    }
}

@Composable
fun HomeScreenHalfListOneWidget(
    viewModel: LauncherViewModel,
    settings: LauncherSettings,
    suppressWallpaperDialogs: Boolean = false,
    onAppDrawerOpen: () -> Unit,
    onSettingsOpen: () -> Unit,
    onTechnicalOpen: () -> Unit,
    appsintopbar: Int,
    appsinnextbar: Int
) {

    val installedApps by viewModel.filteredApps.collectAsState()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppActionDialog by remember { mutableStateOf(false) }
    val widgetApps = rememberWidgetApps(count = 1)
    val widgetPackages = remember(widgetApps) {
        widgetApps.map { it.packageName }.toSet()
    }
    val availableApps = remember(installedApps, widgetPackages) {
        installedApps.filterNot { it.packageName in widgetPackages }
    }

    val mostUsedApps = remember(availableApps, appsintopbar) {
        availableApps.take(appsintopbar)
    }

    val homeApps = remember(availableApps, appsintopbar, appsinnextbar) {
        availableApps.drop(appsintopbar)
            .take(appsinnextbar) // 1 row of 4 icons
    }
    val listApps = remember(availableApps, appsintopbar, appsinnextbar) {
        availableApps.drop(appsintopbar + appsinnextbar) // 1 row of 4 icons
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackground(
            settings = settings,
            suppressFeedbackDialog = suppressWallpaperDialogs
        )

        MainTopActions(
            settings = settings,
            viewModel = viewModel,
            onTechnicalOpen = onTechnicalOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            GreetingChip(settings.language)

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                mostUsedApps.forEach { app ->
                    AppIcon(
                        drawable = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size((240/maxOf(appsintopbar,3)).dp)
                            .combinedClickable(
                                onClick = { viewModel.launchApp(app.packageName) },
                                onLongClick = {
                                    selectedApp = app
                                    showAppActionDialog = true
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = dynamicUiCopy(DynamicUiCopy.YOUR_MOST_USED, settings.language),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(20.dp))

            widgetApps.firstOrNull()?.let { widgetApp ->
                Widget(
                    selectedApp = widgetApp,
                    settings = settings
                )

                Spacer(Modifier.height(16.dp))
            }


            LazyVerticalGrid(
                columns = GridCells.Fixed(appsinnextbar),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clipToBounds(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = false
            ) {
                items(homeApps) { app ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            drawable = app.icon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size((224/appsinnextbar).dp)
                                .combinedClickable(
                                    onClick = { viewModel.launchApp(app.packageName) },
                                    onLongClick = {
                                        selectedApp = app
                                        showAppActionDialog = true
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp)
                ) {
                    items(listApps) { app ->
                        CompactListRow(
                            app = app,
                            onClick = { viewModel.launchApp(app.packageName) },
                            onLongPress = {
                                selectedApp = app
                                showAppActionDialog = true
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            TinySearchButton(
                onClick = onAppDrawerOpen,
                language = settings.language
            )
            Spacer(Modifier.weight(1f))

            Text(
                text = "© caleb teo, 2026",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        HomeAppActionDialog(
            selectedApp = selectedApp,
            showDialog = showAppActionDialog,
            language = settings.language,
            viewModel = viewModel,
            onDismiss = {
                showAppActionDialog = false
                selectedApp = null
            }
        )
    }
}

@Composable
fun HomeScreenFullListOneWidget(
    viewModel: LauncherViewModel,
    settings: LauncherSettings,
    suppressWallpaperDialogs: Boolean = false,
    onAppDrawerOpen: () -> Unit,
    onSettingsOpen: () -> Unit,
    onTechnicalOpen: () -> Unit,
    appsintopbar: Int
) {

    val installedApps by viewModel.filteredApps.collectAsState()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppActionDialog by remember { mutableStateOf(false) }
    val widgetApps = rememberWidgetApps(count = 1)
    val widgetPackages = remember(widgetApps) {
        widgetApps.map { it.packageName }.toSet()
    }
    val availableApps = remember(installedApps, widgetPackages) {
        installedApps.filterNot { it.packageName in widgetPackages }
    }
    val listApps = remember(availableApps, appsintopbar) {
        val base = availableApps
        base.drop(appsintopbar) // 1 row of 4 icons
    }
    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackground(
            settings = settings,
            suppressFeedbackDialog = suppressWallpaperDialogs
        )

        MainTopActions(
            settings = settings,
            viewModel = viewModel,
            onTechnicalOpen = onTechnicalOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            GreetingChip(settings.language)

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                availableApps.take(appsintopbar).forEach { app ->
                    AppIcon(
                        drawable = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size((240/maxOf(appsintopbar,3)).dp)
                            .combinedClickable(
                                onClick = { viewModel.launchApp(app.packageName) },
                                onLongClick = {
                                    selectedApp = app
                                    showAppActionDialog = true
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = dynamicUiCopy(DynamicUiCopy.YOUR_MOST_USED, settings.language),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(20.dp))

            widgetApps.firstOrNull()?.let { widgetApp ->
                Widget(
                    selectedApp = widgetApp,
                    settings = settings
                )

                Spacer(Modifier.height(16.dp))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp), // ≈ 4 rows
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp)
                ) {
                    items(listApps) { app ->
                        CompactListRow(
                            app = app,
                            onClick = {
                                viewModel.launchApp(app.packageName)
                            },
                            onLongPress = {
                                selectedApp = app
                                showAppActionDialog = true
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            TinySearchButton(
                onClick = onAppDrawerOpen,
                language = settings.language
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "© caleb teo, 2026",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        HomeAppActionDialog(
            selectedApp = selectedApp,
            showDialog = showAppActionDialog,
            language = settings.language,
            viewModel = viewModel,
            onDismiss = {
                showAppActionDialog = false
                selectedApp = null
            }
        )
    }
}

@Composable
private fun HomeAppActionDialog(
    selectedApp: AppInfo?,
    showDialog: Boolean,
    language: AppLanguage,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    if (showDialog && selectedApp != null) {
        AppActionDialog(
            app = selectedApp,
            language = language,
            onDismiss = onDismiss,
            onAppInfo = {
                viewModel.openAppInfo(selectedApp.packageName)
                onDismiss()
            },
            onUninstall = {
                viewModel.uninstallApp(selectedApp.packageName)
                onDismiss()
            }
        )
    }
}

@Composable
private fun MainTopActions(
    settings: LauncherSettings,
    viewModel: LauncherViewModel,
    onTechnicalOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overflowExpanded by remember { mutableStateOf(false) }
    var showWallpaperInfoDialog by remember { mutableStateOf(false) }
    val currentDynamicWallpaper = WallpaperSessionCache.getDefaultWallpaper()
    val canShowWallpaperInfo =
        settings.wallpaperSource == WallpaperSource.DEFAULT &&
            settings.language == AppLanguage.ENGLISH &&
            hasInternetConnection(context) &&
            currentDynamicWallpaper != null
    val infoInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .combinedClickable(
                    interactionSource = infoInteractionSource,
                    indication = LocalIndication.current,
                    enabled = settings.language == AppLanguage.ENGLISH,
                    onClick = onTechnicalOpen,
                    onLongClick = {
                        scope.launch {
                            sendDebugReviewPromptNotification(context, settings.language)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Technical details",
                tint = if (settings.language == AppLanguage.ENGLISH) {
                    Color.White
                } else {
                    Color.White.copy(alpha = 0.45f)
                }
            )
        }

        Box {
            IconButton(onClick = { overflowExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White
                )
            }
            DropdownMenu(
                expanded = overflowExpanded,
                onDismissRequest = { overflowExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(dynamicUiCopy(DynamicUiCopy.WALLPAPER_DETAILS, settings.language)) },
                    enabled = canShowWallpaperInfo,
                    onClick = {
                        overflowExpanded = false
                        showWallpaperInfoDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text(dynamicUiCopy(DynamicUiCopy.EMAIL_SUPPORT, settings.language)) },
                    onClick = {
                        overflowExpanded = false
                        val emailIntent = Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("mailto:h2310015@nushigh.edu.sg")
                        ).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Launcher support")
                        }
                        if (emailIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(emailIntent)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(dynamicUiCopy(DynamicUiCopy.RESET_WALLPAPER, settings.language)) },
                    onClick = {
                        overflowExpanded = false
                        scope.launch {
                            viewModel.updateCustomWallpaperUri(null)
                            viewModel.updateWallpaperSource(WallpaperSource.DEFAULT)
                            if (hasInternetConnection(context)) {
                                WallpaperSessionCache.refreshDefaultWallpaper(
                                    context = context,
                                    settings = settings.copy(
                                        wallpaperSource = WallpaperSource.DEFAULT,
                                        customWallpaperUri = null
                                    )
                                )
                            } else {
                                WallpaperSessionCache.refreshOfflineWallpaper(context)
                            }
                        }
                    }
                )
            }
        }
    }

    if (showWallpaperInfoDialog && currentDynamicWallpaper != null) {
        AlertDialog(
            onDismissRequest = { showWallpaperInfoDialog = false },
            title = { Text(currentDynamicWallpaper.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${dynamicUiCopy(DynamicUiCopy.WALLPAPER_CATEGORY, settings.language)}: " +
                            formatWallpaperCategory(currentDynamicWallpaper)
                    )
                    Text(
                        "${dynamicUiCopy(DynamicUiCopy.WALLPAPER_DESCRIPTION, settings.language)}: " +
                            currentDynamicWallpaper.description
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showWallpaperInfoDialog = false }) {
                    Text(dynamicUiCopy(DynamicUiCopy.CLOSE, settings.language))
                }
            }
        )
    }
}
@Composable
fun TinySearchButton(
    onClick: () -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dynamicUiCopy(DynamicUiCopy.SEARCH, language),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.75f),
            letterSpacing = 1.sp
        )

        Spacer(Modifier.width(4.dp))

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = dynamicUiCopy(DynamicUiCopy.SEARCH, language),
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(14.dp)
        )
    }
}

private enum class GreetingCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    YOU_LOST_THE_GAME("You Lost The Game", "你输了这个游戏", "Anda Kalah Permainan Ini", "Perdiste el Juego", "Tu as perdu le jeu"),
    MONDAY_BLUES("Monday Blues", "周一忧郁", "Murung Hari Isnin", "Bajon de Lunes", "Blues du Lundi"),
    RARING_TO_GO("Raring to Go", "准备出发", "Bersedia untuk Mula", "Listo para Empezar", "Pret a partir"),
    GOOD_MORNING("Good Morning", "早上好", "Selamat Pagi", "Buenos Dias", "Bonjour"),
    GOOD_AFTERNOON("Good Afternoon", "下午好", "Selamat Petang", "Buenas Tardes", "Bon Apres-midi"),
    GOOD_EVENING("Good Evening", "晚上好", "Selamat Malam", "Buenas Noches", "Bonsoir"),
    WINDING_DOWN("Winding Down", "放松一下", "Masa untuk Berehat", "Relajandote", "On se detend"),
    HAPPY_TUESDAY("Happy Tuesday", "星期二愉快", "Selamat Hari Selasa", "Feliz Martes", "Bon Mardi"),
    ITS_MIDWEEK("It's Midweek!", "已经周中了", "Sudah Pertengahan Minggu", "Ya es Mitad de Semana", "C'est le Milieu de la Semaine"),
    THURSDAYS_GREETINGS("Thursday's Greetings", "星期四问候", "Salam Khamis", "Saludos de Jueves", "Salutations du Jeudi"),
    TGIF("Thank God It's Friday", "终于星期五了", "Syukurlah Hari Jumaat", "Por Fin es Viernes", "Enfin Vendredi"),
    FINISHED_FOR_THE_WEEK("Finished for the Week!", "这周结束了", "Minggu Ini Selesai", "Semana Terminada", "Semaine Terminee"),
    PARTY_TIME("Party Time", "派对时间", "Masa Berparti", "Hora de Fiesta", "C'est l'heure de faire la fete"),
    HAPPY_SATURDAY("Happy Saturday", "星期六愉快", "Selamat Hari Sabtu", "Feliz Sabado", "Bon Samedi"),
    UNWIND_TIME("Unwind Time", "放松时间", "Masa untuk Rehat", "Hora de Relajarse", "Moment de detente"),
    HAPPY_SUNDAY("Happy Sunday", "星期天愉快", "Selamat Hari Ahad", "Feliz Domingo", "Bon Dimanche"),
    READY_FOR_THE_WEEK("Ready for the Week", "准备迎接新一周", "Bersedia untuk Minggu Baru", "Listo para la Semana", "Pret pour la semaine");

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

private enum class DynamicUiCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    YOUR_MOST_USED("your most used", "你最常用的", "paling kerap digunakan", "tus mas usadas", "tes plus utilisees"),
    MORE_APPS("more apps", "更多应用程序", "lebih banyak aplikasi", "mas aplicaciones", "plus d'applications"),
    SEARCH("search", "搜索", "cari", "buscar", "rechercher"),
    EMAIL_SUPPORT("Email support", "发送支持邮件", "E-mel sokongan", "Correo de soporte", "E-mail de support"),
    RESET_WALLPAPER("Reset wallpaper", "重置壁纸", "Tetapkan semula kertas dinding", "Restablecer fondo", "Reinitialiser le fond d'ecran"),
    WALLPAPER_DETAILS("Wallpaper details", "壁纸详情", "Butiran kertas dinding", "Detalles del fondo", "Details du fond d'ecran"),
    WALLPAPER_CATEGORY("Category", "类别", "Kategori", "Categoria", "Categorie"),
    WALLPAPER_DESCRIPTION("Description", "描述", "Penerangan", "Descripcion", "Description"),
    CLOSE("Close", "关闭", "Tutup", "Cerrar", "Fermer")
}

private fun dynamicUiCopy(copy: DynamicUiCopy, language: AppLanguage): String =
    when (language) {
        AppLanguage.ENGLISH -> copy.english
        AppLanguage.MANDARIN -> copy.mandarin
        AppLanguage.MALAY -> copy.malay
        AppLanguage.SPANISH -> copy.spanish
        AppLanguage.FRENCH -> copy.french
    }

private fun formatWallpaperCategory(wallpaper: Wallpaper): String =
    WallpaperCategory.fromLegacy(wallpaper.category)
        .id
        .split('_')
        .joinToString(" ") { token ->
            token.replaceFirstChar { char -> char.uppercase() }
        }

fun getGreeting(language: AppLanguage): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val minute = Calendar.getInstance().get(Calendar.MINUTE)
    val dayofweek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    if (hour==14 && minute==34){return GreetingCopy.YOU_LOST_THE_GAME.text(language)}
    when (dayofweek) {
        2 -> {
            return when (hour) {
                in 0..8 -> GreetingCopy.MONDAY_BLUES.text(language)
                in 9..11 -> GreetingCopy.RARING_TO_GO.text(language)
                in 12..16 -> GreetingCopy.GOOD_AFTERNOON.text(language)
                in 17..18 -> GreetingCopy.GOOD_EVENING.text(language)
                in 19..24 -> GreetingCopy.WINDING_DOWN.text(language)
                else -> ""
            }
        }
        3 -> {
            return when (hour) {
                in 0..7 -> GreetingCopy.HAPPY_TUESDAY.text(language)
                in 8..11 -> GreetingCopy.GOOD_MORNING.text(language)
                in 12..16 -> GreetingCopy.GOOD_AFTERNOON.text(language)
                in 17..18 -> GreetingCopy.GOOD_EVENING.text(language)
                in 19..24 -> GreetingCopy.WINDING_DOWN.text(language)
                else -> ""
            }
        }
        4 -> {
            return when (hour) {
                in 0..7 -> GreetingCopy.ITS_MIDWEEK.text(language)
                in 8..11 -> GreetingCopy.GOOD_MORNING.text(language)
                in 12..16 -> GreetingCopy.GOOD_AFTERNOON.text(language)
                in 17..18 -> GreetingCopy.GOOD_EVENING.text(language)
                in 19..24 -> GreetingCopy.WINDING_DOWN.text(language)
                else -> ""
            }
        }
        5 -> {
            return when (hour) {
                in 0..7 -> GreetingCopy.THURSDAYS_GREETINGS.text(language)
                in 8..11 -> GreetingCopy.GOOD_MORNING.text(language)
                in 12..16 -> GreetingCopy.GOOD_AFTERNOON.text(language)
                in 17..18 -> GreetingCopy.GOOD_EVENING.text(language)
                in 19..24 -> GreetingCopy.WINDING_DOWN.text(language)
                else -> ""
            }
        }
        6 -> {
            return when (hour) {
                in 0..10 -> GreetingCopy.TGIF.text(language)
                in 11..11 -> GreetingCopy.GOOD_MORNING.text(language)
                in 12..16 -> GreetingCopy.GOOD_AFTERNOON.text(language)
                in 17..18 -> GreetingCopy.FINISHED_FOR_THE_WEEK.text(language)
                in 19..24 -> GreetingCopy.PARTY_TIME.text(language)
                else -> ""
            }
        }
        7 -> {
            return when (hour) {
                in 0..2 -> GreetingCopy.PARTY_TIME.text(language)
                in 3..5 -> GreetingCopy.WINDING_DOWN.text(language)
                in 6..12 -> GreetingCopy.HAPPY_SATURDAY.text(language)
                in 13..16 -> GreetingCopy.GOOD_AFTERNOON.text(language)
                in 17..23 -> GreetingCopy.UNWIND_TIME.text(language)
                else -> ""
            }
        }
        1 -> {
            return when (hour) {
                in 0..9 -> GreetingCopy.HAPPY_SUNDAY.text(language)
                in 10..12 -> GreetingCopy.GOOD_MORNING.text(language)
                in 13..16 -> GreetingCopy.GOOD_AFTERNOON.text(language)
                in 17..19 -> GreetingCopy.READY_FOR_THE_WEEK.text(language)
                in 20..24 -> GreetingCopy.GOOD_EVENING.text(language)
                else -> ""
            }
        }
        else -> {
            return ""
        }
    }


}
@Composable
fun GreetingChip(language: AppLanguage) {
    val context = LocalContext.current
    Surface(

        color = if(shouldUseDarkTheme(context)){
            Color.Black
        }else{
            Color.White
        },
        modifier = Modifier.alpha(0.8f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = getGreeting(language).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
