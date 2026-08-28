package com.calebteo.project.ui.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.calebteo.project.R
import com.calebteo.project.ReminderItem
import com.calebteo.project.ReminderStore
import com.calebteo.project.data.AppLanguage
import com.calebteo.project.data.LauncherSettings
import com.calebteo.project.data.WallpaperSessionCache
import com.calebteo.project.data.WallpaperSource
import com.calebteo.project.data.cacheWallpaperBitmap
import com.calebteo.project.data.getCachedWallpaperBitmap
import com.calebteo.project.data.hasInternetConnection
import com.calebteo.project.ui.theme.shouldUseDarkTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class MenuCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    ACTIONS_HUB("Actions Hub", "操作中心", "Hab Tindakan", "Centro de acciones", "Hub d'actions"),
    TUTORIAL("Tutorial", "教程", "Tutorial", "Tutorial", "Tutoriel"),
    SEARCH_THE_WEB("Search the Web", "搜索网页", "Cari di Web", "Buscar en la Web", "Rechercher sur le Web"),
    COMMUNICATIONS_HUB("Communications Hub", "通讯中心", "Hab Komunikasi", "Centro de comunicaciones", "Hub de communication"),
    SEND_A_MESSAGE("Send a Message", "发送消息", "Hantar Mesej", "Enviar un Mensaje", "Envoyer un Message"),
    SEND_AN_EMAIL("Send an Email", "发送电子邮件", "Hantar E-mel", "Enviar un Correo", "Envoyer un E-mail"),
    REMINDERS("Reminders", "提醒", "Peringatan", "Recordatorios", "Rappels"),
    REMINDER_TITLE("Reminder title", "提醒标题", "Tajuk peringatan", "Titulo del recordatorio", "Titre du rappel"),
    PICK_DATE("Pick date", "选择日期", "Pilih tarikh", "Elegir fecha", "Choisir la date"),
    PICK_TIME("Pick time", "选择时间", "Pilih masa", "Elegir hora", "Choisir l'heure"),
    ADD_REMINDER("Add reminder", "添加提醒", "Tambah peringatan", "Agregar recordatorio", "Ajouter un rappel"),
    REMOVE("Remove", "移除", "Buang", "Eliminar", "Supprimer"),
    NO_REMINDERS("No reminders yet.", "还没有提醒。", "Belum ada peringatan.", "Todavia no hay recordatorios.", "Aucun rappel pour le moment."),
    BRIGHTNESS("Brightness", "亮度", "Kecerahan", "Brillo", "Luminosite"),
    VOLUME("Volume", "音量", "Kelantangan", "Volumen", "Volume"),
    TYPE_YOUR_MESSAGE("Type your message", "输入你的消息", "Taip mesej anda", "Escribe tu mensaje", "Ecrivez votre message"),
    TYPE_YOUR_EMAIL_SUBJECT("Type your email subject", "输入电子邮件主题", "Taip subjek e-mel anda", "Escribe el asunto del correo", "Ecrivez l'objet de l'e-mail"),
    TYPE_YOUR_EMAIL("Type your email", "输入你的电子邮件", "Taip e-mel anda", "Escribe tu correo", "Ecrivez votre e-mail"),
    SEND("Send", "发送", "Hantar", "Enviar", "Envoyer"),
    EXPAND("Expand", "展开", "Kembangkan", "Expandir", "Developper"),
    COLLAPSE("Collapse", "收起", "Kecilkan", "Contraer", "Reduire"),
    BACK("Back", "返回", "Kembali", "Volver", "Retour");

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

private enum class MenuPage {
    MAIN,
    TUTORIAL
}

