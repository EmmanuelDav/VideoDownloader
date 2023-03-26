package com.cyberIyke.allvideodowloader.helperimport

import android.content.*
import android.util.Log
import java.io.*

com.cyberIyke.allvideodowloader.MyApp.Companion.getInstance
import com.cyberIyke.allvideodowloader.MyApp.getDownloadService
import com.cyberIyke.allvideodowloader.model.VidInfoItem.VidFormatItem.vidInfo
import com.cyberIyke.allvideodowloader.viewModel.VidInfoViewModel.vidFormats
import androidx.lifecycle.ViewModelProvider.get
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getBaseDomain
import com.cyberIyke.allvideodowloader.viewModel.VidInfoViewModel.fetchInfo
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.disableSSLCertificateChecking
import com.cyberIyke.allvideodowloader.viewModel.VidInfoViewModel.startDownload
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.hideSoftKeyboard
import com.cyberIyke.allvideodowloader.interfaces.DownloadInterface.loading
import com.cyberIyke.allvideodowloader.viewModel.DownloadsViewModel.allDownloads
import com.cyberIyke.allvideodowloader.database.Download.id
import com.cyberIyke.allvideodowloader.interfaces.DownloadInterface.notLoading
import com.cyberIyke.allvideodowloader.database.Download.timestamp
import com.cyberIyke.allvideodowloader.database.Download.downloadedPath
import com.cyberIyke.allvideodowloader.database.Download.name
import com.cyberIyke.allvideodowloader.viewModel.DownloadsViewModel.viewContent
import com.cyberIyke.allvideodowloader.viewModel.DownloadsViewModel.startDelete
import com.cyberIyke.allvideodowloader.viewModel.DownloadsViewModel.loadState
import com.cyberIyke.allvideodowloader.database.Download.downloadedPercent
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getStringSizeLengthFile
import com.cyberIyke.allvideodowloader.database.Download.downloadedSize
import com.cyberIyke.allvideodowloader.database.Download.totalSize
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.convertSecondsToHMmSs
import com.cyberIyke.allvideodowloader.model.DownloadInfo.progress
import com.cyberIyke.allvideodowloader.model.DownloadInfo.line
import com.cyberIyke.allvideodowloader.model.DownloadInfo.name
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getStatusBarHeight
import com.tonyodev.fetch2.FetchConfiguration.Builder.setDownloadConcurrentLimit
import com.tonyodev.fetch2.FetchConfiguration.Builder.build
import com.tonyodev.fetch2.Fetch.Impl.getInstance
import com.tonyodev.fetch2.Fetch.getDownloads
import com.tonyodev.fetch2.Fetch.addListener
import com.tonyodev.fetch2.Download.file
import com.cyberIyke.allvideodowloader.utils.ThemeSettings.Companion.getInstance
import com.cyberIyke.allvideodowloader.utils.ThemeSettings.save
import com.cyberIyke.allvideodowloader.MyApp.getOnBackPressedListener
import com.cyberIyke.allvideodowloader.MyApp.setOnBackPressedListener
import okhttp3.OkHttpClient.Builder.connectTimeout
import okhttp3.OkHttpClient.Builder.writeTimeout
import okhttp3.OkHttpClient.Builder.readTimeout
import okhttp3.OkHttpClient.Builder.build
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import com.cyberIyke.allvideodowloader.utils.VisitedPage
import com.cyberIyke.allvideodowloader.utils.HistorySQLite
import android.annotation.SuppressLint
import com.hjq.permissions.IPermissionInterceptor
import android.app.Activity
import com.hjq.permissions.OnPermissionCallback
import com.cyberIyke.allvideodowloader.utils.PermissionNameConvert
import com.cyberIyke.allvideodowloader.R
import android.widget.TextView
import android.view.WindowManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.OnPermissionPageCallback
import android.view.LayoutInflater
import android.view.ViewGroup
import com.cyberIyke.allvideodowloader.views.cardstack.ScrollDelegate
import com.cyberIyke.allvideodowloader.views.cardstack.StackAdapter
import com.cyberIyke.allvideodowloader.views.cardstack.CardStackView.ViewDataObserver
import com.cyberIyke.allvideodowloader.views.cardstack.CardStackView
import com.cyberIyke.allvideodowloader.views.cardstack.AnimatorAdapter
import android.widget.OverScroller
import android.view.VelocityTracker
import com.cyberIyke.allvideodowloader.views.cardstack.CardStackView.ItemExpendListener
import kotlin.jvm.JvmOverloads
import android.annotation.TargetApi
import android.content.res.TypedArray
import android.view.ViewConfiguration
import com.cyberIyke.allvideodowloader.views.cardstack.AllMoveDownAnimatorAdapter
import com.cyberIyke.allvideodowloader.views.cardstack.UpDownAnimatorAdapter
import com.cyberIyke.allvideodowloader.views.cardstack.UpDownStackAnimatorAdapter
import com.cyberIyke.allvideodowloader.views.cardstack.StackScrollDelegateImpl
import android.view.MotionEvent
import android.view.ViewParent
import android.view.ViewGroup.MarginLayoutParams
import android.animation.AnimatorSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.widget.Checkable
import com.cyberIyke.allvideodowloader.views.SwitchButton
import android.animation.ValueAnimator
import android.view.View.MeasureSpec
import android.view.View.OnLongClickListener
import android.graphics.RectF
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.util.TypedValue
import android.view.SurfaceView
import com.cyberIyke.allvideodowloader.helper.OrientationDetector.OrientationChangeListener
import com.cyberIyke.allvideodowloader.views.CustomVideoView
import android.view.SurfaceHolder
import android.media.MediaPlayer
import com.cyberIyke.allvideodowloader.views.CustomMediaController
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import com.cyberIyke.allvideodowloader.helper.OrientationDetector
import com.cyberIyke.allvideodowloader.views.CustomVideoView.VideoViewCallback
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.MediaPlayer.OnVideoSizeChangedListener
import android.media.MediaPlayer.OnBufferingUpdateListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.cyberIyke.allvideodowloader.views.Badge
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.cyberIyke.allvideodowloader.views.BadgeRed
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.ImageButton
import android.widget.SeekBar
import android.view.View.OnTouchListener
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.EditText
import com.cyberIyke.allvideodowloader.activities.MainActivity
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.AdView
import android.widget.LinearLayout
import com.cyberIyke.allvideodowloader.helper.AdController
import android.util.DisplayMetrics
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import androidx.lifecycle.LifecycleObserver
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.cyberIyke.allvideodowloader.helper.AppOpenManager
import android.os.Bundle
import androidx.lifecycle.OnLifecycleEvent
import com.cyberIyke.allvideodowloader.helper.DownloadVideo
import android.os.Environment
import com.cyberIyke.allvideodowloader.MyApp
import com.cyberIyke.allvideodowloader.helper.DownloadQueues
import com.cyberIyke.allvideodowloader.helper.CompletedVideos
import android.app.IntentService
import com.cyberIyke.allvideodowloader.helper.InactiveDownloads
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import com.cyberIyke.allvideodowloader.helper.DownloadManager.OnDownloadFinishedListener
import com.cyberIyke.allvideodowloader.helper.DownloadManager.OnLinkNotFoundListener
import android.content.pm.PackageManager
import android.view.OrientationEventListener
import android.hardware.SensorManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yausername.youtubedl_android.mapper.VideoInfo
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.cyberIyke.allvideodowloader.model.VidInfoItem.VidFormatItem
import com.cyberIyke.allvideodowloader.browser.VideoList.VideoListAdapter
import androidx.recyclerview.widget.GridLayoutManager
import com.cyberIyke.allvideodowloader.utils.PermissionInterceptor
import com.cyberIyke.allvideodowloader.browser.VideoList
import com.cyberIyke.allvideodowloader.browser.VideoList.VideoListAdapter.VideoItem
import com.cyberIyke.allvideodowloader.model.VidInfoItem
import com.bumptech.glide.Glide
import com.cyberIyke.allvideodowloader.browser.BrowserWindow
import com.cyberIyke.allvideodowloader.fragments.base.BaseFragment
import com.cyberIyke.allvideodowloader.activities.MainActivity.OnBackPressedListener
import com.cyberIyke.allvideodowloader.fragments.DownloadPathDialogFragment.DialogListener
import com.cyberIyke.allvideodowloader.browser.TouchableWebView
import com.cyberIyke.allvideodowloader.viewModel.VidInfoViewModel
import com.cyberIyke.allvideodowloader.viewModel.DownloadsViewModel
import com.cyberIyke.allvideodowloader.fragments.DownloadPathDialogFragment
import android.webkit.WebViewClient
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.graphics.Bitmap
import android.os.Looper
import com.cyberIyke.allvideodowloader.browser.VideoContentSearch
import android.webkit.WebResourceResponse
import androidx.annotation.RequiresApi
import android.webkit.WebChromeClient
import androidx.webkit.WebViewFeature
import androidx.webkit.WebSettingsCompat
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import android.view.animation.BounceInterpolator
import android.preference.PreferenceManager
import android.widget.Toast
import android.view.Gravity
import kotlin.Throws
import com.cyberIyke.allvideodowloader.browser.AdBlocker
import com.cyberIyke.allvideodowloader.browser.BrowserManager.BrowserTabAdapter
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.cyberIyke.allvideodowloader.browser.BrowserManager.AllWindowsAdapter
import com.cyberIyke.allvideodowloader.views.NotificationBadge
import com.cyberIyke.allvideodowloader.activities.IntroActivity
import android.widget.TextView.OnEditorActionListener
import android.view.inputmethod.EditorInfo
import com.cyberIyke.allvideodowloader.helper.WebConnect
import com.cyberIyke.allvideodowloader.adapters.ShortcutAdapter
import com.cyberIyke.allvideodowloader.interfaces.ShortcutListner
import com.cyberIyke.allvideodowloader.database.ShortcutTable
import com.cyberIyke.allvideodowloader.database.AppExecutors
import com.cyberIyke.allvideodowloader.database.ShortcutAppDatabase
import com.cyberIyke.allvideodowloader.browser.BrowserManager.BrowserTabAdapter.ColorItemViewHolder
import com.cyberIyke.allvideodowloader.browser.BrowserManager.WindowItem
import org.json.JSONObject
import org.json.JSONException
import android.media.MediaMetadataRetriever
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import com.cyberIyke.allvideodowloader.adapters.SuggestionAdapter.SuggetionListner
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.PrimaryKey
import androidx.room.Database
import androidx.room.RoomDatabase
import com.cyberIyke.allvideodowloader.database.ShortcutDao
import androidx.room.Room
import android.widget.ArrayAdapter
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView
import android.webkit.WebStorage
import com.cyberIyke.allvideodowloader.fragments.AllDownloadFragment.DownloadAdapter
import com.cyberIyke.allvideodowloader.fragments.AllDownloadFragment.DownloadData
import com.cyberIyke.allvideodowloader.interfaces.DownloadInterface
import com.cyberIyke.allvideodowloader.helper.RenameVideoPref
import androidx.documentfile.provider.DocumentFile
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cyberIyke.allvideodowloader.fragments.AllDownloadFragment.DownloadAdapter.ProgressViewHolder
import androidx.work.WorkInfo
import com.cyberIyke.allvideodowloader.fragments.AllDownloadFragment
import wseemann.media.FFmpegMediaMetadataRetriever
import android.webkit.MimeTypeMap
import com.cyberIyke.allvideodowloader.browser.BrowserManager
import com.cyberIyke.allvideodowloader.adapters.SuggestionAdapter
import com.tonyodev.fetch2.Fetch
import com.cyberIyke.allvideodowloader.views.NotificationBadgeRed
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2core.Func
import com.tonyodev.fetch2.FetchListener
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.tonyodev.fetch2core.DownloadBlock
import com.cyberIyke.allvideodowloader.activities.HistoryActivity
import com.cyberIyke.allvideodowloader.activities.FeedbackActivity
import com.cyberIyke.allvideodowloader.utils.ThemeSettings
import android.text.TextWatcher
import android.text.Editable
import com.cyberIyke.allvideodowloader.webservice.SearchModel
import com.cyberIyke.allvideodowloader.webservice.RetrofitClient
import com.cyberIyke.allvideodowloader.webservice.Gossip
import com.gyf.immersionbar.ImmersionBar
import com.cyberIyke.allvideodowloader.fragments.SettingsFragment
import androidx.viewpager.widget.ViewPager
import com.cyberIyke.allvideodowloader.activities.IntroActivity.MyViewPagerAdapter
import com.rd.PageIndicatorView
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import androidx.viewpager.widget.PagerAdapter
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.method.LinkMovementMethod
import com.cyberIyke.allvideodowloader.activities.HistoryActivity.VisitedPagesAdapter
import com.cyberIyke.allvideodowloader.activities.HistoryActivity.VisitedPagesAdapter.VisitedPageItem
import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Expose
import retrofit2.http.GET
import retrofit2.Retrofit
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.converter.gson.GsonConverterFactory
import com.cyberIyke.allvideodowloader.webservice.RequestApi
import kotlin.jvm.Synchronized

class CompletedVideos : Serializable {
    val videos: List<String>

    init {
        videos = ArrayList()
    }

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