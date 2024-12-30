// MainActivity.kt
package com.wesupport.finaldude

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var dateText: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var usageList: RecyclerView
    private lateinit var adapter: AppUsageAdapter
    private var currentDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasUsageStatsPermission()) {
            Log.d("AppUsage", "No usage stats permission - requesting now")
            requestUsageStatsPermission()
            return
        }

        setupViews()
        updateUsageStats()
    }

    private fun setupViews() {
        dateText = findViewById(R.id.dateText)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        usageList = findViewById(R.id.usageList)

        adapter = AppUsageAdapter()
        usageList.layoutManager = LinearLayoutManager(this)
        usageList.adapter = adapter

        prevButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_YEAR, -1)
            updateUsageStats()
        }

        nextButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_YEAR, 1)
            updateUsageStats()
        }
    }

    private fun updateUsageStats() {
        val startCal = currentDate.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        val startTime = startCal.timeInMillis

        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_YEAR, 1)
        val now = System.currentTimeMillis()
        val endTime = if (endCal.timeInMillis > now) now else endCal.timeInMillis

        dateText.text = formatDate(startTime)

        try {
            // Log all installed packages first
            val installedApps = packageManager.getInstalledApplications(0)
            Log.d("AppUsage", "Total installed apps: ${installedApps.size}")
            installedApps.forEach { appInfo ->
                Log.d("AppUsage", "Installed app: ${appInfo.packageName}")
            }

            val tracker = AppUsageTracker(this)
            val usageStats = tracker.getForegroundUsageStats(startTime, endTime)

            // Log all apps from usage stats
            Log.d("AppUsage", "Apps with usage data: ${usageStats.size}")
            usageStats.forEach { (packageName, duration) ->
                Log.d("AppUsage", "Usage - Package: $packageName, Duration: $duration")
            }

            val usageList = usageStats
                // Removed the filter to show all apps
                .map { (packageName, duration) ->
                    val appName = try {
                        packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(packageName, 0)
                        ).toString()
                    } catch (e: Exception) {
                        packageName
                    }
                    UsageData(appName, packageName, duration)
                }
                .sortedByDescending { it.duration }

            // Log final displayed apps
            Log.d("AppUsage", "Final displayed apps: ${usageList.size}")
            usageList.forEach { usage ->
                Log.d("AppUsage", "Displayed - App: ${usage.appName}, Package: ${usage.packageName}, Duration: ${usage.duration}")
            }

            adapter.setData(usageList)

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