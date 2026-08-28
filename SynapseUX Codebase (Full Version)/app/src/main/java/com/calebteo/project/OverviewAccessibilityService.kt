package com.calebteo.project

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.calebteo.project.data.hasInternetConnection
import com.calebteo.project.data.SettingsManager
import com.calebteo.project.ui.theme.ThemeColor
import com.calebteo.project.ui.theme.getBestThemeColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class OverviewAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val overlayButtons = mutableListOf<OverlayButtonView>()
    private var navigationVisible = true
    private var currentPackageName: String? = null
    private val overlayVisibilityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            navigationVisible = intent?.getBooleanExtra(EXTRA_NAVIGATION_VISIBLE, true) ?: true
            updateOverlayVisibility()
        }
    }

    override fun onServiceConnected() {
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        serviceInfo = serviceInfo?.apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        navigationVisible = runBlocking {
            SettingsManager(this@OverviewAccessibilityService)
                .settingsFlow
                .first()
                .synapseNavigationEnabled
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                overlayVisibilityReceiver,
                IntentFilter(ACTION_SET_NAVIGATION_VISIBILITY),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                overlayVisibilityReceiver,
                IntentFilter(ACTION_SET_NAVIGATION_VISIBILITY)
            )
        }
        applySynapseWallpaper(this)
        showHomeOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        currentPackageName = event?.packageName?.toString() ?: currentPackageName
        updateOverlayVisibility()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        runCatching { unregisterReceiver(overlayVisibilityReceiver) }
        removeHomeOverlay()
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    private fun showHomeOverlay() {
        if (overlayView != null) {
            updateOverlayStyle()
            return
        }

        val context = this
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 64f
            setColor(0xE6FFFFFF.toInt())
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            this.background = background
            elevation = 16f
            setPadding(13, 11, 13, 11)
            val buttonPadding = 33
            overlayButtons.clear()
            listOf(
                createOverlayButton(
                    context = context,
                    iconRes = R.drawable.ic_overlay_switcher,
                    contentDescription = "App switcher",
                    padding = buttonPadding
                ) {
                    performGlobalAction(GLOBAL_ACTION_RECENTS)
                },
                createOverlayButton(
                    context = context,
                    iconRes = R.drawable.ic_overlay_home,
                    contentDescription = "Home",
                    padding = buttonPadding,
                    highlighted = true
                ) {
                    launchLauncherHome(
                        context = context,
                        bringToFront = currentPackageName != context.packageName
                    )
                },
                createOverlayButton(
                    context = context,
                    iconRes = R.drawable.ic_overlay_menu,
                    contentDescription = "Menu",
                    padding = buttonPadding,
                    highlighted = true
                ) {
                    launchLauncherMenu(context)
                },
                createOverlayButton(
                    context = context,
                    iconRes = R.drawable.ic_overlay_back,
                    contentDescription = "Back",
                    padding = buttonPadding
                ) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            ).forEach { button ->
                overlayButtons += button
                addView(button.container)
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 48
        }

        overlayView = container
        windowManager?.addView(container, params)
        updateOverlayStyle()
        updateOverlayVisibility()
    }

    private fun removeHomeOverlay() {
        overlayView?.let { view ->
            runCatching {
                windowManager?.removeView(view)
            }
        }
        overlayView = null
        overlayButtons.clear()
    }

    private fun updateOverlayVisibility() {
        overlayView?.visibility = if (
            navigationVisible &&
            !isDeviceLocked() &&
            !isSystemUiForeground() &&
            !isKeyboardVisible()
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isKeyguardLocked == true
    }

    private fun isSystemUiForeground(): Boolean {
        return currentPackageName == "com.android.systemui"
    }

    private fun isKeyboardVisible(): Boolean {
        return windows?.any { window ->
            window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
                window.isActive &&
                window.root != null
        } == true
    }

    private fun updateOverlayStyle() {
        val highlightedColor = resolveOverlayPrimaryColor()
        overlayButtons.forEach { button ->
            val background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(
                    if (!button.enabled) 0x99BDB8C7.toInt()
                    else if (button.highlighted) highlightedColor
                    else 0xE6E4E0EA.toInt()
                )
            }
            button.container.background = background
            button.icon.setColorFilter(
                ContextCompat.getColor(
                    this,
                    if (!button.enabled) android.R.color.darker_gray
                    else if (button.highlighted) android.R.color.white
                    else android.R.color.black
                )
            )
            button.container.isEnabled = button.enabled
            button.container.alpha = if (button.enabled) 1f else 0.6f
        }
    }

    private fun createOverlayButton(
        context: Context,
        iconRes: Int,
        contentDescription: String,
        padding: Int,
        highlighted: Boolean = false,
        enabled: Boolean = true,
        onClick: () -> Unit
    ): OverlayButtonView {
        val iconView = ImageView(context).apply {
            setImageResource(iconRes)
            setPadding(padding, padding, padding, padding)
            this.contentDescription = contentDescription
        }
        val container = FrameLayout(context).apply {
            val buttonBackground = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE6E4E0EA.toInt())
            }
            background = buttonBackground
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                clipToOutline = true
            }
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 6
                rightMargin = 6
            }
            this.layoutParams = layoutParams
            addView(
                iconView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            setOnClickListener {
                if (enabled) {
                    onClick()
                }
            }
        }
        return OverlayButtonView(
            container = container,
            icon = iconView,
            highlighted = highlighted,
            enabled = enabled
        )
    }

    private fun resolveOverlayPrimaryColor(): Int {
        val settings = runBlocking {
            SettingsManager(this@OverviewAccessibilityService)
                .settingsFlow
                .first()
        }
        val themeColor = if (settings.overrideColorTheme) {
            getBestThemeColor()
        } else {
            settings.themeColor
        }
        return when (themeColor) {
            ThemeColor.RED -> 0xFFE53935.toInt()
            ThemeColor.GREEN -> 0xFF43A047.toInt()
            ThemeColor.BLUE -> 0xFF1E88E5.toInt()
            ThemeColor.INDIGO -> 0xFF5E35B1.toInt()
            ThemeColor.ORANGE -> 0xFFFB8C00.toInt()
            ThemeColor.YELLOW -> 0xFFFDD835.toInt()
            ThemeColor.VIOLET -> 0xFF8E24AA.toInt()
            ThemeColor.GREY -> 0xFF9E9E9E.toInt()
            ThemeColor.MAROON -> 0xFF800000.toInt()
            ThemeColor.OLIVE -> 0xFF808000.toInt()
            ThemeColor.CYAN -> 0xFF00ACC1.toInt()
            ThemeColor.PINK -> 0xFFEC407A.toInt()
            ThemeColor.MAGENTA -> 0xFFD81B60.toInt()
            ThemeColor.TAN -> 0xFFD2B48C.toInt()
            ThemeColor.TEAL -> 0xFF00897B.toInt()
        }
    }

    companion object {
        private var instance: OverviewAccessibilityService? = null
        const val ACTION_OVERLAY_HOME_PRESSED =
            "com.calebteo.project.OVERLAY_HOME_PRESSED"
        const val ACTION_OVERLAY_MENU_PRESSED =
            "com.calebteo.project.OVERLAY_MENU_PRESSED"
        private const val ACTION_SET_NAVIGATION_VISIBILITY =
            "com.calebteo.project.SET_NAVIGATION_VISIBILITY"
        private const val EXTRA_NAVIGATION_VISIBLE = "extra_navigation_visible"

        fun openRecents(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        fun setNavigationVisible(context: Context, visible: Boolean) {
            context.sendBroadcast(
                Intent(ACTION_SET_NAVIGATION_VISIBILITY).apply {
                    setPackage(context.packageName)
                    putExtra(EXTRA_NAVIGATION_VISIBLE, visible)
                }
            )
        }

        fun refreshNavigationStyle() {
            instance?.updateOverlayStyle()
        }
    }
}

