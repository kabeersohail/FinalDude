package com.wesupport.finaldude

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppScreenTime(
    val appName: String,
    val screenTime: String,
    val lastTimeStamp: Long,  // Add this field
    val firstTimeStamp: Long // Add this field
)

data class UsageGroup(
    val startTimestamp: String,
    val endTimestamp: String,
    val apps: List<String>
)


class ScreenTimeAdapter(private val screenTimeData: List<AppScreenTime>) : RecyclerView.Adapter<ScreenTimeAdapter.ScreenTimeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenTimeViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_screen_time, parent, false)
        return ScreenTimeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ScreenTimeViewHolder, position: Int) {
        val appScreenTime = screenTimeData[position]
        holder.appNameTextView.text = appScreenTime.appName
        holder.screenTimeTextView.text = appScreenTime.screenTime

        // Format the lastTimeUsed and firstTimeUsed into a readable format (e.g., date/time)
        val lastUsedFormatted = formatTimestamp(appScreenTime.lastTimeStamp)
        val firstUsedFormatted = formatTimestamp(appScreenTime.firstTimeStamp)

        holder.lastUsedTextView.text = "Last Used: $lastUsedFormatted"
        holder.firstUsedTextView.text = "First Used: $firstUsedFormatted"
    }

    override fun getItemCount(): Int {
        return screenTimeData.size
    }

    // Format timestamps to a human-readable date/time
    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    class ScreenTimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appNameTextView: TextView = itemView.findViewById(R.id.tvAppName)
        val screenTimeTextView: TextView = itemView.findViewById(R.id.tvScreenTime)
        val lastUsedTextView: TextView = itemView.findViewById(R.id.tvLastUsed)
        val firstUsedTextView: TextView = itemView.findViewById(R.id.tvFirstUsed)
    }
}

