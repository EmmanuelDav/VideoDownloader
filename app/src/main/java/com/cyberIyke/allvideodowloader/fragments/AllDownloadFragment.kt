package com.cyberIyke.allvideodowloader.fragments

import android.app.Dialog
import android.content.*
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnLongClickListener
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.bumptech.glide.Glide
import com.cyberIyke.allvideodowloader.BuildConfig
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.activities.MainActivity
import com.cyberIyke.allvideodowloader.database.*
import com.cyberIyke.allvideodowloader.helper.RenameVideoPref
import com.cyberIyke.allvideodowloader.helper.WebConnect
import com.cyberIyke.allvideodowloader.interfaces.DownloadInterface
import com.cyberIyke.allvideodowloader.model.DownloadInfo
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.convertSecondsToHMmSs
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getStringSizeLengthFile
import com.cyberIyke.allvideodowloader.viewModel.DownloadsViewModel
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AllDownloadFragment : Fragment() {

    lateinit var imgCast: ImageView
    lateinit var downloadsList: RecyclerView
    lateinit var downloadAdapter: DownloadAdapter
    var downloadsViewModel: DownloadsViewModel? = null
    var selectedList: ArrayList<DownloadData> = ArrayList()
    var isSelectedMode: Boolean = false
    private lateinit var llBottom: LinearLayout
    private lateinit var rlTopSelected: RelativeLayout
    var downloadInterface: DownloadInterface? = null
    lateinit var txtSelectedCount: TextView
    lateinit var imgCancel: ImageView
    lateinit var llDeleteSelected: LinearLayout
    lateinit var llSelectAll: LinearLayout
    lateinit var renameVideoPref: RenameVideoPref
    var progressReceiver: BroadcastReceiver? = null
    private var view: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.fragment_all_download, container, false)
        downloadsViewModel = ViewModelProvider(this)[DownloadsViewModel::class.java]
        renameVideoPref = RenameVideoPref(requireActivity())
        downloadsList = view!!.findViewById(R.id.downloadsList)
        downloadAdapter = DownloadAdapter()
        downloadsList.layoutManager = LinearLayoutManager(activity)
        downloadsList.adapter = downloadAdapter
        llSelectAll = view!!.findViewById(R.id.llSelectAll)
        llDeleteSelected = view.findViewById(R.id.llDeleteSelected)
        imgCancel = view.findViewById(R.id.imgCancel)
        txtSelectedCount = view.findViewById(R.id.txtSelectedCount)
        rlTopSelected = view.findViewById(R.id.rlTopSelected)
        llBottom = view.findViewById(R.id.llBottom)
        imgCast = view.findViewById(R.id.imgCast)
        imgCast.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                enablingWiFiDisplay()
            }
        })
        imgCancel.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                unSelectAll()
            }
        })
        llDeleteSelected.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val dialog: Dialog = Dialog((activity)!!)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.dialog_confirmation)
                val txtTitle: TextView = dialog.findViewById(R.id.txtTitle)
                val txtDesc: TextView = dialog.findViewById(R.id.txtDesc)
                txtTitle.text = "Confirm"
                txtDesc.text = "Are you sure you want to delete this video?"
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
                        for (download: DownloadData in selectedList) {
                            val file: DocumentFile? = DocumentFile.fromSingleUri(
                                activity!!.applicationContext, Uri.parse(
                                    download.download!!.downloadedPath
                                )
                            )
                            if (file!!.exists()) {
                                file.delete()
                                //fetch.remove((int) download.download.getId());;
                            }
                        }
                        downloadAdapter.notifyDataSetChanged()
                        onResume()
                        unSelectAll()
                    }
                })
                dialog.show()
                dialog.window!!
                    .setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                    )
                dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        })
        llSelectAll.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                selectedList.clear()
                selectedList.addAll(downloadAdapter.downloads)
                txtSelectedCount.text = selectedList.size.toString() + " selected"
                downloadAdapter.notifyDataSetChanged()
            }
        })
        progressReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if ((intent.action == "DOWNLOAD_PROGRESS")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val downloadInfoArrayList: ArrayList<DownloadInfo>? =
                            intent.getParcelableArrayListExtra(
                                "downloadList",
                                DownloadInfo::class.java
                            )
                        downloadAdapter.loadProgress(downloadInfoArrayList)
                    }
                    if (downloadInterface != null) {
                        downloadInterface!!.loading()
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(progressReceiver as BroadcastReceiver, IntentFilter("DOWNLOAD_PROGRESS"))
        return view
    }

    fun unSelectAll() {
        txtSelectedCount.text = "0 selected"
        isSelectedMode = false
        selectedList.clear()
        downloadAdapter.notifyDataSetChanged()
        (activity as MainActivity?)!!.navView.visibility = View.VISIBLE
        llBottom.visibility = View.GONE
        rlTopSelected.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver((progressReceiver)!!)
    }

    override fun onResume() {
        super.onResume()
        downloadsViewModel!!.allDownloads.observe(
            viewLifecycleOwner,
            Observer { downloads: List<Download>? ->
                val list: ArrayList<Download> = ArrayList(downloads)
                for (download: Download in list) {
                    val file: DocumentFile? = DocumentFile.fromSingleUri(
                        requireActivity().applicationContext,
                        Uri.parse(download.downloadedPath)
                    )
                    val strRename: String? = renameVideoPref.getString(download.id.toString(), "")
                    if (strRename != null) {
                        if (strRename.isNotEmpty()) {
                            val desFile: File = File(strRename)
                            if (desFile.exists()) {
                                downloadAdapter.addDownload(download)
                            }
                        } else if (file!!.exists()) {
                            downloadAdapter.addDownload(download)
                        }
                    }
                    downloadAdapter.notifyDataSetChanged()
                }
                if (downloadInterface != null) {
                    downloadInterface!!.notLoading()
                }
            }
        )
    }

    inner class DownloadAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        DownloadInterface {
        val downloads: MutableList<DownloadData> = ArrayList()
        private var downloadList: List<DownloadInfo>? = ArrayList()
        var downloaded: Boolean = false
        var originalHeight: Int = 0
        var headerViewHolder: ProgressViewHolder? = null
        fun loadProgress(downloadDataList: List<DownloadInfo>?) {
            downloadList = downloadDataList
            notifyDataSetChanged()
        }

        fun addDownload(download: Download) {
            downloadInterface = this
            var found: Boolean = false
            var data: DownloadData? = null
            var dataPosition: Int = -1
            for (i in downloads.indices) {
                val downloadData: DownloadData = downloads.get(i)
                if (downloadData.id.toLong() == download.id) {
                    data = downloadData
                    dataPosition = i
                    found = true
                    break
                }
            }
            if (!found) {
                val downloadData: DownloadData = DownloadData()
                downloadData.id = download.id.toInt()
                downloadData.download = download
                downloads.add(downloadData)
                downloads.sortWith(Comparator { o1, o2 ->
                    o2.download!!.timestamp.toString()
                        .compareTo(o1.download!!.timestamp.toString())
                })
                notifyItemInserted(downloads.indexOf(downloadData))
            } else {
                data!!.download = download
                notifyItemChanged(dataPosition)
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val inflater: LayoutInflater = LayoutInflater.from(parent.context)
            if (viewType == Companion.VIEW_TYPE_DOWNLOAD) {
                val view: View = inflater.inflate(R.layout.item_download, parent, false)
                return ProgressViewHolder(view)
            } else {
                val view: View = inflater.inflate(R.layout.item_download, parent, false)
                return ViewHolder(view)
            }
        }

        override fun getItemViewType(position: Int): Int {
            if (position < downloadList!!.size) {
                return Companion.VIEW_TYPE_DOWNLOAD
            } else {
                return Companion.VIEW_TYPE_ITEM
            }
        }

        override fun onBindViewHolder(itemHolder: RecyclerView.ViewHolder, position: Int) {
            if (itemHolder is ViewHolder) {
                val downloadData: DownloadData
                val holder: ViewHolder = itemHolder
                downloadData = downloads.get(position - downloadList!!.size)
                val documentFile: DocumentFile? = DocumentFile.fromSingleUri(
                    (context)!!, Uri.parse(
                        downloadData.download!!.downloadedPath
                    )
                )
                var tempFile: File = File(downloadData.download!!.downloadedPath)
                val file: File = tempFile
                Glide.with((activity)!!).load(downloadData.download!!.downloadedPath)
                    .into(holder.imgVideo)
                val strRename: String? = renameVideoPref.getString(downloadData.download!!.id.toString(), "")
                if (strRename!!.isNotEmpty()) {
                    val desFile: File = File(strRename)
                    if (desFile.exists()) {
                        tempFile = desFile
                    }
                }
                holder.downloadVideoName.text = downloadData.download!!.name
                holder.imgVideo.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        if (isSelectedMode) {
                            var isContain: Boolean = false
                            for (data: DownloadData in selectedList) {
                                if (data.download!!.id == downloadData.download!!.id) {
                                    isContain = true
                                }
                            }
                            if (isContain) {
                                selectedList.remove(downloadData)
                            } else {
                                selectedList.add(downloadData)
                            }
                            txtSelectedCount.text = selectedList.size.toString() + " selected"
                            notifyDataSetChanged()
                            return
                        }
                        if (downloaded) {
                            MediaScannerConnection.scanFile(
                                activity,
                                arrayOf(documentFile.toString()),
                                null,
                                OnScanCompletedListener({ path: String?, uri: Uri? ->
                                    downloadsViewModel!!.viewContent(
                                        downloadData.download!!.downloadedPath, (context)!!
                                    )
                                })
                            )
                        }
                    }
                })
                holder.imgVideo.setOnLongClickListener(object : OnLongClickListener {
                    override fun onLongClick(v: View?): Boolean {
                        if (!isSelectedMode) {
                            selectedList.add(downloadData)
                            isSelectedMode = true
                            txtSelectedCount.text = selectedList.size.toString() + " selected"
                            downloadAdapter.notifyDataSetChanged()
                            (activity as MainActivity?)!!.navView.visibility = View.GONE
                            llBottom.visibility = View.VISIBLE
                            rlTopSelected.visibility = View.VISIBLE
                        }
                        return false
                    }
                })
                holder.llContent.setOnLongClickListener(object : OnLongClickListener {
                    override fun onLongClick(v: View?): Boolean {
                        if (!isSelectedMode) {
                            selectedList.add(downloadData)
                            isSelectedMode = true
                            txtSelectedCount.text = selectedList.size.toString() + " selected"
                            downloadAdapter.notifyDataSetChanged()
                            (activity as MainActivity?)!!.navView.visibility = View.GONE
                            llBottom.visibility = View.VISIBLE
                            rlTopSelected.visibility = View.VISIBLE
                        }
                        return false
                    }
                })
                holder.imgSelect.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        var isContain: Boolean = false
                        for (data: DownloadData in selectedList) {
                            if (data.download!!.id == downloadData.download!!.id) {
                                isContain = true
                            }
                        }
                        if (isContain) {
                            selectedList.remove(downloadData)
                        } else {
                            selectedList.add(downloadData)
                        }
                        txtSelectedCount.text = selectedList.size.toString() + " selected"
                        notifyDataSetChanged()
                    }
                })
                holder.llContent.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        if (isSelectedMode) {
                            var isContain: Boolean = false
                            for (data: DownloadData in selectedList) {
                                if (data.download!!.id == downloadData.download!!.id) {
                                    isContain = true
                                }
                            }
                            if (isContain) {
                                selectedList.remove(downloadData)
                            } else {
                                selectedList.add(downloadData)
                            }
                            txtSelectedCount.text = selectedList.size.toString() + " selected"
                            notifyDataSetChanged()
                            return
                        }
                        if (downloaded) {
                            MediaScannerConnection.scanFile(
                                activity,
                                arrayOf(documentFile.toString()),
                                null,
                                object : OnScanCompletedListener {
                                    override fun onScanCompleted(path: String?, uri: Uri?) {
                                        downloadsViewModel!!.viewContent(
                                            downloadData.download!!.downloadedPath,
                                            (context)!!
                                        )
                                    }
                                })
                        }
                    }
                })
                holder.imgCancel.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        //  fetch.remove(downloadData.download.getId());
                    }
                })
                holder.imgResume.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
