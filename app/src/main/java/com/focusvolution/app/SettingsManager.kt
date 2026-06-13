package com.focusvolution.app

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEFAULT_DURATION = "default_duration_minutes"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_REMEMBER_LAST = "remember_last_duration"
        private const val KEY_LAST_MINUTES = "last_minutes"
        private const val KEY_LAST_SECONDS = "last_seconds"
        private const val DEFAULT_DURATION = 25
    }

    var defaultDurationMinutes: Int
        get() = prefs.getInt(KEY_DEFAULT_DURATION, DEFAULT_DURATION)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_DURATION, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var rememberLastDuration: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_LAST, false)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_LAST, value).apply()

    var lastMinutes: Int
        get() = prefs.getInt(KEY_LAST_MINUTES, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_MINUTES, value).apply()

    var lastSeconds: Int
        get() = prefs.getInt(KEY_LAST_SECONDS, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_SECONDS, value).apply()
}
