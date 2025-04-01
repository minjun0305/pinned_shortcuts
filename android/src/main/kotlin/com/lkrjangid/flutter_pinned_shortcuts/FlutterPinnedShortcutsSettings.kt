package com.lkrjangid.flutter_pinned_shortcuts

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject

/**
 * Helper class to manage the plugin settings and store information about created shortcuts
 */
class FlutterPinnedShortcutsSettings(context: Context) {
    companion object {
        private const val TAG = "ShortcutsSettings"
        private const val PREFS_NAME = "flutter_pinned_shortcuts"
        private const val KEY_SHORTCUTS = "shortcuts"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Save information about a created shortcut
     */
    fun saveShortcut(id: String, label: String, longLabel: String, extraData: String?) {
        val shortcuts = getShortcuts()

        // Create shortcut data
        val shortcutData = JSONObject().apply {
            put("id", id)
            put("label", label)
            put("longLabel", longLabel)
            if (extraData != null) {
                put("extraData", extraData)
            }
            put("createdAt", System.currentTimeMillis())
        }

        // Add to shortcuts data
        shortcuts.put(id, shortcutData)

        // Save updated shortcuts
        prefs.edit()
                .putString(KEY_SHORTCUTS, shortcuts.toString())
                .apply()

        Log.d(TAG, "Saved shortcut: $id")
    }

    /**
     * Check if we have information about a shortcut
     */
    fun hasShortcut(id: String): Boolean {
        val shortcuts = getShortcuts()
        val exists = shortcuts.has(id)
        Log.d(TAG, "Checking if shortcut exists: $id, result: $exists")
        return exists
    }

    /**
     * Get information about all shortcuts
     */
    fun getShortcuts(): JSONObject {
        val shortcutsString = prefs.getString(KEY_SHORTCUTS, "{}")
        return try {
            JSONObject(shortcutsString!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing shortcuts JSON: ${e.message}")
            JSONObject()
        }
    }

    /**
     * Get information about a specific shortcut
     */
    fun getShortcut(id: String): JSONObject? {
        val shortcuts = getShortcuts()
        return if (shortcuts.has(id)) {
            shortcuts.getJSONObject(id)
        } else {
            Log.w(TAG, "Shortcut not found: $id")
            null
        }
    }

    /**
     * Clear all shortcut information
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cleared all shortcut data")
    }
}