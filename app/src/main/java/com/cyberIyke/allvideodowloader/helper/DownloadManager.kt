package com.cyberIyke.allvideodowloader.helper

import android.app.IntentService
import android.content.Intent
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.os.Environment
import android.util.Log
import com.cyberIyke.allvideodowloader.MyApp
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.helper.CompletedVideos
import java.io.*
import java.net.URL
import java.net.URLConnection
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel

class DownloadManager() : IntentService("DownloadManager") {
    override fun onHandleIntent(intent: Intent?) {
        stop = false
        downloadThread = Thread.currentThread()
        if (intent != null) {
            DownloadManager.Companion.chunked =
                intent.getBooleanExtra(CHUNCKED, false)
            if (chunked) {
                downloadFile = null
                DownloadManager.Companion.prevDownloaded = 0
                DownloadManager.Companion.downloadSpeed = 0
                DownloadManager.Companion.totalSize = 0
                handleChunkedDownload(intent)
            } else {
                DownloadManager.Companion.prevDownloaded = 0
                val connection: URLConnection?
                try {
                    DownloadManager.Companion.totalSize = intent.getStringExtra("size")!!
                        .toLong()
                    connection = (URL(intent.getStringExtra("link"))).openConnection()
                    val filename =
                        intent.getStringExtra("name") + "." + intent.getStringExtra("type")
                    val directory =
                        Environment.getExternalStoragePublicDirectory(getString(R.string.app_name))
                    val directotryExists: Boolean
                    directotryExists = directory.exists() || directory.mkdir() || directory
                        .createNewFile()
                    if (directotryExists) {
                        DownloadManager.Companion.downloadFile = File(
                            Environment.getExternalStoragePublicDirectory(getString(R.string.app_name)),
                            filename
                        )
                        if (connection != null) {
                            var out: FileOutputStream? = null
                            if (DownloadManager.Companion.downloadFile!!.exists()) {
                                DownloadManager.Companion.prevDownloaded =
                                    DownloadManager.Companion.downloadFile!!.length()
                                connection.setRequestProperty(
                                    "Range",
                                    "bytes=" + DownloadManager.Companion.downloadFile!!.length() + "-"
                                )
                                connection.connect()
                                out = FileOutputStream(
                                    Environment.getExternalStoragePublicDirectory(getString(R.string.app_name))
                                        .toString() + "/" + filename, true
                                )
                            } else {
                                connection.connect()
                                if (DownloadManager.Companion.downloadFile!!.createNewFile()) {
                                    out = FileOutputStream(
                                        DownloadManager.Companion.downloadFile!!.getAbsolutePath(),
                                        true
                                    )
                                }
                            }
                            if (out != null && DownloadManager.Companion.downloadFile!!.exists()) {
                                val `in` = connection.getInputStream()
                                var fileChannel: FileChannel
                                Channels.newChannel(`in`).use { readableByteChannel ->
                                    fileChannel = out.getChannel()
                                    while (DownloadManager.Companion.downloadFile!!.length() < DownloadManager.Companion.totalSize) {
                                        if (stop) return
                                        fileChannel.transferFrom(readableByteChannel, 0, 1024)
                                    }
                                }
                                `in`.close()
                                out.flush()
                                out.close()
                                fileChannel.close()
                                downloadFinished(filename)
                            }
                        }
                    }
                } catch (e: FileNotFoundException) {
                    linkNotFound(intent)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun downloadFinished(filename: String) {
        if (DownloadManager.Companion.onDownloadFinishedListener != null) {
            DownloadManager.Companion.onDownloadFinishedListener!!.onDownloadFinished()
        } else {
            val queues: DownloadQueues? = DownloadQueues.Companion.load(applicationContext)
            queues!!.deleteTopVideo(applicationContext)
            val completedVideos: CompletedVideos? = CompletedVideos.Companion.load(
                applicationContext
            )
            completedVideos!!.addVideo(applicationContext, filename)
            val topVideo = queues.topVideo
            if (topVideo != null) {
                val downloadService = MyApp.getInstance()!!.getDownloadService()
                downloadService!!.putExtra("link", topVideo.link)
                downloadService.putExtra("name", topVideo.name)
                downloadService.putExtra("type", topVideo.type)
                downloadService.putExtra("size", topVideo.size)
                downloadService.putExtra("page", topVideo.page)
                downloadService.putExtra(CHUNCKED, topVideo.chunked)
                downloadService.putExtra(DownloadManager.Companion.WEBSITE, topVideo.website)
                onHandleIntent(downloadService)
            }
        }
    }

    private fun linkNotFound(intent: Intent) {
        if (DownloadManager.Companion.onLinkNotFoundListener != null) {
            DownloadManager.Companion.onLinkNotFoundListener!!.onLinkNotFound()
        } else {
            val queues: DownloadQueues? = DownloadQueues.load(applicationContext)
            queues!!.deleteTopVideo(applicationContext)
            val inactiveDownload = DownloadVideo()
            inactiveDownload.name = intent.getStringExtra("name")
            inactiveDownload.link = intent.getStringExtra("link")
            inactiveDownload.type = intent.getStringExtra("type")
            inactiveDownload.size = intent.getStringExtra("size")
            inactiveDownload.page = intent.getStringExtra("page")
            inactiveDownload.website = intent.getStringExtra(DownloadManager.Companion.WEBSITE)
            inactiveDownload.chunked =
                intent.getBooleanExtra(CHUNCKED, false)
            val inactiveDownloads: InactiveDownloads? = InactiveDownloads.Companion.load(
                applicationContext
            )
            inactiveDownloads!!.add(applicationContext, inactiveDownload)
            val topVideo = queues!!.topVideo
            if (topVideo != null) {
                val downloadService = MyApp.getInstance()!!.getDownloadService()
                downloadService!!.putExtra("link", topVideo.link)
                downloadService.putExtra("name", topVideo.name)
                downloadService.putExtra("type", topVideo.type)
                downloadService.putExtra("size", topVideo.size)
                downloadService.putExtra("page", topVideo.page)
                downloadService.putExtra(CHUNCKED, topVideo.chunked)
                downloadService.putExtra(DownloadManager.Companion.WEBSITE, topVideo.website)
                onHandleIntent(downloadService)
            }
        }
    }

    private fun handleChunkedDownload(intent: Intent) {
        try {
            val name = intent.getStringExtra("name")
            val type = intent.getStringExtra("type")
            val directory =
                Environment.getExternalStoragePublicDirectory(getString(R.string.app_name))
            val directotryExists: Boolean = directory.exists() || directory.mkdir() || directory
                .createNewFile()
            if (directotryExists) {
                val progressFile = File(cacheDir, "$name.dat")
                val videoFile = File(
                    Environment.getExternalStoragePublicDirectory(getString(R.string.app_name)),
                    "$name.$type"
                )
                var totalChunks: Long = 0
                if (progressFile.exists()) {
                    val `in` = FileInputStream(progressFile)
                    val data = DataInputStream(`in`)
                    totalChunks = data.readLong()
                    data.close()
                    `in`.close()
                } else if (videoFile.exists()) {
                    downloadFinished("$name.$type")
                }
                if (videoFile.exists() && progressFile.exists()) {
                    while (true) {
                        DownloadManager.Companion.prevDownloaded = 0
                        val website = intent.getStringExtra(DownloadManager.Companion.WEBSITE)
                        var chunkUrl: String? = null
                        when (website) {
                            "dailymotion.com" -> chunkUrl =
                                getNextChunkWithDailymotionRule(intent, totalChunks)
                            "vimeo.com" -> chunkUrl = getNextChunkWithVimeoRule(intent, totalChunks)
                            DownloadManager.Companion.TWITTER, DownloadManager.Companion.METACAFE, DownloadManager.Companion.MYSPACE -> chunkUrl =
                                getNextChunkWithM3U8Rule(intent, totalChunks)
                            else -> {}
                        }
                        if (chunkUrl == null) {
                            downloadFinished("$name.$type")
                        }
                        DownloadManager.bytesOfChunk = ByteArrayOutputStream()
                        try {
                            val uCon = URL(chunkUrl).openConnection()
                            if (uCon != null) {
                                val `in` = uCon.getInputStream()
                                Channels.newChannel(`in`).use { readableByteChannel ->
                                    Channels.newChannel(
                                        DownloadManager.Companion.bytesOfChunk
                                    ).use { writableByteChannel ->
                                        var read: Int
                                        while (true) {
                                            if (stop) return
                                            val buffer: ByteBuffer = ByteBuffer.allocateDirect(1024)
                                            read = readableByteChannel.read(buffer)
                                            if (read != -1) {
                                                buffer.flip()
                                                writableByteChannel.write(buffer)
                                            } else {
                                                FileOutputStream(videoFile, true).use { vAddChunk ->
                                                    vAddChunk.write(
                                                        DownloadManager.Companion.bytesOfChunk!!.toByteArray()
                                                    )
                                                }
                                                val outputStream: FileOutputStream =
                                                    FileOutputStream(progressFile, false)
                                                DataOutputStream(outputStream).use { dataOutputStream ->
                                                    dataOutputStream.writeLong(
                                                        ++totalChunks
                                                    )
                                                }
                                                outputStream.close()
                                                break
                                            }
                                        }
                                    }
                                }
                                `in`.close()
                                DownloadManager.Companion.bytesOfChunk!!.close()
                            }
                        } catch (e: FileNotFoundException) {
                            downloadFinished("$name.$type")
                            break
                        } catch (e: IOException) {
                            e.printStackTrace()
                            break
                        }
                    }
                }
                MediaScannerConnection.scanFile(
                    applicationContext,
                    arrayOf(videoFile.absolutePath),
                    null,
                    OnScanCompletedListener { path, uri ->
                        //nada
                    })
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getNextChunkWithDailymotionRule(intent: Intent, totalChunks: Long): String {
        val link = intent.getStringExtra("link")
        return link!!.replace("FRAGMENT", "frag(" + (totalChunks + 1) + ")")
    }

    private fun getNextChunkWithVimeoRule(intent: Intent, totalChunks: Long): String {
        val link = intent.getStringExtra("link")
        return link!!.replace("SEGMENT", "segment-" + (totalChunks + 1))
    }

    private fun getNextChunkWithM3U8Rule(intent: Intent, totalChunks: Long): String? {
        val link = intent.getStringExtra("link")
        val website = intent.getStringExtra(DownloadManager.Companion.WEBSITE)
        var line: String? = null
        try {
            val m3u8Con = URL(link).openConnection()
            val `in` = m3u8Con.getInputStream()
            val inReader = InputStreamReader(`in`)
            val buffReader = BufferedReader(inReader)
            while ((buffReader.readLine().also { line = it }) != null) {
                if (((website == TWITTER) || (website == MYSPACE)) && line
                        !!.endsWith(".ts") || (website == METACAFE) && line!!.endsWith(
                        ".mp4"
                    )
                ) {
                    break
                }
            }
            if (line != null) {
                var l: Long = 1
                while (l < (totalChunks + 1)) {
                    line = buffReader.readLine()
                    l++
                }
            }
            buffReader.close()
            inReader.close()
            `in`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (line != null) {
            val prefix: String
            when (website) {
                DownloadManager.Companion.TWITTER -> {
                    Log.i(
                        "VDInfo", ("downloading chunk " + (totalChunks + 1) + ": " +
                                "https://video.twimg.com" + line)
                    )
                    return "https://video.twimg.com$line"
                }
                DownloadManager.Companion.METACAFE, DownloadManager.Companion.MYSPACE -> {
                    prefix = link!!.substring(0, link.lastIndexOf("/") + 1)
                    Log.i(
                        "VDInfo", ("downloading chunk " + (totalChunks + 1) + ": " + prefix +
                                line)
                    )
                    return prefix + line
                }
                else -> return null
            }
        } else {
            return null
        }
    }

    interface OnDownloadFinishedListener {
        fun onDownloadFinished()
    }

    interface OnLinkNotFoundListener {
        fun onLinkNotFound()
    }

    override fun onDestroy() {
        DownloadManager.Companion.downloadFile = null
        Thread.currentThread().interrupt()
        super.onDestroy()
    }

    companion object {
        private val CHUNCKED = "chunked"
        private var downloadFile: File? = null
        private var prevDownloaded: Long = 0

        /**
         * Should be called every second
         *
         * @return download speed in bytes per second
         */
        var downloadSpeed: Long = 0
            get() {
                if (!DownloadManager.Companion.chunked) {
                    if (DownloadManager.Companion.downloadFile != null) {
                        val downloaded: Long = DownloadManager.Companion.downloadFile!!.length()
                        DownloadManager.Companion.downloadSpeed =
                            downloaded - DownloadManager.Companion.prevDownloaded
                        DownloadManager.Companion.prevDownloaded = downloaded
                        return DownloadManager.Companion.downloadSpeed
                    }
                    return 0
                } else {
                    if (DownloadManager.Companion.bytesOfChunk != null) {
                        val downloaded: Long =
                            DownloadManager.Companion.bytesOfChunk!!.size().toLong()
                        DownloadManager.Companion.downloadSpeed =
                            downloaded - DownloadManager.Companion.prevDownloaded
                        DownloadManager.Companion.prevDownloaded = downloaded
                        return DownloadManager.Companion.downloadSpeed
                    }
                    return 0
                }
            }
        private var totalSize: Long = 0
        private val WEBSITE = "website"
        private val TWITTER = "twitter.com"
        private val METACAFE = "metacafe.com"
        private val MYSPACE = "myspace.com"
        private var chunked = false
        private var bytesOfChunk: ByteArrayOutputStream? = null
        private var stop = false
        private var downloadThread: Thread? = null
        private var onDownloadFinishedListener: OnDownloadFinishedListener? = null
        fun setOnDownloadFinishedListener(listener: OnDownloadFinishedListener) {
            DownloadManager.Companion.onDownloadFinishedListener = listener
        }

        private var onLinkNotFoundListener: OnLinkNotFoundListener? = null
        fun setOnLinkNotFoundListener(listener: OnLinkNotFoundListener) {
            DownloadManager.Companion.onLinkNotFoundListener = listener
        }

        fun stop() {
            Log.d("debug", "stop: called")
            val downloadService = MyApp.getInstance()!!.getDownloadService()
            MyApp.getInstance()!!.stopService(downloadService)
            DownloadManager.Companion.forceStopIfNecessary()
        }

        fun forceStopIfNecessary() {
            if (downloadThread != null) {
                Log.d("debug", "force: called")
                downloadThread = Thread.currentThread()
                if (downloadThread!!.isAlive) {
                    stop = true
                }
            }
        }

        /**
         * @return remaining time to download video in milliseconds
         */
        val remaining: Long
            get() {
                if (!DownloadManager.Companion.chunked && (DownloadManager.Companion.downloadFile != null)) {
                    val remainingLength: Long =
                        DownloadManager.Companion.totalSize - DownloadManager.Companion.prevDownloaded
                    return (1000 * (remainingLength / DownloadManager.Companion.downloadSpeed))
                } else {
                    return 0
                }
            }
    }
}