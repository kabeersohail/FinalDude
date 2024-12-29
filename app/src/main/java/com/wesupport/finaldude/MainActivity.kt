package com.wesupport.finaldude

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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

    private val screenTimeData = mutableListOf<AppScreenTime>()
    private val adapter = ScreenTimeAdapter(screenTimeData)
    private val calendar = Calendar.getInstance() // Calendar object for managing the selected date

    private lateinit var tvSelectedDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val btnPrevDay: ImageButton = findViewById(R.id.btnPrevDay)
        val btnNextDay: ImageButton = findViewById(R.id.btnNextDay)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Check for permissions
        if (!hasUsageStatsPermission(this)) {
            requestUsageStatsPermission(this)
        } else {
            updateScreenTime()
        }

        btnPrevDay.setOnClickListener {
            calendar.add(Calendar.DAY_OF_YEAR, -1) // Move to the previous day
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            println("Selected Date (Previous): ${dateFormatter.format(calendar.time)}") // Debug log
            updateScreenTime()
        }

        btnNextDay.setOnClickListener {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis >= today.timeInMillis) {
                // Prevent navigating to future days, including today
                Snackbar.make(
                    findViewById(R.id.rootLayout), // Replace 'rootLayout' with the ID of your root view
                    "Can't show future app usage",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // Move to the next day
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                println("Selected Date (Next): ${dateFormatter.format(calendar.time)}") // Debug log
                updateScreenTime()
            }
        }


    }

    private fun updateScreenTime() {
        // Format and display the selected date
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = dateFormatter.format(calendar.time)
        findViewById<TextView>(R.id.tvSelectedDate).text = formattedDate

        // Define start and end time for the selected day
        val startTime = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endTime = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        println("Start Time: $startTime, End Time: $endTime") // Debugging log

        // Fetch usage stats for the defined time range
        val usageStats = getUsageStats(this, startTime, endTime)

        // Update the RecyclerView with formatted data
        screenTimeData.clear()
        screenTimeData.addAll(formatUsageStats(usageStats))
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
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }

    private fun getUsageStats(context: Context, startTime: Long, endTime: Long): List<UsageStats> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            .filter { it.totalTimeInForeground > 0 } // Filter apps with non-zero usage time
    }

    private fun formatUsageStats(usageStats: List<UsageStats>): List<AppScreenTime> {
        val packageManager = packageManager

        // Log the requested time range for debugging purposes
        val requestedStartTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(calendar.timeInMillis))
        val requestedEndTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(calendar.timeInMillis + (1000 * 60 * 60 * 24))) // +24 hours
        println(" %&^%&^% Requested Time Range: startTimestamp: $requestedStartTime, endTimestamp: $requestedEndTime")

        // Log the actual usage stats' time range (after query) and remove duplicates based on start and end timestamps
        usageStats.map {
            val firstFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.firstTimeStamp))
            val lastFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.lastTimeStamp))

            // Calculate the duration in milliseconds
            val durationMillis = it.lastTimeStamp - it.firstTimeStamp

            // Convert duration to hours, minutes, and seconds
            val hours = durationMillis / (1000 * 60 * 60)
            val minutes = (durationMillis / (1000 * 60)) % 60
            val seconds = (durationMillis / 1000) % 60
            val durationFormatted = String.format("%02dh %02dm %02ds", hours, minutes, seconds)

            // Return formatted bucket details with start, end, and duration
            "$firstFormatted - $lastFormatted | Duration: $durationFormatted"
        }
            .distinct() // Remove duplicates based on the entire formatted string
            .forEach {
                println(" %&^%&^% $it")
            }

        // Return formatted AppScreenTime objects (without duplicates)
        return usageStats
            .sortedByDescending { it.totalTimeInForeground }
            .map { stats ->
                val appName = try {
                    val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(stats.packageName, 0)
                    packageManager.getApplicationLabel(applicationInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    stats.packageName // Fallback to package name if app name is not found
                }

                val totalTimeInMillis = stats.totalTimeInForeground
                val hours = totalTimeInMillis / (1000 * 60 * 60)
                val minutes = (totalTimeInMillis / (1000 * 60)) % 60
                val seconds = (totalTimeInMillis / 1000) % 60

                // Fetch the last and first time used
                val lastTimeStamp = stats.lastTimeStamp
                val firstTimeStamp = stats.firstTimeStamp

                // Create and return the AppScreenTime object with the additional fields
                AppScreenTime(
                    appName = appName,
                    screenTime = String.format("%02dh %02dm %02ds", hours, minutes, seconds),
                    lastTimeStamp = lastTimeStamp,
                    firstTimeStamp = firstTimeStamp
                )
            }
    }




}
