package com.example.gamepadmapper

import android.app.NotificationChannel
import android.app.NotificationManager
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
 * Menampilkan tombol-tombol virtual, ditambah 1 tombol kontrol khusus
 * untuk toggle antara Play Mode (tap = kirim tap ke game) dan
 * Edit Mode (tap diabaikan, tahan-lama = hapus tombol, geser tetap bisa).
 */
class OverlayService : Service() {

    companion object {
        // true = Edit Mode aktif, false = Play Mode
        var editMode = false
    }

    private lateinit var windowManager: WindowManager
    private val overlayViews = mutableListOf<View>()
    private val NOTIF_CHANNEL_ID = "overlay_service_channel"
    private val NOTIF_ID = 1001

    override fun onCreate() {
        super.onCreate()
        editMode = false
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        drawControlButton()
        drawButtons()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

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
        val notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Gamepad Mapper aktif")
            .setContentText("Tombol virtual sedang ditampilkan")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    /** Tombol kecil di pojok kiri atas untuk toggle Edit Mode / Play Mode. */
    private fun drawControlButton() {
        val controlView = TextView(this).apply {
            text = "🔓 EDIT"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC1565C0"))
            gravity = Gravity.CENTER
            textSize = 12f
        }

        val params = WindowManager.LayoutParams(
            220, 100,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 100
        }

        controlView.setOnClickListener {
            editMode = !editMode
            controlView.text = if (editMode) "🔒 PLAY" else "🔓 EDIT"
            updateButtonsVisualState()
        }

        windowManager.addView(controlView, params)
        overlayViews.add(controlView)
    }

    private fun updateButtonsVisualState() {
        // Tombol jadi agak transparan saat Edit Mode, sebagai penanda visual.
        overlayViews.forEach { view ->
            if (view is TextView && view.text != "🔓 EDIT" && view.text != "🔒 PLAY") {
                view.alpha = if (editMode) 0.5f else 1f
            }
        }
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

            attachDragTapAndDelete(buttonView, params, mapping)

            windowManager.addView(buttonView, params)
            overlayViews.add(buttonView)
        }
    }

    private fun attachDragTapAndDelete(
        view: View,
        params: WindowManager.LayoutParams,
        mapping: ButtonMapping
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false
        var downTime = 0L

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    downTime = System.currentTimeMillis()
                    v.alpha = if (editMode) 0.3f else 0.6f
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
                    v.alpha = if (editMode) 0.5f else 1f
                    val heldDuration = System.currentTimeMillis() - downTime

                    when {
                        moved -> {
                            // Drag -> simpan posisi baru
                            mapping.x = params.x + mapping.radiusPx
                            mapping.y = params.y + mapping.radiusPx
                            val all = MappingStore.load(this)
                            val idx = all.indexOfFirst { it.id == mapping.id }
                            if (idx >= 0) all[idx] = mapping
                            MappingStore.save(this, all)
                        }
                        editMode && heldDuration >= 600 -> {
                            // Tahan lama di Edit Mode -> hapus tombol ini
                            MappingStore.removeMapping(this, mapping.id)
                            runCatching { windowManager.removeView(v) }
                            overlayViews.remove(v)
                        }
                        !editMode && !moved -> {
                            // Tap singkat di Play Mode -> kirim tap ke game
                            TapAccessibilityService.instance?.performTap(
                                (params.x + mapping.radiusPx).toFloat(),
                                (params.y + mapping.radiusPx).toFloat()
                            )
                        }
                        // Tap singkat di Edit Mode -> sengaja diabaikan (tidak ada aksi)
                    }
                    true
                }
                else -> false
            }
        }
    }
}
