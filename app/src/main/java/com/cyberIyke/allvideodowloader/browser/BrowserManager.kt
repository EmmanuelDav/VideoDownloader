package com.cyberIyke.allvideodowloader.browser

import android.app.Activity
import android.app.Dialog
import android.content.*
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.activities.IntroActivity
import com.cyberIyke.allvideodowloader.adapters.ShortcutAdapter
import com.cyberIyke.allvideodowloader.browser.BrowserWindow
import com.cyberIyke.allvideodowloader.browserimportimport.BrowserWindow
import com.cyberIyke.allvideodowloader.database.AppExecutors
import com.cyberIyke.allvideodowloader.database.ShortcutAppDatabase
import com.cyberIyke.allvideodowloader.database.ShortcutTable
import com.cyberIyke.allvideodowloader.fragments.base.BaseFragment
import com.cyberIyke.allvideodowloader.fragments.baseimport.BaseFragment
import com.cyberIyke.allvideodowloader.helper.WebConnect
import com.cyberIyke.allvideodowloader.interfaces.ShortcutListner
import com.cyberIyke.allvideodowloader.utils.ThemeSettings.Companion.getInstance
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getBaseDomain
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.hideSoftKeyboard
import com.cyberIyke.allvideodowloader.views.Badge
import com.cyberIyke.allvideodowloader.views.NotificationBadge
import com.cyberIyke.allvideodowloader.views.cardstack.CardStackView
import com.cyberIyke.allvideodowloader.views.cardstack.CardStackView.ItemExpendListener
import com.cyberIyke.allvideodowloader.views.cardstack.StackAdapter
import com.cyberIyke.allvideodowloader.viewsimport.Badge
import com.cyberIyke.allvideodowloader.viewsimport.NotificationBadge
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tonyodev.fetch2.Fetch.Impl.getInstance
import java.io.*
import java.util.*

