package com.cyberIyke.allvideodowloader.database;

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DownloadsDao {
    @Insert
     fun insert(item: Download)

    @Update
     fun update(item: Download)

    @Delete
     fun delete(item: Download)

    @Query("SELECT * from downloads_table WHERE id = :id")
    fun getById(id: Long): Download

    @Query("SELECT * from downloads_table ORDER BY timestamp DESC")
    fun getAllDownloads(): LiveData<List<Download>>
}