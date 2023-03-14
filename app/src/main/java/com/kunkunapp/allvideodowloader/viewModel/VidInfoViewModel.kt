package com.kunkunapp.allvideodowloader.viewModel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.kunkunapp.allvideodowloader.MyApp
import com.kunkunapp.allvideodowloader.activities.MainActivity
import com.kunkunapp.allvideodowloader.database.AppDatabase
import com.kunkunapp.allvideodowloader.database.Download
import com.kunkunapp.allvideodowloader.database.DownloadsRepository
import com.kunkunapp.allvideodowloader.model.VidInfoItem
import com.kunkunapp.allvideodowloader.work.DeleteWorker
import com.kunkunapp.allvideodowloader.work.DownloadWorker
import com.kunkunapp.allvideodowloader.work.YoutubeDLUpdateWorker
import com.kunkunapp.allvideodowloader.work.YoutubeDLUpdateWorker.Companion.workTag
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VidInfoViewModel(context: Application) : AndroidViewModel(context) {

    val vidFormats: MutableLiveData<VideoInfo?> = MutableLiveData()
    val loadState: MutableLiveData<LoadState> = MutableLiveData(LoadState.INITIAL)
    val thumbnail: MutableLiveData<String> = MutableLiveData()

    lateinit var selectedItem: VidInfoItem.VidFormatItem

    private fun submit(vidInfoItems: VideoInfo?) {
        vidFormats.postValue(vidInfoItems)
    }

    private fun updateLoading(loadState: LoadState) {
        this.loadState.postValue(loadState)
    }

    private fun updateThumbnail(thumbnail: String?) {
        this.thumbnail.postValue(thumbnail)
    }

    fun fetchInfo(url: String) {
        viewModelScope.launch {
            updateLoading(LoadState.LOADING)
            submit(null)
            updateThumbnail(null)
            lateinit var vidInfo: VideoInfo
            Log.d(MyApp.TAG, "fetchInfo: In Progress")
            try {
                withContext(Dispatchers.IO) {
                    vidInfo = YoutubeDL.getInstance().getInfo(url)
                }
            } catch (e: Exception) {
                updateLoading(LoadState.FAILED)
                Log.d(MyApp.TAG, "fetchInfo: Failed " + e.message + "  " + e.cause + "  " + e.localizedMessage)
                return@launch
            }
            updateLoading(LoadState.LOADED)
            updateThumbnail(vidInfo.thumbnail)
            submit(vidInfo)
        }
    }


     fun startDownload(vidFormatItem: VidInfoItem.VidFormatItem, downloadDir: String, activity:Activity) {
        val vidInfo = vidFormatItem.vidInfo
        val vidFormat = vidFormatItem.vidFormat
        val workTag = vidInfo.id
        val workManager = WorkManager.getInstance(activity.applicationContext!!)
        val state = workManager.getWorkInfosByTag(workTag).get()?.getOrNull(0)?.state

        val running = state === WorkInfo.State.RUNNING || state === WorkInfo.State.ENQUEUED
        if (running) {
            Toast.makeText(
                activity,
                "download_already_running",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val workData = workDataOf(
            DownloadWorker.urlKey to vidInfo.webpageUrl,
            DownloadWorker.nameKey to vidInfo.title,
            DownloadWorker.formatIdKey to vidFormat.formatId,
            DownloadWorker.acodecKey to vidFormat.acodec,
            DownloadWorker.vcodecKey to vidFormat.vcodec,
            DownloadWorker.downloadDirKey to downloadDir,
            DownloadWorker.sizeKey to vidFormat.fileSize,
            DownloadWorker.taskIdKey to vidInfo.id
        )

//         CoroutineScope(Dispatchers.IO).launch {
//             val downloadsDao = AppDatabase.getDatabase(activity).downloadsDao()
//             val repository = DownloadsRepository(downloadsDao)
//             val download = Download(vidInfo.title,vidInfo.duration.toLong(),vidInfo.fileSizeApproximate)
//             download.downloadedPath = downloadDir
//             download.id = vidInfo.id.toLong()
//             download.mediaType= if (vidFormat.vcodec == "none" && vidFormat.acodec != "none") "audio" else "video"
//             download.downloadedPercent = 0.0
//             repository.insert(download)
//         }

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag(workTag)
            .setInputData(workData)
            .build()

        workManager.enqueueUniqueWork(
            workTag,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        Toast.makeText(
            activity,
            "download_queued",
            Toast.LENGTH_LONG
        ).show()
    }
}

enum class LoadState {
    INITIAL, LOADING, LOADED, FAILED
}
