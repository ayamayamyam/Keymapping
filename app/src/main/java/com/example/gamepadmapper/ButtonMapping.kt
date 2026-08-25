package com.example.gamepadmapper

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class ButtonMapping(
    val id: String,
    val label: String,
    var x: Int,
    var y: Int,
    val radiusPx: Int = 70
)

object MappingStore {
    private const val PREFS = "gamepad_mapper_prefs"
    private const val KEY_MAPPINGS = "mappings"

    fun load(context: Context): MutableList<ButtonMapping> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_MAPPINGS, null) ?: return defaultMappings()
        val arr = JSONArray(raw)
        val result = mutableListOf<ButtonMapping>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            result.add(
                ButtonMapping(
                    id = obj.getString("id"),
                    label = obj.getString("label"),
                    x = obj.getInt("x"),
                    y = obj.getInt("y"),
                    radiusPx = obj.optInt("radiusPx", 70)
                )
            )
        }
        return result
    }

    fun save(context: Context, mappings: List<ButtonMapping>) {
        val arr = JSONArray()
        mappings.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("label", it.label)
            obj.put("x", it.x)
            obj.put("y", it.y)
            obj.put("radiusPx", it.radiusPx)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MAPPINGS, arr.toString())
            .apply()
    }

    /** Tambah tombol baru dengan posisi default di tengah layar, lalu simpan. */
    fun addMapping(context: Context, label: String) {
        val current = load(context)
        val newMapping = ButtonMapping(
            id = "btn_${System.currentTimeMillis()}",
            label = label,
            x = 500,
            y = 900
        )
        current.add(newMapping)
        save(context, current)
    }

    /** Hapus satu tombol berdasarkan id, lalu simpan. */
    fun removeMapping(context: Context, id: String) {
        val current = load(context)
        current.removeAll { it.id == id }
        save(context, current)
    }

    /** Kembalikan seluruh tombol ke posisi & daftar bawaan (menghapus kustomisasi user). */
    fun resetToDefault(context: Context) {
        save(context, defaultMappings())
    }

    /**
     * Daftarkan listener yang dipanggil setiap kali daftar mapping berubah
     * (tambah/hapus/geser/reset) — dari komponen manapun, termasuk activity lain.
     * Dipakai OverlayService supaya tombol yang sedang tampil ikut ter-refresh
     * tanpa perlu mematikan-menyalakan ulang overlay secara manual.
     */
    fun registerChangeListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun defaultMappings() = mutableListOf(
        ButtonMapping("btn_a", "A", 900, 1600),
        ButtonMapping("btn_b", "B", 1050, 1500),
        ButtonMapping("btn_up", "↑", 200, 1500),
        ButtonMapping("btn_down", "↓", 200, 1700)
    )
}
