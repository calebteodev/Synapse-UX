package com.calebteo.project

import android.Manifest
import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.calebteo.project.data.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.reminderDataStore by preferencesDataStore(name = "menu_reminders")

private object ReminderKeys {
    val ITEMS = stringPreferencesKey("items")
}

private const val ReminderChannelId = "menu_reminder_channel"
private const val ReminderAlarmAction = "com.calebteo.project.REMINDER_ALARM"
private const val ReminderExtraId = "reminder_id"
private const val ReminderExtraTitle = "reminder_title"

private val reminderJson = Json { ignoreUnknownKeys = true }

@Serializable
data class ReminderItem(
    val id: String,
    val title: String,
    val triggerAtMillis: Long
)

private enum class ReminderCopy(
    val english: String,
    val mandarin: String,
    val malay: String,
    val spanish: String,
    val french: String
) {
    TITLE("Reminder", "提醒", "Peringatan", "Recordatorio", "Rappel"),
    CHANNEL("Reminders", "提醒", "Peringatan", "Recordatorios", "Rappels");

    fun text(language: AppLanguage): String =
        when (language) {
            AppLanguage.ENGLISH -> english
            AppLanguage.MANDARIN -> mandarin
            AppLanguage.MALAY -> malay
            AppLanguage.SPANISH -> spanish
            AppLanguage.FRENCH -> french
        }
}

private fun reminderNotificationText(
    language: AppLanguage,
    triggerAtMillis: Long,
    title: String
): String {
    val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(triggerAtMillis))
    return when (language) {
        AppLanguage.ENGLISH -> "Reminder at $formattedTime: $title"
        AppLanguage.MANDARIN -> "$formattedTime 的提醒：$title"
        AppLanguage.MALAY -> "Peringatan pada $formattedTime: $title"
        AppLanguage.SPANISH -> "Recordatorio a las $formattedTime: $title"
        AppLanguage.FRENCH -> "Rappel a $formattedTime : $title"
    }
}

object ReminderStore {
    fun remindersFlow(context: Context): Flow<List<ReminderItem>> {
        return context.reminderDataStore.data.map { prefs ->
            decodeReminders(prefs[ReminderKeys.ITEMS])
                .sortedBy { it.triggerAtMillis }
        }
    }

    suspend fun addReminder(
        context: Context,
        reminder: ReminderItem
    ) {
        val items = remindersFlow(context).first().filterNot { it.id == reminder.id } + reminder
        saveReminders(context, items)
        scheduleReminder(context, reminder)
    }

    suspend fun removeReminder(
        context: Context,
        reminderId: String
    ) {
        cancelReminder(context, reminderId)
        val items = remindersFlow(context).first().filterNot { it.id == reminderId }
        saveReminders(context, items)
    }

    suspend fun consumeReminder(
        context: Context,
        reminderId: String
    ): ReminderItem? {
        val items = remindersFlow(context).first()
        val reminder = items.firstOrNull { it.id == reminderId } ?: return null
        saveReminders(context, items.filterNot { it.id == reminderId })
        return reminder
    }

