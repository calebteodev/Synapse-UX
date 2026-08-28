package com.calebteo.project.ui.screens

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.calebteo.project.R
import com.calebteo.project.data.AppInfo
import com.calebteo.project.data.AppLanguage
import com.calebteo.project.data.LauncherSettings
import com.calebteo.project.data.UrlImage
import com.calebteo.project.data.Wallpaper
import com.calebteo.project.data.WallpaperCategory
import com.calebteo.project.data.WallpaperCategoryRanking
import com.calebteo.project.data.WallpaperSessionCache
import com.calebteo.project.data.WallpaperSource
import com.calebteo.project.data.cacheWallpaperBitmap
import com.calebteo.project.data.getCachedWallpaperBitmap
import com.calebteo.project.data.hasInternetConnection
import com.calebteo.project.data.shouldAskFeedback
import com.calebteo.project.ui.theme.ThemeColor
import com.calebteo.project.ui.theme.initializeBestThemeColor
import com.calebteo.project.ui.theme.shouldUseDarkTheme
import com.calebteo.project.ui.theme.updateColorScores
import com.calebteo.project.viewmodel.LauncherViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

sealed class WallpaperUiState {
    object Loading : WallpaperUiState()
    data class Remote(val url: String) : WallpaperUiState()
    data class Bitmap(val bitmap: ImageBitmap) : WallpaperUiState()
    object Empty : WallpaperUiState()
    data class Error(val throwable: Throwable) : WallpaperUiState()
}

