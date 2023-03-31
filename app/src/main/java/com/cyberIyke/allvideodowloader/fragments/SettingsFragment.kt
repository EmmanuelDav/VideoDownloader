package com.cyberIyke.allvideodowloader.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AlertDialog
import com.cyberIyke.allvideodowloader.BuildConfig
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.activities.IntroActivity
import com.cyberIyke.allvideodowloader.activities.MainActivity.OnBackPressedListener
import com.cyberIyke.allvideodowloader.fragments.base.BaseFragment
import com.cyberIyke.allvideodowloader.helper.AdController
import com.cyberIyke.allvideodowloader.helper.WebConnect
import com.cyberIyke.allvideodowloader.utils.HistorySQLite
import com.cyberIyke.allvideodowloader.views.SwitchButton
import java.io.File

class SettingsFragment : BaseFragment(), OnBackPressedListener, View.OnClickListener {

    private var mView: View? = null
    private var searchEngine: String? = null
    var txtSelectedSearchEngine: TextView? = null
    var txtDownloadLocation: TextView? = null
    private var strDownloadLocation: String? = null
    var prefs: SharedPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        retainInstance = true
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_settings, container, false)
            baseActivity!!.setOnBackPressedListener(this)
            prefs = requireActivity().getSharedPreferences("settings", 0)
            strDownloadLocation =
                prefs!!.getString(
                    "downloadLocation",
                    "/storage/emulated/0/Download/Videodownloader"
                )
            txtDownloadLocation = mView!!.findViewById(R.id.txtDownloadLocation)
            if (!strDownloadLocation!!.endsWith("/")) {
                strDownloadLocation = strDownloadLocation + "/"
            }
            txtDownloadLocation!!.text = strDownloadLocation
            searchEngine = prefs!!.getString("searchEngine", "Google")
            txtSelectedSearchEngine = mView!!.findViewById(R.id.txtSelectedSearchEngine)
            txtSelectedSearchEngine!!.text = searchEngine
            //Back
            val btnSettingsBack: ImageView = mView!!.findViewById(R.id.backBtn)
            btnSettingsBack.setOnClickListener(this)

            // Switch wifi only switch
            val wifiSwitch: SwitchButton = mView!!.findViewById(R.id.wifiSwitch)
            val wifiOn: Boolean = prefs!!.getBoolean(getString(R.string.wifiON), false)
            wifiSwitch.isChecked = wifiOn
            wifiSwitch.setOnCheckedChangeListener(object : SwitchButton.OnCheckedChangeListener {
                override fun onCheckedChanged(
                    buttonView: SwitchButton?,
                    isChecked: Boolean
                ) {
                    prefs!!.edit().putBoolean(getString(R.string.wifiON), isChecked).commit()
                }
            })

            // Switch ad blocker switch
            val adBlockerSwitch: SwitchButton = mView!!.findViewById(R.id.adBlockerSwitch)
            val adBlockOn: Boolean = prefs!!.getBoolean(getString(R.string.adBlockON), true)
            adBlockerSwitch.isChecked = adBlockOn
            adBlockerSwitch.setOnCheckedChangeListener(object :
                SwitchButton.OnCheckedChangeListener {
                override fun onCheckedChanged(
                    buttonView: SwitchButton?,
                    isChecked: Boolean
                ) {
                    prefs!!.edit().putBoolean(getString(R.string.adBlockON), isChecked).commit()
                }
            })
            val llDownloadLocation: LinearLayout = mView!!.findViewById(R.id.llDownloadLocation)
            llDownloadLocation.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    selectFolder()
                }
            })
            val llSearchEngine: LinearLayout = mView!!.findViewById(R.id.llSearchEngine)
            llSearchEngine.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    changeSearchEngine()
                }
            })
            val txtClearCache: TextView = mView!!.findViewById(R.id.txtClearCache)
            txtClearCache.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    clearCache()
                }
            })
            val txtClearHistory: TextView = mView!!.findViewById(R.id.txtClearHistory)
            txtClearHistory.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    clearHistory()
                }
            })
            val txtClearCookies: TextView = mView!!.findViewById(R.id.txtClearCookies)
            txtClearCookies.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    clearCookies()
                }
            })
            val txtHowToDownload: TextView = mView!!.findViewById(R.id.txtHowToDownload)
            txtHowToDownload.setOnClickListener {
                startActivity(
                    Intent(
                        activity,
                        IntroActivity::class.java
                    )
                )
            }
            val txtPrivacyPolicy: TextView = mView!!.findViewById(R.id.txtPrivacyPolicy)
            txtPrivacyPolicy.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    requireActivity().onBackPressed()
                    baseActivity!!.isEnableSuggetion = false
                    baseActivity!!.navView.selectedItemId = R.id.navHome
                    WebConnect(mView!!.findViewById(R.id.edtSearch), baseActivity!!).connect()
                }
            })
            val txtVersion: TextView = mView!!.findViewById(R.id.txtVersion)
            txtVersion.text = BuildConfig.VERSION_NAME

            /*admob*/
            val adContainer: LinearLayout = mView!!.findViewById(R.id.banner_container)
            AdController.loadBannerAd(requireActivity(), adContainer)
            AdController.loadInterAd(requireActivity())
        }
        return mView
    }

    fun selectFolder() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(
            (activity)!!
        )
        builder.setTitle("Choose folder to save videos")

        // Get the layout inflater
        val inflater: LayoutInflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_selectfolder, null)
        val lvDirectories: ListView = dialogView.findViewById<View>(R.id.lvDirectories) as ListView
        val path: String = Environment.getExternalStorageDirectory().toString()
        (dialogView.findViewById<View>(R.id.tvJamesBond) as TextView).text = path
        val items: ArrayList<String> = listFolders(path)
        val adapter: ArrayAdapter<String> =
            ArrayAdapter((activity)!!, android.R.layout.simple_list_item_1, items)
        lvDirectories.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                val dest: String =
                    (dialogView.findViewById<View>(R.id.lvDirectories) as ListView).getItemAtPosition(
                        i
                    ).toString().trim({ it <= ' ' })
                val path: String
                if (dest.compareTo("...") == 0) {
                    val lastSlash: Int =
                        (dialogView.findViewById<View>(R.id.tvJamesBond) as TextView).text
                            .toString().lastIndexOf("/")
                    path = (dialogView.findViewById<View>(R.id.tvJamesBond) as TextView).text
                        .toString().substring(0, lastSlash)
                } else {
                    path = (dialogView.findViewById<View>(R.id.tvJamesBond) as TextView).text
                        .toString() + "/" + dest
                }
                items.clear()
                items.addAll(listFolders(path))
                (dialogView.findViewById<View>(R.id.tvJamesBond) as TextView).text = path
                adapter.notifyDataSetChanged()
            }
        }
        lvDirectories.adapter = adapter
        adapter.notifyDataSetChanged()
        builder.setView(dialogView)

        // Add the buttons
        builder.setPositiveButton("Select", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, id: Int) {
                val location: String =
                    (dialogView.findViewById<View>(R.id.tvJamesBond) as TextView).text
                        .toString()
                prefs!!.edit().putString("downloadLocation", location).commit()
                strDownloadLocation = location
                if (!strDownloadLocation!!.endsWith("/")) {
                    strDownloadLocation = strDownloadLocation + "/"
                }
                txtDownloadLocation!!.text = strDownloadLocation
            }
        })
        builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, id: Int) {}
        })

        // Get the AlertDialog from create()
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    fun listFolders(path: String): ArrayList<String> {
        val result: ArrayList<String> = ArrayList()
        val f: File = File(path)
        val files: Array<File> = f.listFiles()
        Log.d("TEST PATH1", path)
        Log.d("TEST PATH1", Environment.getExternalStorageDirectory().toString())
        if (path.compareTo(Environment.getExternalStorageDirectory().toString()) != 0) {
            result.add("...")
        }
        for (inFile: File in files) {
            if (inFile.isDirectory) {
                result.add(inFile.name)
            }
        }
        return result
    }

    private fun clearCache() {
        val dialog: Dialog = Dialog((activity)!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirmation)
        val txtTitle: TextView = dialog.findViewById(R.id.txtTitle)
        val txtDesc: TextView = dialog.findViewById(R.id.txtDesc)
        txtTitle.text = "Clear cache"
        txtDesc.text = "Would you like to clear all the browsing cache?"
        val txtNO: TextView = dialog.findViewById(R.id.txtNO)
        val txtOK: TextView = dialog.findViewById(R.id.txtOK)
        txtNO.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                dialog.dismiss()
            }
        })
        txtOK.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                dialog.dismiss()
                WebView((activity)!!).clearCache(true)
            }
        })
        dialog.show()
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun clearCookies() {
        val dialog: Dialog = Dialog((activity)!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirmation)
        val txtTitle: TextView = dialog.findViewById(R.id.txtTitle)
        val txtDesc: TextView = dialog.findViewById(R.id.txtDesc)
        txtTitle.text = "Clear cookies"
        txtDesc.text = "Would you like to clear all the browsing cookies?"
        val txtNO: TextView = dialog.findViewById(R.id.txtNO)
        val txtOK: TextView = dialog.findViewById(R.id.txtOK)
        txtNO.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                dialog.dismiss()
            }
        })
        txtOK.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                dialog.dismiss()
                WebStorage.getInstance().deleteAllData()
            }
        })
        dialog.show()
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun deleteCache() {
        try {
            val dir: File = requireActivity().cacheDir
            deleteDir(dir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children: Array<String> = dir.list()
            for (i in children.indices) {
                val success: Boolean = deleteDir(File(dir, children.get(i)))
                if (!success) {
                    return false
                }
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        } else {
            return false
        }
    }

    private fun clearHistory() {
        val dialog: Dialog = Dialog((activity)!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirmation)
        val txtTitle: TextView = dialog.findViewById(R.id.txtTitle)
        val txtDesc: TextView = dialog.findViewById(R.id.txtDesc)
        txtTitle.text = "Clear history"
        txtDesc.text = "Would you like to clear all the browsing history?"
        val txtNO: TextView = dialog.findViewById(R.id.txtNO)
        val txtOK: TextView = dialog.findViewById(R.id.txtOK)
        txtNO.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                dialog.dismiss()
            }
        })
        txtOK.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                dialog.dismiss()
                HistorySQLite(activity).clearHistory()
            }
        })
        dialog.show()
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun changeSearchEngine() {
        val dialog: Dialog = Dialog((activity)!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_search_engine)
        val llGoogle: LinearLayout = dialog.findViewById(R.id.llGoogle)
        val llBing: LinearLayout = dialog.findViewById(R.id.llBing)
        val llAsk: LinearLayout = dialog.findViewById(R.id.llAsk)
        val llYahoo: LinearLayout = dialog.findViewById(R.id.llYahoo)
        val llBaidu: LinearLayout = dialog.findViewById(R.id.llBaidu)
        val llYandex: LinearLayout = dialog.findViewById(R.id.llYandex)
        val imgGoogle: ImageView = dialog.findViewById(R.id.imgGoogle)
        val imgBing: ImageView = dialog.findViewById(R.id.imgBing)
        val imgAsk: ImageView = dialog.findViewById(R.id.imgAsk)
        val imgYahoo: ImageView = dialog.findViewById(R.id.imgYahoo)
        val imgBaidu: ImageView = dialog.findViewById(R.id.imgBaidu)
        val imgYandex: ImageView = dialog.findViewById(R.id.imgYandex)
        val txtSelect: TextView = dialog.findViewById(R.id.txtSelect)
        val prefs: SharedPreferences = requireActivity().getSharedPreferences("settings", 0)
        searchEngine = prefs.getString("searchEngine", "Google")
        txtSelect.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                txtSelectedSearchEngine!!.text = searchEngine
                prefs.edit().putString("searchEngine", searchEngine).commit()
                dialog.dismiss()
            }
        })
        changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex)
        llGoogle.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                searchEngine = "Google"
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex)
            }
        })
        llBing.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                searchEngine = "Bing"
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex)
            }
        })
        llAsk.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                searchEngine = "Ask"
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex)
            }
        })
        llYahoo.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                searchEngine = "Yahoo"
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex)
            }
        })
        llBaidu.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                searchEngine = "Baidu"
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex)
            }
        })
        llYandex.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                searchEngine = "Yandex"
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex)
            }
        })
        dialog.show()
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun changeSelection(
        imgGoogle: ImageView,
        imgBing: ImageView,
        imgAsk: ImageView,
        imgYahoo: ImageView,
        imgBaidu: ImageView,
        imgYandex: ImageView
    ) {
        imgGoogle.isSelected = false
        imgBing.isSelected = false
        imgAsk.isSelected = false
        imgYahoo.isSelected = false
        imgBaidu.isSelected = false
        imgYandex.isSelected = false
        when (searchEngine) {
            "Google" -> imgGoogle.isSelected = true
            "Bing" -> imgBing.isSelected = true
            "Ask" -> imgAsk.isSelected = true
            "Yahoo" -> imgYahoo.isSelected = true
            "Baidu" -> imgBaidu.isSelected = true
            "Yandex" -> imgYandex.isSelected = true
        }
    }

    override fun onBackpressed() {
        baseActivity!!.transStatusBar(true)
        baseActivity!!.browserManager.unhideCurrentWindow()
        requireFragmentManager().beginTransaction().remove(this).commit()
    }

    override fun onClick(view: View) {
        if (view.id == R.id.backBtn) {
            requireActivity().onBackPressed()
        }
    }
}