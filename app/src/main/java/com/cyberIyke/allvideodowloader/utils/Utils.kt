package com.cyberIyke.allvideodowloader.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.cyberIyke.allvideodowloader.R
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

    }

}