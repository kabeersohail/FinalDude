package com.wesupport.finaldude

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 2. TimeRange.kt
data class TimeRange(
    val startTime: Long,
    val endTime: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimeRange) return false
        return startTime == other.startTime && endTime == other.endTime
    }

    override fun hashCode(): Int {
        var result = startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        return result
    }

    fun formatRange(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm aa", Locale.ENGLISH)
        return "${dateFormat.format(Date(startTime))} - ${dateFormat.format(Date(endTime))}"
    }
}