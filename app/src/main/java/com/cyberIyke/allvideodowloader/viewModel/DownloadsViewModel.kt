package com.cyberIyke.allvideodowloader.viewModel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.cyberIyke.allvideodowloader.database.*
import com.cyberIyke.allvideodowloader.work.DeleteWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DownloadsRepository
    private val progressRepo: DownloadProgressRepo
    val allDownloads: LiveData<List<Download>>
    val allProgress: LiveData<List<DownloadProgress>>
    val loadState: MutableLiveData<WorkInfo.State?> = MutableLiveData(WorkInfo.State.SUCCEEDED)

    init {
        val downloadsDao = AppDatabase.getDatabase(application).downloadsDao()
        val downloadProgress = AppDatabase.getDatabase(application).downloadProgressDao()
        repository = DownloadsRepository(downloadsDao)
        progressRepo = DownloadProgressRepo(downloadProgress)
        allDownloads = repository.allDownloads
        allProgress = progressRepo.allDownloads
    }

    fun insert(word: Download) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(word)
    }

    fun update(word: Download) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(word)
    }

    fun delete(word: Download) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(word)
    }

    fun startDelete(id: Long, context: Context) {
        val workTag = "tag_$id"
        val workManager = WorkManager.getInstance(context.applicationContext!!)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Use withContext to switch to a background thread for the blocking call
                val workInfos = withContext(Dispatchers.IO) {
                    workManager.getWorkInfosByTag(workTag).await()
                }

                val state = workInfos.firstOrNull()?.state

                if (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED) {
                    return@launch
                }

                val workData = workDataOf(
                    DeleteWorker.fileIdKey to id
                )

                val workRequest = OneTimeWorkRequestBuilder<DeleteWorker>()
                    .addTag(workTag)
                    .setInputData(workData)
                    .build()

                workManager.enqueueUniqueWork(
                    workTag,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )

            } catch (e: Exception) {
                // Handle exceptions
            }
        }
    }

    fun viewContent(path: String, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.parse(path)
        val downloadedFile = DocumentFile.fromSingleUri(context, uri)!!
        if (!downloadedFile.exists()) {
            Toast.makeText(context, "file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val mimeType = context.contentResolver.getType(uri) ?: "*/*"
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (intent.resolveActivity(context.packageManager) != null) {
            ContextCompat.startActivity(context, intent, null)
        } else {
            Toast.makeText(context, "app not found", Toast.LENGTH_SHORT).show()
        }
    }

}