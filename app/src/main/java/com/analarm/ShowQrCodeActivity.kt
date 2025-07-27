package com.analarm // 你的包名

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class ShowQrCodeActivity : AppCompatActivity() {

    private var qrBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_qr_code)

        val qrCodeImageView: ImageView = findViewById(R.id.qrCodeImageView)
        val saveButton: Button = findViewById(R.id.saveButton)

        // 从上一个界面接收“暗号”
        val qrData = intent.getStringExtra("QR_DATA")

        if (qrData.isNullOrEmpty()) {
            Toast.makeText(this, "二维码数据无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- 核心：生成二维码图片 ---
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            qrBitmap = bmp
            qrCodeImageView.setImageBitmap(qrBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "生成二维码失败", Toast.LENGTH_SHORT).show()
        }
        // -------------------------

        saveButton.setOnClickListener {
            saveQrCodeToGallery()
        }
    }

    private fun saveQrCodeToGallery() {
        qrBitmap?.let { bitmap ->
            val resolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "alarm_qr_${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Alarms")
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                try {
                    resolver.openOutputStream(uri).use { outputStream ->
                        outputStream?.let { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                    }
                    Toast.makeText(this, "二维码已保存到相册！", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}