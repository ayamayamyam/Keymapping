package com.example.gamepadmapper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Satu tombol virtual: posisinya di layar (x, y) sekaligus jadi
 * koordinat target tap yang akan disimulasikan ke game di bawahnya.
 */
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

    // Beberapa tombol contoh saat pertama kali dijalankan (bisa digeser/diedit user)
    private fun defaultMappings() = mutableListOf(
        ButtonMapping("btn_a", "A", 900, 1600),
        ButtonMapping("btn_b", "B", 1050, 1500),
        ButtonMapping("btn_up", "↑", 200, 1500),
        ButtonMapping("btn_down", "↓", 200, 1700)
    )
}
