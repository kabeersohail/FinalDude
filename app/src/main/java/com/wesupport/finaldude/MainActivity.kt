package com.wesupport.finaldude

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val screenTimeGroups = mutableListOf<AppScreenTimeGroup>()
    private val adapter = ScreenTimeAdapter(screenTimeGroups)
    private val calendar = Calendar.getInstance().apply {
        timeZone = TimeZone.getDefault()
    }

    private lateinit var tvSelectedDate: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        setupListeners()

        if (!hasUsageStatsPermission(this)) {
            requestUsageStatsPermission(this)
        } else {
            updateScreenTime()
        }
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnPrevDay).setOnClickListener {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            logDateChange("Previous")
            updateScreenTime()
        }

        findViewById<ImageButton>(R.id.btnNextDay).setOnClickListener {
            if (isNavigatingToFuture()) {
                showFutureNavigationError()
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                logDateChange("Next")
                updateScreenTime()
            }
        }
    }

    private fun isNavigatingToFuture(): Boolean {
        val today = Calendar.getInstance().apply {
            timeZone = TimeZone.getDefault()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis >= today.timeInMillis
    }

    private fun showFutureNavigationError() {
        Snackbar.make(
            findViewById(R.id.rootLayout),
            "Can't show future app usage",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun logDateChange(direction: String) {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        println("Selected Date ($direction): ${dateFormatter.format(calendar.time)}")
    }

    private fun updateScreenTime() {
        updateDateDisplay()
        val timeRange = calculateTimeRange()
        logTimeRange(timeRange)
        updateUsageStats(timeRange)
    }

    private fun updateDateDisplay() {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tvSelectedDate.text = dateFormatter.format(calendar.time)
    }

    private data class TimeRangeInternal(val startTime: Long, val endTime: Long)

    private fun calculateTimeRange(): TimeRangeInternal {
        val startTime = calendar.clone() as Calendar
        startTime.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endTime = if (isToday()) {
            System.currentTimeMillis()
        } else {
            (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        return TimeRangeInternal(startTime.timeInMillis, endTime)
    }

    private fun isToday(): Boolean {
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun logTimeRange(timeRange: TimeRangeInternal) {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        println("Requested Time Range:")
        println("Start: ${formatter.format(Date(timeRange.startTime))}")
        println("End: ${formatter.format(Date(timeRange.endTime))}")
    }

    private fun updateUsageStats(timeRange: TimeRangeInternal) {
        val usageStats = getUsageStats(this, timeRange.startTime, timeRange.endTime)
        screenTimeGroups.clear()
        screenTimeGroups.addAll(formatUsageStats(usageStats))
        adapter.notifyDataSetChanged()
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission(context: Context) {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun getUsageStats(context: Context, startTime: Long, endTime: Long): List<UsageStats> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime)
            .filter { stats ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    stats.totalTimeVisible > 0 || stats.totalTimeInForeground > 0
                } else {
                    stats.totalTimeInForeground > 0
                }
            }
    }

    private fun formatUsageStats(usageStats: List<UsageStats>): List<AppScreenTimeGroup> {
        // First, get distinct time ranges
        val timeRanges = usageStats
            .map { stats ->
                TimeRange(stats.firstTimeStamp, stats.lastTimeStamp)
            }
            .distinct()

        // Group apps by time ranges
        return timeRanges.map { timeRange ->
            val appsInRange = usageStats
                .filter { stats ->
                    TimeRange(stats.firstTimeStamp, stats.lastTimeStamp) == timeRange
                }
                .distinctBy { it.packageName }
                .mapNotNull { stats -> createAppScreenTime(stats, timeRange) }
                .sortedByDescending { it.totalTimeInMillis }

            AppScreenTimeGroup(timeRange, appsInRange)
        }
    }

    private fun createAppScreenTime(stats: UsageStats, timeRange: TimeRange): AppScreenTime? {
        return try {
            val appInfo = packageManager.getApplicationInfo(stats.packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()

            val totalTimeInMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                stats.totalTimeVisible
            } else {
                stats.totalTimeInForeground
            }

            AppScreenTime(
                appName = appName,
                screenTime = formatDuration(totalTimeInMillis),
                timeRange = timeRange,
                totalTimeInMillis = totalTimeInMillis
            )
        } catch (e: PackageManager.NameNotFoundException) {
            println("Package not found: ${stats.packageName}")
            null
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }
}