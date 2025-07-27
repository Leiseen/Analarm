package com.analarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.DocumentsContract
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val NOTIFICATION_ID = 123
    private lateinit var database: AppDatabase
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP") {
            stopAndCleanup()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra("ALARM_ID", -1L) ?: -1L
        if (alarmId == -1L || mediaPlayer?.isPlaying == true) {
            return START_STICKY
        }

        serviceScope.launch {
            val alarm = database.alarmDao().getAlarmById(alarmId)
            if (alarm != null) {
                // 把从数据库查到的完整信息，构建一个新的intent存入AlarmState
                val fullInfoIntent = Intent().apply {
                    putExtra("DISMISS_METHOD", alarm.dismissMethod)
                    putExtra("QR_DATA", alarm.qrCodeData)
                    putExtra("SONG_URI", alarm.musicFolderPath)
                }
                AlarmState.ringingAlarmIntent = fullInfoIntent

                launch(Dispatchers.Main) {
                    val notification = createNotification(alarm)
                    startForeground(NOTIFICATION_ID, notification)
                    playMusicLogic(alarm.musicFolderPath)
                }
            }
        }
        return START_STICKY
    }

    private fun createNotification(alarm: Alarm): Notification {
        val targetIntent = when (alarm.dismissMethod) {
            "QR_CODE" -> Intent(this, QrScannerActivity::class.java)
            else -> Intent(this, MathProblemActivity::class.java)
        }
        // 把必要信息传递给解谜界面
        targetIntent.putExtra("QR_DATA", alarm.qrCodeData)
        targetIntent.putExtra("SONG_URI", alarm.musicFolderPath) // 传递音乐路径，虽然服务在放，但界面可能需要
        targetIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, NOTIFICATION_ID, targetIntent, pendingIntentFlag
        )

        // 添加停止按钮
        val stopIntent = Intent(this, AlarmService::class.java).apply { action = "ACTION_STOP" }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "ALARM_CHANNEL_ID")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("闹钟正在响铃")
            .setContentText("点击以关闭")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "停止", stopPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        return notification
    }

    private fun playMusicLogic(pathOrUri: String?) {
        if (pathOrUri.isNullOrEmpty()) {
            playDefaultRingtone()
            return
        }

        if (pathOrUri.startsWith("content://")) {
            try {
                val folderUri = Uri.parse(pathOrUri)
                val docTreeUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, DocumentsContract.getTreeDocumentId(folderUri))
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(docTreeUri, DocumentsContract.getTreeDocumentId(docTreeUri))

                val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val cursor = contentResolver.query(childrenUri, projection, null, null, null)

                val musicFiles = mutableListOf<Uri>()
                cursor?.use {
                    while (it.moveToNext()) {
                        val docId = it.getString(0)
                        val name = it.getString(1)
                        if (name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".wav")) {
                            musicFiles.add(DocumentsContract.buildDocumentUriUsingTree(docTreeUri, docId))
                        }
                    }
                }

                if (musicFiles.isNotEmpty()) {
                    playMusicFromUri(musicFiles.random())
                } else {
                    playDefaultRingtone()
                }
            } catch (e: Exception) {
                playDefaultRingtone()
            }
        } else {
            val musicFolder = File(pathOrUri)
            if (musicFolder.exists() && musicFolder.isDirectory) {
                val musicFiles = musicFolder.listFiles { file ->
                    file.isFile && (file.extension == "mp3" || file.extension == "m4a" || file.extension == "wav")
                }
                if (musicFiles != null && musicFiles.isNotEmpty()) {
                    playMusicFromFile(musicFiles.random())
                } else {
                    playDefaultRingtone()
                }
            } else {
                playDefaultRingtone()
            }
        }
    }

    private fun playMusicFromFile(songFile: File) {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(songFile.path)
                isLooping = true
                prepare()
                start()
            } catch (e: Exception) { playDefaultRingtone() }
        }
    }

    private fun playMusicFromUri(songUri: Uri) {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(applicationContext, songUri)
                isLooping = true
                prepare()
                start()
            } catch (e: Exception) { playDefaultRingtone() }
        }
    }

    private fun playDefaultRingtone() {
        val ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(this, ringtoneUri)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAndCleanup()
    }

    private fun stopAndCleanup() {
        AlarmState.ringingAlarmIntent = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        // 确保服务停止时，通知也被移除
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}