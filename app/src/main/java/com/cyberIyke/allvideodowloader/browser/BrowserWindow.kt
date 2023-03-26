package com.cyberIyke.allvideodowloader.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.view.animation.BounceInterpolator
import android.webkit.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider.get
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.cyberIyke.allvideodowloader.MyApp
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.activities.MainActivity
import com.cyberIyke.allvideodowloader.activities.MainActivity.OnBackPressedListener
import com.cyberIyke.allvideodowloader.browser.BrowserWindow
import com.cyberIyke.allvideodowloader.fragments.DownloadPathDialogFragment
import com.cyberIyke.allvideodowloader.fragments.DownloadPathDialogFragment.DialogListener
import com.cyberIyke.allvideodowloader.fragments.base.BaseFragment
import com.cyberIyke.allvideodowloader.model.VidInfoItem.VidFormatItem
import com.cyberIyke.allvideodowloader.utils.HistorySQLite
import com.cyberIyke.allvideodowloader.utils.PermissionInterceptor
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.disableSSLCertificateChecking
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getBaseDomain
import com.cyberIyke.allvideodowloader.utils.VisitedPage
import com.cyberIyke.allvideodowloader.viewModel.DownloadsViewModel
import com.cyberIyke.allvideodowloader.viewModel.VidInfoViewModel
import com.cyberIyke.allvideodowloader.views.CustomMediaController
import com.cyberIyke.allvideodowloader.views.CustomVideoView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tonyodev.fetch2.Fetch.addListener
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import javax.net.ssl.SSLSocketFactory

