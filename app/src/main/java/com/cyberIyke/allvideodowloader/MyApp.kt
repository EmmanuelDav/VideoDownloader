package com.cyberIyke.allvideodowloader

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.cyberIyke.allvideodowloader.activities.MainActivity
import com.cyberIyke.allvideodowloader.helper.AdController
import com.cyberIyke.allvideodowloader.helper.AppOpenManager
import com.onesignal.OneSignal
import com.cyberIyke.allvideodowloader.utils.Constants
import com.cyberIyke.allvideodowloader.work.CancelReceiver
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.cyberIyke.allvideodowloader.utils.ThemeSettings.Companion.getInstance
import com.cyberIyke.allvideodowloader.work.PauseReceiver
import kotlinx.coroutines.*

class MyApp : Application() {

    private var downloadService: Intent? = null
    private var onBackPressedListener: MainActivity.OnBackPressedListener? = null
    private var context: Context? = null
    lateinit var applicationScope: CoroutineScope
    private var appOpenManager: AppOpenManager? = null


    override fun onCreate() {
        super.onCreate()
        instance = this
        context = applicationContext
        applicationScope = CoroutineScope(SupervisorJob())

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(
            preferences.getString(
                getString(R.string.app_name),
                AppCompatDelegate.MODE_NIGHT_YES.toString()
            )!!.toInt()
        )
        context = this.applicationContext
        applicationScope.launch((Dispatchers.IO)) {
            try {
                YoutubeDL.getInstance().init(this@MyApp)
                FFmpeg.getInstance().init(this@MyApp)
                Log.d(TAG, "onCreate: Successful")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, "Failed Initialization: " + e.message)
            }
        }
        registerReceiver(CancelReceiver(), IntentFilter())
        registerReceiver(PauseReceiver(), IntentFilter())


        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)

        getInstance(this)!!.refreshTheme()

        // OneSignal Initialization

        // OneSignal Initialization
        OneSignal.initWithContext(this)
        OneSignal.setAppId(Constants.ONESIGNAL_APP_ID)

        AdController.initAd(this)
        appOpenManager = AppOpenManager(this)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        private var instance: MyApp? = null
        var TAG = "Video_Downloader"


        @JvmStatic
        fun getInstance(): MyApp? {
            return instance
        }
    }


    fun getDownloadService(): Intent? {
        return downloadService
    }


    fun getOnBackPressedListener(): MainActivity.OnBackPressedListener? {
        return onBackPressedListener
    }

    fun setOnBackPressedListener(onBackPressedListener: MainActivity.OnBackPressedListener?) {
        this.onBackPressedListener = onBackPressedListener
    }
}