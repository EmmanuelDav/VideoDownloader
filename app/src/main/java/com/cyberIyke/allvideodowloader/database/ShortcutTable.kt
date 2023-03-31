package com.cyberIyke.allvideodowloader.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ShortcutTable")
class ShortcutTable(var imgLogo: Int, var strTitle: String, var strURL: String) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}