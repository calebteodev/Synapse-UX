package com.example.launcher.data

import android.app.usage.UsageStatsManager
import android.content.Context
import com.calebteo.project.data.AppInfo
import java.util.Calendar


object AppSortingHelper {
    fun filterAndSortByUsage(
        apps: List<AppInfo>,
        searchQuery: String,
        usageCountMap: Map<String, Int>
    ): List<AppInfo> {
        val query = searchQuery.trim().lowercase()

        val filteredApps = if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.label.lowercase().contains(query)
            }
        }

        return filteredApps.sortedWith(
            compareByDescending<AppInfo> {
                usageCountMap[it.packageName] ?: 0
            }.thenBy {
                it.label.lowercase()
            }
        )
    }


}
object TopBarAppSelector {

    private const val SIGNIFICANT_RATIO = 2.0

    fun determineTopBarCount(
        apps: List<AppInfo>,
        usageCountMap: Map<String, Int>
    ): Int {

        val usages = apps
            .sortedByDescending { usageCountMap[it.packageName] ?: 0 }
            .take(5)
            .map { (usageCountMap[it.packageName] ?: 0).coerceAtLeast(1) }
        // avoid division by zero

        if (usages.size < 2) return 3

        for (i in 0 until minOf(usages.size - 1, 4)) {

            val current = usages[i].toDouble()
            val next = usages[i + 1].toDouble()

            val ratio = current / next

            if (ratio >= SIGNIFICANT_RATIO) {
                return when (i) {
                    0 -> 3
                    1 -> 2
                    2 -> 3
                    3 -> 4
                    else -> 3
                }
            }
        }

        return 3
    }
}
object NextBarAppSelector {

    private const val TARGET_PERCENT = 0.05

    fun determineNextBarCount(
        apps: List<AppInfo>
    ): Int {

        val totalApps = apps.size

        if (totalApps == 0) return 3

        val calculated = (totalApps * TARGET_PERCENT).toInt()

        return calculated
            .coerceIn(3, 6)
    }
}

class UsageStatsRepository(private val context: Context) {

    fun getUsageCountMap(): Map<String, Int> {
        val usm =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val start = 0L
        val end = System.currentTimeMillis()

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_YEARLY,
            start,
            end
        )

        return stats.associate {
            it.packageName to (it.totalTimeInForeground).toInt()
        }
    }

    fun getLastWeekUsagePresenceMap(): Map<String, Int> {
        val usm =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - (7L * 24 * 60 * 60 * 1000)

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        )

        return stats
            .filter { it.totalTimeInForeground > 0L }
            .associate { it.packageName to 1 }
    }
}