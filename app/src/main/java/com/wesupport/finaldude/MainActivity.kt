// MainActivity.kt
package com.wesupport.finaldude

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.util.TimeZone
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get yesterday's stats
        getYesterdayWPSUsage()
    }


    private fun getYesterdayWPSUsage() {
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Move to yesterday

        println("Getting usage for date: ${formatDate(calendar.timeInMillis)}")

        val yesterdayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayEnd = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        println("\nTime window being analyzed:")
        println("Start: ${formatTime(yesterdayStart)}")
        println("End: ${formatTime(yesterdayEnd)}")

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,  // Changed to INTERVAL_BEST
            yesterdayStart,
            yesterdayEnd
        )

        println("\nFound ${stats.size} usage records")

        var totalYesterdayTime = 0L
        var sessionsFound = 0

        stats.forEach { stat ->
            if (stat.packageName == "cn.wps.moffice_eng") {
                sessionsFound++
                val sessionTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    stat.totalTimeVisible
                } else {
                    stat.totalTimeInForeground
                }

                println("\n=== WPS SESSION #$sessionsFound ===")
                println("Session span:")
                println("Start: ${formatTime(stat.firstTimeStamp)}")
                println("End: ${formatTime(stat.lastTimeStamp)}")
                println("Raw foreground time: ${formatDuration(stat.totalTimeInForeground)}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    println("Raw visible time: ${formatDuration(stat.totalTimeVisible)}")
                }
                println("Adding time: ${formatDuration(sessionTime)}")

                totalYesterdayTime += sessionTime
            }
        }

        if (sessionsFound == 0) {
            println("\nNo WPS Office usage found for yesterday")
        } else {
            println("\n=== SUMMARY ===")
            println("Total sessions found: $sessionsFound")
            println("Total calculated usage time: ${formatDuration(totalYesterdayTime)}")
            val totalMinutes = (totalYesterdayTime / (1000 * 60))
            println("Rounded to minutes: ${totalMinutes} minutes")
            println("=============")
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun formatTime(timeInMillis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timeInMillis))
    }

    private fun formatDate(timeInMillis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(timeInMillis))
    }

    private fun formatDuration(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }


}