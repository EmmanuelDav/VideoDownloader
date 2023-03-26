package com.cyberIyke.allvideodowloader.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class ThemeSettings private constructor(context: Context) {

    private object Key {
        const val NIGHT_MODE = "nightMode"
    }

    @JvmField
    var nightMode: Boolean
    fun save(context: Context) {
        val editor = context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
        editor.putBoolean(Key.NIGHT_MODE, nightMode)
        editor.apply()
    }

    fun refreshTheme() {
        AppCompatDelegate.setDefaultNightMode(if (nightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    companion object {
        private var instance: ThemeSettings? = null
        @JvmStatic
        fun getInstance(context: Context): ThemeSettings? {
            if (instance == null) instance = ThemeSettings(context)
            return instance
        }
    }

    init {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        nightMode = prefs.getBoolean(Key.NIGHT_MODE, false)
    }

}