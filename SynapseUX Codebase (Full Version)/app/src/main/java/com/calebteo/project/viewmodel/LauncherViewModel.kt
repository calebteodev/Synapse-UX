package com.calebteo.project.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calebteo.project.data.*
import com.calebteo.project.ui.theme.ThemeColor
import com.example.launcher.data.AppSortingHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val settingsManager = SettingsManager(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _usageCountMap =
        MutableStateFlow<Map<String, Int>>(emptyMap())

    val usageCountMap: StateFlow<Map<String, Int>> =
        _usageCountMap.asStateFlow()

    /**
     * Settings Flow (clean architecture)
     */
    val settings: StateFlow<LauncherSettings> =
        settingsManager.settingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = LauncherSettings()
            )

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {

            _isLoading.value = true

            try {
                val apps = appRepository.getInstalledApps()
                _installedApps.value = apps
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredApps: StateFlow<List<AppInfo>> =
        combine(
            installedApps,
            searchQuery,
            usageCountMap,
            settings
        ) { apps, query, usageMap, launcherSettings ->
            val sortedApps = AppSortingHelper.filterAndSortByUsage(
                apps = apps,
                searchQuery = query,
                usageCountMap = usageMap
            )
            val appsByPackage = sortedApps.associateBy { it.packageName }
            val pinnedApps = launcherSettings.pinnedAppPackages.mapNotNull { packageName ->
                appsByPackage[packageName]
            }
            val remainingApps = sortedApps.filterNot { app ->
                app.packageName in launcherSettings.selectedAppPackages
            }

            pinnedApps + remainingApps
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateUsageStats(usageMap: Map<String, Int>) {
        _usageCountMap.value = usageMap
    }

    /**
     * Launcher app actions
     */

    fun launchApp(packageName: String) {
        appRepository.launchApp(packageName)
    }

    fun openAppInfo(packageName: String) {
        appRepository.openAppInfo(packageName)
    }

    fun uninstallApp(packageName: String) {
        appRepository.uninstallApp(packageName)
    }

    /**
     * SETTINGS UPDATES
     */

    suspend fun updateAppGridSize(size: Int) {
        settingsManager.updateAppGridSize(size)
    }
    suspend fun updateAppRowSize(size: Int) {
        settingsManager.updateAppRowSize(size)
    }


    suspend fun updateIconSize(size: Int) {
        settingsManager.updateIconSize(size)
    }

    suspend fun updateWallpaperSource(source: WallpaperSource) {
        settingsManager.updateWallpaperSource(source)
    }

    suspend fun updateCustomWallpaperUri(uri: String?) {
        settingsManager.updateCustomWallpaperUri(uri)
    }

    suspend fun updateWallpaperTheme(theme: WallpaperCategory) {
        settingsManager.updateWallpaperTheme(theme)
    }

    suspend fun updateSystemLayout(layout: SystemLayout) {
        settingsManager.updateSystemLayout(layout)
    }
    suspend fun updateSelectedAppPackages(packages: Set<String>)
    { settingsManager.updateSelectedAppPackages(packages) }
    suspend fun updatePinnedApps(packages: List<String>) {
        settingsManager.updatePinnedApps(packages)
    }
    suspend fun updateThemeColor(color: ThemeColor) {

        settingsManager.updateThemeColor(color)
    }
    suspend fun updateAppFont(font: AppFont) {
        settingsManager.updateAppFont(font)
    }
    suspend fun updateSynapseNavigationEnabled(enabled: Boolean) {
        settingsManager.updateSynapseNavigationEnabled(enabled)
    }
    fun updateOverrideTopBar(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateOverrideTopBar(enabled)
        }
    }
    fun updateOverrideGridView(enabled2: Boolean) {
        viewModelScope.launch {
            settingsManager.updateOverrideGridView(enabled2)
        }
    }
    fun updateOverrideSystemLayout(enabled3: Boolean) {
        viewModelScope.launch {
            settingsManager.updateOverrideSystemLayout(enabled3)
        }
    }
    fun updateOverrideColorTheme(enabled4: Boolean) {
        viewModelScope.launch {
            settingsManager.updateOverrideColorTheme(enabled4)
        }
    }
    fun updateOverrideWallpaperTheme(enabled5: Boolean) {
        viewModelScope.launch {
            settingsManager.updateOverrideWallpaperTheme(enabled5)
        }
    }
    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsManager.updateLanguage(language)
        }
    }
    fun updateUsername(username: String) {
        viewModelScope.launch {
            settingsManager.updateUsername(username)
        }
    }
}
