package com.cyberIyke.allvideodowloader.interfaces

import com.cyberIyke.allvideodowloader.database.ShortcutTable

open interface ShortcutListner {
    fun shortcutClick(shortcutTable: ShortcutTable?)
    fun shortcutRemoveClick(shortcutTable: ShortcutTable?)
}