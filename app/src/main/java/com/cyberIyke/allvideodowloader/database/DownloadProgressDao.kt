package com.cyberIyke.allvideodowloader.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DownloadProgressDao {

    @Insert
    fun insert(item: DownloadProgress)

    @Update
    fun update(item: DownloadProgress)

    @Delete
    fun delete(item: DownloadProgress)

    @Query("SELECT * from download_progress WHERE taskId = :id")
    fun getById(id: Long): DownloadProgress

    @Query(/* value = */ "SELECT * from download_progress")
    fun getAllDownloads(): LiveData<List<DownloadProgress>>
}
