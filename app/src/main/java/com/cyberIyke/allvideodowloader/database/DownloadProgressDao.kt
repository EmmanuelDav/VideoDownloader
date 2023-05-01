package com.cyberIyke.allvideodowloader.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DownloadProgressDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(item: DownloadProgress)

    @Update
    fun update(item: DownloadProgress)

    @Delete
    fun delete(item: DownloadProgress)

    @Query(/* value = */ "SELECT * from download_progress")
    fun getAllDownloads(): LiveData<List<DownloadProgress>>
}
