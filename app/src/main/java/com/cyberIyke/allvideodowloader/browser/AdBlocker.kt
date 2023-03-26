package com.cyberIyke.allvideodowloader.browser

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.cyberIyke.allvideodowloader.R
import java.io.*
import java.net.URL
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*

internal class AdBlocker : Serializable {
    private var filters: List<String>
    private var easylistLastModified: String? = null

    init {
        filters = ArrayList()
    }

    fun update(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences("settings", 0)
        val today: String = SimpleDateFormat("dd MM yyyy", Locale.getDefault()).format(Date())
        if (!(today == prefs.getString(context.getString(R.string.adFiltersLastUpdated), ""))) {
            object : Thread() {
                public override fun run() {
                    val easyList: String = "https://easylist.to/easylist/easylist.txt"
                    val tempFilters: MutableList<String> = ArrayList()
                    try {
                        val uCon: URLConnection? = URL(easyList).openConnection()
                        if (uCon != null) {
                            val `in`: InputStream = uCon.getInputStream()
                            val reader: BufferedReader = BufferedReader(InputStreamReader(`in`))
                            var line: String
                            while ((reader.readLine().also({ line = it })) != null) {
                                if (line.contains("Last modified")) {
                                    if ((line == easylistLastModified)) {
                                        reader.close()
                                        `in`.close()
                                        return
                                    } else {
                                        easylistLastModified = line
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
                            val file: File = File(context.getFilesDir(), "ad_filters.dat")
                            val fileOutputStream: FileOutputStream = FileOutputStream(file)
                            ObjectOutputStream(fileOutputStream).use({ objectOutputStream ->
                                objectOutputStream.writeObject(
                                    this@AdBlocker
                                )
                            })
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
        for (filter: String in filters) {
            val filterFormat: String = filter.replace("||", "//")
            if (url.contains(filterFormat)) {
                Log.d("VDDebug", "checkThroughFilters: " + filter + " " + url)
                return true
            }
        }
        return false
    }
}