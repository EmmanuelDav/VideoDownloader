package com.cyberIyke.allvideodowloader.database;

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Download::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadsDao(): DownloadsDao
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val name = "Video_DL"

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    name
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
