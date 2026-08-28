package com.calebteo.project

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.calebteo.project.data.AppLanguage
import kotlinx.coroutines.flow.first

private val Context.reviewPromptDataStore by preferencesDataStore(name = "review_prompt")

private object ReviewPromptKeys {
    val FIRST_ELIGIBLE_AT = longPreferencesKey("first_eligible_at")
    val LAST_NOTIFIED_AT = longPreferencesKey("last_notified_at")
}

private const val ReviewPromptChannelId = "review_prompt_channel"
private const val ReviewPromptNotificationId = 5001
private const val FiveDaysMillis = 5L * 24L * 60L * 60L * 1000L

private enum class ReviewPromptCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    TITLE(
        "Liking Synapse UX?",
        "喜欢联想用户体验吗？",
        "Suka Synapse UX?",
        "Te gusta Synapse UX?",
        "Vous aimez Synapse UX ?"
    ),
    BODY(
        "Give us a five-star review on Google Play!",
        "请在 Google Play 给我们五星好评！",
        "Berikan kami ulasan lima bintang di Google Play!",
        "Danos una resena de cinco estrellas en Google Play!",
        "Donnez-nous une note de cinq etoiles sur Google Play !"
    ),
    CHANNEL_NAME(
        "Reviews",
        "评价",
        "Ulasan",
        "Resenas",
        "Avis"
    );

    fun text(language: AppLanguage): String =
        when (language) {
            AppLanguage.ENGLISH -> english
            AppLanguage.MANDARIN -> mandarin
            AppLanguage.MALAY -> malay
            AppLanguage.SPANISH -> spanish
            AppLanguage.FRENCH -> french
        }
}

suspend fun maybeSendReviewPromptNotification(
    context: Context,
    language: AppLanguage
) {
    val now = System.currentTimeMillis()
    val prefs = context.reviewPromptDataStore.data.first()
    val firstEligibleAt = prefs[ReviewPromptKeys.FIRST_ELIGIBLE_AT]
    val lastNotifiedAt = prefs[ReviewPromptKeys.LAST_NOTIFIED_AT]

    if (firstEligibleAt == null) {
        context.reviewPromptDataStore.edit {
            it[ReviewPromptKeys.FIRST_ELIGIBLE_AT] = now
        }
        return
    }

    val nextNotificationBase = lastNotifiedAt ?: firstEligibleAt
    if (now - nextNotificationBase < FiveDaysMillis) return

    if (sendReviewPromptNotification(context, language)) {
        context.reviewPromptDataStore.edit {
            it[ReviewPromptKeys.LAST_NOTIFIED_AT] = now
        }
    }
}

suspend fun sendDebugReviewPromptNotification(
    context: Context,
    language: AppLanguage
) {
    sendReviewPromptNotification(context, language)
}

private fun sendReviewPromptNotification(
    context: Context,
    language: AppLanguage
): Boolean {
    if (!canPostNotifications(context)) return false

    ensureReviewPromptChannel(context, language)

    val notification = NotificationCompat.Builder(context, ReviewPromptChannelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(ReviewPromptCopy.TITLE.text(language))
        .setContentText(ReviewPromptCopy.BODY.text(language))
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(ReviewPromptCopy.BODY.text(language))
        )
        .setContentIntent(buildPlayStorePendingIntent(context))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    NotificationManagerCompat.from(context)
        .notify(ReviewPromptNotificationId, notification)

    return true
}

private fun ensureReviewPromptChannel(context: Context, language: AppLanguage) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        ReviewPromptChannelId,
        ReviewPromptCopy.CHANNEL_NAME.text(language),
        NotificationManager.IMPORTANCE_DEFAULT
    )
    manager.createNotificationChannel(channel)
}

private fun buildPlayStorePendingIntent(context: Context) =
    androidx.core.app.PendingIntentCompat.getActivity(
        context,
        0,
        buildPlayStoreIntent(context),
        android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        false
    )

private fun buildPlayStoreIntent(context: Context): Intent {
    val packageName = context.packageName
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    ).apply {
        `package` = "com.android.vending"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    return try {
        context.packageManager.resolveActivity(marketIntent, 0)
        marketIntent
    } catch (_: ActivityNotFoundException) {
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}

private fun canPostNotifications(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
