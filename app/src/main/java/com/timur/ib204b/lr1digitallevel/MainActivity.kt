package com.timur.ib204b.lr1digitallevel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var levelArea: FrameLayout
    private lateinit var bubble: View
    private lateinit var tvStatus: TextView
    private lateinit var tvXValue: TextView
    private lateinit var tvYValue: TextView

    private var filteredX = 0f
    private var filteredY = 0f

    private val alpha = 0.15f
    private val levelThreshold = 0.5f
    private var wasLevel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        levelArea = findViewById(R.id.levelArea)
        bubble = findViewById(R.id.bubble)
        tvStatus = findViewById(R.id.tvStatus)
        tvXValue = findViewById(R.id.tvXValue)
        tvYValue = findViewById(R.id.tvYValue)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            tvStatus.text = "На устройстве нет акселерометра"
            tvXValue.text = "X: недоступно"
            tvYValue.text = "Y: недоступно"
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val rawX = event.values[0]
        val rawY = event.values[1]

        filteredX += alpha * (rawX - filteredX)
        filteredY += alpha * (rawY - filteredY)

        updateLevel(filteredX, filteredY)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Ничего не делаем
    }

    private fun updateLevel(x: Float, y: Float) {
        tvXValue.text = String.format(Locale.US, "X: %.2f", x)
        tvYValue.text = String.format(Locale.US, "Y: %.2f", y)

        val isLevel = abs(x) < levelThreshold && abs(y) < levelThreshold

        if (isLevel) {
            tvStatus.text = "Статус: устройство почти горизонтально"
            tvStatus.setTextColor(0xFF2E7D32.toInt())

            if (!wasLevel) {
                vibrateShort()
            }
        } else {
            tvStatus.text = "Статус: есть отклонение"
            tvStatus.setTextColor(0xFFD32F2F.toInt())
        }

        wasLevel = isLevel
        moveBubble(x, y)
    }

    private fun moveBubble(x: Float, y: Float) {
        levelArea.post {
            val maxX = (levelArea.width - bubble.width) / 2f
            val maxY = (levelArea.height - bubble.height) / 2f

            val normalizedX = (x / 7f).coerceIn(-1f, 1f)
            val normalizedY = (y / 7f).coerceIn(-1f, 1f)

            bubble.translationX = -normalizedX * maxX
            bubble.translationY = normalizedY * maxY
        }
    }

    private fun vibrateShort() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    80,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    }
}