private enum class WebPanelCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    OFFLINE(
        "Web access is unavailable offline. Reconnect to use search.",
        "离线时无法使用网页访问。重新联网即可搜索。",
        "Akses web tidak tersedia di luar talian. Sambung semula internet untuk mencari.",
        "El acceso web no esta disponible sin conexion. Vuelve a conectarte para buscar.",
        "L'acces web n'est pas disponible hors ligne. Reconnectez-vous pour lancer une recherche."
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

private enum class ReminderPanelCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    CHOOSE_DATE_TIME(
        "Choose a date and time, then add it to your reminder list.",
        "选择日期和时间，然后将其添加到提醒列表。",
        "Pilih tarikh dan masa, kemudian tambah ke senarai peringatan anda.",
        "Elige una fecha y hora y agregala a tu lista de recordatorios.",
        "Choisissez une date et une heure, puis ajoutez-les a votre liste de rappels."
    ),
    PICKED_TIME_MISSING(
        "Please choose both a date and a time.",
        "请选择日期和时间。",
        "Sila pilih tarikh dan masa.",
        "Elige una fecha y una hora.",
        "Veuillez choisir une date et une heure."
    ),
    TITLE_MISSING(
        "Please enter a reminder title.",
        "请输入提醒标题。",
        "Sila masukkan tajuk peringatan.",
        "Escribe un titulo para el recordatorio.",
        "Veuillez saisir un titre pour le rappel."
    ),
    PAST_TIME(
        "Please choose a future time.",
        "请选择未来的时间。",
        "Sila pilih masa pada masa hadapan.",
        "Elige una hora futura.",
        "Veuillez choisir une heure dans le futur."
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

@Composable
fun MenuScreen(settings: LauncherSettings) {
    val context = LocalContext.current
    val isDarkTheme = shouldUseDarkTheme(context)
    var currentPage by remember { mutableStateOf(MenuPage.MAIN) }
    val cardColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.78f)
    }
    val contentColor = if (isDarkTheme) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxSize()) {
        MenuWallpaperBackground(settings = settings)

        when (currentPage) {
            MenuPage.MAIN -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        val actionsHubBackground = if (isDarkTheme) Color.Black else Color.White
                        val actionsHubTextColor = if (isDarkTheme) Color.White else Color.Black
                        Text(
                            text = MenuCopy.ACTIONS_HUB.text(settings.language),
                            style = MaterialTheme.typography.headlineMedium,
                            color = actionsHubTextColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(actionsHubBackground.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 10.dp)

                        )
                    }
                    item {
                        TutorialNavigationCard(
                            title = MenuCopy.TUTORIAL.text(settings.language),
                            cardColor = cardColor,
                            enabled = settings.language == AppLanguage.ENGLISH,
                            onClick = { currentPage = MenuPage.TUTORIAL }
                        )
                    }
                    item {
                        MenuPanel(
                            title = null,
                            cardColor = cardColor,
                            contentColor = contentColor
                        ) {
                            ControlsPanel(
                                contentColor = contentColor,
                                language = settings.language
                            )
                        }
                    }
                    item {
                        MenuPanel(
                            title = MenuCopy.SEARCH_THE_WEB.text(settings.language),
                            cardColor = cardColor,
                            contentColor = contentColor
                        ) {
                            WebPanel(language = settings.language)
                        }
                    }
                    item {
                        MenuPanel(
                            title = MenuCopy.REMINDERS.text(settings.language),
                            cardColor = cardColor,
                            contentColor = contentColor
                        ) {
                            ReminderPanel(
                                contentColor = contentColor,
                                language = settings.language
                            )
                        }
                    }
                    item {
                        MenuPanel(
                            title = MenuCopy.COMMUNICATIONS_HUB.text(settings.language),
                            cardColor = cardColor,
                            contentColor = contentColor
                        ) {
                            CommunicationPanels(
                                contentColor = contentColor,
                                language = settings.language
                            )
                        }
                    }
                    item {
                        Text(
                            text = "© Caleb Teo 2026",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                }
            }

            MenuPage.TUTORIAL -> {
                TutorialScreen(
                    language = settings.language,
                    onBack = { currentPage = MenuPage.MAIN }
                )
            }
        }
    }
}

