package com.wesupport.finaldude

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// AppScreenTime.kt
data class AppScreenTime(
    val appName: String,
    val screenTime: String,
    val lastTimeStamp: Long,
    val firstTimeStamp: Long,
    val totalTimeInMillis: Long
)

// ScreenTimeAdapter.kt
class ScreenTimeAdapter(private val screenTimeData: List<AppScreenTime>) :
    RecyclerView.Adapter<ScreenTimeAdapter.ScreenTimeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenTimeViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screen_time, parent, false)
        return ScreenTimeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ScreenTimeViewHolder, position: Int) {
        val appScreenTime = screenTimeData[position]
        holder.bind(appScreenTime)
    }

    override fun getItemCount() = screenTimeData.size

    class ScreenTimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appNameTextView: TextView = itemView.findViewById(R.id.tvAppName)
        private val screenTimeTextView: TextView = itemView.findViewById(R.id.tvScreenTime)
        private val lastUsedTextView: TextView = itemView.findViewById(R.id.tvLastUsed)
        private val firstUsedTextView: TextView = itemView.findViewById(R.id.tvFirstUsed)

        fun bind(appScreenTime: AppScreenTime) {
            appNameTextView.text = appScreenTime.appName
            screenTimeTextView.text = appScreenTime.screenTime
            lastUsedTextView.text = "Last Used: ${formatTimestamp(appScreenTime.lastTimeStamp)}"
            firstUsedTextView.text = "First Used: ${formatTimestamp(appScreenTime.firstTimeStamp)}"
        }

        private fun formatTimestamp(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }
}
