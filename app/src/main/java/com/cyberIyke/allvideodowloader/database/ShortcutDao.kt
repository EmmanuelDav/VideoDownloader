package com.cyberIyke.allvideodowloader.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cyberIyke.allvideodowloader.database.ShortcutTable

@Dao
interface ShortcutDao {
    @Insert
    fun insert(shortcutTable: ShortcutTable?)

    @Delete
    fun delete(shortcutTable: ShortcutTable?)

    @get:Query("SELECT * FROM ShortcutTable")
    val allShortcut: LiveData<List<ShortcutTable?>?>?

    @get:Query("SELECT * FROM ShortcutTable")
    val allShortcutList: List<ShortcutTable?>?
}