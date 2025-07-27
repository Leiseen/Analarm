package com.analarm // 你的包名

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "alarms")
@TypeConverters(DaysOfWeekConverter::class) // 告诉Room要使用我们的转换器
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var hour: Int,
    var minute: Int,
    var isEnabled: Boolean,
    var musicFolderPath: String? = null,
    var dismissMethod: String = "MATH",
    var qrCodeData: String? = null,
    // --- 这是新加的属性 ---
    var daysOfWeek: Set<Int> = emptySet() // 用一个集合来存储重复日（周日=1, 周一=2...）
)