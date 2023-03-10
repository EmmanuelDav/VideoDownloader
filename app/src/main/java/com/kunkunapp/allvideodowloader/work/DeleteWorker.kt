package com.kunkunapp.allvideodowloader.work;

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kunkunapp.allvideodowloader.database.AppDatabase
import com.kunkunapp.allvideodowloader.database.DownloadsRepository
import com.kunkunapp.allvideodowloader.viewModel.DownloadState
import com.kunkunapp.allvideodowloader.viewModel.DownloadsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    var downloadsViewModel = DownloadsViewModel(appContext as Application)

    override suspend fun doWork(): Result {
        val fileId = inputData.getLong(fileIdKey, 0)

        val downloadsDao = AppDatabase.getDatabase(applicationContext).downloadsDao()
        val repository = DownloadsRepository(downloadsDao)
        val download = downloadsDao.getById(fileId)

        val fileName = download.name
        val fileUri = download.downloadedPath

        repository.delete(download)

        val file = DocumentFile.fromSingleUri(applicationContext, Uri.parse(fileUri))!!
        if (file.exists()) {
            file.delete()
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Deleted $fileName", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        downloadsViewModel.updateLoading(DownloadState.CANCELED)

        return Result.success()
    }

    companion object {
        const val fileIdKey = "id"
    }

}