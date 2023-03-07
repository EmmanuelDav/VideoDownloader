package com.kunkunapp.allvideodowloader.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ShortcutTable.class}, version = 1, exportSchema = false)
public abstract class ShortcutAppDatabase extends RoomDatabase {
    private static final Object LOCK = new Object();
    private static final String DATABASE_NAME = "VideoDownloaderData";
    private static ShortcutAppDatabase sInstance;

    public static ShortcutAppDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                sInstance = Room.databaseBuilder(context.getApplicationContext(),
                                ShortcutAppDatabase.class, ShortcutAppDatabase.DATABASE_NAME)
                        .build();
            }
        }
        return sInstance;
    }

    public abstract ShortcutDao shortcutDao();
}
