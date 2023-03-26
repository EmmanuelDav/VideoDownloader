package com.cyberIyke.allvideodowloader.helper

import android.content.Context
import java.io.*

class InactiveDownloads constructor() : Serializable {
    private val list: MutableList<DownloadVideo>

    init {
        list = ArrayList()
    }

    fun add(context: Context, inactiveDownload: DownloadVideo) {
        list.add(inactiveDownload)
        save(context)
    }

    fun save(context: Context) {
        try {
            val file: File = File(context.getFilesDir(), "inactive.dat")
            val fileOutputStream: FileOutputStream = FileOutputStream(file)
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

    fun getList(): List<DownloadVideo> {
        return list
    }

    companion object {
        fun load(context: Context): InactiveDownloads? {
            val file: File = File(context.getFilesDir(), "inactive.dat")
            var inactiveDownloads: InactiveDownloads? = InactiveDownloads()
            if (file.exists()) {
                try {
                    val fileInputStream: FileInputStream = FileInputStream(file)
                    ObjectInputStream(fileInputStream).use({ objectInputStream ->
                        inactiveDownloads = objectInputStream.readObject() as InactiveDownloads?
                    })
                    fileInputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }
            }
            return inactiveDownloads
        }
    }
}