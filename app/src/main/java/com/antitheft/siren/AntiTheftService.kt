package com.antitheft.siren

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class AntiTheftService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var wakeLock: PowerManager.WakeLock
    private var mediaPlayer: MediaPlayer? = null
    private var isArmed = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AntiTheft::WakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "START_ARM") {
            isArmed = true
            if (!wakeLock.isHeld) {
                wakeLock.acquire(15 * 60 * 1000L)
            }
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            startForegroundServiceNotification()
        } else if (action == "STOP_ARM") {
            stopAlarmAndService()
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "anti_theft_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Anti-Theft Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Anti-Maling Aktif")
            .setContentText("Perangkat sedang dipantau dari gerakan...")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()

        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isArmed || event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt((x * x + y * y + z * z).toDouble())

        if (acceleration > 13.0) {
            triggerAlarmAndScreenOn()
        }
    }

    private fun triggerAlarmAndScreenOn() {
        // 1. Nyalakan Layar HP
        val wakeIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(wakeIntent)

        // 2. Putar MP3 pilihan pengguna dari penyimpanan HP
        if (mediaPlayer == null) {
            try {
                val prefs = getSharedPreferences("AntiTheftPrefs", MODE_PRIVATE)
                val uriStr = prefs.getString("audio_uri", null)

                if (uriStr != null) {
                    // Putar dari file pilihan di memori HP
                    val audioUri = Uri.parse(uriStr)
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(applicationContext, audioUri)
                        isLooping = true
                        prepare()
                        start()
                    }
                } else {
                    // Fallback jika belum pernah pilih file: gunakan ringtone bawaan sistem HP
                    val defaultRingtone = android.provider.Settings.System.DEFAULT_RINGTONE_URI
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(applicationContext, defaultRingtone)
                        isLooping = true
                        prepare()
                        start()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAlarmAndService() {
        isArmed = false
        try {
            sensorManager.unregisterListener(this)
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null
}
