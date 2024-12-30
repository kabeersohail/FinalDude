package com.wesupport.finaldude

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.util.concurrent.TimeUnit

class AppUsageTracker(private val context: Context) {
    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    fun getForegroundUsageStats(startTime: Long, endTime: Long): Map<String, Long> {
        val usageMap = mutableMapOf<String, Long>()

        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            val lastResumeTime = mutableMapOf<String, Long>()

            var lastTimeStamp = 0L

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                lastTimeStamp = event.timeStamp

                // Only process events within our time window
                if (event.timeStamp < startTime) {
                    continue
                }

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        lastResumeTime[event.packageName] = event.timeStamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        val resumeTime = lastResumeTime[event.packageName]
                        if (resumeTime != null && resumeTime >= startTime) {
                            val duration = event.timeStamp - resumeTime
                            if (duration > 0) {
                                usageMap[event.packageName] = (usageMap[event.packageName] ?: 0) + duration
                            }
                        }
                        lastResumeTime.remove(event.packageName)
                    }
                }
            }

            // For apps still in foreground
            val currentTime = System.currentTimeMillis().coerceAtMost(endTime)
            lastResumeTime.forEach { (packageName, resumeTime) ->
                if (resumeTime >= startTime) {
                    val duration = currentTime - resumeTime
                    if (duration > 0) {
                        usageMap[packageName] = (usageMap[packageName] ?: 0) + duration
                    }
                }
            }

            // Log the actual time range we processed
            Log.d("AppUsageTracker", "Processed events from ${formatTime(startTime)} to ${formatTime(lastTimeStamp)}")

        } catch (e: Exception) {
            Log.e("AppUsageTracker", "Error getting usage stats", e)
        }

        return usageMap
    }

    private fun formatTime(timeMillis: Long): String {
        return java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timeMillis))
    }
}