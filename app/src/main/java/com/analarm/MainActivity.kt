package com.analarm

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var alarmsRecyclerView: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private val alarmList = mutableListOf<Alarm>()
    private lateinit var database: AppDatabase

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                checkStoragePermission()
            } else {
                Toast.makeText(this, "需要通知权限才能显示闹钟", Toast.LENGTH_LONG).show()
            }
        }

    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startActivity(Intent(this, AlarmDetailActivity::class.java))
            } else {
                Toast.makeText(this, "需要存储权限才能播放音乐", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)

        alarmAdapter = AlarmAdapter(
            alarmList,
            onSwitchChanged = { alarm ->
                lifecycleScope.launch {
                    database.alarmDao().update(alarm)
                    if (alarm.isEnabled) {
                        scheduleAlarm(alarm)
                    } else {
                        cancelAlarm(alarm)
                    }
                }
            },
            onAlarmLongPressed = { alarm ->
                showDeleteConfirmationDialog(alarm)
            },
            onAlarmClicked = { alarm ->
                val intent = Intent(this, AlarmDetailActivity::class.java).apply {
                    putExtra("ALARM_ID", alarm.id)
                }
                startActivity(intent)
            }
        )

        alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView)
        alarmsRecyclerView.adapter = alarmAdapter

        lifecycleScope.launch {
            database.alarmDao().getAllAlarms().collect { alarmsFromDb ->
                alarmList.clear()
                alarmList.addAll(alarmsFromDb)
                alarmAdapter.notifyDataSetChanged()
            }
        }

        val addAlarmFab: FloatingActionButton = findViewById(R.id.addAlarmFab)
        addAlarmFab.setOnClickListener {
            checkNotificationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        checkRingingState()
    }

    private fun checkRingingState() {
        if (AlarmState.ringingAlarmIntent != null) {
            val intent = AlarmState.ringingAlarmIntent
            val dismissMethod = intent?.getStringExtra("DISMISS_METHOD")
            val targetIntent = when (dismissMethod) {
                "QR_CODE" -> Intent(this, QrScannerActivity::class.java)
                else -> Intent(this, MathProblemActivity::class.java)
            }
            targetIntent.putExtras(intent?.extras ?: Bundle())
            startActivity(targetIntent)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    checkStoragePermission()
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            checkStoragePermission()
        }
    }

    private fun checkStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                startActivity(Intent(this, AlarmDetailActivity::class.java))
            }
            else -> {
                requestStoragePermissionLauncher.launch(permission)
            }
        }
    }

    private fun scheduleAlarm(alarm: Alarm) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // --- 核心修正：Intent里只放ID，保持干净 ---
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }
        // -----------------------------------------

        val pendingIntent = PendingIntent.getBroadcast(
            this, alarm.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.daysOfWeek.isEmpty()) {
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            val now = Calendar.getInstance()
            val today = now.get(Calendar.DAY_OF_WEEK)
            for (i in 0..7) {
                val dayToTest = ((today - 1 + i) % 7) + 1
                if (alarm.daysOfWeek.contains(dayToTest)) {
                    val alarmTimeForDay = calendar.clone() as Calendar
                    alarmTimeForDay.add(Calendar.DAY_OF_YEAR, i)
                    if (alarmTimeForDay.timeInMillis > now.timeInMillis) {
                        calendar.time = alarmTimeForDay.time
                        break
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                Toast.makeText(this, "闹钟已设定", Toast.LENGTH_SHORT).show()
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            Toast.makeText(this, "闹钟已设定", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelAlarm(alarm: Alarm) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun showDeleteConfirmationDialog(alarm: Alarm) {
        AlertDialog.Builder(this)
            .setTitle("删除闹钟")
            .setMessage("确定要删除 ${String.format("%02d:%02d", alarm.hour, alarm.minute)} 这个闹钟吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteAlarm(alarm)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteAlarm(alarm: Alarm) {
        lifecycleScope.launch {
            cancelAlarm(alarm)
            database.alarmDao().delete(alarm)
            Toast.makeText(this@MainActivity, "闹钟已删除", Toast.LENGTH_SHORT).show()
        }
    }
}