package com.cyberIyke.allvideodowloader.helper

import android.content.Context
import android.content.SharedPreferences

class RenameVideoPref constructor(context: Context) {
    var editor: SharedPreferences.Editor
    var preferences: SharedPreferences

    init {
        preferences = context.getSharedPreferences("RenameVideoPref", 0)
        editor = preferences.edit()
    }

    fun setString(key: String?, value: String?) {
        editor.putString(key, value).commit()
    }

    fun getString(key: String?, defValue: String?): String? {
        return preferences.getString(key, defValue)
    }
}