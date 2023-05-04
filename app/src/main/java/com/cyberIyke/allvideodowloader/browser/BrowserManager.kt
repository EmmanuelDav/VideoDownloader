package com.cyberIyke.allvideodowloader.browser

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
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
import com.cyberIyke.allvideodowloader.MyApp.Companion.TAG
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.activities.IntroActivity
import com.cyberIyke.allvideodowloader.adapters.ShortcutAdapter
import com.cyberIyke.allvideodowloader.database.AppDatabase
import com.cyberIyke.allvideodowloader.database.AppExecutors
import com.cyberIyke.allvideodowloader.database.ShortcutTable
import com.cyberIyke.allvideodowloader.fragments.base.BaseFragment
import com.cyberIyke.allvideodowloader.helper.WebConnect
import com.cyberIyke.allvideodowloader.interfaces.ShortcutListner
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getBaseDomain
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.hideSoftKeyboard
import com.cyberIyke.allvideodowloader.views.Badge
import com.cyberIyke.allvideodowloader.views.NotificationBadge
import com.cyberIyke.allvideodowloader.views.cardstack.CardStackView
import com.cyberIyke.allvideodowloader.views.cardstack.StackAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import java.io.*

class BrowserManager(val activity: Activity) : BaseFragment() {
    private var adBlock: AdBlocker? = null
    var windowsList: MutableList<BrowserWindow>? = null
    private var blockedWebsites: List<String?>? = null
    private lateinit var allWindows: RecyclerView
    private lateinit var llCloseAll: LinearLayout
    lateinit var cardWindowTab: CardStackView
    lateinit var browserTabAdapter: BrowserTabAdapter
    lateinit var relativeLayout: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        Log.d("debug", "Browser Manager added")
        windowsList = ArrayList()
        relativeLayout = (LayoutInflater.from(getActivity()).inflate(
            R.layout.all_windows_popup,
            requireActivity().findViewById<View>(16908290) as ViewGroup?,
            false
        ) as RelativeLayout?)!!
        val llAdd: LinearLayout = relativeLayout.findViewById(R.id.llAdd)
        llAdd.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                addTabDialog()
            }
        })
        llCloseAll = relativeLayout.findViewById(R.id.llCloseAll)
        llCloseAll.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                closeAllWindow()
            }
        })
        allWindows = relativeLayout.findViewById(R.id.rvRecent)
        allWindows.layoutManager = LinearLayoutManager(getActivity())
        allWindows.adapter = AllWindowsAdapter()
        cardWindowTab = relativeLayout.findViewById(R.id.cardWindowTab)
        browserTabAdapter = BrowserTabAdapter(getActivity())
        cardWindowTab.setAdapter(browserTabAdapter)
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val adBlockerJson = sharedPrefs.getString("adBlocker", null)
        adBlock = if (adBlockerJson != null) {
            Gson().fromJson(adBlockerJson, AdBlocker::class.java)
        } else {
            AdBlocker()
        }
        val adBlockerJsonn = Gson().toJson(adBlock)
        sharedPrefs.edit().putString("adBlocker", adBlockerJsonn).apply()
        updateAdFilters()
        blockedWebsites = listOf(*resources.getStringArray(R.array.blocked_sites))
    }

    fun addTabDialog() {
        val dialog = Dialog(baseActivity!!)
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
                        baseActivity!!.navView.selectedItemId = R.id.navHome
                        return true
                    }
                    R.id.navDownloadTab -> {
                        baseActivity!!.navView.setSelectedItemId(R.id.navDownload)
                        return true
                    }
                    R.id.navTabsTab -> {
                        baseActivity!!.navView.setSelectedItemId(R.id.navTabs)
                        return true
                    }
                    else -> {}
                }
                return false
            }
        })
        val badgeDialog: Badge = NotificationBadge.getBadge(bottomNavigationView, 2)
        badgeDialog.number = baseActivity!!.badge.number
        badgeDialog.tabSelected(false)
        val howToUseBtn: ImageView = dialog.findViewById(R.id.howToUseBtn)
        howToUseBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                dialog.dismiss()
                val intent: Intent = Intent(baseActivity, IntroActivity::class.java)
                startActivity(intent)
            }
        })
        val appSettingsBtn: ImageView = dialog.findViewById(R.id.appSettingsBtn)
        appSettingsBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                dialog.dismiss()
                baseActivity!!.settingsClicked()
            }
        })
        val imgMore: ImageView = dialog.findViewById(R.id.imgMore)
        imgMore.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                baseActivity!!.onPopupButtonChild(imgMore)
            }
        })
        val edtSearch: EditText = dialog.findViewById(R.id.edtSearch)
        edtSearch.setOnEditorActionListener(object : OnEditorActionListener {
            override fun onEditorAction(
                v: TextView?,
                actionId: Int,
                event: KeyEvent?
            ): Boolean {
                baseActivity!!.isDisableOnResume = true
                dialog.dismiss()
                hideSoftKeyboard(baseActivity!!, edtSearch.windowToken)
                baseActivity!!.navView.selectedItemId = R.id.navHome
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    baseActivity!!.isEnableSuggetion = false
                    baseActivity!!.suggestionAdapter!!.resultList(null)
                    WebConnect(edtSearch, baseActivity!!).connect()
                } else if (actionId == EditorInfo.IME_ACTION_GO) {
                    baseActivity!!.isEnableSuggetion = false
                    baseActivity!!.suggestionAdapter!!.resultList(null)
                    WebConnect(edtSearch, baseActivity!!).connect()
                }
                return false
            }
        })
        val searchBtn: ImageView = dialog.findViewById(R.id.searchBtn)
        val rvShortcut: RecyclerView = dialog.findViewById(R.id.rvShortcut)
        rvShortcut.layoutManager = GridLayoutManager(baseActivity, 4)
        val shortcutAdapter: ShortcutAdapter? =
            ShortcutAdapter(baseActivity!!, object : ShortcutListner {
                override fun shortcutClick(shortcutTable: ShortcutTable?) {
                    baseActivity!!.isDisableOnResume = true
                    dialog.dismiss()
                    if (shortcutTable!!.strTitle.equals(
                            getString(R.string.add_shortcut),
                            ignoreCase = true
                        )
                    ) {
                        baseActivity!!.addShortcut()
                    } else {
                        baseActivity!!.isEnableSuggetion = false
                        baseActivity!!.navView.selectedItemId = R.id.navHome
                        edtSearch.setText(shortcutTable.strURL)
                        hideSoftKeyboard(baseActivity!!, edtSearch.windowToken)
                        WebConnect(edtSearch, baseActivity!!).connect()
                    }
                }

                override fun shortcutRemoveClick(shortcutTable: ShortcutTable?) {}
            })
        rvShortcut.adapter = shortcutAdapter
        AppExecutors.instance!!.diskIO().execute {
            val shortcutTableList: List<ShortcutTable>? = AppDatabase.getDatabase(baseActivity!!).shortcutDao().allShortcutList as List<ShortcutTable>?
            if (shortcutTableList != null && shortcutAdapter != null) shortcutAdapter.setShortcutArrayList(shortcutTableList)
        }
        searchBtn.setOnClickListener {
            baseActivity!!.isDisableOnResume = true
            dialog.dismiss()
            baseActivity!!.isEnableSuggetion = false
            baseActivity!!.navView.setSelectedItemId(R.id.navHome)
            hideSoftKeyboard(baseActivity!!, edtSearch.windowToken)
            WebConnect(edtSearch, baseActivity!!).connect()
        }
        dialog.show()
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun newWindow(url: String?) {
        if (blockedWebsites!!.contains(getBaseDomain(url))) {
            val dialog: Dialog = Dialog((getActivity())!!)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_youtube_not_supported)
            val txtGotIt: TextView = dialog.findViewById(R.id.txtGotIt)
            txtGotIt.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    dialog.dismiss()
                }
            })
            dialog.show()
            dialog.window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        } else {
            //getBaseActivity().hideTopMenu();
            val data: Bundle = Bundle()
            data.putString("url", url)
            var window: BrowserWindow? = BrowserWindow(activity)
            window!!.arguments = data
            requireFragmentManager().beginTransaction()
                .add(R.id.homeContainer, (window), null)
                .commit()
            windowsList!!.add(window)
            baseActivity!!.setOnBackPressedListener(window)
            if (windowsList!!.size > 1) {
                window = windowsList!!.get(windowsList!!.size - 2)
                if (window != null && window.view != null) {
                    window.requireView().visibility = View.GONE
                    //window.onPause();
                }
            }
            updateNumWindows()
            allWindows.adapter!!.notifyDataSetChanged()
            browserTabAdapter.setData(windowsList!!)
            for (posWindow in windowsList!!.indices) {
                val windowTemp: BrowserWindow? = windowsList!![posWindow]
                windowTemp!!.onPause()
            }
            val windowCurrentTemp: BrowserWindow? = windowsList!![windowsList!!.size - 1]
            Handler().postDelayed({
                //windowCurrentTemp.onResume();
            }, 1500)
        }
    }

    private fun updateNumWindows() {
        if (windowsList!!.size == 0) {
            updateNumWindows(1)
            baseActivity!!.showTopMenu()
        } else {
            baseActivity!!.hideTopMenu()
        }
        for (window: BrowserWindow? in windowsList!!) {
            updateNumWindows(windowsList!!.size)
        }
    }

    fun updateNumWindows(num: Int) {
        num.toString()
        Handler(Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                baseActivity!!.badge.number = num
            }
        })
    }

    fun closeWindow(window: BrowserWindow?) {
        val inputURLText: EditText = baseActivity!!.findViewById(R.id.inputURLText)
        windowsList!!.remove(window)
        requireFragmentManager().beginTransaction().remove((window)!!).commit()
        if (!windowsList!!.isEmpty()) {
            val topWindow: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            if (topWindow != null && topWindow.view != null) {
                topWindow.onResume()
                topWindow.requireView().visibility = View.VISIBLE
            }
            if (topWindow != null) {
                baseActivity!!.isEnableSuggetion = false
                inputURLText.setText(topWindow.url)
                baseActivity!!.setOnBackPressedListener(topWindow)
            }
        } else {
            baseActivity!!.isEnableSuggetion = false
            inputURLText.text.clear()
            baseActivity!!.setOnBackPressedListener(null)
        }
        browserTabAdapter.setData(windowsList!!)
        updateNumWindows()
    }

    fun closeAllWindow() {
        if (windowsList!!.isNotEmpty()) {
            val iterator: MutableIterator<BrowserWindow?> = windowsList!!.iterator()
            while (iterator.hasNext()) {
                val window: BrowserWindow? = iterator.next()
                requireFragmentManager().beginTransaction().remove((window)!!).commit()
                iterator.remove()
            }
            baseActivity!!.setOnBackPressedListener(null)
        } else {
            baseActivity!!.setOnBackPressedListener(null)
        }
        windowsList!!.clear()
        allWindows.adapter!!.notifyDataSetChanged()
        browserTabAdapter.setData(windowsList!!)
        updateNumWindows()
    }

    fun hideCurrentWindow() {
        if (!windowsList!!.isEmpty()) {
            val topWindow: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            if (topWindow!!.view != null) {
                topWindow.requireView().visibility = View.GONE
            }
        }
    }

    fun unhideCurrentWindow() {
        if (windowsList!!.isNotEmpty()) {
            val topWindow: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            if (topWindow!!.view != null) {
                topWindow.requireView().visibility = View.VISIBLE
                baseActivity!!.setOnBackPressedListener(topWindow)
            }
        } else {
            baseActivity!!.setOnBackPressedListener(null)
        }
    }

    fun pauseCurrentWindow() {
        if (!windowsList!!.isEmpty()) {
            val topWindow: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            if (topWindow!!.view != null) {
                topWindow.onPause()
            }
        }
    }

    fun resumeCurrentWindow() {
        if (!windowsList!!.isEmpty()) {
            val topWindow: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
            if (topWindow!!.view != null) {
                Handler().postDelayed({ topWindow.onResume() }, 500)
                baseActivity!!.setOnBackPressedListener(topWindow)
            }
        } else {
            baseActivity!!.setOnBackPressedListener(null)
        }
    }

    fun updateAdFilters() {
        adBlock!!.update(requireContext())
    }

    fun checkUrlIfAds(url: String?): Boolean {
        return adBlock!!.checkThroughFilters(url!!)
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
        val topWindow: BrowserWindow? = list!![list.size - 1]
        if (topWindow!!.view != null) {
            topWindow.requireView().visibility = View.GONE
        }
        val window: BrowserWindow? = windowsList!![index]
        windowsList!!.removeAt(index)
        windowsList!!.add(window!!)
        if (window!!.view != null) {
            window.requireView().visibility = View.VISIBLE
            baseActivity!!.setOnBackPressedListener(window)
            baseActivity!!.isEnableSuggetion = false
            baseActivity!!.inputURLText!!.setText(window.url)
        }
        for (posWindow in windowsList!!.indices) {
            val windowTemp: BrowserWindow? = windowsList!!.get(posWindow)
            windowTemp!!.onPause()
        }
        val windowCurrentTemp: BrowserWindow? = windowsList!!.get(windowsList!!.size - 1)
        Handler().postDelayed(object : Runnable {
            override fun run() {
                windowCurrentTemp!!.onResume()
            }
        }, 500)
        allWindows.adapter!!.notifyDataSetChanged()
        browserTabAdapter.setData(windowsList!!)
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

        override fun bindView(
            data: BrowserWindow,
            position: Int,
            viewHolder: CardStackView.ViewHolder?
        ) {
            if (viewHolder is ColorItemViewHolder) {
                val holder: ColorItemViewHolder = viewHolder
                val webView: WebView? = data.webView
                if (webView != null) {
                    if (webView.title == null || webView.title!!.length == 0) {
                        holder.windowTitle.text = "Home"
                    } else {
                        holder.windowTitle.text = webView.title
                    }
                    if (webView.favicon == null) {
                        Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon)
                        Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon)
                    } else {
                        holder.favicon.setImageBitmap(webView.favicon)
                        holder.imgLargeIcon.setImageBitmap(webView.favicon)
                    }
                } else {
                    holder.windowTitle.text = "Home"
                    Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon)
                    Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon)
                }
                holder.close.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        closeWindow(windowsList!!.get(position))
                        notifyDataSetChanged()
                    }
                })
                holder.itemView.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        //getBaseActivity().homeContainer.setVisibility(View.VISIBLE);
                        switchWindow(position)
                        baseActivity!!.navView.setSelectedItemId(R.id.navHome)
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

            override fun onItemExpand(b: Boolean) {}
        }
    }

    private inner class AllWindowsAdapter : RecyclerView.Adapter<WindowItem>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowItem {
            return WindowItem(
                LayoutInflater.from(getActivity())
                    .inflate(R.layout.all_windows_popup_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: WindowItem, position: Int) {
            val webView: WebView? = (windowsList!![position])!!.webView
            if (webView != null) {
                if (webView.title == null || webView.title!!.length == 0) {
                    holder.windowTitle.text = "Home"
                } else {
                    holder.windowTitle.text = webView.title
                }
                if (webView.favicon == null) {
                    Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon)
                    Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon)
                } else {
                    holder.favicon.setImageBitmap(webView.favicon)
                    holder.imgLargeIcon.setImageBitmap(webView.favicon)
                }
            } else {
                holder.windowTitle.text = "Home"
                Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon)
                Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon)
            }
            holder.close.setOnClickListener { closeWindow(windowsList!!.get(position)) }
            holder.itemView.setOnClickListener {
                switchWindow(position)
                baseActivity!!.navView.selectedItemId = R.id.navHome
            }
        }

        override fun getItemCount(): Int {
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
        val pixelCount: Int = bitmap.width * bitmap.height
        val pixels: IntArray = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var y: Int = 0
        val h: Int = bitmap.height
        while (y < h) {
            var x: Int = 0
            val w: Int = bitmap.width
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