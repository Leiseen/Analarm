package com.analarm // 你的包名

import android.content.Intent

object AlarmState {
    // 用来记录正在响铃的那个闹钟的启动意图
    // 如果它不为null，就说明有闹钟在响
    var ringingAlarmIntent: Intent? = null
}