@Composable
private fun MenuWallpaperBackground(settings: LauncherSettings) {
    val context = LocalContext.current
    val uiState by produceState<WallpaperUiState>(
        initialValue = WallpaperUiState.Loading,
        settings.wallpaperSource,
        settings.customWallpaperUri,
        settings.wallpaperTheme,
        settings.overrideWallpaperTheme
    ) {
        value = when (settings.wallpaperSource) {
            WallpaperSource.DEFAULT -> {
                if (hasInternetConnection(context)) {
                    val selectedWallpaper = WallpaperSessionCache.getDefaultWallpaper()
                        ?: WallpaperSessionCache.getOrInitializeDefaultWallpaper(context, settings)
                    val bitmap = getCachedWallpaperBitmap(context, selectedWallpaper.url)
                        ?: cacheWallpaperBitmap(context, selectedWallpaper.url)
                    if (bitmap != null) {
                        WallpaperUiState.Bitmap(bitmap.asImageBitmap())
                    } else {
                        WallpaperUiState.Remote(selectedWallpaper.url)
                    }
                } else {
                    val offlineResId = WallpaperSessionCache.getOrInitializeOfflineWallpaper(context)
                    val bitmap = BitmapFactory.decodeResource(context.resources, offlineResId)
                    if (bitmap != null) {
                        WallpaperUiState.Bitmap(bitmap.asImageBitmap())
                    } else {
                        WallpaperUiState.Empty
                    }
                }
            }

            WallpaperSource.SYSTEM -> {
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.synapse)
                if (bitmap != null) {
                    WallpaperUiState.Bitmap(bitmap.asImageBitmap())
                } else {
                    WallpaperUiState.Empty
                }
            }

            WallpaperSource.CUSTOM -> {
                val bitmap = settings.customWallpaperUri?.let { uri ->
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                if (bitmap != null) {
                    WallpaperUiState.Bitmap(bitmap.asImageBitmap())
                } else {
                    WallpaperUiState.Empty
                }
            }
        }
    }

    when (val current = uiState) {
        is WallpaperUiState.Bitmap -> Image(
            bitmap = current.bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        is WallpaperUiState.Remote -> Image(
            painter = rememberAsyncImagePainter(model = current.url),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        else -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Composable
private fun MenuPanel(
    title: String?,
    cardColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            content()
        }
    }
}

@Composable
private fun TutorialNavigationCard(
    title: String,
    cardColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
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
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold
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
private fun VideoPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val videoUri = remember(context) {
        Uri.parse("android.resource://${context.packageName}/${R.raw.tutorial_video}")
    }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setOnPreparedListener {
                    it.isLooping = true
                    start()
                }
            }
        },
        update = { view ->
            if (view.tag != videoUri.toString()) {
                view.tag = videoUri.toString()
                view.setVideoURI(videoUri)
            } else if (!view.isPlaying) {
                view.start()
            }
        },
        modifier = modifier
            .background(Color.Black)
    )
}

@Composable
private fun TutorialScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = MenuCopy.BACK.text(language),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = MenuCopy.BACK.text(language),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            VideoPanel(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(28.dp))
                    .height(560.dp)
            )
        }
    }
}

@Composable
private fun ControlsPanel(
    contentColor: Color,
    language: AppLanguage
) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var volume by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.toFloat()
        )
    }
    val activity = remember(context) { context.findActivity() }
    var brightness by remember {
        mutableFloatStateOf(
            readSystemBrightness(context)
        )
    }

    LaunchedEffect(context, audioManager, maxVolume) {
        while (true) {
            volume =
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.toFloat()
            brightness = readSystemBrightness(context)
            delay(250)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Brightness6,
                    contentDescription = MenuCopy.BRIGHTNESS.text(language),
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = MenuCopy.BRIGHTNESS.text(language),
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Slider(
                value = brightness,
                onValueChange = {
                    if (Settings.System.canWrite(context)) {
                        brightness = it
                        val brightnessInt = (it * 255).toInt().coerceIn(1, 255)
                        Settings.System.putInt(
                            context.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            brightnessInt
                        )
                        activity?.window?.attributes =
                            activity.window.attributes.apply { screenBrightness = it }
                    }
                },
                valueRange = 0.1f..1f
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = MenuCopy.VOLUME.text(language),
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = MenuCopy.VOLUME.text(language),
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        (it * maxVolume).toInt(),
                        0
                    )
                },
                valueRange = 0f..1f
            )
        }
    }
}

private fun readSystemBrightness(context: Context): Float {
    return runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        ) / 255f
    }.getOrDefault(1f).coerceIn(0.1f, 1f)
}

