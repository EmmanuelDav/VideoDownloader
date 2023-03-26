package com.cyberIyke.allvideodowloader.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ShortcutDao {
    @Insert
    void insert(ShortcutTable shortcutTable);

    @Delete
    void delete(ShortcutTable shortcutTable);

    @Query("SELECT * FROM ShortcutTable")
    LiveData<List<ShortcutTable>> getAllShortcut();

    @Query("SELECT * FROM ShortcutTable")
    List<ShortcutTable> getAllShortcutList();
}
