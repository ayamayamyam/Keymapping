package com.example.gamepadmapper

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var mappingListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        mappingListContainer = findViewById(R.id.mappingListContainer)

        findViewById<Button>(R.id.btnGrantOverlay).setOnClickListener {
            requestOverlayPermission()
        }
        findViewById<Button>(R.id.btnGrantAccessibility).setOnClickListener {
            openAccessibilitySettings()
        }
        findViewById<Button>(R.id.btnStartOverlay).setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, OverlayService::class.java))
            } else {
                requestOverlayPermission()
            }
        }
        findViewById<Button>(R.id.btnStopOverlay).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
        }
        findViewById<Button>(R.id.btnAddButton).setOnClickListener {
            showAddButtonDialog()
        }
        findViewById<Button>(R.id.btnResetDefault).setOnClickListener {
            confirmResetToDefault()
        }
    }

    /** Konfirmasi dulu sebelum reset, karena aksi ini menghapus semua kustomisasi user. */
    private fun confirmResetToDefault() {
        AlertDialog.Builder(this)
            .setTitle("Reset ke Default?")
            .setMessage("Semua tombol kustom dan posisi yang sudah diatur akan hilang, lalu diganti dengan tombol bawaan (A, B, ↑, ↓).")
            .setPositiveButton("Reset") { _, _ ->
                MappingStore.resetToDefault(this)
                refreshMappingList()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshMappingList()
    }

    private fun updateStatus() {
        val overlayOk = Settings.canDrawOverlays(this)
        val accessibilityOk = isAccessibilityServiceEnabled()
        statusText.text = buildString {
            append(if (overlayOk) "✅ Izin overlay: aktif\n" else "❌ Izin overlay: belum aktif\n")
            append(if (accessibilityOk) "✅ Accessibility service: aktif" else "❌ Accessibility service: belum aktif")
        }
    }

    /** Menampilkan dialog input nama tombol baru, lalu menyimpannya. */
    private fun showAddButtonDialog() {
        val input = EditText(this)
        input.hint = "Nama tombol, misal: X"

        AlertDialog.Builder(this)
            .setTitle("Tambah Tombol Baru")
            .setView(input)
            .setPositiveButton("Tambah") { _, _ ->
                val label = input.text.toString().trim()
                if (label.isNotEmpty()) {
                    MappingStore.addMapping(this, label)
                    refreshMappingList()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /** Menggambar ulang daftar tombol yang tersimpan sebagai baris label + tombol hapus. */
    private fun refreshMappingList() {
        mappingListContainer.removeAllViews()
        val mappings = MappingStore.load(this)

        if (mappings.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Belum ada tombol. Tekan '+ Tambah Tombol' di atas."
            mappingListContainer.addView(empty)
            return
        }

        mappings.forEach { mapping ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, 12, 0, 12)

            val labelView = TextView(this)
            labelView.text = mapping.label
            labelView.textSize = 16f
            labelView.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )

            val deleteButton = Button(this)
            deleteButton.text = "Hapus"
            deleteButton.setOnClickListener {
                MappingStore.removeMapping(this, mapping.id)
                refreshMappingList()
            }

            row.addView(labelView)
            row.addView(deleteButton)
            mappingListContainer.addView(row)
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