@Composable
private fun WebPanel(language: AppLanguage) {
    val isInternetAvailable = rememberInternetAvailability()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    isVerticalScrollBarEnabled = true
                    overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_MOVE -> {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                            }

                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> {
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }
                    loadUrl("https://www.google.com")
                }
            },
            update = { webView ->
                webView.isEnabled = isInternetAvailable
                webView.alpha = if (isInternetAvailable) 1f else 0.35f
                if (isInternetAvailable && webView.url.isNullOrBlank()) {
                    webView.loadUrl("https://www.google.com")
                } else if (!isInternetAvailable) {
                    webView.stopLoading()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isInternetAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = WebPanelCopy.OFFLINE.text(language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun ReminderPanel(
    contentColor: Color,
    language: AppLanguage
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val reminders by ReminderStore.remindersFlow(context).collectAsState(initial = emptyList())
    val sortedReminders = reminders.sortedBy { it.triggerAtMillis }
    val dateFormatter = remember(language) { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    var title by remember { mutableStateOf("") }
    var selectedTimeMillis by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = ReminderPanelCopy.CHOOSE_DATE_TIME.text(language),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor.copy(alpha = 0.8f)
        )
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    MenuCopy.REMINDER_TITLE.text(language),
                    color = contentColor.copy(alpha = 0.7f)
                )
            },
            colors = reminderFieldColors(contentColor)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    showReminderDateTimePicker(
                        context = context,
                        initialTimeMillis = selectedTimeMillis,
                        onDateTimePicked = {
                            selectedTimeMillis = it
                            errorMessage = null
                        }
                    )
                },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "${MenuCopy.PICK_DATE.text(language)} / ${MenuCopy.PICK_TIME.text(language)}",
                    tint = contentColor
                )
            }
            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    val triggerAtMillis = selectedTimeMillis
                    errorMessage = when {
                        trimmedTitle.isBlank() -> ReminderPanelCopy.TITLE_MISSING.text(language)
                        triggerAtMillis == null -> ReminderPanelCopy.PICKED_TIME_MISSING.text(language)
                        triggerAtMillis <= System.currentTimeMillis() -> ReminderPanelCopy.PAST_TIME.text(language)
                        else -> null
                    }

                    if (errorMessage == null && triggerAtMillis != null) {
                        val reminder = ReminderItem(
                            id = UUID.randomUUID().toString(),
                            title = trimmedTitle,
                            triggerAtMillis = triggerAtMillis
                        )
                        scope.launch {
                            ReminderStore.addReminder(context, reminder)
                        }
                        title = ""
                        selectedTimeMillis = null
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = MenuCopy.ADD_REMINDER.text(language)
                )
                Text(
                    text = MenuCopy.ADD_REMINDER.text(language),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        if (selectedTimeMillis != null) {
            Text(
                text = dateFormatter.format(Date(selectedTimeMillis!!)),
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (sortedReminders.isEmpty()) {
            Text(
                text = MenuCopy.NO_REMINDERS.text(language),
                color = contentColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            sortedReminders.forEach { reminder ->
                ReminderListItem(
                    reminder = reminder,
                    contentColor = contentColor,
                    formattedDate = dateFormatter.format(Date(reminder.triggerAtMillis)),
                    removeLabel = MenuCopy.REMOVE.text(language),
                    onRemove = {
                        scope.launch {
                            ReminderStore.removeReminder(context, reminder.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderListItem(
    reminder: ReminderItem,
    contentColor: Color,
    formattedDate: String,
    removeLabel: String,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formattedDate,
                    color = contentColor.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(
                onClick = onRemove,
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = removeLabel
                )
                Text(
                    text = removeLabel,
                    modifier = Modifier.padding(start = 6.dp),
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun CommunicationPanels(
    contentColor: Color,
    language: AppLanguage
) {
    val isInternetAvailable = rememberInternetAvailability()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExpandableCommunicationCard(
            title = MenuCopy.SEND_A_MESSAGE.text(language),
            contentColor = contentColor,
            expandLabel = MenuCopy.EXPAND.text(language),
            collapseLabel = MenuCopy.COLLAPSE.text(language)
        ) {
            SmsPanel(
                contentColor = contentColor,
                language = language,
                isInternetAvailable = isInternetAvailable
            )
        }

        ExpandableCommunicationCard(
            title = MenuCopy.SEND_AN_EMAIL.text(language),
            contentColor = contentColor,
            expandLabel = MenuCopy.EXPAND.text(language),
            collapseLabel = MenuCopy.COLLAPSE.text(language)
        ) {
            EmailPanel(
                contentColor = contentColor,
                language = language,
                isInternetAvailable = isInternetAvailable
            )
        }
    }
}

@Composable
private fun ExpandableCommunicationCard(
    title: String,
    contentColor: Color,
    expandLabel: String,
    collapseLabel: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold
                )
                Button(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null
                    )
                    Text(
                        text = if (expanded) collapseLabel else expandLabel,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            if (expanded) {
                content()
            }
        }
    }
}

@Composable
private fun SmsPanel(
    contentColor: Color,
    language: AppLanguage,
    isInternetAvailable: Boolean
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            placeholder = {
                Text(
                    MenuCopy.TYPE_YOUR_MESSAGE.text(language),
                    color = contentColor.copy(alpha = 0.7f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = contentColor,
                unfocusedBorderColor = contentColor.copy(alpha = 0.55f),
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                cursorColor = contentColor
            )
        )
        Button(
            enabled = isInternetAvailable,
            onClick = {
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:")
                    putExtra("sms_body", message)
                }
                if (smsIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(smsIntent)
                    message = ""
                }
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = MenuCopy.SEND.text(language)
            )
            Text(
                text = MenuCopy.SEND.text(language),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun EmailPanel(
    contentColor: Color,
    language: AppLanguage,
    isInternetAvailable: Boolean
) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    MenuCopy.TYPE_YOUR_EMAIL_SUBJECT.text(language),
                    color = contentColor.copy(alpha = 0.7f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = contentColor,
                unfocusedBorderColor = contentColor.copy(alpha = 0.55f),
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                cursorColor = contentColor
            )
        )
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            placeholder = {
                Text(
                    MenuCopy.TYPE_YOUR_EMAIL.text(language),
                    color = contentColor.copy(alpha = 0.7f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = contentColor,
                unfocusedBorderColor = contentColor.copy(alpha = 0.55f),
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                cursorColor = contentColor
            )
        )
        Button(
            enabled = isInternetAvailable,
            onClick = {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                if (emailIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(emailIntent)
                    subject = ""
                    message = ""
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = MenuCopy.SEND_AN_EMAIL.text(language)
            )
            Text(
                text = MenuCopy.SEND.text(language),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun reminderFieldColors(contentColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = contentColor,
    unfocusedBorderColor = contentColor.copy(alpha = 0.55f),
    focusedTextColor = contentColor,
    unfocusedTextColor = contentColor,
    cursorColor = contentColor
)

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun rememberInternetAvailability(): Boolean {
    val context = LocalContext.current
    var isInternetAvailable by remember(context) {
        mutableStateOf(context.isInternetAvailable())
    }

    DisposableEffect(context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (connectivityManager == null) {
            isInternetAvailable = false
            onDispose { }
        } else {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isInternetAvailable = context.isInternetAvailable()
                }

                override fun onLost(network: Network) {
                    isInternetAvailable = context.isInternetAvailable()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    isInternetAvailable =
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            }

            isInternetAvailable = context.isInternetAvailable()
            runCatching {
                connectivityManager.registerDefaultNetworkCallback(callback)
            }.onFailure {
                isInternetAvailable = context.isInternetAvailable()
            }

            onDispose {
                runCatching {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            }
        }
    }

    return isInternetAvailable
}

private fun Context.isInternetAvailable(): Boolean {
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun showReminderDateTimePicker(
    context: Context,
    initialTimeMillis: Long?,
    onDateTimePicked: (Long) -> Unit
) {
    val initialCalendar = Calendar.getInstance().apply {
        timeInMillis = initialTimeMillis ?: System.currentTimeMillis()
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, initialCalendar.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, initialCalendar.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    pickedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    pickedCalendar.set(Calendar.MINUTE, minute)
                    onDateTimePicked(pickedCalendar.timeInMillis)
                },
                pickedCalendar.get(Calendar.HOUR_OF_DAY),
                pickedCalendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        initialCalendar.get(Calendar.YEAR),
        initialCalendar.get(Calendar.MONTH),
        initialCalendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
