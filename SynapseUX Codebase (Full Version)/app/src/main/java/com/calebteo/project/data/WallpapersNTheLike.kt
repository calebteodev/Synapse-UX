package com.calebteo.project.data

import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import com.calebteo.project.R

@Serializable
data class Wallpaper(
    val name: String,
    val filename: String,
    val category: String,
    val path: String,
    val url: String,
    val description: String,
    val tags: List<String>
)

val Context.wallpaperDataStore by preferencesDataStore(
    name = "wallpaper_cache"
)

private val jsonParser = Json {
    ignoreUnknownKeys = true
}

private const val WallpaperRepoRawBase =
    "https://raw.githubusercontent.com/calebteo18/project-wallpapers/main"
private const val WallpaperRepoCdnBase =
    "https://cdn.jsdelivr.net/gh/calebteo18/project-wallpapers@main"
private const val NetworkTimeoutMs = 15_000
private const val ConnectivityProbeUrl = "https://www.google.com/generate_204"
private const val ConnectivityProbeTimeoutMs = 1_500
private const val ConnectivityProbeCacheMs = 10_000L

private object ConnectivityProbeCache {
    @Volatile
    var timestampMs: Long = 0L

    @Volatile
    var isReachable: Boolean = false
}

private object WallpaperMemoryCache {
    var wallpapers: List<Wallpaper>? = null
    val bitmapCache = ConcurrentHashMap<String, Bitmap>()
    val selectedUrlCache = ConcurrentHashMap<String, String>()
}

private val offlineWallpaperResIds = listOf(
    R.drawable.offline_wallpaper_king_tut,
    R.drawable.offline_wallpaper_palm_tree,
    R.drawable.offline_wallpaper_penida_kelingkeling,
    R.drawable.offline_wallpaper_robin,
    R.drawable.offline_wallpaper_vlamingh_head_lighthouse
)

private val offlineWallpaperIndexKey =
    intPreferencesKey("offline_wallpaper_index")

fun hasInternetConnection(context: Context): Boolean {
    return try {
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val hasInternetCapability =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val now = System.currentTimeMillis()

        if (now - ConnectivityProbeCache.timestampMs <= ConnectivityProbeCacheMs) {
            return hasInternetCapability || ConnectivityProbeCache.isReachable
        }

        val isReachable = isGoogleReachable()
        ConnectivityProbeCache.timestampMs = now
        ConnectivityProbeCache.isReachable = isReachable
        hasInternetCapability || isReachable
    } catch (_: SecurityException) {
        false
    }
}

