package com.wesupport.finaldude

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit
import java.util.Calendar
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!hasUsageStatsPermission()) {
            Log.d("AppUsage", "No usage stats permission - requesting now")
            requestUsageStatsPermission()
            return
        }

        val button: Button = findViewById(R.id.click_me)

        button.setOnClickListener {
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

                // Combine Settings and Digital Wellbeing usage
                val settingsTime = (usageStats["com.android.settings"] ?: 0L) +
                        (usageStats["com.google.android.apps.wellbeing"] ?: 0L)

                val combinedStats = usageStats.toMutableMap().apply {
                    remove("com.android.settings")
                    remove("com.google.android.apps.wellbeing")
                    if (settingsTime > 0) {
                        put("com.android.settings", settingsTime)
                    }
                }

                if (combinedStats.isEmpty()) {
                    Log.d("AppUsage", "No usage data found for today")
                } else {
                    Log.d("AppUsage", "=== Today's App Usage (${formatDate(startTime)}) ===")
                    combinedStats.toList()
                        .sortedByDescending { it.second }
                        .forEach { (packageName, duration) ->
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                            if (minutes > 0) {
                                val appName = when (packageName) {
                                    "com.android.settings" -> "Settings"
                                    "com.wesupport.finaldude" -> "FinalDude"
                                    else -> try {
                                        packageManager.getApplicationLabel(
                                            packageManager.getApplicationInfo(packageName, 0)
                                        ).toString()
                                    } catch (e: Exception) {
                                        packageName
                                    }
                                }
                                Log.d("AppUsage", "$appName: $minutes min")

                                // Debug raw time
                                Log.d("AppUsageTracker", "Raw time for $appName: $minutes minutes")
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e("AppUsage", "Error getting usage stats", e)
                e.printStackTrace()
            }
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