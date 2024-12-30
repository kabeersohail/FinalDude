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
            // Store all events in chronological order
            val eventsByApp = TreeMap<String, MutableList<Event>>()
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val timestamp = event.timeStamp
                val packageName = event.packageName

                if (timestamp in startTime..endTime) {
                    val events = eventsByApp.getOrPut(packageName) { mutableListOf() }
                    events.add(Event(timestamp, event.eventType == UsageEvents.Event.ACTIVITY_RESUMED))
                }
            }

            // Process events chronologically for each app
            eventsByApp.forEach { (packageName, events) ->
                events.sortBy { it.timestamp }
                var lastResumeTime: Long? = null
                var totalTime = 0L

                for (event in events) {
                    if (event.isResume) {
                        if (lastResumeTime == null) {
                            lastResumeTime = event.timestamp
                        }
                    } else { // PAUSE event
                        if (lastResumeTime != null) {
                            totalTime += event.timestamp - lastResumeTime
                            lastResumeTime = null
                        }
                    }
                }

                // Handle still running apps
                lastResumeTime?.let { resumeTime ->
                    totalTime += endTime - resumeTime
                }

                if (totalTime > 0) {
                    usageMap[packageName] = totalTime
                }
            }

            // Log processing details
            Log.d("AppUsageTracker", "Total apps tracked: ${eventsByApp.size}")
            eventsByApp.forEach { (pkg, events) ->
                Log.d("AppUsageTracker", "$pkg: ${events.size} events")
            }

        } catch (e: Exception) {
            Log.e("AppUsageTracker", "Error getting usage stats", e)
        }

        return usageMap
    }

    private data class Event(val timestamp: Long, val isResume: Boolean)
}