    suspend fun consumeDueReminders(
        context: Context,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<ReminderItem> {
        val items = remindersFlow(context).first()
        val dueReminders = items.filter { it.triggerAtMillis <= currentTimeMillis }
        if (dueReminders.isEmpty()) return emptyList()
        saveReminders(context, items.filterNot { reminder ->
            dueReminders.any { it.id == reminder.id }
        })
        return dueReminders
    }

    private suspend fun saveReminders(
        context: Context,
        reminders: List<ReminderItem>
    ) {
        context.reminderDataStore.edit { prefs ->
            prefs[ReminderKeys.ITEMS] = reminderJson.encodeToString(reminders)
        }
    }
}

class ReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderAlarmAction) return
        val reminderId = intent.getStringExtra(ReminderExtraId) ?: return
        val fallbackTitle = intent.getStringExtra(ReminderExtraTitle).orEmpty()

        val reminder = runBlocking {
            ReminderStore.consumeReminder(context, reminderId)
        }

        val title = reminder?.title?.ifBlank { fallbackTitle } ?: fallbackTitle
        if (title.isBlank()) return

        val language = runBlocking {
            com.calebteo.project.data.SettingsManager(context).settingsFlow.first().language
        }
        postReminderNotification(
            context = context,
            reminderId = reminderId,
            title = title,
            triggerAtMillis = reminder?.triggerAtMillis ?: System.currentTimeMillis(),
            language = language
        )
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        runBlocking {
            ReminderStore.remindersFlow(context).first().forEach { reminder ->
                if (reminder.triggerAtMillis <= System.currentTimeMillis()) {
                    postReminderNotification(
                        context = context,
                        reminderId = reminder.id,
                        title = reminder.title,
                        triggerAtMillis = reminder.triggerAtMillis,
                        language = com.calebteo.project.data.SettingsManager(context).settingsFlow.first().language
                    )
                    ReminderStore.removeReminder(context, reminder.id)
                } else {
                    scheduleReminder(context, reminder)
                }
            }
        }
    }
}

suspend fun deliverDueReminders(
    context: Context,
    currentTimeMillis: Long = System.currentTimeMillis()
) {
    val dueReminders = ReminderStore.consumeDueReminders(context, currentTimeMillis)
    if (dueReminders.isEmpty()) return
    val language = com.calebteo.project.data.SettingsManager(context).settingsFlow.first().language
    dueReminders.forEach { reminder ->
        postReminderNotification(
            context = context,
            reminderId = reminder.id,
            title = reminder.title,
            triggerAtMillis = reminder.triggerAtMillis,
            language = language
        )
    }
}

fun scheduleReminder(
    context: Context,
    reminder: ReminderItem
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent = reminderPendingIntent(context, reminder.id, reminder.title)
    val triggerAtMillis = reminder.triggerAtMillis

    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            val showIntent = reminderOpenAppPendingIntent(context, reminder.id)
            alarmManager.setAlarmClock(
                AlarmClockInfo(triggerAtMillis, showIntent),
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }.getOrElse {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}

fun cancelReminder(
    context: Context,
    reminderId: String
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent = reminderPendingIntent(context, reminderId, "")
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
}

private fun reminderPendingIntent(
    context: Context,
    reminderId: String,
    title: String
): PendingIntent {
    val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
        action = ReminderAlarmAction
        setPackage(context.packageName)
        putExtra(ReminderExtraId, reminderId)
        putExtra(ReminderExtraTitle, title)
    }
    return PendingIntent.getBroadcast(
        context,
        reminderId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun reminderOpenAppPendingIntent(
    context: Context,
    reminderId: String
): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
    }
    return PendingIntent.getActivity(
        context,
        reminderId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun ensureReminderChannel(context: Context, language: AppLanguage) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val channel = NotificationChannel(
        ReminderChannelId,
        ReminderCopy.CHANNEL.text(language),
        NotificationManager.IMPORTANCE_HIGH
    )
    manager.createNotificationChannel(channel)
}

private fun decodeReminders(raw: String?): List<ReminderItem> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        reminderJson.decodeFromString<List<ReminderItem>>(raw)
    }.getOrDefault(emptyList())
}

private fun canPostNotifications(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun postReminderNotification(
    context: Context,
    reminderId: String,
    title: String,
    triggerAtMillis: Long,
    language: AppLanguage
) {
    if (!canPostNotifications(context)) return
    ensureReminderChannel(context, language)
    val bodyText = reminderNotificationText(language, triggerAtMillis, title)

    val notification = NotificationCompat.Builder(context, ReminderChannelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(ReminderCopy.TITLE.text(language))
        .setContentText(bodyText)
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(bodyText)
        )
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
}
