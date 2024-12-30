package com.wesupport.finaldude

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit
import java.util.Calendar
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    private lateinit var dateText: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private var currentDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasUsageStatsPermission()) {
            Log.d("AppUsage", "No usage stats permission - requesting now")
            requestUsageStatsPermission()
            return
        }

        dateText = findViewById(R.id.dateText)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)

        // Set up click listeners
        prevButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_YEAR, -1)
            updateUsageStats()
        }

        nextButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_YEAR, 1)
            updateUsageStats()
        }

        // Initial load
        updateUsageStats()
    }

    private fun updateUsageStats() {
        // Set start time to midnight of current selected date
        val startCal = currentDate.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        val startTime = startCal.timeInMillis

        // Set end time to midnight of next day or current time if today
        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_YEAR, 1)
        val now = System.currentTimeMillis()
        val endTime = if (endCal.timeInMillis > now) now else endCal.timeInMillis

        // Update date display
        dateText.text = formatDate(startTime)

        try {
            val tracker = AppUsageTracker(this)
            val usageStats = tracker.getForegroundUsageStats(startTime, endTime)

            if (usageStats.isEmpty()) {
                Log.d("AppUsage", "No usage data found for ${formatDate(startTime)}")
            } else {
                Log.d("AppUsage", "=== App Usage for ${formatDate(startTime)} ===")
                usageStats.toList()
                    .sortedByDescending { it.second }
                    .forEach { (packageName, duration) ->
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                        if (minutes > 0) {
                            val appName = try {
                                packageManager.getApplicationLabel(
                                    packageManager.getApplicationInfo(packageName, 0)
                                ).toString()
                            } catch (e: Exception) {
                                packageName
                            }
                            val hours = TimeUnit.MILLISECONDS.toHours(duration)
                            val mins = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
                            val secs = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
                            val timeStr = String.format("%02d:%02d:%02d", hours, mins, secs)
                            Log.d("AppUsage", "$appName: $timeStr")
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("AppUsage", "Error getting usage stats", e)
            e.printStackTrace()
        }
    }

    private fun formatDate(timeMillis: Long): String {
        return java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
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