private fun isGoogleReachable(): Boolean {
    return runCatching {
        val connection = URL(ConnectivityProbeUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = ConnectivityProbeTimeoutMs
        connection.readTimeout = ConnectivityProbeTimeoutMs
        connection.instanceFollowRedirects = true
        connection.requestMethod = "GET"
        connection.useCaches = false
        connection.connect()
        val responseCode = connection.responseCode
        responseCode in 200..299 || responseCode == 204
    }.getOrDefault(false)
}

object WallpaperSessionCache {
    private val mutex = Mutex()
    private var defaultWallpaper: Wallpaper? = null
    private var offlineWallpaperResId: Int? = null
    var defaultWallpaperGeneration by mutableIntStateOf(0)
        private set

    private fun matchesSettings(
        wallpaper: Wallpaper,
        settings: LauncherSettings
    ): Boolean {
        return settings.overrideWallpaperTheme ||
            WallpaperCategory.fromLegacy(wallpaper.category) == settings.wallpaperTheme
    }

    suspend fun getOrInitializeDefaultWallpaper(
        context: Context,
        settings: LauncherSettings
    ): Wallpaper = mutex.withLock {
        defaultWallpaper
            ?.takeIf { matchesSettings(it, settings) }
            ?: run {
                val wallpapers = loadWallpapers()
                WallpaperSelectionEngine.chooseWallpaper(
                    settings = settings,
                    context = context,
                    wallpapers = wallpapers
                ).also { selected ->
                    defaultWallpaper = selected
                }
            }
    }

    suspend fun refreshDefaultWallpaper(
        context: Context,
        settings: LauncherSettings
    ): Wallpaper = mutex.withLock {
        val wallpapers = loadWallpapers()
        WallpaperSelectionEngine.chooseWallpaper(
            settings = settings,
            context = context,
            wallpapers = wallpapers
        ).also { selected ->
            defaultWallpaper = selected
            offlineWallpaperResId = null
            defaultWallpaperGeneration += 1
        }
    }

    suspend fun getOrInitializeOfflineWallpaper(
        context: Context
    ): Int = mutex.withLock {
        if (offlineWallpaperResId == null) {
            val prefs = context.wallpaperDataStore.data.first()
            val index =
                (prefs[offlineWallpaperIndexKey] ?: 0).mod(offlineWallpaperResIds.size)
            offlineWallpaperResId = offlineWallpaperResIds[index]
        }
        offlineWallpaperResId!!
    }

    suspend fun refreshOfflineWallpaper(
        context: Context
    ): Int = mutex.withLock {
        val prefs = context.wallpaperDataStore.data.first()
        val currentIndex = prefs[offlineWallpaperIndexKey] ?: -1
        val nextIndex = (currentIndex + 1).mod(offlineWallpaperResIds.size)

        context.wallpaperDataStore.edit {
            it[offlineWallpaperIndexKey] = nextIndex
        }

        offlineWallpaperResId = offlineWallpaperResIds[nextIndex]
        defaultWallpaper = null
        defaultWallpaperGeneration += 1
        offlineWallpaperResId!!
    }

    fun getDefaultWallpaper(): Wallpaper? = defaultWallpaper
}



private fun wallpaperCacheFile(context: Context, url: String): File {
    val dir = File(context.cacheDir, "wallpapers")
    if (!dir.exists()) dir.mkdirs()
    return File(dir, "${url.hashCode()}.img")
}

private fun openUrlBytes(url: String): ByteArray {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = NetworkTimeoutMs
    connection.readTimeout = NetworkTimeoutMs
    connection.instanceFollowRedirects = true
    return connection.inputStream.use { it.readBytes() }
}

private fun openUrlText(url: String): String =
    openUrlBytes(url).decodeToString()

private fun encodedRepoPath(path: String): String =
    path.replace(" ", "%20")

private fun wallpaperUrlCandidates(
    url: String,
    path: String? = null
): List<String> {
    val candidates = linkedSetOf<String>()
    if (path != null) {
        candidates += "$WallpaperRepoCdnBase/${encodedRepoPath(path)}"
    }
    candidates += url
    if (url.startsWith(WallpaperRepoRawBase)) {
        candidates += url.replace(WallpaperRepoRawBase, WallpaperRepoCdnBase)
    }
    return candidates.toList()
}

suspend fun loadWallpapers(): List<Wallpaper> {
    WallpaperMemoryCache.wallpapers?.let { return it }

    return withContext(Dispatchers.IO) {
        val jsonUrls = listOf(
            "$WallpaperRepoCdnBase/wallpapers.json",
            "$WallpaperRepoRawBase/wallpapers.json"
        )

        var lastError: Exception? = null

        for (jsonUrl in jsonUrls) {
            try {
                val jsonText = openUrlText(jsonUrl)
                val parsed = jsonParser.decodeFromString<List<Wallpaper>>(jsonText)
                    .map { wallpaper ->
                        wallpaper.copy(
                            url = wallpaperUrlCandidates(
                                url = wallpaper.url,
                                path = wallpaper.path
                            ).first()
                        )
                    }
                WallpaperMemoryCache.wallpapers = parsed
                return@withContext parsed
            } catch (e: Exception) {
                lastError = e
            }
        }

        throw lastError ?: IllegalStateException("Unable to load wallpapers")
    }
}


suspend fun getCachedWallpaperBitmap(
    context: Context,
    url: String
): Bitmap? {
    WallpaperMemoryCache.bitmapCache[url]?.let { return it }

    return withContext(Dispatchers.IO) {
        val file = wallpaperCacheFile(context, url)
        if (!file.exists()) return@withContext null

        BitmapFactory.decodeFile(file.absolutePath)?.also {
            WallpaperMemoryCache.bitmapCache[url] = it
        }
    }
}

suspend fun cacheWallpaperBitmap(
    context: Context,
    url: String
): Bitmap? {
    getCachedWallpaperBitmap(context, url)?.let { return it }

    return withContext(Dispatchers.IO) {
        for (candidateUrl in wallpaperUrlCandidates(url)) {
            try {
                val bytes = openUrlBytes(candidateUrl)

                val file = wallpaperCacheFile(context, url)
                FileOutputStream(file).use { it.write(bytes) }

                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.also {
                    WallpaperMemoryCache.bitmapCache[url] = it
                    return@withContext it
                }
            } catch (_: Exception) {
            }
        }

        null
    }
}



@Composable
fun UrlImage(
    url: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = rememberAsyncImagePainter(model = url),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