//                    if (status == Status.FAILED || status == Status.CANCELLED) {
//                        //fetch.retry(downloadData.download.getId());
//                    } else {
//                      //  fetch.resume(downloadData.download.getId());
//                    }
                    }
                })
                holder.imgPause.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        //fetch.pause((int) downloadData.download.getId());
                    }
                })
                holder.imgMore.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        val popup: PopupMenu = PopupMenu(activity, holder.imgMore)
                        popup.menuInflater.inflate(R.menu.menu_download, popup.menu)
                        popup.setOnMenuItemClickListener(object :
                            PopupMenu.OnMenuItemClickListener {
                            override fun onMenuItemClick(item: MenuItem): Boolean {
                                when (item.itemId) {
                                    R.id.menu_share -> shareFile(downloadData.download!!.downloadedPath)
                                    R.id.menu_rename -> renameFile(downloadData, documentFile)
                                    R.id.menu_edit_video -> {
                                        val videoURI: Uri = FileProvider.getUriForFile(
                                            (activity)!!,
                                            BuildConfig.APPLICATION_ID + ".fileprovider",
                                            file
                                        )
                                        val intent: Intent = Intent(Intent.ACTION_EDIT)
                                        //intent.setDataAndType(videoURI, "video/*");
                                        intent.setDataAndType(videoURI, getMimeType(videoURI))
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        try {
                                            val choose: Intent =
                                                Intent.createChooser(intent, "Edit with")
                                            startActivityForResult(choose, 1005)
                                        } catch (e: ActivityNotFoundException) {
                                            try {
                                                startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("market://details?id=com.kunkun.videoeditor.videomaker")
                                                    )
                                                )
                                            } catch (exception: ActivityNotFoundException) {
                                                startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("https://play.google.com/store/apps/details?id=com.kunkun.videoeditor.videomaker")
                                                    )
                                                )
                                            }
                                        } catch (e: Exception) {
                                            try {
                                                startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("market://details?id=com.kunkun.videoeditor.videomaker")
                                                    )
                                                )
                                            } catch (exception: ActivityNotFoundException) {
                                                startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("https://play.google.com/store/apps/details?id=com.kunkun.videoeditor.videomaker")
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    R.id.menu_open_link -> {
                                        activity!!.onBackPressed()
                                        (activity as MainActivity?)!!.isEnableSuggetion = false
                                        (activity as MainActivity?)!!.navView.selectedItemId =
                                            R.id.navHome
                                        WebConnect(
                                            holder.edtSearch,
                                            (activity as MainActivity)
                                        ).connect()
                                    }
                                    R.id.menu_delete -> {
                                        val dialog: Dialog = Dialog((activity)!!)
                                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                        dialog.setContentView(R.layout.dialog_confirmation)
                                        val txtTitle: TextView = dialog.findViewById(R.id.txtTitle)
                                        val txtDesc: TextView = dialog.findViewById(R.id.txtDesc)
                                        txtTitle.text = "Confirm"
                                        txtDesc.text = "Are you sure you want to delete this video?"
                                        val txtNO: TextView = dialog.findViewById(R.id.txtNO)
                                        val txtOK: TextView = dialog.findViewById(R.id.txtOK)
                                        txtNO.setOnClickListener(object : View.OnClickListener {
                                            override fun onClick(v: View?) {
                                                dialog.dismiss()
                                            }
                                        })
                                        txtOK.setOnClickListener(object : View.OnClickListener {
                                            override fun onClick(v: View?) {
                                                downloadsViewModel!!.startDelete(
                                                    downloads.get(
                                                        position
                                                    ).id.toLong(), dialog.context
                                                )
                                                dialog.dismiss()
                                                onResume()
                                            }
                                        })
                                        dialog.show()
                                        dialog.window!!.setLayout(
                                            WindowManager.LayoutParams.MATCH_PARENT,
                                            WindowManager.LayoutParams.MATCH_PARENT
                                        )
                                        dialog.window!!.setBackgroundDrawable(
                                            ColorDrawable(
                                                Color.TRANSPARENT
                                            )
                                        )
                                    }
                                }
                                return true
                            }
                        })
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            popup.setForceShowIcon(true)
                        }
                        popup.show()
                    }
                })
                holder.imgCancel.visibility = View.GONE
                holder.imgPause.visibility = View.GONE
                holder.imgResume.visibility = View.GONE
                holder.txtDuration.visibility = View.GONE
                holder.imgMore.visibility = View.GONE
                holder.imgSelect.visibility = View.GONE
                holder.downloadProgressBar.visibility = View.VISIBLE
                downloadsViewModel!!.loadState.observe(
                    viewLifecycleOwner,
                    Observer({ state: WorkInfo.State ->
                        Log.d(AllDownloadFragment.Companion.TAG, "onBindViewHolder: " + state)
                        when (state) {
                            WorkInfo.State.FAILED -> {
                                holder.imgCancel.visibility = View.VISIBLE
                                holder.imgPause.visibility = View.GONE
                                holder.imgResume.visibility = View.VISIBLE
                                val strDescFailed: String =
                                    "Failed " + downloadData.download!!.downloadedPercent + "% " + getStringSizeLengthFile(
                                        downloadData.download!!.downloadedSize
                                    ) + "/" + getStringSizeLengthFile(
                                        downloadData.download!!.totalSize
                                    )
                                holder.downloadProgressText.text = strDescFailed
                            }
                            WorkInfo.State.RUNNING -> {}
                            WorkInfo.State.ENQUEUED -> {
                                holder.imgCancel.visibility = View.VISIBLE
                                holder.imgPause.visibility = View.VISIBLE
                                holder.imgResume.visibility = View.GONE
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                downloaded = true
                                holder.imgCancel.visibility = View.VISIBLE
                                holder.imgPause.visibility = View.VISIBLE
                                holder.imgResume.visibility = View.GONE
                                holder.imgCancel.visibility = View.GONE
                                holder.imgPause.visibility = View.GONE
                                holder.imgResume.visibility = View.GONE
                                holder.downloadProgressBar.visibility = View.GONE
                                holder.txtDuration.visibility = View.VISIBLE
                                holder.imgMore.visibility = View.VISIBLE
                                val dateString: String = SimpleDateFormat("MMMM dd yyyy").format(
                                    Date(
                                        downloadData.download!!.timestamp
                                    )
                                )
                                val strDescComplete: String = getStringSizeLengthFile(
                                    downloadData.download!!.downloadedSize
                                ) + "  " + dateString
                                holder.downloadProgressText.text = strDescComplete
                                if (documentFile!!.exists()) {
                                    var duration: String? = null
                                    try {
                                        duration = convertSecondsToHMmSs(
                                            getFileDuration(
                                                downloadData.download!!.downloadedPath, context
                                            )
                                        )
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                    if (duration != null) holder.txtDuration.text = duration
                                }
                            }
                            WorkInfo.State.CANCELLED -> {
                                val strDesc2: String =
                                    "Cancelled " + downloadData.download!!.downloadedPercent + "% " + getStringSizeLengthFile(
                                        downloadData.download!!.downloadedSize
                                    ) + "/" + getStringSizeLengthFile(
                                        downloadData.download!!.totalSize
                                    )
                                holder.downloadProgressText.text = strDesc2
                                holder.imgCancel.visibility = View.GONE
                                holder.imgPause.visibility = View.GONE
                                holder.imgResume.visibility = View.VISIBLE
                            }
                            WorkInfo.State.BLOCKED -> {}
                            else -> {}
                        }
                    } as (WorkInfo.State?) -> Unit)
                )
                if (isSelectedMode) {
                    holder.imgCancel.visibility = View.GONE
                    holder.imgPause.visibility = View.GONE
                    holder.imgResume.visibility = View.GONE
                    holder.imgMore.visibility = View.GONE
                    holder.imgSelect.visibility = View.VISIBLE
                    var isContain: Boolean = false
                    for (data: DownloadData in selectedList) {
                        if (data.download!!.id == downloadData.download!!.id) {
                            isContain = true
                        }
                    }
                    if (isContain) {
                        Glide.with((activity)!!).load(R.drawable.ic_box_selected)
                            .into(holder.imgSelect)
                    } else {
                        Glide.with((activity)!!).load(R.drawable.ic_box_unselect)
                            .into(holder.imgSelect)
                    }
                }
            } else if (itemHolder is ProgressViewHolder) {
                headerViewHolder = itemHolder
                val downloadInfo: DownloadInfo = downloadList!!.get(position)
                headerViewHolder!!.downloadProgressBar.progress = downloadInfo.progress
                headerViewHolder!!.downloadProgressText.text = downloadInfo.line
                headerViewHolder!!.imgSelect.visibility = View.GONE
                headerViewHolder!!.downloadVideoName.text = downloadInfo.name
                originalHeight = headerViewHolder!!.itemView.layoutParams.height
            }
        }

        override fun getItemCount(): Int {
            return downloads.size + downloadList!!.size
        }

        override fun loading() {
            Log.d(AllDownloadFragment.Companion.TAG, "loading: loading")
            if (headerViewHolder != null) {
                headerViewHolder!!.itemView.visibility = View.VISIBLE
                headerViewHolder!!.itemView.layoutParams.height = -2
            }
        }

        override fun notLoading() {
            Log.d(AllDownloadFragment.Companion.TAG, "loading: not loading")
            if (headerViewHolder != null) {
                headerViewHolder!!.itemView.visibility = View.GONE
                headerViewHolder!!.itemView.layoutParams.height = 0
            }
        }

        internal inner class ViewHolder constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            var downloadVideoName: TextView
            var downloadProgressBar: ProgressBar
            var imgVideo: ImageView
            var downloadProgressText: TextView
            var imgCancel: ImageView
            var imgPause: ImageView
            var imgResume: ImageView
            var imgMore: ImageView
            var txtDuration: TextView
            var edtSearch: EditText
            var llContent: LinearLayout
            var imgSelect: ImageView

            init {
                imgSelect = itemView.findViewById(R.id.imgSelect)
                llContent = itemView.findViewById(R.id.llContent)
                edtSearch = itemView.findViewById(R.id.edtSearch)
                txtDuration = itemView.findViewById(R.id.txtDuration)
                imgMore = itemView.findViewById(R.id.imgMore)
                imgResume = itemView.findViewById(R.id.imgResume)
                imgPause = itemView.findViewById(R.id.imgPause)
                imgCancel = itemView.findViewById(R.id.imgCancel)
                downloadProgressText = itemView.findViewById(R.id.downloadProgressText)
                imgVideo = itemView.findViewById(R.id.imgVideo)
                downloadVideoName = itemView.findViewById(R.id.downloadVideoName)
                downloadProgressBar = itemView.findViewById(R.id.downloadProgressBar)
            }
        }

        inner class ProgressViewHolder constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            var downloadProgressBar: ProgressBar
            var imgVideo: ImageView
            var downloadProgressText: TextView
            var downloadVideoName: TextView
            var imgMore: ImageView
            var imgCancel: ImageView
            var imgPause: ImageView
            var imgSelect: ImageView

            init {
                imgSelect = itemView.findViewById(R.id.imgSelect)
                imgPause = itemView.findViewById(R.id.imgPause)
                imgMore = itemView.findViewById(R.id.imgMore)
                imgCancel = itemView.findViewById(R.id.imgCancel)
                downloadProgressText = itemView.findViewById(R.id.downloadProgressText)
                imgVideo = itemView.findViewById(R.id.imgVideo)
                downloadVideoName = itemView.findViewById(R.id.downloadVideoName)
                downloadProgressBar = itemView.findViewById(R.id.downloadProgressBar)
            }
        }
    }

    fun renameFile(downloadData: DownloadData, file: DocumentFile?) {
        val dialog: Dialog = Dialog((activity)!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_rename)
        val edtName: EditText = dialog.findViewById(R.id.edtName)
        edtName.setText(downloadData.download!!.name)
        val txtNO: TextView = dialog.findViewById(R.id.txtNO)
        val txtOK: TextView = dialog.findViewById(R.id.txtOK)
        txtNO.setOnClickListener { dialog.dismiss() }
        txtOK.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val strName: String = edtName.text.toString().trim { it <= ' ' }
                    .replace("[^\\w ()'!\\[\\]\\-]".toRegex(), "")
                if (strName.isEmpty() || strName.length == 0) {
                    Toast.makeText(activity, "Please enter video name", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                try {
                    val os: OutputStream? = context!!.contentResolver.openOutputStream(
                        file!!.uri
                    )
                    os!!.write(edtName.text.toString().toByteArray())
                    os.close()
                } catch (exception: Exception) {
                }
                onResume()
                dialog.dismiss()
            }
        })
        dialog.show()
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    class DownloadData {
        var id: Int = 0
        var download: Download? = null
        var eta: Long = -1
        var downloadedBytesPerSecond: Long = 0
        override fun hashCode(): Int {
            return id
        }

        override fun toString(): String {
            if (download == null) {
                return ""
            }
            return download.toString()
        }

        override fun equals(obj: Any?): Boolean {
            return obj === this || obj is DownloadData && obj.id == id
        }
    }

    fun enablingWiFiDisplay() {
        try {
            startActivity(Intent("android.settings.WIFI_DISPLAY_SETTINGS"))
            return
        } catch (activitynotfoundexception: ActivityNotFoundException) {
            activitynotfoundexception.printStackTrace()
        }
        try {
            startActivity(Intent("android.settings.CAST_SETTINGS"))
            return
        } catch (exception1: Exception) {
            Toast.makeText(activity, "Device not supported", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(file: String) {
        val fileUri: DocumentFile? = DocumentFile.fromSingleUri((context)!!, Uri.parse(file))
        val msg: StringBuilder = StringBuilder()
        msg.append(resources.getString(R.string.share_message))
        msg.append("\n")
        msg.append("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)
        if (fileUri != null) {
            val shareIntent: Intent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.type = "*/*"
            shareIntent.putExtra(Intent.EXTRA_TEXT, msg.toString())
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(file))
            try {
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(activity, "No App Available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Throws(IOException::class)
    fun getFileDuration(file: String?, context: Context?): Long {
        var result: Long = 0
        var mFFmpegMediaMetadataRetrieve: FFmpegMediaMetadataRetriever? = null
        try {
            mFFmpegMediaMetadataRetrieve = FFmpegMediaMetadataRetriever()
            mFFmpegMediaMetadataRetrieve.setDataSource(file)
            val mVideoDuration: String =
                mFFmpegMediaMetadataRetrieve.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION)
            result = mVideoDuration.toLong()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        return result
    }

    fun getMimeType(uri: Uri): String? {
        var mimeType: String? = null
        if ((ContentResolver.SCHEME_CONTENT == uri.scheme)) {
            val cr: ContentResolver = requireActivity().contentResolver
            mimeType = cr.getType(uri)
        } else {
            val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                fileExtension.lowercase(
                    Locale.getDefault()
                )
            )
        }
        return mimeType
    }

    companion object {
        private val TAG: String = AllDownloadFragment::class.java.canonicalName
        const val VIEW_TYPE_DOWNLOAD: Int = 0
        const val VIEW_TYPE_ITEM: Int = 1
    }
}