class BrowserManager constructor(private val activity: Activity) : BaseFragment() {
    private var adBlock: AdBlocker? = null
    var windowsList: MutableList<BrowserWindow?>? = null
    private var blockedWebsites: List<String?>? = null
    private var allWindows: RecyclerView? = null
    private var llCloseAll: LinearLayout? = null
    var cardWindowTab: CardStackView? = null
    var browserTabAdapter: BrowserTabAdapter? = null
    var relativeLayout: RelativeLayout? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
        Log.d("debug", "Browser Manager added")
        windowsList = ArrayList()
        relativeLayout = LayoutInflater.from(getActivity()).inflate(
            R.layout.all_windows_popup,
            getActivity()!!.findViewById<View>(16908290) as ViewGroup?,
            false
        ) as RelativeLayout?
        val llAdd: LinearLayout = relativeLayout!!.findViewById(R.id.llAdd)
        llAdd.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                addTabDialog()
            }
        })
        llCloseAll = relativeLayout!!.findViewById(R.id.llCloseAll)
        llCloseAll.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                closeAllWindow()
            }
        })
        allWindows = relativeLayout!!.findViewById(R.id.rvRecent)
        allWindows.setLayoutManager(LinearLayoutManager(getActivity()))
        allWindows.setAdapter(AllWindowsAdapter())
        cardWindowTab = relativeLayout!!.findViewById(R.id.cardWindowTab)
        browserTabAdapter = BrowserTabAdapter(getActivity())
        cardWindowTab.setItemExpendListener(object : ItemExpendListener {
            public override fun onItemExpend(expend: Boolean) {}
        })
        cardWindowTab.setAdapter(browserTabAdapter)
        val file: File = File(getActivity()!!.getFilesDir(), "ad_filters.dat")
        try {
            if (file.exists()) {
                Log.d("debug", "file exists")
                val fileInputStream: FileInputStream = FileInputStream(file)
                ObjectInputStream(fileInputStream).use({ objectInputStream ->
                    adBlock = objectInputStream.readObject() as AdBlocker?
                })
                fileInputStream.close()
            } else {
                adBlock = AdBlocker()
                val fileOutputStream: FileOutputStream = FileOutputStream(file)
                ObjectOutputStream(fileOutputStream).use({ objectOutputStream ->
                    objectOutputStream.writeObject(
                        adBlock
                    )
                })
                fileOutputStream.close()
            }
        } catch (ignored: IOException) {
            //
        } catch (ignored: ClassNotFoundException) {
        }
        updateAdFilters()
        blockedWebsites = Arrays.asList(*getResources().getStringArray(R.array.blocked_sites))
    }

    fun addTabDialog() {
        val dialog: Dialog = Dialog(getBaseActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_tab)
        val bottomNavigationView: BottomNavigationView =
            dialog.findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener(object :
            BottomNavigationView.OnNavigationItemSelectedListener {
            public override fun onNavigationItemSelected(item: MenuItem): Boolean {
                dialog.dismiss()
                when (item.getItemId()) {
                    R.id.navHomeTab -> {
                        getBaseActivity().navView.setSelectedItemId(R.id.navHome)
                        return true
                    }
                    R.id.navDownloadTab -> {
                        getBaseActivity().navView.setSelectedItemId(R.id.navDownload)
                        return true
                    }
                    R.id.navTabsTab -> {
                        getBaseActivity().navView.setSelectedItemId(R.id.navTabs)
                        return true
                    }
                    else -> {}
                }
                return false
            }
        })
        val badgeDialog: Badge = NotificationBadge.getBadge(bottomNavigationView, 2)
        badgeDialog.setNumber(getBaseActivity().badge.getNumber())
        badgeDialog.tabSelected(false)
        val howToUseBtn: ImageView = dialog.findViewById(R.id.howToUseBtn)
        howToUseBtn.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                dialog.dismiss()
                val intent: Intent = Intent(getBaseActivity(), IntroActivity::class.java)
                startActivity(intent)
            }
        })
        val appSettingsBtn: ImageView = dialog.findViewById(R.id.appSettingsBtn)
        appSettingsBtn.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                dialog.dismiss()
                getBaseActivity().settingsClicked()
            }
        })
        val imgMore: ImageView = dialog.findViewById(R.id.imgMore)
        imgMore.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                getBaseActivity().onPopupButtonChild(imgMore)
            }
        })
        val edtSearch: EditText = dialog.findViewById(R.id.edtSearch)
        edtSearch.setOnEditorActionListener(object : OnEditorActionListener {
            public override fun onEditorAction(
                v: TextView?,
                actionId: Int,
                event: KeyEvent?
            ): Boolean {
                getBaseActivity().isDisableOnResume = true
                dialog.dismiss()
                hideSoftKeyboard(getBaseActivity(), edtSearch.getWindowToken())
                getBaseActivity().navView.setSelectedItemId(R.id.navHome)
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    getBaseActivity().isEnableSuggetion = false
                    getBaseActivity().suggestionAdapter.setResultList(null)
                    WebConnect(edtSearch, getBaseActivity()).connect()
                } else if (actionId == EditorInfo.IME_ACTION_GO) {
                    getBaseActivity().isEnableSuggetion = false
                    getBaseActivity().suggestionAdapter.setResultList(null)
                    WebConnect(edtSearch, getBaseActivity()).connect()
                }
                return false
            }
        })
        val searchBtn: ImageView = dialog.findViewById(R.id.searchBtn)
        val rvShortcut: RecyclerView = dialog.findViewById(R.id.rvShortcut)
        rvShortcut.setLayoutManager(GridLayoutManager(getBaseActivity(), 4))
        val shortcutAdapter: ShortcutAdapter? =
            ShortcutAdapter(getBaseActivity(), object : ShortcutListner {
                public override fun shortcutClick(shortcutTable: ShortcutTable) {
                    getBaseActivity().isDisableOnResume = true
                    dialog.dismiss()
                    if (shortcutTable.strTitle.equals(
                            getString(R.string.add_shortcut),
                            ignoreCase = true
                        )
                    ) {
                        getBaseActivity().addShortcut()
                    } else {
                        getBaseActivity().isEnableSuggetion = false
                        getBaseActivity().navView.setSelectedItemId(R.id.navHome)
                        edtSearch.setText(shortcutTable.strURL)
                        hideSoftKeyboard(getBaseActivity(), edtSearch.getWindowToken())
                        WebConnect(edtSearch, getBaseActivity()).connect()
                    }
                }

                public override fun shortcutRemoveClick(shortcutTable: ShortcutTable?) {}
            })
        rvShortcut.setAdapter(shortcutAdapter)
        AppExecutors.Companion.getInstance().diskIO().execute(object : Runnable {
            public override fun run() {
                val shortcutTableList: List<ShortcutTable>? =
                    ShortcutAppDatabase.Companion.getInstance(getBaseActivity()).shortcutDao()
                        .getAllShortcutList()
                if (shortcutTableList != null && shortcutAdapter != null) shortcutAdapter.setShortcutArrayList(
                    shortcutTableList
                )
            }
        })
        searchBtn.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                getBaseActivity().isDisableOnResume = true
                dialog.dismiss()
                getBaseActivity().isEnableSuggetion = false
                getBaseActivity().navView.setSelectedItemId(R.id.navHome)
                hideSoftKeyboard(getBaseActivity(), edtSearch.getWindowToken())
                WebConnect(edtSearch, getBaseActivity()).connect()
            }
        })
        dialog.show()
        dialog.getWindow()!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun newWindow(url: String?) {
        if (blockedWebsites!!.contains(getBaseDomain(url))) {
            val dialog: Dialog = Dialog((getActivity())!!)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_youtube_not_supported)
            val txtGotIt: TextView = dialog.findViewById(R.id.txtGotIt)
            txtGotIt.setOnClickListener(object : View.OnClickListener {
                public override fun onClick(v: View?) {
                    dialog.dismiss()
                }
            })
            dialog.show()
            dialog.getWindow()!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        } else {
            //getBaseActivity().hideTopMenu();
            val data: Bundle = Bundle()
            data.putString("url", url)
            var window: BrowserWindow? = BrowserWindow(activity)
            window!!.setArguments(data)
            getFragmentManager()!!.beginTransaction()
                .add(R.id.homeContainer, (window), null)
                .commit()
            windowsList!!.add(window)
            getBaseActivity().setOnBackPressedListener(window)
            if (windowsList!!.size > 1) {
                window = windowsList!!.get(windowsList!!.size - 2)
                if (window != null && window.getView() != null) {
                    window.getView()!!.setVisibility(View.GONE)
                    //window.onPause();
                }
            }
            updateNumWindows()
            allWindows!!.getAdapter()!!.notifyDataSetChanged()
            browserTabAdapter!!.setData(windowsList)
            for (posWindow in windowsList!!.indices) {
                val windowTemp: BrowserWindow? = windowsList!!.get(posWindow)
                windowTemp!!.onPause()
            }
            val windowCurrentTemp: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            Handler().postDelayed(object : Runnable {
                public override fun run() {
                    //windowCurrentTemp.onResume();
                }
            }, 1500)
        }
    }

    fun updateNumWindows() {
        if (windowsList!!.size == 0) {
            updateNumWindows(1)
            getBaseActivity().showTopMenu()
        } else {
            getBaseActivity().hideTopMenu()
        }
        for (window: BrowserWindow? in windowsList!!) {
            updateNumWindows(windowsList!!.size)
        }
    }

    fun updateNumWindows(num: Int) {
        val numWindowsString: String = num.toString()
        Handler(Looper.getMainLooper()).post(object : Runnable {
            public override fun run() {
                getBaseActivity().badge.setNumber(num)
            }
        })
    }

    fun closeWindow(window: BrowserWindow?) {
        val inputURLText: EditText = getBaseActivity().findViewById(R.id.inputURLText)
        windowsList!!.remove(window)
        getFragmentManager()!!.beginTransaction().remove((window)!!).commit()
        if (!windowsList!!.isEmpty()) {
            val topWindow: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            if (topWindow != null && topWindow.getView() != null) {
                topWindow.onResume()
                topWindow.getView()!!.setVisibility(View.VISIBLE)
            }
            if (topWindow != null) {
                getBaseActivity().isEnableSuggetion = false
                inputURLText.setText(topWindow.getUrl())
                getBaseActivity().setOnBackPressedListener(topWindow)
            }
        } else {
            getBaseActivity().isEnableSuggetion = false
            inputURLText.getText().clear()
            getBaseActivity().setOnBackPressedListener(null)
        }
        browserTabAdapter!!.setData(windowsList)
        updateNumWindows()
    }

    fun closeAllWindow() {
        if (!windowsList!!.isEmpty()) {
            val iterator: MutableIterator<BrowserWindow?> = windowsList!!.iterator()
            while (iterator.hasNext()) {
                val window: BrowserWindow? = iterator.next()
                getFragmentManager()!!.beginTransaction().remove((window)!!).commit()
                iterator.remove()
            }
            getBaseActivity().setOnBackPressedListener(null)
        } else {
            getBaseActivity().setOnBackPressedListener(null)
        }
        windowsList!!.clear()
        allWindows!!.getAdapter()!!.notifyDataSetChanged()
        browserTabAdapter!!.setData(windowsList)
        updateNumWindows()
    }

    fun hideCurrentWindow() {
        if (!windowsList!!.isEmpty()) {
            val topWindow: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            if (topWindow!!.getView() != null) {
                topWindow.getView()!!.setVisibility(View.GONE)
            }
        }
    }

    fun unhideCurrentWindow() {
        if (!windowsList!!.isEmpty()) {
            val topWindow: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            if (topWindow!!.getView() != null) {
                topWindow.getView()!!.setVisibility(View.VISIBLE)
                getBaseActivity().setOnBackPressedListener(topWindow)
            }
        } else {
            getBaseActivity().setOnBackPressedListener(null)
        }
    }

    fun pauseCurrentWindow() {
        if (!windowsList!!.isEmpty()) {
            val topWindow: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            if (topWindow!!.getView() != null) {
                topWindow.onPause()
            }
        }
    }

    fun resumeCurrentWindow() {
        if (!windowsList!!.isEmpty()) {
            val topWindow: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            if (topWindow!!.getView() != null) {
                // topWindow.onResume();
                Handler().postDelayed(object : Runnable {
                    public override fun run() {
                        topWindow.onResume()
                    }
                }, 500)
                getBaseActivity().setOnBackPressedListener(topWindow)
            }
        } else {
            getBaseActivity().setOnBackPressedListener(null)
        }
    }

    fun updateAdFilters() {
        adBlock!!.update(getContext())
    }

    fun checkUrlIfAds(url: String?): Boolean {
        return adBlock!!.checkThroughFilters(url)
    }

    fun getAllWindows(): View? {
        return allWindows
    }

    val tabMain: View?
        get() {
            return relativeLayout
        }

    fun switchWindow(index: Int) {
        val list: List<BrowserWindow?>? = windowsList
        val topWindow: BrowserWindow? = list!!.get(list.size - 1)
        if (topWindow!!.getView() != null) {
            topWindow.getView()!!.setVisibility(View.GONE)
        }
        val window: BrowserWindow? = windowsList!!.get(index)
        windowsList!!.removeAt(index)
        windowsList!!.add(window)
        if (window!!.getView() != null) {
            window.getView()!!.setVisibility(View.VISIBLE)
            getBaseActivity().setOnBackPressedListener(window)
            getBaseActivity().isEnableSuggetion = false
            getBaseActivity().inputURLText.setText(window.getUrl())
        }
        for (posWindow in windowsList!!.indices) {
            val windowTemp: BrowserWindow? = windowsList!!.get(posWindow)
            windowTemp!!.onPause()
        }
        val windowCurrentTemp: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
        Handler().postDelayed(object : Runnable {
            public override fun run() {
                windowCurrentTemp!!.onResume()
            }
        }, 500)
        allWindows!!.getAdapter()!!.notifyDataSetChanged()
        browserTabAdapter!!.setData(windowsList)
    }

    inner class BrowserTabAdapter constructor(context: Context?) :
        StackAdapter<BrowserWindow>(context) {
        override fun onCreateView(parent: ViewGroup?, viewType: Int): CardStackView.ViewHolder {
            return ColorItemViewHolder(
                getLayoutInflater().inflate(
                    R.layout.all_windows_popup_item,
                    parent,
                    false
                )
            )
        }

        public override fun bindView(
            data: BrowserWindow,
            position: Int,
            viewHolder: CardStackView.ViewHolder?
        ) {
            if (viewHolder is ColorItemViewHolder) {
                val holder: ColorItemViewHolder = viewHolder
                val webView: WebView? = data.getWebView()
                if (webView != null) {
                    if (webView.getTitle() == null || webView.getTitle()!!.length == 0) {
                        holder.windowTitle.setText("Home")
                    } else {
                        holder.windowTitle.setText(webView.getTitle())
                    }
                    if (webView.getFavicon() == null) {
                        Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon)
                        Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon)
                    } else {
                        holder.favicon.setImageBitmap(webView.getFavicon())
                        holder.imgLargeIcon.setImageBitmap(webView.getFavicon())
                    }
                } else {
                    holder.windowTitle.setText("Home")
                    Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon)
                    Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon)
                }
                holder.close.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
                        closeWindow(windowsList!!.get(position))
                        notifyDataSetChanged()
                    }
                })
                holder.itemView.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
                        //getBaseActivity().homeContainer.setVisibility(View.VISIBLE);
                        switchWindow(position)
                        getBaseActivity().navView.setSelectedItemId(R.id.navHome)
                    }
                })
            }
        }

        internal inner class ColorItemViewHolder constructor(view: View) :
            CardStackView.ViewHolder(view) {
            var favicon: ImageView
            var windowTitle: TextView
            var close: ImageView
            var imgLargeIcon: ImageView

            init {
                windowTitle = itemView.findViewById<View>(R.id.windowTitle) as TextView
                favicon = itemView.findViewById<View>(R.id.favicon) as ImageView
                close = itemView.findViewById<View>(R.id.close) as ImageView
                imgLargeIcon = itemView.findViewById(R.id.imgLargeIcon)
            }

            public override fun onItemExpand(b: Boolean) {}
        }
    }

    private inner class AllWindowsAdapter() : RecyclerView.Adapter<WindowItem>() {
        public override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowItem {
            return WindowItem(
                LayoutInflater.from(getActivity())
                    .inflate(R.layout.all_windows_popup_item, parent, false)
            )
        }

        public override fun onBindViewHolder(holder: WindowItem, position: Int) {
            val webView: WebView? = (windowsList!!.get(position))!!.getWebView()
            if (webView != null) {
                if (webView.getTitle() == null || webView.getTitle()!!.length == 0) {
                    holder.windowTitle.setText("Home")
                } else {
                    holder.windowTitle.setText(webView.getTitle())
                }
                if (webView.getFavicon() == null) {
                    Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon)
                    Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon)
                } else {
                    holder.favicon.setImageBitmap(webView.getFavicon())
                    holder.imgLargeIcon.setImageBitmap(webView.getFavicon())
                }
            } else {
                holder.windowTitle.setText("Home")
                Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon)
                Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon)
            }
            holder.close.setOnClickListener(object : View.OnClickListener {
                public override fun onClick(v: View?) {
                    closeWindow(windowsList!!.get(position))
                }
            })
            holder.itemView.setOnClickListener(object : View.OnClickListener {
                public override fun onClick(v: View?) {
                    //getBaseActivity().homeContainer.setVisibility(View.VISIBLE);
                    switchWindow(position)
                    getBaseActivity().navView.setSelectedItemId(R.id.navHome)
                }
            })
        }

        public override fun getItemCount(): Int {
            return windowsList!!.size
        }
    }

    fun getDominantColor(bitmap: Bitmap?): Int {
        if (null == bitmap) return Color.TRANSPARENT
        var redBucket: Int = 0
        var greenBucket: Int = 0
        var blueBucket: Int = 0
        var alphaBucket: Int = 0
        val hasAlpha: Boolean = bitmap.hasAlpha()
        val pixelCount: Int = bitmap.getWidth() * bitmap.getHeight()
        val pixels: IntArray = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight())
        var y: Int = 0
        val h: Int = bitmap.getHeight()
        while (y < h) {
            var x: Int = 0
            val w: Int = bitmap.getWidth()
            while (x < w) {
                val color: Int = pixels.get(x + y * w) // x + y * width
                redBucket += (color shr 16) and 0xFF // Color.red
                greenBucket += (color shr 8) and 0xFF // Color.greed
                blueBucket += (color and 0xFF) // Color.blue
                if (hasAlpha) alphaBucket += (color ushr 50) // Color.alpha
                x++
            }
            y++
        }
        return Color.argb(
            if ((hasAlpha)) (alphaBucket / pixelCount) else 255,
            redBucket / pixelCount,
            greenBucket / pixelCount,
            blueBucket / pixelCount
        )
    }

    inner class WindowItem internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var favicon: ImageView
        var windowTitle: TextView
        var close: ImageView
        var imgLargeIcon: ImageView

        init {
            windowTitle = itemView.findViewById<View>(R.id.windowTitle) as TextView
            favicon = itemView.findViewById<View>(R.id.favicon) as ImageView
            close = itemView.findViewById<View>(R.id.close) as ImageView
            imgLargeIcon = itemView.findViewById(R.id.imgLargeIcon)
        }
    }
}