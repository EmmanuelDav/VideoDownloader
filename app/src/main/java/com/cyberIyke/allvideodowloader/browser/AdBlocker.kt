package com.cyberIyke.allvideodowloader.browser

import android.content.Context
import android.util.Log
import com.cyberIyke.allvideodowloader.R
import java.io.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

internal class AdBlocker : Serializable {
    private var filters: List<String>
    private var easylistLastModified: String? = null

    init {
        filters = ArrayList()
    }

    fun update(context: Context) {
        val prefs = context.getSharedPreferences("settings", 0)
        val today = SimpleDateFormat("dd MM yyyy", Locale.getDefault()).format(Date())
        if (today != prefs.getString(context.getString(R.string.adFiltersLastUpdated), "")) {
            object : Thread() {
                override fun run() {
                    val easyList = "https://easylist.to/easylist/easylist.txt"
                    val tempFilters: MutableList<String> = ArrayList()
                    try {
                        val uCon = URL(easyList).openConnection()
                        if (uCon != null) {
                            val `in` = uCon.getInputStream()
                            val reader = BufferedReader(InputStreamReader(`in`))
                            var line: String = ""
                            while (reader.readLine()?.also { line = it } != null) {
                                if (line.contains("Last modified")) {
                                    easylistLastModified = if (line == easylistLastModified) {
                                        reader.close()
                                        `in`.close()
                                        return
                                    } else {
                                        line
                                    }
                                } else if (!line.startsWith("!") || !line.startsWith("[")) {
                                    tempFilters.add(line)
                                }
                            }
                            if (!tempFilters.isEmpty()) {
                                filters = tempFilters
                                Log.i(
                                    "VDDebug", "updating ads filters complete. Total: " +
                                            filters.size
                                )
                            }
                            val file = File(context.filesDir, "ad_filters.dat")
                            val fileOutputStream = FileOutputStream(file)
                            ObjectOutputStream(fileOutputStream).use { objectOutputStream ->
                                objectOutputStream.writeObject(
                                    this@AdBlocker
                                )
                            }
                            fileOutputStream.close()
                        }
                        prefs.edit()
                            .putString(context.getString(R.string.adFiltersLastUpdated), today)
                            .apply()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }.start()
        }
    }

    fun checkThroughFilters(url: String): Boolean {
        for (filter in filters) {
            val filterFormat = filter.replace("||", "//")
            if (url.contains(filterFormat)) {
                Log.d("VDDebug", "checkThroughFilters: $filter $url")
                return true
            }
        }
        return false
    }
}