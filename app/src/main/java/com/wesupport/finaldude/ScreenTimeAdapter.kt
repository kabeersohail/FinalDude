package com.wesupport.finaldude

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 5. ScreenTimeAdapter.kt
class ScreenTimeAdapter(private var screenTimeGroups: List<AppScreenTimeGroup>) : 
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    override fun getItemViewType(position: Int): Int {
        var currentPosition = position
        for (group in screenTimeGroups) {
            if (currentPosition == 0) return VIEW_TYPE_HEADER
            if (currentPosition <= group.apps.size) return VIEW_TYPE_ITEM
            currentPosition -= (group.apps.size + 1)
        }
        return VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_timerange_header, parent, false)
                TimeRangeViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_screen_time, parent, false)
                ScreenTimeViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPosition = position
        for (group in screenTimeGroups) {
            if (currentPosition == 0) {
                (holder as TimeRangeViewHolder).bind(group.timeRange)
                return
            }
            if (currentPosition <= group.apps.size) {
                (holder as ScreenTimeViewHolder).bind(group.apps[currentPosition - 1])
                return
            }
            currentPosition -= (group.apps.size + 1)
        }
    }

    override fun getItemCount(): Int {
        return screenTimeGroups.sumOf { it.apps.size + 1 } // +1 for each header
    }

    class TimeRangeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeRangeTextView: TextView = itemView.findViewById(R.id.tvTimeRange)

        fun bind(timeRange: TimeRange) {

            val durationMillis = timeRange.endTime - timeRange.startTime
            val formattedDuration = formatDuration(durationMillis)
            timeRangeTextView.text = timeRange.formatRange()  + " Duration: $formattedDuration"
        }

        private fun formatDuration(millis: Long): String {
            val seconds = millis / 1000
            val minutes = seconds / 60
            val hours = minutes / 60

            return when {
                hours > 0 -> "${hours}h ${minutes % 60}m"
                minutes > 0 -> "${minutes}m ${seconds % 60}s"
                else -> "${seconds}s"
            }
        }

    }


    class ScreenTimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appNameTextView: TextView = itemView.findViewById(R.id.tvAppName)
        private val screenTimeTextView: TextView = itemView.findViewById(R.id.tvScreenTime)

        fun bind(appScreenTime: AppScreenTime) {
            appNameTextView.text = appScreenTime.appName
            screenTimeTextView.text = appScreenTime.screenTime
        }
    }
}