private data class OverlayButtonView(
    val container: FrameLayout,
    val icon: ImageView,
    val highlighted: Boolean,
    val enabled: Boolean
)

private fun applySynapseWallpaper(context: Context) {
    val wallpaperManager = WallpaperManager.getInstance(context)
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wallpaperManager.setResource(
                R.drawable.synapse,
                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            )
        } else {
            wallpaperManager.setResource(R.drawable.synapse)
        }
    }
}

private fun launchLauncherHome(
    context: Context,
    bringToFront: Boolean = true
) {
    context.sendBroadcast(
        Intent(OverviewAccessibilityService.ACTION_OVERLAY_HOME_PRESSED).apply {
            setPackage(context.packageName)
        }
    )
    if (!bringToFront) {
        return
    }
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
    }
    context.startActivity(intent)
}

private fun launchLauncherMenu(context: Context) {
    context.sendBroadcast(
        Intent(OverviewAccessibilityService.ACTION_OVERLAY_MENU_PRESSED).apply {
            setPackage(context.packageName)
        }
    )
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
    }
    context.startActivity(intent)
}

fun openSystemAppSwitcher(context: Context): Boolean {
    val opened = OverviewAccessibilityService.openRecents()
    if (!opened) {
        openOverviewAccessibilitySettings(context)
    }
    return opened
}
fun openOverviewAccessibilitySettings(context: Context) {
    val serviceComponent = ComponentName(
        context,
        OverviewAccessibilityService::class.java
    )

    val intents = listOf(
        Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )

    for (intent in intents) {
        val launched = runCatching {
            context.startActivity(intent)
            true
        }.getOrElse { false }

        if (launched) {
            return
        }
    }
}

fun isOverviewAccessibilityEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val expectedComponent = ComponentName(
        context,
        OverviewAccessibilityService::class.java
    ).flattenToString()
    return enabledServices
        .split(':')
        .any { it.equals(expectedComponent, ignoreCase = true) }
}
