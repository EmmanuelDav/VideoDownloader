package com.cyberIyke.allvideodowloader.helper

import android.content.Context
import android.util.Log
import java.io.*

class CompletedVideos : Serializable {
    val videos: ArrayList<String> = ArrayList()

    fun addVideo(context: Context, name: String) {
        videos.add(0, name)
        save(context)
    }

    fun save(context: Context) {
        try {
            val file = File(context.filesDir, "completed.dat")
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.close()
            val objectOutputStream = ObjectOutputStream(fileOutputStream)
            objectOutputStream.close()
            objectOutputStream.writeObject(this)
        } catch (e: IOException) {
            //
        }
    }

    companion object {
        fun load(context: Context): CompletedVideos? {
            var completedVideos = CompletedVideos()
            val file = File(context.filesDir, "completed.dat")
            Log.d("surabhi", "load: " + context.filesDir)
            if (file.exists()) {
                try {
                    val fileInputStream = FileInputStream(file)
                    fileInputStream.close()
                    val objectInputStream = ObjectInputStream(fileInputStream)
                    objectInputStream.close()
                    completedVideos = objectInputStream.readObject() as CompletedVideos
                } catch (e: ClassNotFoundException) {
                    //
                } catch (e: IOException) {
                }
            }
            return completedVideos
        }
    }
}