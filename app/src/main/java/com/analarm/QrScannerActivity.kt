package com.analarm // 你的包名

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QrScannerActivity : AppCompatActivity() {

    private lateinit var capture: CaptureManager
    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private val NOTIFICATION_ID = 123 // 和数学题界面使用同一个ID，这样它们会互相覆盖

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        barcodeScannerView = findViewById(R.id.barcode_scanner)
        val correctQrData = intent.getStringExtra("QR_DATA")

        setupScanner(correctQrData)
    }

    private fun setupScanner(correctQrData: String?) {
        capture = CaptureManager(this, barcodeScannerView)
        capture.initializeFromIntent(intent, null)
        capture.decode()

        barcodeScannerView.decodeSingle { result ->
            if (result.text == correctQrData) {
                dismissAlarm()
            } else {
                Toast.makeText(this, "二维码不匹配！请扫描正确的二维码", Toast.LENGTH_SHORT).show()
                // 稍后重新开始扫描，给用户反应时间
                barcodeScannerView.postDelayed({ setupScanner(correctQrData) }, 2000)
            }
        }
    }

    private fun dismissAlarm() {
        Toast.makeText(this, "扫描成功！闹钟已停止", Toast.LENGTH_LONG).show()
        stopService(Intent(this, AlarmService::class.java))
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        finish()
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }
}