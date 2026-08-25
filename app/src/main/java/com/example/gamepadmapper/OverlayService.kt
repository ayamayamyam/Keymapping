package com.example.gamepadmapper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * Service ini menggambar setiap tombol dari ButtonMapping sebagai View
 * melayang (TYPE_APPLICATION_OVERLAY). Saat tombol ditekan:
 *  1. tampilan tombol memberi efek visual ditekan
 *  2. TapAccessibilityService mensimulasikan tap di koordinat yang sama,
 *     sehingga aplikasi/game DI BAWAH overlay menerima sentuhan itu.
 * Tombol juga bisa digeser (drag) untuk mengubah posisinya.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private val overlayViews = mutableListOf<View>()
    private val NOTIF_CHANNEL_ID = "overlay_service_channel"
    private val NOTIF_ID = 1001

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        drawButtons()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        overlayViews.forEach { runCatching { windowManager.removeView(it) } }
        overlayViews.clear()
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID, "Overlay aktif",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val stopIntent = Intent(this, OverlayService::class.java)
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Gamepad Mapper aktif")
            .setContentText("Tombol virtual sedang ditampilkan")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    private fun drawButtons() {
        val mappings = MappingStore.load(this)

        mappings.forEach { mapping ->
            val buttonView = TextView(this).apply {
                text = mapping.label
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#88222222"))
                gravity = Gravity.CENTER
                textSize = 16f
            }

            val size = mapping.radiusPx * 2
            val params = WindowManager.LayoutParams(
                size, size,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = mapping.x - mapping.radiusPx
                y = mapping.y - mapping.radiusPx
            }

            attachDragAndTap(buttonView, params, mapping)

            windowManager.addView(buttonView, params)
            overlayViews.add(buttonView)
        }
    }

    /**
     * Satu listener yang menangani dua gestur berbeda:
     * - Tap singkat (gerakan kecil) -> kirim perintah tap ke Accessibility Service
     * - Drag (gerakan signifikan)   -> pindahkan posisi tombol & simpan mapping baru
     */
    private fun attachDragAndTap(
        view: View,
        params: WindowManager.LayoutParams,
        mapping: ButtonMapping
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    v.alpha = 0.6f
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 15 || Math.abs(dy) > 15) {
                        moved = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(v, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.alpha = 1f
                    if (moved) {
                        // Simpan posisi baru sebagai koordinat target tap juga
                        mapping.x = params.x + mapping.radiusPx
                        mapping.y = params.y + mapping.radiusPx
                        val all = MappingStore.load(this)
                        val idx = all.indexOfFirst { it.id == mapping.id }
                        if (idx >= 0) all[idx] = mapping
                        MappingStore.save(this, all)
                    } else {
                        // Ini tap, bukan drag -> simulasikan tap ke game di bawah
                        TapAccessibilityService.instance?.performTap(
                            (params.x + mapping.radiusPx).toFloat(),
                            (params.y + mapping.radiusPx).toFloat()
                        )
                    }
                    true
                }
                else -> false
            }
        }
    }
}
