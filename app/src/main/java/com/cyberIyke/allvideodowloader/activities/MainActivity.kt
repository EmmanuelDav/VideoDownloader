package com.cyberIyke.allvideodowloader.activities

import android.app.Activity
import android.app.Dialog
import android.content.*
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.impl.Schedulers
import com.cyberIyke.allvideodowloader.BuildConfig
import com.cyberIyke.allvideodowloader.MyApp
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.adapters.ShortcutAdapter
import com.cyberIyke.allvideodowloader.adapters.SuggestionAdapter
import com.cyberIyke.allvideodowloader.browser.BrowserManager
import com.cyberIyke.allvideodowloader.database.AppDatabase
import com.cyberIyke.allvideodowloader.database.AppExecutors
import com.cyberIyke.allvideodowloader.database.ShortcutTable
import com.cyberIyke.allvideodowloader.fragments.AllDownloadFragment
import com.cyberIyke.allvideodowloader.fragments.SettingsFragment
import com.cyberIyke.allvideodowloader.helper.WebConnect
import com.cyberIyke.allvideodowloader.interfaces.ShortcutListner
import com.cyberIyke.allvideodowloader.utils.ThemeSettings.Companion.getInstance
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getStatusBarHeight
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.hideSoftKeyboard
import com.cyberIyke.allvideodowloader.views.Badge
import com.cyberIyke.allvideodowloader.views.BadgeRed
import com.cyberIyke.allvideodowloader.views.NotificationBadge
import com.cyberIyke.allvideodowloader.views.NotificationBadgeRed
import com.cyberIyke.allvideodowloader.webservice.Gossip
import com.cyberIyke.allvideodowloader.webservice.Result
import com.cyberIyke.allvideodowloader.webservice.RetrofitClient
import com.cyberIyke.allvideodowloader.webservice.SearchModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gyf.immersionbar.ImmersionBar
import com.yausername.youtubedl_android.YoutubeDL
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity(), View.OnClickListener, OnEditorActionListener {

    private lateinit var searchTextBar: EditText
    lateinit var browserManager: BrowserManager
    private var appLinkData: Uri? = null
    private lateinit var manager: FragmentManager
    lateinit var navView: BottomNavigationView
    lateinit var badge: Badge
    var downloadCount: Int = 0
    lateinit var badgeDownload: BadgeRed
    lateinit var appSettingsBtn: ImageView
    lateinit var searchView: LinearLayout
    private var activity: Activity? = null
    lateinit var imgMore: ImageView
    lateinit var imgTitle: ImageView
    lateinit var imgBlur: ImageView
    lateinit var howToUseBtn: ImageView
    lateinit var llOption: LinearLayout
    lateinit var mainContent: RelativeLayout
    lateinit var rvShortcut: RecyclerView
    private var shortcutAdapter: ShortcutAdapter? = null
    lateinit var tabContainer: RelativeLayout
    lateinit var appSettingsBtn2: ImageView
    lateinit var imgMore2: ImageView
    private var allDownloadFragment: AllDownloadFragment? = null
    var inputURLText: EditText? = null
    var homeContainer: RelativeLayout? = null
    var suggestionAdapter: SuggestionAdapter? = null
    var isEnableSuggetion: Boolean = false
    var isDisableOnResume: Boolean = false
    private var isUpdated = false
    private var preferences: SharedPreferences? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activity = this
        transStatusBar(true)
        val appLinkIntent: Intent = intent
        appLinkData = appLinkIntent.data
        manager = supportFragmentManager
        supportFragmentManager.findFragmentByTag("BM")?.let { fragment ->
            browserManager = fragment as BrowserManager
        } ?: run {
            browserManager = BrowserManager(this@MainActivity)
            manager.beginTransaction().add(browserManager, "BM").commit()
        }
        // Bottom navigation
        navView = findViewById(R.id.bottomNavigationView)
        navView.itemIconTintList = null
        badgeDownload = NotificationBadgeRed.getBadge(navView, 1)
        badgeDownload.setNumber(0)
        badge = NotificationBadge.getBadge(navView, 2)
        badge.number = 1
        badge.tabSelected(false)
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        setUPBrowserToolbarView()
        updateYoutubeDL()
        initViews()
    }

    private fun initViews() {
        allDownloadFragment = AllDownloadFragment()
        homeContainer = findViewById(R.id.homeContainer)
        inputURLText = findViewById(R.id.inputURLText)
        appSettingsBtn2 = findViewById(R.id.appSettingsBtn2)
        imgMore2 = findViewById(R.id.imgMore2)
        tabContainer = findViewById(R.id.tabContainer)
        mainContent = findViewById(R.id.mainContent)
        howToUseBtn = findViewById(R.id.howToUseBtn)
        imgTitle = findViewById(R.id.imgTitle)
        llOption = findViewById(R.id.llOption)
        imgMore = findViewById(R.id.imgMore)
        imgBlur = findViewById(R.id.imgBlur)
        rvShortcut = findViewById(R.id.rvShortcut)
        appSettingsBtn = findViewById(R.id.appSettingsBtn)
        searchView = findViewById(R.id.searchView)
        val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        params.setMargins(0, getStatusBarHeight(this), 0, 0)
        params.addRule(RelativeLayout.ABOVE, R.id.bottomNavigationView)
        mainContent.layoutParams = params
        tabContainer.layoutParams = params
        appSettingsBtn2.setOnClickListener { settingsClicked() }
        appSettingsBtn.setOnClickListener { settingsClicked() }
        imgMore2.setOnClickListener { onPopupButtonClick(imgMore2) }
        imgMore.setOnClickListener(View.OnClickListener { onPopupButtonClick(imgMore) })
        howToUseBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, IntroActivity::class.java)
            startActivity(intent)
        }
        val prefs: SharedPreferences = getSharedPreferences("settings", 0)
        if (!prefs.getBoolean("first_shortcut", false)) {
            prefs.edit().putBoolean("first_shortcut", true).apply()
            insertDefaultShortcut()
        }
        shortcutAdapter = ShortcutAdapter(this, object : ShortcutListner {
            override fun shortcutClick(shortcutTable: ShortcutTable?) {
                if (shortcutTable!!.strTitle.equals(
                        getString(R.string.add_shortcut),
                        ignoreCase = true
                    )
                ) {
                    addShortcut()
                } else {
                    isEnableSuggetion = false
                    searchTextBar.setText(shortcutTable.strURL)
                    browserManager.newWindow(shortcutTable.strURL)
                }
            }

            override fun shortcutRemoveClick(shortcutTable: ShortcutTable?) {
                AppExecutors.instance!!.diskIO().execute {
                    AppDatabase.getDatabase(this@MainActivity).shortcutDao().delete(shortcutTable)
                }
            }
        })
        rvShortcut.layoutManager = GridLayoutManager(this, 4)
        rvShortcut.adapter = shortcutAdapter
        fetchShortcut()
    }

    fun openFile(file: File?) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this@MainActivity,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                (file)!!
            )
            val mime: String? = contentResolver.getType(uri)

            // Open file with user selected app
            val intent: Intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(uri, mime)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addShortcut() {
        val mBottomSheetDialog =
            BottomSheetDialog(this@MainActivity, R.style.CustomBottomSheetDialogTheme)
        mBottomSheetDialog.setContentView(R.layout.dialog_add_shortcut)
        val edtName: EditText? = mBottomSheetDialog.findViewById(R.id.edtName)
        val edtURL: EditText? = mBottomSheetDialog.findViewById(R.id.edtURL)
        edtURL!!.setText(inputURLText!!.text.toString())
        val txtCancel: TextView? = mBottomSheetDialog.findViewById(R.id.txtCancel)
        val txtOK: TextView? = mBottomSheetDialog.findViewById(R.id.txtOK)
        txtCancel!!.setOnClickListener { mBottomSheetDialog.dismiss() }
        txtOK!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val strName: String = edtName!!.text.toString().trim { it <= ' ' }
                var strURL: String = edtURL.text.toString().trim { it <= ' ' }
                if (strName.isEmpty()) {
                    Toast.makeText(activity, "Please enter website name!", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                if (strURL.isEmpty()) {
                    Toast.makeText(activity, "Please enter URL of website!", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                if (strURL.startsWith("http://") || strURL.startsWith("https://")) {
                } else {
                    strURL = "https://$strURL"
                }

                /* if (!Patterns.WEB_URL.matcher(strURL).matches()) {
                    Toast.makeText(activity, "Please enter valid URL of website!", Toast.LENGTH_SHORT).show();
                    return;
                }*/

                mBottomSheetDialog.dismiss()
                val finalStrURL: String = strURL
                AppExecutors.instance!!.diskIO().execute {
                    AppDatabase.getDatabase(this@MainActivity).shortcutDao()
                        .insert(ShortcutTable(R.drawable.ic_default, strName, finalStrURL))
                }
            }
        })
        mBottomSheetDialog.show()
    }

    private fun updateYoutubeDL() {
        preferences = getSharedPreferences("youtbe-dl", MODE_PRIVATE);
        isUpdated = preferences!!.getBoolean("isUpdated", false)
        if (!isUpdated) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val status = withContext(Dispatchers.IO) {
                        YoutubeDL.getInstance().updateYoutubeDL(this@MainActivity, YoutubeDL.UpdateChannel._STABLE)
                    }
                    when (status) {
                        YoutubeDL.UpdateStatus.DONE -> {
                            preferences!!.edit().putBoolean("isUpdated", true).apply()
                        }

                        else -> {

                        }
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "failed to update", e)
                }
            }
        }
    }

    private fun insertDefaultShortcut() {
        AppExecutors.instance!!.diskIO().execute {
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_add_shortcut,
                    getString(R.string.add_shortcut),
                    ""
                )
            )
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_facebook,
                    getString(R.string.facebook),
                    "https://www.facebook.com/"
                )
            )
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_instagram_,
                    getString(R.string.instagram),
                    "https://www.instagram.com/"
                )
            )
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_linkedin,
                    getString(R.string.linkedin),
                    "https://www.linkedin.com/"
                )
            )
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_pinterest,
                    getString(R.string.pinterest),
                    "https://in.pinterest.com/"
                )
            )
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_tiktok,
                    getString(R.string.tiktok),
                    "https://www.tiktok.com/"
                )
            )
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_dailymotion,
                    getString(R.string.dailymotion),
                    "https://www.dailymotion.com/"
                )
            )
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_vimeo,
                    getString(R.string.vimeo),
                    "https://vimeo.com/"
                )
            )
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_buzz_video,
                    getString(R.string.buzz_video),
                    "https://www.buzzvideo.com/"
                )
            )
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_imdb,
                    getString(R.string.imdb),
                    "https://www.imdb.com/"
                )
            )
            AppDatabase.getDatabase(this@MainActivity).shortcutDao().insert(
                ShortcutTable(
                    R.drawable.ic_vlive,
                    getString(R.string.vlive),
                    "https://www.vlive.tv/"
                )
            )
        }
    }

    private fun fetchShortcut() {
        AppDatabase.getDatabase(this).shortcutDao().allShortcut!!.observe(this,
            Observer<List<ShortcutTable?>?> { shortcutTables ->
                if (isFinishing || isDestroyed) return@Observer
                shortcutAdapter!!.setShortcutArrayList(shortcutTables.filterNotNull())
            })
    }

    private fun onPopupButtonClick(button: View?) {
        val popup: PopupMenu = PopupMenu(this, button)
        popup.menuInflater.inflate(R.menu.menu_home, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_new_tab -> addTabDialog()
                R.id.menu_add_shortcut -> addShortcut()
                R.id.menu_copy -> {
                    val strLink: String =
                        searchTextBar.text.toString().trim { it <= ' ' }
                    if (strLink.isNotEmpty()) {
                        val clipboard: ClipboardManager =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip: ClipData = ClipData.newPlainText("label", strLink)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this@MainActivity, "Copied", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.menu_history -> {
                    val intent: Intent = Intent(this@MainActivity, HistoryActivity::class.java)
                    startActivityForResult(intent, 151)
                }
                R.id.menu_feedback -> {
                    val intentFeed: Intent =
                        Intent(this@MainActivity, FeedbackActivity::class.java)
                    startActivity(intentFeed)
                }
            }
            true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true)
        }
        popup.show()
    }

    private fun addTabDialog() {
        val dialog = Dialog(this@MainActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_tab)
        val bottomNavigationView: BottomNavigationView =
            dialog.findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener(object :
            BottomNavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                dialog.dismiss()
                when (item.itemId) {
                    R.id.navHomeTab -> {
                        navView.selectedItemId = R.id.navHome
                        return true
                    }
                    R.id.navDownloadTab -> {
                        navView.selectedItemId = R.id.navDownload
                        return true
                    }
                    R.id.navTabsTab -> {
                        navView.selectedItemId = R.id.navTabs
                        return true
                    }
                    else -> {}
                }
                return false
            }
        })
        val badgeDialog: Badge = NotificationBadge.getBadge(bottomNavigationView, 2)
        badgeDialog.number = badge.number
        badgeDialog.tabSelected(false)
        val howToUseBtn: ImageView = dialog.findViewById(R.id.howToUseBtn)
        howToUseBtn.setOnClickListener {
            dialog.dismiss()
            val intent: Intent = Intent(this@MainActivity, IntroActivity::class.java)
            startActivity(intent)
        }
        val appSettingsBtn: ImageView = dialog.findViewById(R.id.appSettingsBtn)
        appSettingsBtn.setOnClickListener {
            dialog.dismiss()
            settingsClicked()
        }
        val imgMore: ImageView = dialog.findViewById(R.id.imgMore)
        imgMore.setOnClickListener { onPopupButtonChild(imgMore) }
        val edtSearch: EditText = dialog.findViewById(R.id.edtSearch)

        edtSearch.setOnEditorActionListener { v, actionId, event ->
            isDisableOnResume = true
            dialog.dismiss()
            hideSoftKeyboard(this@MainActivity, edtSearch.windowToken)
            navView.selectedItemId = R.id.navHome
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                isEnableSuggetion = false
                suggestionAdapter!!.resultList(null)
                WebConnect(edtSearch, this@MainActivity).connect()
            } else if (actionId == EditorInfo.IME_ACTION_GO) {
                isEnableSuggetion = false
                suggestionAdapter!!.resultList(null)
                WebConnect(edtSearch, this@MainActivity).connect()
            }
            false
        }
        val searchBtn: ImageView = dialog.findViewById(R.id.searchBtn)
        val rvShortcut: RecyclerView = dialog.findViewById(R.id.rvShortcut)
        rvShortcut.layoutManager = GridLayoutManager(this@MainActivity, 4)
        val shortcutAdapter: ShortcutAdapter =
            ShortcutAdapter(this@MainActivity, object : ShortcutListner {
                override fun shortcutClick(shortcutTable: ShortcutTable?) {
                    isDisableOnResume = true
                    dialog.dismiss()
                    if (shortcutTable!!.strTitle.equals(
                            getString(R.string.add_shortcut),
                            ignoreCase = true
                        )
                    ) {
                        addShortcut()
                    } else {
                        isEnableSuggetion = false
                        navView.selectedItemId = R.id.navHome
                        edtSearch.setText(shortcutTable.strURL)
                        hideSoftKeyboard(this@MainActivity, edtSearch.windowToken)
                        WebConnect(edtSearch, this@MainActivity).connect()
                    }
                }

                override fun shortcutRemoveClick(shortcutTable: ShortcutTable?) {}
            })
        rvShortcut.adapter = shortcutAdapter
        AppExecutors.instance!!.diskIO().execute {
            val shortcutTableList: List<ShortcutTable?>? =
                AppDatabase.getDatabase(this@MainActivity).shortcutDao().allShortcutList
            if (shortcutTableList != null) shortcutAdapter.setShortcutArrayList(shortcutTableList.filterNotNull())
        }
        searchBtn.setOnClickListener {
            isDisableOnResume = true
            dialog.dismiss()
            isEnableSuggetion = false
            navView.selectedItemId = R.id.navHome
            hideSoftKeyboard(this@MainActivity, edtSearch.windowToken)
            WebConnect(edtSearch, this@MainActivity).connect()
        }
        dialog.show()
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun onPopupButtonChild(button: View?) {
        val popup: PopupMenu = PopupMenu(this, button)
        popup.menuInflater.inflate(R.menu.menu_new_tab, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_add_shortcut -> addShortcut()
                R.id.menu_copy -> {
                    val strLink: String =
                        searchTextBar.text.toString().trim { it <= ' ' }
                    if (strLink.isNotEmpty()) {
                        val clipboard: ClipboardManager =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip: ClipData = ClipData.newPlainText("label", strLink)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this@MainActivity, "Copied", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.menu_history -> {
                    val intent: Intent = Intent(this@MainActivity, HistoryActivity::class.java)
                    startActivityForResult(intent, 151)
                }
                R.id.menu_feedback -> {
                    val intentFeed: Intent =
                        Intent(this@MainActivity, FeedbackActivity::class.java)
                    startActivity(intentFeed)
                }
            }
            true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true)
        }
        popup.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 151 && data != null) {
            val link: String? = data.getStringExtra("link")
            if (link != null && link.length > 0) {
                isEnableSuggetion = false
                searchTextBar.setText(link)
                WebConnect(searchTextBar, this@MainActivity).connect()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getInstance(this)!!.save(this)
    }

    private fun setUPBrowserToolbarView() {
        val rvSuggetion: RecyclerView = findViewById(R.id.rvSuggetion)
        suggestionAdapter = SuggestionAdapter(object : SuggestionAdapter.SuggetionListner {
            override fun onSuggetion(str: String?) {
                Log.d(MainActivity.Companion.TAG, "onSuggetion: selected: $str")
                hideSoftKeyboard(this@MainActivity, searchTextBar.windowToken)
                isEnableSuggetion = false
                searchTextBar.setText(str)
                WebConnect(searchTextBar, this@MainActivity).connect()
                suggestionAdapter!!.resultList(null)
            }
        })
        rvSuggetion.layoutManager = LinearLayoutManager(this@MainActivity)
        rvSuggetion.adapter = suggestionAdapter
        val btnSearch: ImageView = findViewById(R.id.searchBtn)
        searchTextBar = findViewById(R.id.inputURLText)
        val searchViewTextWatcher: TextWatcher = object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (searchTextBar.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    if (imm.isAcceptingText && isEnableSuggetion) {
                        fetchSearchList(searchTextBar.text.toString().trim { it <= ' ' })
                    }
                    isEnableSuggetion = true
                } else {
                    suggestionAdapter!!.resultList(null)
                }
            }
        }
        searchTextBar.addTextChangedListener(searchViewTextWatcher)
        searchTextBar.setOnEditorActionListener(this)
        btnSearch.setOnClickListener(this)
    }

    private var searchModelCall: Call<SearchModel?>? = null
    private fun fetchSearchList(str: String) {
        if (searchModelCall != null) {
            searchModelCall!!.cancel()
        }
        suggestionAdapter!!.resultList(null)
        Log.d(TAG, "fetchSearchList: $str")
        searchModelCall = RetrofitClient.instance.api.getSearchResult("json", 5, str)
        searchModelCall!!.enqueue(object : Callback<SearchModel?> {
            override fun onResponse(
                call: Call<SearchModel?>?,
                response: Response<SearchModel?>
            ) {
                if (response.isSuccessful && (response.body() != null) && (response.body()!!
                        .gossip != null) && (response.body()!!.gossip?.results != null)
                ) {
                    val searchModel: SearchModel? = response.body()
                    val gossip: Gossip? = searchModel!!.gossip
                    val resultList: List<Result>? = gossip!!.results
                    if (searchTextBar.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                        suggestionAdapter!!.resultList(resultList)
                    } else {
                        suggestionAdapter!!.resultList(null)
                    }
                }
            }

            override fun onFailure(call: Call<SearchModel?>?, t: Throwable?) {
                suggestionAdapter!!.resultList(null)
            }
        })
    }

    private val mOnNavigationItemSelectedListener: BottomNavigationView.OnNavigationItemSelectedListener =
        object : BottomNavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.navHome -> {
                        badge.tabSelected(false)
                        tabContainer.visibility = View.GONE
                        transStatusBar(true)
                        homeClicked()
                        return true
                    }
                    R.id.navDownload -> {
                        downloadCount = 0
                        badgeDownload.setNumber(downloadCount)
                        badge.tabSelected(false)
                        transStatusBar(false)
                        downloadClicked()
                        //searchView.setVisibility(View.GONE);
                        tabContainer.visibility = View.GONE
                        return true
                    }
                    R.id.navTabs -> {
                        badge.tabSelected(true)
                        transStatusBar(false)
                        tabContainer.visibility = View.VISIBLE
                        tabContainer.removeAllViews()
                        tabContainer.addView(browserManager.tabMain)
                        browserManager.pauseCurrentWindow()
                        return true
                    }
                    else -> {}
                }
                return false
            }
        }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.searchBtn -> {
                isEnableSuggetion = false
                suggestionAdapter!!.resultList(null)
                WebConnect(searchTextBar, this).connect()
            }
            else -> {}
        }
    }

    override fun onEditorAction(
        textView: TextView?,
        actionId: Int,
        keyEvent: KeyEvent?
    ): Boolean {
        val handled: Boolean = false
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            isEnableSuggetion = false
            suggestionAdapter!!.resultList(null)
            WebConnect(searchTextBar, this).connect()
        } else if (actionId == EditorInfo.IME_ACTION_GO) {
            isEnableSuggetion = false
            suggestionAdapter!!.resultList(null)
            WebConnect(searchTextBar, this).connect()
        }
        return handled
    }

    fun transStatusBar(isTrans: Boolean) {
        if (isTrans) {
            ImmersionBar.with(this@MainActivity)
                .transparentStatusBar()
                .navigationBarColor(R.color.white)
                .statusBarDarkFont(true)
                .navigationBarDarkIcon(true)
                .init()
        } else {
            ImmersionBar.with(this@MainActivity)
                .statusBarColor(R.color.white)
                .navigationBarColor(R.color.white)
                .statusBarDarkFont(true)
                .navigationBarDarkIcon(true)
                .init()
        }
    }

    fun showTopMenu() {
        imgTitle.visibility = View.VISIBLE
        imgBlur.visibility = View.VISIBLE
        llOption.visibility = View.VISIBLE
        appSettingsBtn2.visibility = View.GONE
        imgMore2.visibility = View.GONE
    }

    fun hideTopMenu() {
        imgBlur.visibility = View.GONE
        imgTitle.visibility = View.GONE
        llOption.visibility = View.GONE
        appSettingsBtn2.visibility = View.VISIBLE
        imgMore2.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        if (suggestionAdapter!!.itemCount > 0) {
            suggestionAdapter!!.resultList(null)
            return
        }
        if (allDownloadFragment!!.isSelectedMode) {
            allDownloadFragment!!.unSelectAll()
            return
        }
        if (navView.selectedItemId == R.id.navDownload) {
            navView.selectedItemId = R.id.navHome
            return
        }
        if (navView.selectedItemId == R.id.navTabs) {
            navView.selectedItemId = R.id.navHome
            return
        }
        if (manager.findFragmentByTag(MainActivity.Companion.DOWNLOAD) != null || manager.findFragmentByTag(
                MainActivity.Companion.HISTORY
            ) != null
        ) {
            MyApp.getInstance()!!.getOnBackPressedListener()!!.onBackpressed()
            browserManager.resumeCurrentWindow()
            navView.selectedItemId = R.id.navHome
        } else if (manager.findFragmentByTag(SETTING) != null) {
            MyApp.getInstance()!!.getOnBackPressedListener()!!.onBackpressed()
            browserManager.resumeCurrentWindow()
            navView.visibility = View.VISIBLE
            navView.selectedItemId = R.id.navHome
        } else if (MyApp.getInstance()!!.getOnBackPressedListener() != null) {
            MyApp.getInstance()!!.getOnBackPressedListener()!!.onBackpressed()
        } else if (shortcutAdapter!!.selectionMode) {
            shortcutAdapter!!.selectionMode = false
            shortcutAdapter!!.notifyDataSetChanged()
        } else {
            val dialog: Dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_confirmation)
            val txtTitle: TextView = dialog.findViewById(R.id.txtTitle)
            val txtDesc: TextView = dialog.findViewById(R.id.txtDesc)
            txtTitle.text = "Exit from app"
            txtDesc.text = "Are you sure you want to exit?"
            val txtNO: TextView = dialog.findViewById(R.id.txtNO)
            val txtOK: TextView = dialog.findViewById(R.id.txtOK)
            txtNO.setOnClickListener { dialog.dismiss() }
            txtOK.setOnClickListener {
                dialog.dismiss()
                finish()
            }
            dialog.show()
            dialog.window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    interface OnBackPressedListener {
        fun onBackpressed()
    }

    fun setOnBackPressedListener(onBackPressedListener: OnBackPressedListener?) {
        MyApp.getInstance()!!.setOnBackPressedListener(onBackPressedListener)
    }

    override fun onStart() {
        super.onStart()
        if (appLinkData != null) {
            browserManager.newWindow(appLinkData.toString())
        }
    }

    fun browserClicked() {
        browserManager.unhideCurrentWindow()
    }

    fun downloadClicked() {
        closeHistory()
        if (manager.findFragmentByTag(MainActivity.Companion.DOWNLOAD) == null) {
            browserManager.hideCurrentWindow()
            browserManager.pauseCurrentWindow()
            manager.beginTransaction()
                .add(R.id.mainContent, (allDownloadFragment)!!, MainActivity.Companion.DOWNLOAD)
                .commit()
        }
    }

    fun settingsClicked() {
        if (manager.findFragmentByTag(SETTING) == null) {
            transStatusBar(false)
            browserManager.hideCurrentWindow()
            browserManager.pauseCurrentWindow()
            navView.visibility = View.GONE
            manager.beginTransaction()
                .add(R.id.mainContent, SettingsFragment(), SETTING)
                .commit()
        }
    }

    fun homeClicked() {
        browserManager.unhideCurrentWindow()
        browserManager.resumeCurrentWindow()
        closeDownloads()
        closeHistory()
    }

    private fun closeDownloads() {
        val fragment: Fragment? = manager.findFragmentByTag(MainActivity.Companion.DOWNLOAD)
        if (fragment != null) {
            manager.beginTransaction().remove(fragment).commit()
        }
    }

    private fun closeHistory() {
        val fragment: Fragment? = manager.findFragmentByTag(MainActivity.Companion.HISTORY)
        if (fragment != null) {
            manager.beginTransaction().remove(fragment).commit()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResultCallback!!.onRequestPermissionsResult(
            requestCode, permissions,
            grantResults
        )
    }

    private var onRequestPermissionsResultCallback: ActivityCompat.OnRequestPermissionsResultCallback? =
        null

    fun setOnRequestPermissionsResultListener(onRequestPermissionsResultCallback: ActivityCompat.OnRequestPermissionsResultCallback?) {
        this.onRequestPermissionsResultCallback = onRequestPermissionsResultCallback
    }

    companion object {
        private val DOWNLOAD: String = "Downloads"
        private val HISTORY: String = "History"
        private val SETTING: String = "Settings"
        private val TAG: String = MainActivity::class.java.canonicalName
    }

}