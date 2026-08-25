package com.example.gamepadmapper

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        findViewById<android.widget.Button>(R.id.btnGrantOverlay).setOnClickListener {
            requestOverlayPermission()
        }
        findViewById<android.widget.Button>(R.id.btnGrantAccessibility).setOnClickListener {
            openAccessibilitySettings()
        }
        findViewById<android.widget.Button>(R.id.btnStartOverlay).setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, OverlayService::class.java))
            } else {
                requestOverlayPermission()
            }
        }
        findViewById<android.widget.Button>(R.id.btnStopOverlay).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val overlayOk = Settings.canDrawOverlays(this)
        val accessibilityOk = isAccessibilityServiceEnabled()
        statusText.text = buildString {
            append(if (overlayOk) "✅ Izin overlay: aktif\n" else "❌ Izin overlay: belum aktif\n")
            append(if (accessibilityOk) "✅ Accessibility service: aktif" else "❌ Accessibility service: belum aktif")
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${TapAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
