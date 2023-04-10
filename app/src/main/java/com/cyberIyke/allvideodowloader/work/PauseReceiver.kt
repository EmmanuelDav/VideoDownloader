package com.cyberIyke.allvideodowloader.work

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.cyberIyke.allvideodowloader.MyApp.Companion.TAG
import com.cyberIyke.allvideodowloader.R
import com.yausername.youtubedl_android.YoutubeDL


class PauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getStringExtra("taskId")
        val notificationId = intent.getIntExtra("notificationId", 0)
        val isPaused = intent.getBooleanExtra("isPaused", false)
        if (taskId.isNullOrEmpty()) return
        val youtubeDL = YoutubeDL.getInstance()
        if (isPaused) {
            youtubeDL.destroyProcessById(taskId)
        } else {
            val downloadUrl = intent.getStringExtra("downloadUrl")
            val outputFilePath = intent.getStringExtra("outputFilePath")
            if (downloadUrl.isNullOrEmpty() || outputFilePath.isNullOrEmpty()) return
            val newInputData = workDataOf(
                "downloadUrl" to downloadUrl,
                "outputFilePath" to outputFilePath
            )
            val newWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(newInputData)
                .addTag(taskId)
                .build()
            WorkManager.getInstance(context!!).enqueueUniqueWork(
                taskId,
                ExistingWorkPolicy.REPLACE,
                newWorkRequest
            )
        }
        val notificationManager =
            context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (isPaused) {
            Toast.makeText(context, R.string.download_paused, Toast.LENGTH_LONG).show()
            notificationManager?.cancel(notificationId)
        } else {
            Toast.makeText(context, R.string.download_resumed, Toast.LENGTH_LONG).show()
//            notificationManager?.notify(
//                //notificationId, createNotification(context, taskId, downloadUrl, outputFilePath)
//            )
        }
    }

}

