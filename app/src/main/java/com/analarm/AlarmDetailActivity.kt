package com.analarm // 你的包名

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.*

class AlarmDetailActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var dayToggles: List<ToggleButton>
    private lateinit var dismissMethodGroup: RadioGroup
    private lateinit var saveButton: Button
    private lateinit var database: AppDatabase
    private var existingAlarmId: Long? = null
    // --- 新增的控件 ---
    private lateinit var folderPathTextView: TextView
    private lateinit var selectFolderButton: Button
    private var selectedFolderUri: String? = null

    // --- 新增：註冊一個“文件夾選擇器”的結果處理器 ---
    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            if (uri != null) {
                // 這是關鍵一步：獲取對這個文件夾的永久訪問權限
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // 保存URI並更新界面
                selectedFolderUri = uri.toString()
                folderPathTextView.text = getFolderNameFromUri(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_detail)

        database = AppDatabase.getDatabase(this)
        bindViews()

        existingAlarmId = intent.getLongExtra("ALARM_ID", -1L).takeIf { it != -1L }

        if (existingAlarmId != null) {
            loadAlarmData(existingAlarmId!!)
        } else {
            // (新建模式)
        }

        selectFolderButton.setOnClickListener {
            // 啟動文件夾選擇器
            folderPickerLauncher.launch(null)
        }

        saveButton.setOnClickListener {
            saveAlarm()
        }
    }

    private fun bindViews() {
        timePicker = findViewById(R.id.timePicker)
        timePicker.setIs24HourView(true)
        dayToggles = listOf(
            findViewById(R.id.toggle_sun), findViewById(R.id.toggle_mon),
            findViewById(R.id.toggle_tue), findViewById(R.id.toggle_wed),
            findViewById(R.id.toggle_thu), findViewById(R.id.toggle_fri),
            findViewById(R.id.toggle_sat)
        )
        dismissMethodGroup = findViewById(R.id.dismissMethodGroup)
        saveButton = findViewById(R.id.saveButton)
        // --- 綁定新控件 ---
        folderPathTextView = findViewById(R.id.folderPathTextView)
        selectFolderButton = findViewById(R.id.selectFolderButton)
    }

    private fun loadAlarmData(alarmId: Long) {
        lifecycleScope.launch {
            val alarm = database.alarmDao().getAlarmById(alarmId)
            if (alarm != null) {
                timePicker.hour = alarm.hour
                timePicker.minute = alarm.minute
                alarm.daysOfWeek.forEach { day ->
                    dayToggles[day - 1].isChecked = true
                }
                if (alarm.dismissMethod == "QR_CODE") {
                    dismissMethodGroup.check(R.id.radio_qr)
                } else {
                    dismissMethodGroup.check(R.id.radio_math)
                }
                // --- 加載並顯示已選文件夾 ---
                selectedFolderUri = alarm.musicFolderPath
                if (selectedFolderUri != null) {
                    folderPathTextView.text = getFolderNameFromUri(Uri.parse(selectedFolderUri))
                } else {
                    folderPathTextView.text = "默認鈴聲"
                }
            }
        }
    }

    private fun saveAlarm() {
        val hour = timePicker.hour
        val minute = timePicker.minute
        val daysOfWeek = mutableSetOf<Int>()
        dayToggles.forEachIndexed { index, toggleButton ->
            if (toggleButton.isChecked) {
                daysOfWeek.add(index + 1)
            }
        }
        val dismissMethod = if (findViewById<RadioButton>(R.id.radio_qr).isChecked) "QR_CODE" else "MATH"

        lifecycleScope.launch {
            if (existingAlarmId != null) {
                val alarmToUpdate = database.alarmDao().getAlarmById(existingAlarmId!!)!!
                alarmToUpdate.hour = hour
                alarmToUpdate.minute = minute
                alarmToUpdate.daysOfWeek = daysOfWeek
                alarmToUpdate.dismissMethod = dismissMethod
                alarmToUpdate.musicFolderPath = selectedFolderUri // <-- 保存選擇的文件夾
                database.alarmDao().update(alarmToUpdate)
                Toast.makeText(this@AlarmDetailActivity, "鬧鐘已更新", Toast.LENGTH_SHORT).show()
            } else {
                val qrData = if (dismissMethod == "QR_CODE") "analarm-${System.currentTimeMillis()}" else null
                val newAlarm = Alarm(
                    hour = hour, minute = minute, isEnabled = true,
                    daysOfWeek = daysOfWeek, dismissMethod = dismissMethod, qrCodeData = qrData,
                    musicFolderPath = selectedFolderUri // <-- 保存選擇的文件夾
                )
                database.alarmDao().insert(newAlarm)
                Toast.makeText(this@AlarmDetailActivity, "鬧鐘已創建", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    // --- 新增：一個從URI中獲取文件夾名字的輔助函數 ---
    private fun getFolderNameFromUri(uri: Uri): String {
        return try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            val cursor = contentResolver.query(docUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(0)
                } else {
                    "未知文件夾"
                }
            } ?: "未知文件夾"
        } catch (e: Exception) {
            "無法讀取文件夾"
        }
    }
}