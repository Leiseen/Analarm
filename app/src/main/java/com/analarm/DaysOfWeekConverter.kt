package com.analarm // 你的包名

import androidx.room.TypeConverter

class DaysOfWeekConverter {
    @TypeConverter
    fun fromStringSet(days: Set<Int>): String {
        return days.joinToString(",")
    }

    @TypeConverter
    fun toStringSet(data: String): Set<Int> {
        if (data.isEmpty()) return emptySet()
        return data.split(",").map { it.toInt() }.toSet()
    }
}