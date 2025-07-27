package com.analarm // 你的包名

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class MathProblemActivity : AppCompatActivity() {

    private var correctAnswer = 0
    private val NOTIFICATION_ID = 123 // 给通知一个固定的ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_math_problem)

        // 启动音乐服务的代码已经移到AlarmReceiver里了，这里不再需要

        val questionTextView: TextView = findViewById(R.id.questionTextView)
        val answerEditText: EditText = findViewById(R.id.answerEditText)
        val confirmButton: Button = findViewById(R.id.confirmButton)

        generateMathQuestion(questionTextView)

        confirmButton.setOnClickListener {
            val userAnswerString = answerEditText.text.toString()
            if (userAnswerString.isNotEmpty()) {
                val userAnswer = userAnswerString.toInt()
                if (userAnswer == correctAnswer) {
                    dismissAlarm() // 调用统一的关闭闹钟方法
                } else {
                    Toast.makeText(this, "答案错误，请重试！", Toast.LENGTH_SHORT).show()
                    answerEditText.text.clear()
                }
            } else {
                Toast.makeText(this, "请输入答案！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- 新增：一个统一的关闭闹钟方法 ---
    private fun dismissAlarm() {
        Toast.makeText(this, "回答正确！闹钟已停止", Toast.LENGTH_SHORT).show()

        // 1. 停止音乐服务
        stopService(Intent(this, AlarmService::class.java))

        // 2. 清除我们自己创建的通知
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        // 3. 关闭当前界面
        finish()
    }

    private fun generateMathQuestion(textView: TextView) {
        val num1 = Random.nextInt(10, 100)
        val num2 = Random.nextInt(10, 100)
        correctAnswer = num1 + num2
        textView.text = "$num1 + $num2 = ?"
    }
}