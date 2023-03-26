package com.cyberIyke.allvideodowloader.helper

import android.content.*
import android.os.Environment
import com.cyberIyke.allvideodowloader.MyApp
import com.cyberIyke.allvideodowloader.R
import java.io.*

class DownloadQueues : Serializable {
    val list: ArrayList<DownloadVideo> = ArrayList()

    fun save(context: Context) {
        try {
            val file = File(context.filesDir, "downloads.dat")
            val fileOutputStream = FileOutputStream(file)
            ObjectOutputStream(fileOutputStream).use { objectOutputStream ->
                objectOutputStream.writeObject(
                    this
                )
            }
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun insertToTop(
        size: String?,
        type: String,
        link: String?,
        name: String,
        page: String?,
        chunked: Boolean,
        website: String?
    ) {
        var name = name
        name = getValidName(name, type)
        val video = DownloadVideo()
        video.link = link
        video.name = name
        video.page = page
        video.size = size
        video.type = type
        video.chunked = chunked
        video.website = website
        list.add(0, video)
    }

    private fun getValidName(name: String, type: String): String {
        var name = name
        name = name.replace("[^\\w ()'!\\[\\]\\-]".toRegex(), "")
        name = name.trim { it <= ' ' }
        if (name.length > 127) { //allowed filename length is 127
            name = name.substring(0, 127)
        } else if (name == "") {
            name = "video"
        }
        var i = 0
        var file = File(
            Environment.getExternalStoragePublicDirectory(
                MyApp.getInstance()!!.applicationContext.getString(R.string.app_name)
            ), "$name.$type"
        )
        var nameBuilder = StringBuilder(name)
        while (file.exists()) {
            i++
            nameBuilder = StringBuilder(name)
            nameBuilder.append(" ").append(i)
            file = File(
                Environment.getExternalStoragePublicDirectory(
                    MyApp.getInstance()!!.applicationContext.getString(R.string.app_name)
                ), "$nameBuilder.$type"
            )
        }
        while (nameAlreadyExists(nameBuilder.toString())) {
            i++
            nameBuilder = StringBuilder(name)
            nameBuilder.append(" ").append(i)
        }
        return nameBuilder.toString()
    }

    val topVideo: DownloadVideo?
        get() = if (!list.isEmpty()) {
            list[0]
        } else {
            null
        }

    fun deleteTopVideo(context: Context) {
        if (list.isNotEmpty()) {
            list.removeAt(0)
            save(context)
        }
    }

    private fun nameAlreadyExists(name: String): Boolean {
        for (video in list) {
            if (video.name == name) return true
        }
        return false
    }

    fun renameItem(index: Int, newName: String) {
        if (list[index].name != newName) {
            list[index].name = list[index].type?.let { getValidName(newName, it) }
        }
    }

    companion object {
        fun load(context: Context): DownloadQueues? {
            val file = File(context.filesDir, "downloads.dat")
            var queues = DownloadQueues()
            if (file.exists()) {
                try {
                    val fileInputStream = FileInputStream(file)
                    ObjectInputStream(fileInputStream).use { objectInputStream ->
                        queues = objectInputStream.readObject() as DownloadQueues
                    }
                    fileInputStream.close()
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return queues
        }
    }
}