//From YAGL
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppActionDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.updateSearchQuery("")
                onDismiss()
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .background(Color.Transparent)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps) { app ->
                        AppItem(
                            app = app,
                            onClick = {
                                viewModel.launchApp(app.packageName)
                                onDismiss()
                            },
                            onLongPress = {
                                selectedApp = app
                                showAppActionDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAppActionDialog && selectedApp != null) {
        AppActionDialog(
            app = selectedApp!!,
            language = viewModel.settings.collectAsState().value.language,
            onDismiss = {
                showAppActionDialog = false
                selectedApp = null
            },
            onAppInfo = {
                viewModel.openAppInfo(selectedApp!!.packageName)
                showAppActionDialog = false
                selectedApp = null
            },
            onUninstall = {
                viewModel.uninstallApp(selectedApp!!.packageName)
                showAppActionDialog = false
                selectedApp = null
            }
        )
    }
}
//from YAGL
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(
            drawable = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
//from YAGL
@Composable
fun AppIcon(
    drawable: Drawable,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
        drawable.toBitmap(width = width, height = height).asImageBitmap()
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Transparent
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun getCurrentTime(is24Hour: Boolean): String {
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date())
}

private fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    return sdf.format(Date())
}

@Composable
fun WallpaperBackground(
    settings: LauncherSettings,
    suppressFeedbackDialog: Boolean = false
) {

    val context = LocalContext.current

    var showFeedback by remember { mutableStateOf(false) }
    var pendingWallpaper by remember { mutableStateOf<Wallpaper?>(null) }

    val uiState by produceState<WallpaperUiState>(
        initialValue = WallpaperUiState.Loading,
        settings.wallpaperSource,
        settings.customWallpaperUri,
        WallpaperSessionCache.defaultWallpaperGeneration,
        settings.wallpaperTheme,
        settings.overrideWallpaperTheme
    ) {

        value = try {

            when (settings.wallpaperSource) {

                WallpaperSource.DEFAULT -> {
                    if (!hasInternetConnection(context)) {
                        pendingWallpaper = null
                        val offlineResId =
                            WallpaperSessionCache.getOrInitializeOfflineWallpaper(context)
                        val offlineBitmap = BitmapFactory.decodeResource(
                            context.resources,
                            offlineResId
                        )

                        if (offlineBitmap != null) {
                            if (settings.overrideColorTheme) {
                                updateColorScores(context, offlineBitmap)
                                initializeBestThemeColor(context)
                            }

                            WallpaperUiState.Bitmap(offlineBitmap.asImageBitmap())
                        } else {
                            WallpaperUiState.Empty
                        }
                    } else {

                        val selected =
                            WallpaperSessionCache.getOrInitializeDefaultWallpaper(
                                context = context,
                                settings = settings
                            )

                        pendingWallpaper = selected

                        val cachedUrl =
                            selected.url

                        val cachedBitmap =
                            cachedUrl.let { getCachedWallpaperBitmap(context, it) }

                        if (cachedBitmap != null) {

                            if (settings.overrideColorTheme) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    updateColorScores(context, cachedBitmap)
                                    initializeBestThemeColor(context)
                                }
                            }

                            WallpaperUiState.Bitmap(cachedBitmap.asImageBitmap())

                        } else {

                            val bitmap =
                                cacheWallpaperBitmap(context, selected.url)

                            if (bitmap != null) {

                                if (settings.overrideColorTheme) {
                                    updateColorScores(context, bitmap)
                                    initializeBestThemeColor(context)
                                }

                                WallpaperUiState.Bitmap(bitmap.asImageBitmap())

                            } else {

                                WallpaperUiState.Remote(selected.url)

                            }
                        }
                    }
                }

                WallpaperSource.SYSTEM -> {
                    val androidBitmap = BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.synapse
                    )

                    if (androidBitmap != null) {

                        if (settings.overrideColorTheme) {
                            updateColorScores(context, androidBitmap)
                            initializeBestThemeColor(context)
                        }

                        WallpaperUiState.Bitmap(androidBitmap.asImageBitmap())

                    } else {

                        WallpaperUiState.Empty
                    }
                }

                WallpaperSource.CUSTOM -> {

                    val uriString = settings.customWallpaperUri

                    val androidBitmap = uriString?.let {

                        context.contentResolver
                            .openInputStream(Uri.parse(it))
                            ?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                    }

                    if (androidBitmap != null) {

                        if (settings.overrideColorTheme) {
                            updateColorScores(context, androidBitmap)
                            initializeBestThemeColor(context)
                        }

                        WallpaperUiState.Bitmap(androidBitmap.asImageBitmap())

                    } else {

                        WallpaperUiState.Empty
                    }
                }
            }

        } catch (e: Exception) {

            Log.e("Project_Wallpaper", "Wallpaper load failed", e)

            if (settings.wallpaperSource == WallpaperSource.DEFAULT) {
                pendingWallpaper = null
                WallpaperUiState.Error(e)
            } else {
                WallpaperUiState.Error(e)
            }
        }
    }

    when (uiState) {

        WallpaperUiState.Loading -> {
            GradientFallback()
        }

        is WallpaperUiState.Remote -> {

            UrlImage(
                url = (uiState as WallpaperUiState.Remote).url,
                modifier = Modifier.fillMaxSize()
            )
        }

        is WallpaperUiState.Bitmap -> {

            Image(
                bitmap = (uiState as WallpaperUiState.Bitmap).bitmap,
                contentDescription = "Wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        WallpaperUiState.Empty,
        is WallpaperUiState.Error -> {
            GradientFallback()
        }
    }

    /*
       Feedback trigger logic
    */

    LaunchedEffect(pendingWallpaper) {

        val wallpaper = pendingWallpaper ?: return@LaunchedEffect
        if (!hasInternetConnection(context)) return@LaunchedEffect

        delay(5000)

        if (suppressFeedbackDialog) return@LaunchedEffect

        if (shouldAskFeedback(context)) {
            showFeedback = true
        }

    }

    /*
       Feedback dialog UI
    */

    if (showFeedback && pendingWallpaper != null) {

        WallpaperFeedbackDialog(
            wallpaper = pendingWallpaper!!,
            language = settings.language,
            onDismiss = {
                showFeedback = false
            }
        )

    }
}

@Composable
fun GradientFallback() {
    val context = LocalContext.current
    val fallbackRes = if (shouldUseDarkTheme(context)) {
        R.drawable.synapse_darkmode
    } else {
        R.drawable.synapse_lightmode
    }

    Image(
        painter = painterResource(fallbackRes),
        contentDescription = "Fallback wallpaper",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
//from YAGL
@Composable
fun AppActionDialog(
    app: AppInfo,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            AppIcon(
                drawable = app.icon,
                contentDescription = app.label,
                modifier = Modifier.size(56.dp)
            )
        },
        title = {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppInfo() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = homeScreenCopy(HomeScreenCopy.APP_INFO, language),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = homeScreenCopy(HomeScreenCopy.APP_INFO, language),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUninstall() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = homeScreenCopy(HomeScreenCopy.UNINSTALL, language),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = homeScreenCopy(HomeScreenCopy.UNINSTALL, language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(homeScreenCopy(HomeScreenCopy.CANCEL, language))
            }
        }
    )
}

@Composable
fun ListAppDrawer(
    viewModel: LauncherViewModel,
    suppressWallpaperDialogs: Boolean = false,
    onDismiss: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppActionDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackground(
            settings = viewModel.settings.collectAsState().value,
            suppressFeedbackDialog = suppressWallpaperDialogs
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true
            )

            ListDrawerHeader(
                language = settings.language,
                onBack = {
                    viewModel.updateSearchQuery("")
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ListDrawerBody(
                apps = filteredApps,
                onAppClick = {
                    viewModel.launchApp(it.packageName)
                    viewModel.updateSearchQuery("")
                    onDismiss()
                },
                onAppLongPress = {
                    selectedApp = it
                    showAppActionDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ListDrawerFooter {
                viewModel.updateSearchQuery("")
                onDismiss()
            }
        }
    }

    if (showAppActionDialog && selectedApp != null) {
        AppActionDialog(
            app = selectedApp!!,
            language = settings.language,
            onDismiss = {
                showAppActionDialog = false
                selectedApp = null
            },
            onAppInfo = {
                viewModel.openAppInfo(selectedApp!!.packageName)
                showAppActionDialog = false
                selectedApp = null
            },
            onUninstall = {
                viewModel.uninstallApp(selectedApp!!.packageName)
                showAppActionDialog = false
                selectedApp = null
            }
        )
    }
}

@Composable
fun ListDrawerHeader(
    language: AppLanguage,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = homeScreenCopy(HomeScreenCopy.APPS, language),
            style = MaterialTheme.typography.titleLarge,
            letterSpacing = 2.sp,
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .padding(end = 48.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ListDrawerBody(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps) { app ->
                ListAppItem(
                    app = app,
                    onClick = { onAppClick(app) },
                    onLongPress = { onAppLongPress(app) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListAppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(
            drawable = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ListDrawerFooter(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable { onDismiss() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
    }
}

@Composable
fun CompactListRow(
    app: AppInfo,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(
            drawable = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(36.dp)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun WallpaperFeedbackDialog(
    wallpaper: Wallpaper,
    language: AppLanguage,
    onDismiss: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()



    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(homeScreenCopy(HomeScreenCopy.DO_YOU_LIKE_THIS_WALLPAPER, language))
        },

        text = {
            Text(homeScreenCopy(HomeScreenCopy.WALLPAPER_FEEDBACK_BODY, language))
        },

        confirmButton = {

            TextButton(
                onClick = {

                    val category =
                        WallpaperCategory.fromLegacy(wallpaper.category)

                    onDismiss()

                    CoroutineScope(Dispatchers.IO).launch {

                        WallpaperCategoryRanking.adjustScore(
                            context,
                            category,
                            +1
                        )

                        val scores = WallpaperCategoryRanking.getAllScores(context)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                homeScreenCopy(HomeScreenCopy.FEEDBACK_NOTED_POSITIVE, language),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            ){
                Text(homeScreenCopy(HomeScreenCopy.LIKE, language))
            }},

        dismissButton = {

            TextButton(
                onClick = {

                    val category =
                        WallpaperCategory.fromLegacy(wallpaper.category)

                    onDismiss()

                    CoroutineScope(Dispatchers.IO).launch {

                        WallpaperCategoryRanking.adjustScore(
                            context,
                            category,
                            -1
                        )

                        val scores = WallpaperCategoryRanking.getAllScores(context)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                homeScreenCopy(HomeScreenCopy.FEEDBACK_NOTED_NEGATIVE, language),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            ) {
                Text(homeScreenCopy(HomeScreenCopy.DISLIKE, language))
            }
        }

    )
}

private enum class HomeScreenCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    APPS("APPS", "应用程序", "APL", "APLICACIONES", "APPLICATIONS"),
    DO_YOU_LIKE_THIS_WALLPAPER(
        "Do you like this wallpaper?",
        "你喜欢这张壁纸吗？",
        "Adakah anda suka kertas dinding ini?",
        "Te gusta este fondo de pantalla?",
        "Aimez-vous ce fond d'ecran ?"
    ),
    WALLPAPER_FEEDBACK_BODY(
        "Your feedback helps improve recommendations.",
        "你的反馈有助于改进推荐。",
        "Maklum balas anda membantu menambah baik cadangan.",
        "Tus comentarios ayudan a mejorar las recomendaciones.",
        "Vos retours aident a ameliorer les recommandations."
    ),
    FEEDBACK_NOTED_POSITIVE(
        "Feedback Noted!",
        "已记录反馈！",
        "Maklum balas direkodkan!",
        "Comentarios registrados!",
        "Retour enregistre !"
    ),
    FEEDBACK_NOTED_NEGATIVE(
        "Feedback Noted.",
        "已记录反馈。",
        "Maklum balas direkodkan.",
        "Comentarios registrados.",
        "Retour enregistre."
    ),
    APP_INFO("App Info", "应用信息", "Maklumat Apl", "Informacion de la app", "Infos de l'appli"),
    UNINSTALL("Uninstall", "卸载", "Nyahpasang", "Desinstalar", "Desinstaller"),
    CANCEL("Cancel", "取消", "Batal", "Cancelar", "Annuler"),
    LIKE("👍 Like", "👍 喜欢", "👍 Suka", "👍 Me gusta", "👍 J'aime"),
    DISLIKE("👎 Dislike", "👎 不喜欢", "👎 Tidak suka", "👎 No me gusta", "👎 Je n'aime pas")
}

private fun homeScreenCopy(copy: HomeScreenCopy, language: AppLanguage): String =
    when (language) {
        AppLanguage.ENGLISH -> copy.english
        AppLanguage.MANDARIN -> copy.mandarin
        AppLanguage.MALAY -> copy.malay
        AppLanguage.SPANISH -> copy.spanish
        AppLanguage.FRENCH -> copy.french
    }
