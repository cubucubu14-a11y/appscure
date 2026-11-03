package com.antitheft.siren

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggle: Button
    private lateinit var txtStatus: TextView
    private var isArmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Membuat UI sederhana secara dinamis agar tidak perlu repot membuat file XML layout terpisah
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        txtStatus = TextView(this).apply {
            text = "Status: TIDAK AKTIF"
            textSize = 18f
            setPadding(0, 0, 0, 30)
            gravity = android.view.Gravity.CENTER
        }

        btnToggle = Button(this).apply {
            text = "AKTIFKAN MODE ANTI-MALING"
            setOnClickListener {
                if (!isArmed) {
                    requestPermissionsAndStart()
                } else {
                    stopAntiTheftService()
                }
            }
        }

        layout.addView(txtStatus)
        layout.addView(btnToggle)
        setContentView(layout)

        // Cek izin baterai saat pertama buka
        checkAndRequestBatteryOptimization()
        openItelAutoStartMenu()
    }

    private fun requestPermissionsAndStart() {
        checkAndRequestBatteryOptimization()
        startAntiTheftService()
    }

    private fun startAntiTheftService() {
        val serviceIntent = Intent(this, AntiTheftService::class.java).apply {
            action = "START_ARM"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        isArmed = true
        txtStatus.text = "Status: AKTIF (Letakkan HP di Meja)"
        btnToggle.text = "MATIKAN ALARM"
        Toast.makeText(this, "Mode Anti-Maling diaktifkan!", Toast.LENGTH_SHORT).show()
    }

    private fun stopAntiTheftService() {
        val serviceIntent = Intent(this, AntiTheftService::class.java).apply {
            action = "STOP_ARM"
        }
        startService(serviceIntent)
        isArmed = false
        txtStatus.text = "Status: TIDAK AKTIF"
        btnToggle.text = "AKTIFKAN MODE ANTI-MALING"
        Toast.makeText(this, "Alarm dimatikan.", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("BatteryLife")
    private fun checkAndRequestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(fallbackIntent)
                }
            }
        }
    }

    private fun openItelAutoStartMenu() {
        val brand = Build.BRAND.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()

        if (brand.contains("itel") || manufacturer.contains("itel") ||
            brand.contains("infinix") || brand.contains("tecno")) {
            val intents = arrayOf(
                Intent().setComponent(ComponentName("com.transsion.phonemaster", "com.transsion.phonemaster.autostart.AutoStartManagementActivity")),
                Intent().setComponent(ComponentName("com.transsion.phonemanager", "com.transsion.phonemanager.autostart.AutoStartActivity")),
                Intent().setComponent(ComponentName("com.evenwell.powersaving.g3", "com.evenwell.powersaving.g3.exception.PowerWhitelistEditActivity"))
            )
            for (intent in intents) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    break
                } catch (e: Exception) {
                    // Lanjut jika intent gagal
                }
            }
        }
    }
}
