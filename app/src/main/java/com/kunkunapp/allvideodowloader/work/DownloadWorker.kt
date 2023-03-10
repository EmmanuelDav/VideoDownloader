package com.kunkunapp.allvideodowloader.work;

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import org.apache.commons.io.IOUtils
import androidx.work.WorkerParameters
import com.kunkunapp.allvideodowloader.R
import com.kunkunapp.allvideodowloader.database.AppDatabase
import com.kunkunapp.allvideodowloader.database.Download
import com.kunkunapp.allvideodowloader.database.DownloadsRepository
import com.kunkunapp.allvideodowloader.utils.FileNameUtils
import com.kunkunapp.allvideodowloader.viewModel.DownloadState
import com.kunkunapp.allvideodowloader.viewModel.DownloadsViewModel
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.*

private const val TAG = "DownloadWorker"
class DownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    var downloadsViewModel = DownloadsViewModel(appContext as Application)

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager?


    override suspend fun doWork(): Result {
        val url = inputData.getString(urlKey)!!
        val name = FileNameUtils.createFilename(inputData.getString(nameKey)!!)
        val formatId = inputData.getString(formatIdKey)!!
        val acodec = inputData.getString(acodecKey)
        val vcodec = inputData.getString(vcodecKey)
        val downloadDir = inputData.getString(downloadDirKey)!!
        val size = inputData.getLong(sizeKey, 0L)
        val taskId = inputData.getString(taskIdKey)!!
        downloadsViewModel.updateLoading(DownloadState.INIT)

        createNotificationChannel()
        val notificationId = id.hashCode()
        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setSmallIcon(R.mipmap.ic_logo)
            .setContentTitle(name)
            .setContentText(applicationContext.getString(R.string.download_start))
            .build()
        downloadsViewModel.updateLoading(DownloadState.DOWNLOADING)

        val foregroundInfo = ForegroundInfo(notificationId, notification)
        setForeground(foregroundInfo)

        val request = YoutubeDLRequest(url)
        val tmpFile = File.createTempFile("Video_DL", null, applicationContext.externalCacheDir)
        tmpFile.delete()
        tmpFile.mkdir()
        tmpFile.deleteOnExit()
        request.addOption("-o", "${tmpFile.absolutePath}/${name}.%(ext)s")
        val videoOnly = vcodec != "none" && acodec == "none"
        if (videoOnly) {
            request.addOption("-f", "${formatId}+bestaudio")
        } else {
            request.addOption("-f", formatId)
        }

        var destUri: Uri? = null

        try {
            YoutubeDL.getInstance().execute(request, taskId) { progress, _, line ->
                    showProgress(id.hashCode(), taskId, name, progress.toInt(), line, tmpFile)
                }
            val treeUri = Uri.parse(downloadDir)
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val destDir = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            tmpFile.listFiles().forEach {
                val mimeType =
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.extension) ?: "*/*"
                destUri = DocumentsContract.createDocument(
                    applicationContext.contentResolver,
                    destDir,
                    mimeType,
                    it.name
                )
                val ins = it.inputStream()
                val ops = applicationContext.contentResolver.openOutputStream(destUri!!)
                IOUtils.copy(ins, ops)
                IOUtils.closeQuietly(ops)
                IOUtils.closeQuietly(ins)
            }
        } finally {
            tmpFile.deleteRecursively()
        }

        val downloadsDao = AppDatabase.getDatabase(
            applicationContext
        ).downloadsDao()
        val repository =
            DownloadsRepository(downloadsDao)
        val download =
            Download(name, Date().time, size)
        download.downloadedPath = destUri.toString()
        download.downloadedPercent = 100.00
        download.downloadedSize = size
        download.mediaType = if (vcodec == "none" && acodec != "none") "audio" else "video"
        repository.insert(download)

        return Result.success()
    }

    private fun showProgress(
        id: Int,
        taskId: String,
        name: String,
        progress: Int,
        line: String,
        tmpFile: File
    ) {
        val text = line.replace(tmpFile.toString(), "")
        val intent = Intent(applicationContext, CancelReceiver::class.java)
            .putExtra("taskId", taskId)
            .putExtra("notificationId", id)

        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE )
        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.mipmap.ic_logo)
            .setContentTitle(name)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setProgress(100, progress, progress == -1)
            .addAction(
                R.drawable.ic_baseline_stop_24,
                applicationContext.getString(R.string.cancel_download),
                pendingIntent
            )
            .build()
        notificationManager?.notify(id, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var notificationChannel =
                notificationManager?.getNotificationChannel(channelId)
            if (notificationChannel == null) {
                val channelName = applicationContext.getString(R.string.download_noti_channel_name)
                notificationChannel = NotificationChannel(
                    channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW
                )
                notificationChannel.description =
                    channelName
                notificationManager?.createNotificationChannel(notificationChannel)
            }
        }
    }

    companion object {
        private const val channelId = "dvd_download"
        const val urlKey = "url"
        const val nameKey = "name"
        const val formatIdKey = "formatId"
        const val acodecKey = "acodec"
        const val vcodecKey = "vcodec"
        const val downloadDirKey = "downloadDir"
        const val sizeKey = "size"
        const val taskIdKey = "taskId"
    }
}

