package com.cyberIyke.allvideodowloader.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyberIyke.allvideodowloader.model.DownloadInfo


@Entity(tableName = "download_progress")
class DownloadProgress (var thumbnail: String,
                        @PrimaryKey
                        var taskId: String,
                        var name: String,
                        var progress: Int,
                        var line: String)
