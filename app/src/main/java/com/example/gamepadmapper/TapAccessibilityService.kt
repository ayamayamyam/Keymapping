package com.example.gamepadmapper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.util.Log

/**
 * Service ini tidak butuh membaca isi layar (canRetrieveWindowContent = false),
 * tugasnya cuma satu: menerima perintah "tap di koordinat (x,y)" dan
 * mengeksekusinya lewat dispatchGesture, seolah-olah jari user menyentuh
 * layar di titik itu.
 *
 * Instance aktifnya disimpan di companion object supaya OverlayService
 * (komponen lain) bisa memanggilnya langsung.
 */
class TapAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TapAccessibilityService"
        var instance: TapAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service aktif")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Tidak perlu memproses event apa pun untuk fitur ini.
    }

    override fun onInterrupt() {}

    /**
     * Simulasikan satu ketukan (tap) sesaat di koordinat (x, y).
     * durationMs sengaja singkat supaya terasa seperti tap biasa, bukan hold.
     */
    fun performTap(x: Float, y: Float, durationMs: Long = 50) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, null, null)
    }
}
