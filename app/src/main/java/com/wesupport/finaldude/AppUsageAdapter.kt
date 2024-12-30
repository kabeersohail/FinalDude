// AppUsageAdapter.kt
package com.wesupport.finaldude

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class AppUsageAdapter : RecyclerView.Adapter<AppUsageAdapter.ViewHolder>() {
    private var usageList: List<UsageData> = emptyList()

    fun setData(newList: List<UsageData>) {
        usageList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usage = usageList[position]
        holder.bind(usage)
    }

    override fun getItemCount(): Int = usageList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val appNameText: TextView = view.findViewById(R.id.appNameText)
        private val usageTimeText: TextView = view.findViewById(R.id.usageTimeText)

        fun bind(usage: UsageData) {
            appNameText.text = usage.appName
            usageTimeText.text = formatDuration(usage.duration)
        }

        private fun formatDuration(duration: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(duration)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
}

data class UsageData(
    val appName: String,
    val packageName: String,
    val duration: Long
)