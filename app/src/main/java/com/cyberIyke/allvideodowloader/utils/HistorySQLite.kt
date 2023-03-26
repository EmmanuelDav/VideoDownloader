package com.cyberIyke.allvideodowloader.utils

import android.annotation.SuppressLint
import android.content.*
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.cyberIyke.allvideodowloader.utils.VisitedPage
import java.text.SimpleDateFormat
import java.util.*

class HistorySQLite(context: Context?) : SQLiteOpenHelper(context, "history.db", null, 2) {
    private val dB: SQLiteDatabase

    init {
        dB = writableDatabase
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE visited_pages (title TEXT, link TEXT, time TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (newVersion > oldVersion) {
            Log.v("Database Upgrade", "Database version higher than old.")
            clearHistory()
        }
    }

    fun addPageToHistory(page: VisitedPage) {
        val v = ContentValues()
        v.put(TITLE, page.title)
        v.put("link", page.link)
        val time = Calendar.getInstance().time
        val simpleDateFormat = SimpleDateFormat(
            "yyyy MM dd HH mm ss SSS",
            Locale.getDefault()
        )
        v.put("time", simpleDateFormat.format(time))
        if (dB.update(VISITED_PAGES, v, "link = '" + page.link + "'", null) <= 0) {
            dB.insert(VISITED_PAGES, null, v)
        }
    }

    fun deleteFromHistory(link: String) {
        dB.delete(VISITED_PAGES, "link = '$link'", null)
    }

    fun clearHistory() {
        dB.execSQL("DELETE FROM visited_pages")
    }

    @get:SuppressLint("Range")
    val allVisitedPages: MutableList<VisitedPage>
        get() {
            val c = dB.query(
                VISITED_PAGES, arrayOf(TITLE, "link"), null, null, null,
                null, "time DESC"
            )
            val pages: MutableList<VisitedPage> = ArrayList()
            while (c.moveToNext()) {
                val page = VisitedPage()
                page.title = c.getString(c.getColumnIndex(TITLE))
                page.link = c.getString(c.getColumnIndex("link"))
                pages.add(page)
            }
            c.close()
            return pages
        }

    @SuppressLint("Range")
    fun getVisitedPagesByKeyword(keyword: String): List<VisitedPage> {
        val c = dB.query(
            VISITED_PAGES, arrayOf(TITLE, "link"), "title LIKE '%" +
                    keyword + "%'", null, null, null, "time DESC"
        )
        val pages: MutableList<VisitedPage> = ArrayList()
        while (c.moveToNext()) {
            val page = VisitedPage()
            page.title = c.getString(c.getColumnIndex(TITLE))
            page.link = c.getString(c.getColumnIndex("link"))
            pages.add(page)
        }
        c.close()
        return pages
    }

    companion object {
        private const val VISITED_PAGES = "visited_pages"
        private const val TITLE = "title"
    }
}