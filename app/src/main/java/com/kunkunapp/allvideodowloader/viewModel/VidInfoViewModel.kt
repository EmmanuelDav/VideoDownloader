package com.kunkunapp.allvideodowloader.viewModel

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.kunkunapp.allvideodowloader.MyApp
import com.kunkunapp.allvideodowloader.work.DownloadWorker
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VidInfoViewModel : ViewModel() {

    val vidFormats: MutableLiveData<VideoInfo> = MutableLiveData()
    val loadState: MutableLiveData<LoadState> = MutableLiveData(LoadState.INITIAL)
    val thumbnail: MutableLiveData<String> = MutableLiveData()
    lateinit var selectedItem:  VideoInfo
    lateinit var selectedView:  VideoFormat

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


     fun startDownload(vidFormatItem: VideoInfo, videoInfo : VideoFormat, downloadDir: String, activity:Activity) {
         val workTag = vidFormatItem.id
         val workManager = WorkManager.getInstance(activity?.applicationContext!!)
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
             DownloadWorker.urlKey to vidFormatItem.webpageUrl,
             DownloadWorker.nameKey to vidFormatItem.title,
             DownloadWorker.formatIdKey to vidFormatItem.formatId,
             DownloadWorker.acodecKey to videoInfo.acodec,
             DownloadWorker.vcodecKey to videoInfo.vcodec,
             DownloadWorker.downloadDirKey to downloadDir,
             DownloadWorker.sizeKey to vidFormatItem.fileSize,
             DownloadWorker.taskIdKey to vidFormatItem.id
         )
         val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
             .addTag(workTag)
             .setInputData(workData)
             .build()
         workManager.enqueueUniqueWork(workTag, ExistingWorkPolicy.KEEP, workRequest)
     }


}

enum class LoadState {
    INITIAL, LOADING, LOADED, FAILED
}
