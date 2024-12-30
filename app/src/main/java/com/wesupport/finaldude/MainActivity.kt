package com.wesupport.finaldude

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit
import java.util.Calendar
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasUsageStatsPermission()) {
            Log.d("AppUsage", "No usage stats permission - requesting now")
            requestUsageStatsPermission()
            return
        }

        // Get today's start time (midnight) in device's timezone
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        try {
            val tracker = AppUsageTracker(this)
            val usageStats = tracker.getForegroundUsageStats(startTime, endTime)

            // Debug log for raw times
            usageStats.forEach { (pkg, time) ->
                Log.d("AppUsageTracker", "Raw time for $pkg: ${TimeUnit.MILLISECONDS.toMinutes(time)} minutes")
            }

            if (usageStats.isEmpty()) {
                Log.d("AppUsage", "No usage data found for today")
            } else {
                Log.d("AppUsage", "=== Today's App Usage (${formatDate(startTime)}) ===")
                usageStats.toList()
                    .sortedByDescending { it.second }
                    .forEach { (packageName, duration) ->
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                        if (minutes > 0) {
                            // Get app name if possible
                            val appName = try {
                                val pm = packageManager
                                val appInfo = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0))
                                appInfo.toString()
                            } catch (e: Exception) {
                                packageName
                            }
                            Log.d("AppUsage", "$appName: $minutes min")
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("AppUsage", "Error getting usage stats", e)
            e.printStackTrace()
        }
    }

    private fun formatTime(timeMillis: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timeMillis))
    }

    private fun formatDate(timeMillis: Long): String {
        return java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            .format(java.util.Date(timeMillis))
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}