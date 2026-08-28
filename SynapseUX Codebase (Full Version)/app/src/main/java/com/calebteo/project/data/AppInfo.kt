package com.calebteo.project.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false
)
