package com.wesupport.finaldude

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.util.concurrent.TimeUnit
import java.util.TreeMap

class AppUsageTracker(private val context: Context) {
    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    fun getForegroundUsageStats(startTime: Long, endTime: Long): Map<String, Long> {
        val usageMap = mutableMapOf<String, Long>()

        try {
            val eventsByApp = TreeMap<String, MutableList<Event>>()
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val timestamp = event.timeStamp
                val packageName = event.packageName

                if (timestamp in startTime..endTime) {
                    val events = eventsByApp.getOrPut(packageName) { mutableListOf() }
                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED,
                        UsageEvents.Event.ACTIVITY_PAUSED -> {
                            events.add(Event(timestamp, event.eventType == UsageEvents.Event.ACTIVITY_RESUMED))
                        }
                    }
                }
            }

            eventsByApp.forEach { (packageName, events) ->
                events.sortBy { it.timestamp }
                var totalTime = 0L
                var lastResumeTime: Long? = null

                for (event in events) {
                    if (event.isResume) {
                        if (lastResumeTime == null) {
                            lastResumeTime = event.timestamp
                        }
                    } else {
                        lastResumeTime?.let { resumeTime ->
                            val sessionTime = event.timestamp - resumeTime
                            if (sessionTime > 0) {
                                totalTime += sessionTime
                            }
                            lastResumeTime = null
                        }
                    }
                }

                // Handle still active sessions
                lastResumeTime?.let { resumeTime ->
                    val sessionTime = endTime - resumeTime
                    if (sessionTime > 0) {
                        totalTime += sessionTime
                    }
                }

                if (totalTime > 0) {
                    // Convert to minutes and round
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime)
                    if (minutes > 0) {
                        usageMap[packageName] = TimeUnit.MINUTES.toMillis(minutes)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppUsageTracker", "Error getting usage stats", e)
        }

        return usageMap
    }

    private data class Event(val timestamp: Long, val isResume: Boolean)
}