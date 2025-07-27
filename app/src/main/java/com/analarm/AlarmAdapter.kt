package com.analarm // 你的包名

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class AlarmAdapter(
    private val alarms: MutableList<Alarm>,
    private val onSwitchChanged: (Alarm) -> Unit,
    private val onAlarmLongPressed: (Alarm) -> Unit,
    private val onAlarmClicked: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val daysTextView: TextView = itemView.findViewById(R.id.daysTextView) // 找到新標籤
        val alarmSwitch: SwitchMaterial = itemView.findViewById(R.id.alarmSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.timeTextView.text = String.format("%02d:%02d", alarm.hour, alarm.minute)
        holder.daysTextView.text = formatDaysOfWeek(alarm.daysOfWeek) // 設置重複日文本

        holder.alarmSwitch.setOnCheckedChangeListener(null)
        holder.alarmSwitch.isChecked = alarm.isEnabled

        holder.alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            alarm.isEnabled = isChecked
            onSwitchChanged(alarm)
        }

        holder.itemView.setOnClickListener { onAlarmClicked(alarm) }
        holder.itemView.setOnLongClickListener {
            onAlarmLongPressed(alarm)
            true
        }
    }

    override fun getItemCount() = alarms.size

    // --- 新增的輔助函數，用來格式化重複日 ---
    private fun formatDaysOfWeek(days: Set<Int>): String {
        if (days.isEmpty()) return "僅一次"
        if (days.size == 7) return "每天"
        val dayMap = mapOf(1 to "周日", 2 to "周一", 3 to "周二", 4 to "周三", 5 to "周四", 6 to "周五", 7 to "周六")
        // 排序以保證顯示順序一致
        return days.sorted().joinToString(", ") { dayMap[it] ?: "" }
    }
}