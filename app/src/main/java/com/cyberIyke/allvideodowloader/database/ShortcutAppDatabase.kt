package com.cyberIyke.allvideodowloader.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ShortcutTable::class], version = 1, exportSchema = false)
abstract class ShortcutAppDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        private val LOCK: Any = Any()
        private val DATABASE_NAME: String = "VideoDownloaderData"
        private var sInstance: ShortcutAppDatabase? = null

        fun getInstance(context: Context): ShortcutAppDatabase? {
            if (sInstance == null) {
                synchronized(LOCK) {
                    sInstance = Room.databaseBuilder(
                        context.applicationContext,
                        ShortcutAppDatabase::class.java, DATABASE_NAME
                    )
                        .build()
                }
            }
            return sInstance
        }
    }
}