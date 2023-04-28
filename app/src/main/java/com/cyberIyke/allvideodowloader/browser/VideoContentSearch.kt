package com.cyberIyke.allvideodowloader.browser

import android.content.Context
import android.media.MediaMetadataRetriever
import android.webkit.URLUtil
import com.cyberIyke.allvideodowloader.R
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset
import java.util.*

abstract class VideoContentSearch constructor(
    private val context: Context,
    private val url: String,
    private val page: String,
    title: String?
) : Thread() {
    private val title: String
    private var numLinksInspected: Int
    abstract fun onStartInspectingURL()
    abstract fun onFinishedInspectingURL(finishedAll: Boolean)
    abstract fun onVideoFound(
        size: String?, type: String?, link: String, name: String?,
        page: String?, chunked: Boolean, website: String?, audio: Boolean
    )

    init {
        //this.title = title;
        this.title = URLUtil.guessFileName(url, null, null)
        numLinksInspected = 0
    }

    public override fun run() {
        val urlLowerCase: String = url.lowercase(Locale.getDefault())
        val filters: Array<String> = context.getResources().getStringArray(R.array.videourl_filters)
        var urlMightBeVideo: Boolean = false
        for (filter: String? in filters) {
            if (urlLowerCase.contains((filter)!!)) {
                urlMightBeVideo = true
                break
            }
        }
        if (urlMightBeVideo) {
            numLinksInspected++
            onStartInspectingURL()
            var uCon: URLConnection? = null
            try {
                uCon = URL(url).openConnection()
                uCon.connect()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (uCon != null) {
                var contentType: String? = uCon.getHeaderField("content-type")
                if (contentType != null) {
                    contentType = contentType.lowercase(Locale.getDefault())
                    if (contentType.contains(VideoContentSearch.Companion.VIDEO) || contentType.contains(
                            VideoContentSearch.Companion.AUDIO
                        )
                    ) {
                        addVideoToList(uCon, page, title, contentType)
                    } else if ((contentType == "application/x-mpegurl") || (contentType == "application/vnd.apple.mpegurl")) {
                       // addVideosToListFromM3U8(uCon, page, title)
                    }
                }
            }
            numLinksInspected--
            var finishedAll: Boolean = false
            if (numLinksInspected <= 0) {
                finishedAll = true
            }
            onFinishedInspectingURL(finishedAll)
        }
    }

    private fun addVideoToList(
        uCon: URLConnection,
        page: String,
        title: String?,
        contentType: String
    ) {
        try {
            var size: String? = uCon.getHeaderField(VideoContentSearch.Companion.LENGTH)
            var link: String? = uCon.getHeaderField("Location")
            if (link == null) {
                link = uCon.getURL().toString()
            }
            val host: String = URL(page).getHost()
            var website: String? = null
            var chunked: Boolean = false
            val type: String
            var audio: Boolean = false

            // Skip twitter video chunks.
            if (host.contains(VideoContentSearch.Companion.TWITTER) && (contentType == "video/mp2t")) {
                return
            }
            var name: String = VideoContentSearch.Companion.VIDEO
            if (title != null) {
                if (contentType.contains(VideoContentSearch.Companion.AUDIO)) {
                    name = "[AUDIO ONLY]" + title
                } else {
                    name = title
                }
            } else if (contentType.contains(VideoContentSearch.Companion.AUDIO)) {
                name = VideoContentSearch.Companion.AUDIO
            }
            if (host.contains(VideoContentSearch.Companion.YOUTUBE) || (URL(link).getHost()
                    .contains("googlevideo.com"))
            ) {
                val r: Int = link.lastIndexOf("&range")
                if (r > 0) {
                    link = link.substring(0, r)
                }
                val ytCon: URLConnection
                ytCon = URL(link).openConnection()
                ytCon.connect()
                size = ytCon.getHeaderField(VideoContentSearch.Companion.LENGTH)
                if (host.contains(VideoContentSearch.Companion.YOUTUBE)) {
                    val embededURL: URL = URL(
                        ("http://www.youtube.com/oembed?url=" + page +
                                "&format=json")
                    )
                    try {
                        val jSonString: String
                        val `in`: InputStream = embededURL.openStream()
                        val inReader: InputStreamReader = InputStreamReader(
                            `in`, Charset
                                .defaultCharset()
                        )
                        val sb: StringBuilder = StringBuilder()
                        val buffer: CharArray = CharArray(1024)
                        var read: Int
                        while ((inReader.read(buffer).also({ read = it })) != -1) {
                            sb.append(buffer, 0, read)
                        }
                        jSonString = sb.toString()
                        name = JSONObject(jSonString).getString("title")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    if (contentType.contains(VideoContentSearch.Companion.VIDEO)) {
                        name = "[VIDEO ONLY]" + name
                    } else if (contentType.contains(VideoContentSearch.Companion.AUDIO)) {
                        name = "[AUDIO ONLY]" + name
                    }
                    website = VideoContentSearch.Companion.YOUTUBE
                }
            } else if (host.contains("dailymotion.com")) {
                chunked = true
                website = "dailymotion.com"
                link = link.replace("(frag\\()+(\\d+)+(\\))".toRegex(), "FRAGMENT")
                size = null
            } else if (host.contains("vimeo.com") && link.endsWith("m4s")) {
                chunked = true
                website = "vimeo.com"
                link = link.replace("(segment-)+(\\d+)".toRegex(), "SEGMENT")
                size = null
            } else if (host.contains("facebook.com") && link.contains("bytestart")) {
                val b: Int = link.lastIndexOf("&bytestart")
                val f: Int = link.indexOf("fbcdn")
                if (b > 0) {
                    link = "https://video.xx." + link.substring(f, b)
                }
                val fbCon: URLConnection
                fbCon = URL(link).openConnection()
                fbCon.connect()
                size = fbCon.getHeaderField(VideoContentSearch.Companion.LENGTH)
                website = "facebook.com"
                try {
                    val retriever: MediaMetadataRetriever = MediaMetadataRetriever()
                    retriever.setDataSource(link, HashMap())
                    retriever.release()
                    audio = false
                } catch (ex: RuntimeException) {
                    audio = true
                }
            } else if (host.contains("instagram.com")) {
                try {
                    val retriever: MediaMetadataRetriever = MediaMetadataRetriever()
                    retriever.setDataSource(link, HashMap())
                    retriever.release()
                    audio = false
                } catch (ex: RuntimeException) {
                    audio = true
                }
            }
            when (contentType) {
                "video/mp4" -> type = "mp4"
                "video/webm" -> type = "webm"
                "video/mp2t" -> type = "ts"
                "audio/webm" -> type = "webm"
                else -> type = "mp4"
            }
            onVideoFound(size, type, link, name, page, chunked, website, audio)
        } catch (e: IOException) {
            //nada
        }
    }

    private fun addVideosToListFromM3U8(uCon: URLConnection, page: String, title: String?) {
        try {
            val host: String
            val audio: Boolean = false
            host = URL(page).getHost()
            if (host.contains(VideoContentSearch.Companion.TWITTER) || host.contains(
                    VideoContentSearch.Companion.METACAFE
                ) || host.contains(VideoContentSearch.Companion.MYSPACE)
            ) {
                val `in`: InputStream = uCon.getInputStream()
                val inReader: InputStreamReader = InputStreamReader(`in`)
                val buffReader: BufferedReader = BufferedReader(inReader)
                var line: String
                var prefix: String? = null
                var type: String? = null
                var name: String? = VideoContentSearch.Companion.VIDEO
                var website: String? = null
                if (title != null) {
                    name = title
                }
                if (host.contains(VideoContentSearch.Companion.TWITTER)) {
                    prefix = "https://video.twimg.com"
                    type = "ts"
                    website = VideoContentSearch.Companion.TWITTER
                } else if (host.contains(VideoContentSearch.Companion.METACAFE)) {
                    val link: String = uCon.getURL().toString()
                    prefix = link.substring(0, link.lastIndexOf("/") + 1)
                    website = VideoContentSearch.Companion.METACAFE
                    type = "mp4"
                } else if (host.contains(VideoContentSearch.Companion.MYSPACE)) {
                    val link: String = uCon.getURL().toString()
                    website = VideoContentSearch.Companion.MYSPACE
                    type = "ts"
                    onVideoFound(null, type, link, name, page, true, website, audio)
                    return
                }
//                while ((buffReader.readLine().also({ line = it })) != null) {
//                    if (line.endsWith(".m3u8")) {
//                        val link: String = prefix + line
//                        onVideoFound(null, type, link, name, page, true, website, audio)
//                    }
//                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val VIDEO: String = "video"
        private val AUDIO: String = "audio"
        private val LENGTH: String = "content-length"
        private val TWITTER: String = "twitter.com"
        private val YOUTUBE: String = "youtube.com"
        private val METACAFE: String = "metacafe.com"
        private val MYSPACE: String = "myspace.com"
    }
}