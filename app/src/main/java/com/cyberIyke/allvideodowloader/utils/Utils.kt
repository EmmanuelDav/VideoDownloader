package com.cyberIyke.allvideodowloader.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.model.Format
import com.yausername.youtubedl_android.mapper.VideoInfo

import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.text.DecimalFormat
import java.util.*
import javax.net.ssl.*


class Utils(private var context: Context) {

    companion object {
        var permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        @ColorInt
        fun Context.getColorFromAttr(
            @AttrRes attrColor: Int,
            typedValue: TypedValue = TypedValue(),
            resolveRefs: Boolean = true
        ): Int {
            theme.resolveAttribute(attrColor, typedValue, resolveRefs)
            return typedValue.data
        }

        fun getBaseDomain(url: String?): String? {
            val host: String = getHost(url)
            var startIndex = 0
            var nextIndex = host.indexOf('.')
            val lastIndex = host.lastIndexOf('.')
            while (nextIndex < lastIndex) {
                startIndex = nextIndex + 1
                nextIndex = host.indexOf('.', startIndex)
            }
            return if (startIndex > 0) {
                host.substring(startIndex)
            } else {
                host
            }
        }

        fun getHost(url: String?): String {
            if (url == null || url.length == 0) return ""
            var doubleslash = url.indexOf("//")
            if (doubleslash == -1) doubleslash = 0 else doubleslash += 2
            var end = url.indexOf('/', doubleslash)
            end = if (end >= 0) end else url.length
            val port = url.indexOf(':', doubleslash)
            end = if (port > 0 && port < end) port else end
            return url.substring(doubleslash, end)
        }

        /**
         * Disables the SSL certificate checking for new instances of [HttpsURLConnection] This has been created to
         * aid testing on a local box, not for use on production.
         */
        fun disableSSLCertificateChecking() {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? {
                    return null
                }

                @SuppressLint("TrustAllX509TrustManager")
                @Throws(CertificateException::class)
                override fun checkClientTrusted(arg0: Array<X509Certificate>, arg1: String) {
                    // Not implemented
                }

                @SuppressLint("TrustAllX509TrustManager")
                @Throws(CertificateException::class)
                override fun checkServerTrusted(arg0: Array<X509Certificate>, arg1: String) {
                    // Not implemented
                }
            })
            try {
                val sc = SSLContext.getInstance("TLS")
                sc.init(null, trustAllCerts, SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

                // Create all-trusting host name verifier
                val allHostsValid =
                    HostnameVerifier { s, sslSession -> true }

                // Install the all-trusting host verifier
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
            } catch (e: KeyManagementException) {
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
        }

        fun shareApp(activity: Activity, appName: String) {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            val shareBodyText =
                "https://play.google.com/store/apps/details?id=" + activity.packageName
            sharingIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Want to download Videos from your favorite social medias? Try $appName. DOWNLOAD NOW!"
            )
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBodyText)
            activity.startActivity(
                Intent.createChooser(
                    sharingIntent,
                    activity.resources.getString(R.string.share_via)
                )
            )
        }

        fun rateApp(activity: Activity) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + activity.packageName)
                )
            )
        }

        fun moreApps(activity: Activity) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + activity.packageName)
                )
            )
        }

        fun openUrl(activity: Activity, url: String) {
            if (url.isEmpty()) return
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            activity.startActivity(intent)
        }

        fun hideSoftKeyboard(activity: Activity, token: IBinder?) {
            val inputMethodManager = activity.getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            if (inputMethodManager != null && token != null) {
                inputMethodManager.hideSoftInputFromWindow(
                    token, 0
                )
            }
        }

        fun isServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (manager != null) {
                for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                    if (serviceClass.name == service.service.className) {
                        return true
                    }
                }
            }
            return false
        }
        fun getStatusBarHeight(context: Context): Int {
            var result = 0
            val resourceId: Int = context.getResources().getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = context.getResources().getDimensionPixelSize(resourceId)
            }
            return result
        }

        fun getStringSizeLengthFile(size: Long): String? {
            val df = DecimalFormat("0.00")
            val sizeKb = 1024.0f
            val sizeMb = sizeKb * sizeKb
            val sizeGb = sizeMb * sizeKb
            val sizeTerra = sizeGb * sizeKb
            if (size < sizeMb) return df.format(size / sizeKb)
                .toString() + " KB" else if (size < sizeGb) return df.format(size / sizeMb)
                .toString() + " MB" else if (size < sizeTerra) return df.format(size / sizeGb)
                .toString() + " GB"
            return ""
        }

        fun convertSecondsToHMmSs(seconds: Long): String? {
            val s = seconds % 60
            val m = seconds / 60 % 60
            val h = seconds / (60 * 60) % 24
            return String.format("%d:%02d:%02d", h, m, s)
        }

        fun getNumbersFromString(str: String): Int? {
            val numbersOnly = str.replace(Regex("[^0-9]"), "")
            return numbersOnly.toIntOrNull()
        }

        fun formatDuration(durationInSeconds: Long): String {
            val hours = durationInSeconds / 3600
            val minutes = (durationInSeconds % 3600) / 60
            val seconds = durationInSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("0%d:%02d", minutes, seconds)
            }
        }

        private fun estimateVideoSize(durationInSeconds: Int): List<String> {
            val resolutions = listOf(240, 360, 480, 720, 1080)
            val sizes = mutableListOf<String>()
            for (resolution in resolutions) {
                val bitRate: Double = when {
                    resolution <= 240 -> 250.0
                    resolution <= 360 -> 300.0
                    resolution <= 480 -> 420.0
                    resolution <= 720 -> 700.0
                    else -> 1000.0
                }
                val videoSizeInMb = bitRate / 8.0 * durationInSeconds / 60.0 * resolution * resolution / (640.0 * 480.0)
                val size = if (videoSizeInMb >= 1000.0) {
                    String.format("%.2f GB", videoSizeInMb / 1000.0)
                } else {
                    String.format("%.2f MB", videoSizeInMb)
                }
                sizes.add(size)
            }
            return sizes
        }

        suspend fun generateFormatsList(videoInfo: VideoInfo, resources: Resources): MutableList<Format> = withContext(Dispatchers.IO) {
            val formats = mutableListOf<Format>()
            val videoFormats = resources.getStringArray(R.array.video_formats)
            val size = estimateVideoSize(videoInfo.duration)

            if (videoInfo.formats!!.size <= 1){
                if (videoFormats.isNotEmpty() && size.isNotEmpty()) {
                    for (i in 0 until minOf(videoFormats.size, size.size)) {
                        formats.add(Format(videoFormats[i], "", "", "", size[i], 0, ""))
                    }
                }
            }
            formats
        }

        fun extractPercentageAndMB(input: String): Triple<Double, Double, Double>? {
            val pattern = """(\d+\.\d+)%.*?(\d+\.\d+)[KMG]?iB.*?(\d+\.\d+)[KMG]?iB/s.*""".toRegex()
            val matchResult = pattern.find(input)
            if (matchResult != null) {
                val percentage = matchResult.groupValues[1].toDouble()
                val size = matchResult.groupValues[2].toDouble()
                val speed = matchResult.groupValues[3].toDouble()

                return Triple(percentage, size, speed)
            }
            return null
        }

        fun checkForPlaylist(url: String): String? {
            if (url.contains("dailymotion.com")) {
                if (url.contains("playlist")) {
                    val questionMarkIndex = url.indexOf("?")
                    return if (questionMarkIndex != -1) {
                        url.substring(0, questionMarkIndex)
                    } else {
                        url
                    }
                }
            }
            return url
        }
    }
}