class BrowserWindow constructor(private val activity: Activity?) : BaseFragment(),
    View.OnClickListener, OnBackPressedListener, DialogListener {
    var url: String? = null
        private set
    private var view: View? = null
    private var page: TouchableWebView? = null
    private var defaultSSLSF: SSLSocketFactory? = null
    private var videoFoundTV: FrameLayout? = null
    private var videoFoundView: CustomVideoView? = null
    private var videosFoundHUD: ImageView? = null
    private var imgDetacting: ImageView? = null
    private var foundVideosWindow: View? = null
    private var videoList: VideoList? = null
    private val foundVideosClose: ImageView? = null
    private var loadingPageProgress: ProgressBar? = null
    private val orientation: Int = 0
    private var loadedFirsTime: Boolean = false
    private var blockedWebsites: List<String?>? = null
    private var dialog: BottomSheetDialog? = null
    private val mInterstitialAd: InterstitialAd? = null
    private var context: Context? = null
    private var isVisible: Boolean = false
    private var viewModel: VidInfoViewModel? = null
    private var downloadsViewModel: DownloadsViewModel? = null
    var mVideoInfo: VideoInfo? = null
    public override fun onClick(v: View) {
        if (v === videosFoundHUD) {
            if (videoList != null) {
                XXPermissions.with(activity)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .interceptor(PermissionInterceptor())
                    .request(object : OnPermissionCallback {
                        public override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (!all) {
                                return
                            }
                            dialog!!.show()
                        }

                        public override fun onDenied(permissions: List<String>, never: Boolean) {
                            super@OnPermissionCallback.onDenied(permissions, never)
                            Log.d(BrowserWindow.Companion.TAG, "onDenied: =====")
                        }
                    })
            } else {
                showGuide()
            }
        } else if (v === foundVideosClose) {
            dialog!!.dismiss()
        }
    }

    private fun showGuide() {
        val guide: Dialog = Dialog((getContext())!!)
        guide.setContentView(R.layout.dialog_guide_download)
        val txtGotIt: TextView = guide.findViewById(R.id.txtGotIt)
        txtGotIt.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(view: View) {
                guide.dismiss()
            }
        })
        guide.show()
        guide.getWindow()!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        guide.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: Bundle? = getArguments()
        url = data!!.getString("url")
        defaultSSLSF = HttpsURLConnection.getDefaultSSLSocketFactory()
        blockedWebsites = Arrays.asList(*getResources().getStringArray(R.array.blocked_sites))
        setRetainInstance(true)
        context = getContext()
    }

    private fun createVideosFoundTV() {
        videoFoundTV = view!!.findViewById(R.id.videoFoundTV)
        videoFoundView = view!!.findViewById(R.id.videoFoundView)
        val mediaFoundController: CustomMediaController =
            view!!.findViewById(R.id.mediaFoundController)
        mediaFoundController.setFullscreenEnabled()
        videoFoundView.setMediaController(mediaFoundController)
        videoFoundTV.setVisibility(View.GONE)
    }

    private fun createVideosFoundHUD() {
        videosFoundHUD = view!!.findViewById(R.id.videosFoundHUD)
        videosFoundHUD.setOnClickListener(this)
    }

    private fun createFoundVideosWindow() {
        dialog = BottomSheetDialog((activity)!!, R.style.CustomBottomSheetDialogTheme)
        dialog!!.setCancelable(true)
        dialog!!.setContentView(R.layout.bottom_download_options)
        val qualities: RecyclerView? = dialog!!.findViewById(R.id.qualities_rv)
        val imgVideo: ImageView? = dialog!!.findViewById(R.id.imgVideo)
        val txtTitle: EditText? = dialog!!.findViewById(R.id.txtTitle)
        val txtDownload: TextView? = dialog!!.findViewById(R.id.txtDownload)
        val dismiss: ImageView? = dialog!!.findViewById(R.id.dismiss)
        assert(dismiss != null)
        dismiss!!.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(view: View) {
                dialog!!.dismiss()
            }
        })
        qualities!!.setLayoutManager(GridLayoutManager(activity, 3))
        qualities.setHasFixedSize(true)
        foundVideosWindow = view!!.findViewById(R.id.foundVideosWindow)
        viewModel!!.vidFormats.observe(getViewLifecycleOwner(), Observer({ videoInfo: VideoInfo? ->
            if (videoInfo == null || videoInfo.getFormats() == null) {
                return@observe
            }
            videoInfo.getFormats().removeIf(Predicate({ it: VideoFormat ->
                !it.getExt().contains("mp4") || it.getFormat().contains("audio")
            }))
            val namesAlreadySeen: MutableSet<String> = HashSet()
            videoInfo.getFormats().removeIf(Predicate({ p: VideoFormat ->
                !namesAlreadySeen.add(
                    BrowserWindow.Companion.convertSolution(p.getFormat())
                )
            }))
            mVideoInfo = videoInfo
            if (videoList != null) {
                videoList!!.recreateVideoList(
                    qualities,
                    imgVideo,
                    txtTitle,
                    txtDownload,
                    dialog,
                    videoInfo
                )
            } else {
                videoList = object : VideoList(
                    activity,
                    qualities,
                    imgVideo,
                    txtTitle,
                    txtDownload,
                    dialog,
                    videoInfo
                ) {
                    public override fun onItemClicked(vidFormatItem: VidFormatItem?) {
                        viewModel!!.selectedItem = (vidFormatItem)!!
                        DownloadPathDialogFragment().show(
                            getChildFragmentManager(),
                            "download_location_chooser_dialog"
                        )
                    }
                }
            }
            updateFoundVideosBar()
        }))
    }

    public override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (view == null || getResources().getConfiguration().orientation != orientation) {
            var visibility: Int = View.VISIBLE
            if (view != null) {
                visibility = view!!.getVisibility()
            }
            view = inflater.inflate(R.layout.browser_lay, container, false)
            viewModel = ViewModelProvider(this).get(
                VidInfoViewModel::class.java
            )
            downloadsViewModel = ViewModelProvider(this).get(
                DownloadsViewModel::class.java
            )
            view.setVisibility(visibility)
            if (page == null) {
                page = view.findViewById(R.id.page)
            } else {
                val page1: View = view.findViewById(R.id.page)
                (view as ViewGroup?)!!.removeView(page1)
                (page!!.getParent() as ViewGroup).removeView(page)
                (view as ViewGroup?)!!.addView(page)
                (view as ViewGroup?)!!.bringChildToFront(view.findViewById(R.id.videosFoundHUD))
                (view as ViewGroup?)!!.bringChildToFront(view.findViewById(R.id.foundVideosWindow))
            }
            loadingPageProgress = view.findViewById(R.id.loadingPageProgress)
            loadingPageProgress.setVisibility(View.GONE)
            imgDetacting = view.findViewById(R.id.imgDetacting)
            val rotate: ObjectAnimator =
                ObjectAnimator().ofFloat(imgDetacting, "rotation", 0f, 360f)
            rotate.setDuration(1000)
            rotate.setRepeatCount(999999999)
            rotate.start()
            createVideosFoundHUD()
            createVideosFoundTV()
            createFoundVideosWindow()
            webViewLightDark()
        }
        return view
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    public override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val fetchedUrls: MutableSet<String?> = HashSet()
        if (!loadedFirsTime) {
            page!!.getSettings().setJavaScriptEnabled(true)
            page!!.getSettings().setDomStorageEnabled(true)
            page!!.getSettings().setAllowUniversalAccessFromFileURLs(true)
            page!!.getSettings().setJavaScriptCanOpenWindowsAutomatically(true)
            page!!.setWebViewClient(object : WebViewClient() {
                //it seems not setting webclient, launches
                public override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    Log.d(
                        BrowserWindow.Companion.TAG,
                        "shouldOverrideUrlLoading: " + request.getUrl().toString()
                    )
                    if (blockedWebsites!!.contains(getBaseDomain(request.getUrl().toString()))) {
                        val dialog: Dialog = Dialog((getActivity())!!)
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                        dialog.setContentView(R.layout.dialog_youtube_not_supported)
                        val txtGotIt: TextView = dialog.findViewById(R.id.txtGotIt)
                        txtGotIt.setOnClickListener(object : View.OnClickListener {
                            public override fun onClick(v: View) {
                                dialog.dismiss()
                            }
                        })
                        dialog.show()
                        dialog.getWindow()!!
                            .setLayout(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT
                            )
                        dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        return true
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }

                public override fun onPageStarted(webview: WebView, url: String, favicon: Bitmap) {
                    mVideoInfo = null
                    Handler(Looper.getMainLooper()).post(object : Runnable {
                        public override fun run() {
                            mVideoInfo = null
                            Log.d(
                                BrowserWindow.Companion.TAG,
                                "shouldOverrideUrlLoading: " + url + " " + url
                            )
                            val urlBox: EditText = getBaseActivity().findViewById(R.id.inputURLText)
                            getBaseActivity().isEnableSuggetion = false
                            urlBox.setText(url)
                            urlBox.setSelection(urlBox.getText().length)
                            this@BrowserWindow.url = url
                            viewModel!!.fetchInfo(url)
                            updateFoundVideosBar()
                        }
                    })
                    view.findViewById<View>(R.id.loadingProgress).setVisibility(View.GONE)
                    loadingPageProgress!!.setVisibility(View.VISIBLE)
                    super.onPageStarted(webview, url, favicon)
                }

                public override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    mVideoInfo = null
                    if (!fetchedUrls.contains(url) || !(view.getUrl() == url)) {
                        viewModel!!.fetchInfo(url)
                        fetchedUrls.add(url)
                        updateFoundVideosBar()
                        Log.d(BrowserWindow.Companion.TAG, "onPageFinished: fetched" + url)
                    }
                    loadingPageProgress!!.setVisibility(View.GONE)
                }

                public override fun onLoadResource(view: WebView, url: String) {
                    Log.d("fb :", "URL: " + url)
                    val viewUrl: String? = view.getUrl()
                    val title: String? = view.getTitle()
                    object : VideoContentSearch(activity, url, viewUrl, title) {
                        public override fun onStartInspectingURL() {
                            disableSSLCertificateChecking()
                        }

                        public override fun onFinishedInspectingURL(finishedAll: Boolean) {
                            HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSLSF)
                        }

                        public override fun onVideoFound(
                            size: String?,
                            type: String?,
                            link: String,
                            name: String?,
                            page: String?,
                            chunked: Boolean,
                            website: String?,
                            audio: Boolean
                        ) {
                            if (size != null && isNumber(size)) {
                                val numericSize: Long = size.toLong()
                                if (numericSize > 700000) {
                                    if (link.contains("mp4") && (mVideoInfo == null) && !link.contains(
                                            "banners"
                                        )
                                    ) {
                                        val hashMap: SortedSet<String> = TreeSet()
                                        hashMap.add(link)
                                        viewModel!!.fetchInfo(hashMap.first())
                                        hashMap.clear()
                                    }
                                    activity!!.runOnUiThread(object : Runnable {
                                        public override fun run() {
                                            imgDetacting!!.setVisibility(View.VISIBLE)
                                            Handler().postDelayed(object : Runnable {
                                                public override fun run() {
                                                    imgDetacting!!.setVisibility(View.GONE)
                                                    updateFoundVideosBar()
                                                }
                                            }, 1000)
                                        }
                                    })
                                }
                            }
                        }
                    }.start()
                }

                public override fun shouldInterceptRequest(
                    view: WebView,
                    url: String
                ): WebResourceResponse? {
                    if (activity != null) {
                        Log.d("VDDebug", "Url: " + url)
                        if (activity.getSharedPreferences("settings", 0)
                                .getBoolean(getString(R.string.adBlockON), true)
                            && (url.contains("ad") || url.contains("banner") || url.contains("pop")) || url.contains(
                                "banners"
                            )
                            && getBaseActivity().getBrowserManager().checkUrlIfAds(url)
                        ) {
                            Log.d("VDDebug", "Ads detected: " + url)
                            return WebResourceResponse(null, null, null)
                        }
                    }
                    return super.shouldInterceptRequest(view, url)
                }

                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                public override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getBaseActivity() !=
                        null
                    ) {
                        if ((MyApp.getInstance()!!.getSharedPreferences("settings", 0)
                                .getBoolean(getString(R.string.adBlockON), true)
                                    && ((request.getUrl().toString().contains("ad") ||
                                    request.getUrl().toString().contains("banner") ||
                                    request.getUrl().toString().contains("banners") ||
                                    request.getUrl().toString().contains("pop")))
                                    && getBaseActivity().getBrowserManager().checkUrlIfAds(
                                request.getUrl()
                                    .toString()
                            ))
                        ) {
                            Log.i("VDInfo", "Ads detected: " + request.getUrl().toString())
                            return WebResourceResponse(null, null, null)
                        } else return null
                    } else {
                        return shouldInterceptRequest(view, request.getUrl().toString())
                    }
                }
            })
            page!!.setWebChromeClient(object : WebChromeClient() {
                public override fun onProgressChanged(view: WebView, newProgress: Int) {
                    if (!fetchedUrls.contains(url)) {
                        viewModel!!.fetchInfo((url)!!)
                        fetchedUrls.add(url)
                    }
                    loadingPageProgress!!.setProgress(newProgress)
                }

                public override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                    super.onReceivedIcon(view, icon)
                }

                public override fun onReceivedTitle(view: WebView, title: String) {
                    super.onReceivedTitle(view, title)
                    updateFoundVideosBar()
                    val vp: VisitedPage = VisitedPage()
                    vp.title = title
                    vp.link = view.getUrl()
                    val db: HistorySQLite = HistorySQLite(activity)
                    db.addPageToHistory(vp)
                    db.close()
                }

                public override fun getDefaultVideoPoster(): Bitmap? {
                    return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
                }
            })
            page!!.loadUrl((url)!!)
            loadedFirsTime = true
        } else {
            val urlBox: EditText = getBaseActivity().findViewById(R.id.inputURLText)
            getBaseActivity().isEnableSuggetion = false
            urlBox.setText(url)
        }
    }

    fun isNumber(string: String): Boolean {
        try {
            val amount: Int = string.toInt()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun webViewLightDark() {
        val currentNightMode: Int =
            getResources().getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_YES -> if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(
                    page!!.getSettings(),
                    WebSettingsCompat.FORCE_DARK_ON
                )
            }
            Configuration.UI_MODE_NIGHT_NO -> if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(
                    page!!.getSettings(),
                    WebSettingsCompat.FORCE_DARK_OFF
                )
            }
            else -> {}
        }
    }

    public override fun onDestroy() {
        page!!.stopLoading()
        page!!.destroy()
        super.onDestroy()
    }

    private fun updateFoundVideosBar() {
        if (mVideoInfo != null && mVideoInfo!!.getFormats().size > 0) {
            activity!!.runOnUiThread(object : Runnable {
                public override fun run() {
                    val options: RequestOptions = RequestOptions()
                        .skipMemoryCache(true)
                        .centerInside()
                        .placeholder(R.drawable.ic_download_det)
                        .transform(CircleCrop())
                    Glide.with((activity))
                        .load(R.drawable.ic_download_det)
                        .apply(options)
                        .into((videosFoundHUD)!!)
                    val animY: ObjectAnimator =
                        ObjectAnimator.ofFloat(videosFoundHUD, "translationY", -100f, 0f)
                    animY.setDuration(1000)
                    animY.setInterpolator(BounceInterpolator())
                    animY.setRepeatCount(2)
                    animY.addListener(object : AnimatorListenerAdapter() {
                        public override fun onAnimationEnd(
                            animation: Animator?,
                            isReverse: Boolean
                        ) {
                            super.onAnimationEnd((animation)!!, isReverse)
                            Glide.with((activity))
                                .load(R.drawable.ic_download_enable)
                                .into((videosFoundHUD)!!)
                        }
                    })
                    animY.start()
                }
            })
        } else {
            activity!!.runOnUiThread(object : Runnable {
                public override fun run() {
                    Glide.with((activity))
                        .load(R.drawable.ic_download_dis)
                        .into((videosFoundHUD)!!)
                    if (foundVideosWindow!!.getVisibility() == View.VISIBLE) foundVideosWindow!!.setVisibility(
                        View.GONE
                    )
                }
            })
        }
    }

    private fun updateVideoPlayer(url: String) {
        videoFoundTV!!.setVisibility(View.VISIBLE)
        val uri: Uri = Uri.parse(url)
        videoFoundView!!.setVideoURI(uri)
        videoFoundView!!.start()
    }

    public override fun onBackpressed() {
        if ((foundVideosWindow!!.getVisibility() == View.VISIBLE) && !videoFoundView!!.isPlaying() && (videoFoundTV!!.getVisibility() == View.GONE)) {
            foundVideosWindow!!.setVisibility(View.GONE)
        } else if (videoFoundView!!.isPlaying() || videoFoundTV!!.getVisibility() == View.VISIBLE) {
            videoFoundView!!.closePlayer()
            videoFoundTV!!.setVisibility(View.GONE)
        } else if (page!!.canGoBack()) {
            page!!.goBack()
        } else {
            getBaseActivity().getBrowserManager().closeWindow(this@BrowserWindow)
        }
    }

    val webView: WebView?
        get() {
            return page
        }

    public override fun onPause() {
        super.onPause()
        if (page != null) page!!.onPause()
        Log.d("debug", "onPause: ")
    }

    fun onResumeForce() {
        page!!.onResume()
        Log.d("debug", "onResume: ")
    }

    public override fun onResume() {
        super.onResume()
        if (getBaseActivity().navView.getSelectedItemId() == R.id.navHome && !getBaseActivity().isDisableOnResume) {
            if (page != null) page!!.onResume()
        }
        Handler().postDelayed(object : Runnable {
            public override fun run() {
                getBaseActivity().isDisableOnResume = false
            }
        }, 500)
    }

    public override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        isVisible = isVisibleToUser
    }

    public override fun onOk(dialog: DownloadPathDialogFragment) {
        val path: String? = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(getString(R.string.download_location_key), null)
        if (path == null) {
            Toast.makeText(context, R.string.invalid_download_location, Toast.LENGTH_SHORT).show()
        }
        removeDialog()
        viewModel!!.startDownload(
            viewModel!!.selectedItem,
            (path)!!,
            (activity)!!,
            getViewLifecycleOwner()
        )
        // downloadsViewModel.getId(viewModel.selectedItem.getId(), activity);
    }

    public override fun onFilePicker(dialog: DownloadPathDialogFragment) {
        val intent: Intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        )
        startActivityForResult(intent, BrowserWindow.Companion.OPEN_DIRECTORY_REQUEST_CODE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BrowserWindow.Companion.OPEN_DIRECTORY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data!!.getData()
                activity!!.getContentResolver().takePersistableUriPermission(
                    (uri)!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                setDefaultDownloadLocation(uri.toString())
                removeDialog()
                viewModel!!.startDownload(
                    viewModel!!.selectedItem,
                    uri.toString(),
                    (activity),
                    getViewLifecycleOwner()
                )
            }
        }
    }

    private fun setDefaultDownloadLocation(path: String) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if ((prefs.getString(getString(R.string.download_location_key), null) == null)) {
            prefs.edit().putString(getString(R.string.download_location_key), path).apply()
        }
    }

    private fun removeDialog() {
        val inflater: LayoutInflater = activity!!.getLayoutInflater()
        val layout: View = inflater.inflate(
            R.layout.toast_download,
            activity.findViewById<View>(R.id.toast_layout_root) as ViewGroup?
        )
        val toast: Toast = Toast(activity)
        toast.setGravity(Gravity.BOTTOM, 0, 250)
        toast.setDuration(Toast.LENGTH_LONG)
        toast.setView(layout)
        toast.show()
        (activity as MainActivity?)!!.downloadCount =
            ((activity as MainActivity?)!!.downloadCount + 1)
        (activity as MainActivity?)!!.badgeDownload.setNumber((activity as MainActivity?)!!.downloadCount)
        dialog!!.dismiss()
    }

    companion object {
        private val TAG: String = BrowserWindow::class.java.getCanonicalName()
        private val OPEN_DIRECTORY_REQUEST_CODE: Int = 42069
        fun convertSolution(str: String): String {
            val str2: String
            val split: Array<String> = str.split("[^\\d]+".toRegex()).toTypedArray()
            if (split.size >= 2) {
                str2 = split.get(1)
            } else {
                if (split.size == 1) {
                    str2 = split.get(0)
                } else if (!str.matches(".*\\d.*")) {
                    return str
                } else {
                    return "720P"
                }
            }
            try {
                val parseLong: Long = str2.toLong()
                if (parseLong < 240) {
                    return "144P"
                }
                if (parseLong < 360) {
                    return "240P"
                }
                if (parseLong < 480) {
                    return "360P"
                }
                if (parseLong < 720) {
                    return "480P"
                }
                if (parseLong < 1080) {
                    return "720P"
                }
                if (parseLong < 1440) {
                    return "1080P"
                }
                if (parseLong < 2160) {
                    return "1440P"
                }
                return if (parseLong < 4320) "4K" else "8K"
            } catch (unused: NumberFormatException) {
            }
            return str2
        }

        @Throws(IOException::class)
        fun getFileDuration(file: String?, context: Context?): Long {
            val result: IntArray = intArrayOf(0)
            try {
                val mp: MediaPlayer = MediaPlayer()
                mp.setDataSource(file)
                mp.setOnPreparedListener(object : OnPreparedListener {
                    public override fun onPrepared(mp: MediaPlayer) {
                        val duration: Int = mp.getDuration()
                        result.get(0) = duration
                        mp.release() // release the media player once we're done
                    }
                })
                mp.prepareAsync()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return result.get(0).toLong()
        }
    }
}