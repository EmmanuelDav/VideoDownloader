package com.cyberIyke.allvideodowloader.databaseimport

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ShortcutTable::class], version = 1, exportSchema = false)
abstract class ShortcutAppDatabase constructor() : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao?

    companion object {
        private val LOCK: Any = Any()
        private val DATABASE_NAME: String = "VideoDownloaderData"
        private val sInstance: ShortcutAppDatabase? = null
        fun getInstance(context: Context): ShortcutAppDatabase {
            if (sInstance == null) {
                synchronized(ShortcutAppDatabase.Companion.LOCK) {
                    sInstance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        ShortcutAppDatabase::class.java, ShortcutAppDatabase.Companion.DATABASE_NAME
                    )
                        .build()
                }
            }
            return sInstance
        }
    }
}