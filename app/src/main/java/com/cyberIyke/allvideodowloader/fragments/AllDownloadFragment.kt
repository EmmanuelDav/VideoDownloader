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
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import android.view.View.OnLongClickListener
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.annotation.RequiresApi
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
import com.cyberIyke.allvideodowloader.utils.CustomProgressBarDrawable
import com.cyberIyke.allvideodowloader.utils.Utils
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getStringSizeLengthFile
import com.cyberIyke.allvideodowloader.viewModel.DownloadsViewModel
import com.cyberIyke.allvideodowloader.viewModel.VidInfoViewModel
import com.cyberIyke.allvideodowloader.work.CancelReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class AllDownloadFragment : Fragment() {

    lateinit var imgCast: ImageView
    lateinit var downloadsList: RecyclerView
    lateinit var downloadAdapter: DownloadAdapter
    var downloadsViewModel: DownloadsViewModel? = null
    var downloadProgress: VidInfoViewModel? = null
    var selectedList: ArrayList<DownloadData> = ArrayList()
    var isSelectedMode: Boolean = false
    private lateinit var llBottom: LinearLayout
    private lateinit var rlTopSelected: RelativeLayout
    lateinit var txtSelectedCount: TextView
    lateinit var imgCancel: ImageView
    lateinit var llDeleteSelected: LinearLayout
    lateinit var llSelectAll: LinearLayout
    lateinit var renameVideoPref: RenameVideoPref
    var progressReceiver: BroadcastReceiver? = null
    private var mView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mView = inflater.inflate(R.layout.fragment_all_download, container, false)
        downloadsViewModel = ViewModelProvider(this)[DownloadsViewModel::class.java]
        downloadProgress = ViewModelProvider(this)[VidInfoViewModel::class.java]
        renameVideoPref = RenameVideoPref(requireActivity())
        downloadsList = mView!!.findViewById(R.id.downloadsList)
        downloadAdapter = DownloadAdapter()
        downloadsList.layoutManager = LinearLayoutManager(activity)
        downloadsList.adapter = downloadAdapter
        llSelectAll = mView!!.findViewById(R.id.llSelectAll)
        llDeleteSelected = mView!!.findViewById(R.id.llDeleteSelected)
        imgCancel = mView!!.findViewById(R.id.imgCancel)
        txtSelectedCount = mView!!.findViewById(R.id.txtSelectedCount)
        rlTopSelected = mView!!.findViewById(R.id.rlTopSelected)
        llBottom = mView!!.findViewById(R.id.llBottom)
        imgCast = mView!!.findViewById(R.id.imgCast)
        imgCast.setOnClickListener { enablingWiFiDisplay() }
        imgCancel.setOnClickListener { unSelectAll() }
        llDeleteSelected.setOnClickListener {
            val dialog: Dialog = Dialog((activity)!!)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_confirmation)
            val txtTitle: TextView = dialog.findViewById(R.id.txtTitle)
            val txtDesc: TextView = dialog.findViewById(R.id.txtDesc)
            txtTitle.text = "Confirm"
            txtDesc.text = "Are you sure you want to delete this videos?"
            val txtNO: TextView = dialog.findViewById(R.id.txtNO)
            val txtOK: TextView = dialog.findViewById(R.id.txtOK)
            txtNO.setOnClickListener { dialog.dismiss() }
            txtOK.setOnClickListener {
                dialog.dismiss()
                for (download in selectedList) {
                    val file = DocumentFile.fromSingleUri(
                        requireActivity().applicationContext,
                        Uri.parse(download.download!!.downloadedPath)
                    )
                    file?.takeIf { it.exists() }?.delete()
                    downloadAdapter.downloadList.remove(download)
                }
                selectedList.clear()
                downloadAdapter.notifyDataSetChanged()
                unSelectAll()
            }
            dialog.show()
            dialog.window!!
                .setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        llSelectAll.setOnClickListener {
            selectedList.clear()
            selectedList.addAll(downloadAdapter.downloadList)
            txtSelectedCount.text = selectedList.size.toString() + " selected"
            downloadAdapter.notifyDataSetChanged()
        }

        progressReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == "DOWNLOAD_PROGRESS") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        handleDownloadProgress(intent)
                    } else {
                        handleDownloadProgressIn(intent)
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(
                progressReceiver as BroadcastReceiver,
                IntentFilter("DOWNLOAD_PROGRESS")
            )
        return mView!!
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun handleDownloadProgress(intent: Intent) {
        val downloadInfoArrayList: ArrayList<DownloadProgress>? =
            intent.getParcelableArrayListExtra("downloadList", DownloadProgress::class.java)
        downloadInfoArrayList?.forEach { progress ->
            CoroutineScope(Dispatchers.IO).launch {
                downloadProgress?.update(progress)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun handleDownloadProgressIn(intent: Intent) {
        val downloadInfoArrayList: ArrayList<DownloadProgress>? =
            intent.getParcelableArrayListExtra("downloadList")
        downloadInfoArrayList?.forEach { progress ->
            CoroutineScope(Dispatchers.IO).launch {
                downloadProgress?.update(progress)
            }
        }
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
        downloadsViewModel!!.allDownloads.observe(viewLifecycleOwner) { downloads: List<Download>? ->
            val list = ArrayList(downloads!!)
            for (download in list) {
                val file: DocumentFile? = DocumentFile.fromSingleUri(
                    requireActivity().applicationContext,
                    Uri.parse(download.downloadedPath)
                )
                val strRename: String? = renameVideoPref.getString(download.id.toString(), "")
                if (strRename != null) {
                    if (strRename.isNotEmpty()) {
                        val desFile = File(strRename)
                        if (desFile.exists()) {
                            downloadAdapter.addDownload(download)
                        }
                    } else if (file!!.exists()) {
                        downloadAdapter.addDownload(download)
                    }
                }
                downloadAdapter.notifyDataSetChanged()
            }
        }
        downloadsViewModel!!.allProgress.observe(viewLifecycleOwner) {
            val list = kotlin.collections.ArrayList(it)
            downloadAdapter.loadProgress(list)
        }
    }


    inner class DownloadAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val downloadList: MutableList<DownloadData> = ArrayList()
        var progressList: ArrayList<DownloadProgress>? = ArrayList()
        var downloaded: Boolean = false
        var progressViewHolder: ProgressViewHolder? = null

        fun loadProgress(downloadDataList: ArrayList<DownloadProgress>?) {
            progressList = downloadDataList
            notifyDataSetChanged()
        }

        fun addDownload(download: Download) {
            var found = false
            var data: DownloadData? = null
            var dataPosition: Int = -1
            for (i in downloadList.indices) {
                val downloadData: DownloadData = downloadList[i]
                if (downloadData.id.toLong() == download.id) {
                    data = downloadData
                    dataPosition = i
                    found = true
                    break
                }
            }
            if (!found) {
                val downloadData = DownloadData()
                downloadData.id = download.id.toInt()
                downloadData.download = download
                downloadList.add(downloadData)
                downloadList.sortWith(Comparator { o1, o2 ->
                    o2.download!!.timestamp.toString()
                        .compareTo(o1.download!!.timestamp.toString())
                })
                notifyItemInserted(downloadList.indexOf(downloadData))
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
            return if (viewType == Companion.VIEW_TYPE_DOWNLOAD) {
                val view: View = inflater.inflate(R.layout.item_download, parent, false)
                ProgressViewHolder(view)
            } else {
                val view: View = inflater.inflate(R.layout.item_download, parent, false)
                ViewHolder(view)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < progressList!!.size) {
                Companion.VIEW_TYPE_DOWNLOAD
            } else {
                Companion.VIEW_TYPE_ITEM
            }
        }

        override fun onBindViewHolder(itemHolder: RecyclerView.ViewHolder, position: Int) {
            if (itemHolder is ViewHolder) {
                val holder: ViewHolder = itemHolder
                val downloadData: DownloadData = downloadList[position - progressList!!.size]
                val documentFile: DocumentFile? = DocumentFile.fromSingleUri(
                    (context)!!, Uri.parse(
                        downloadData.download!!.downloadedPath
                    )
                )
                var tempFile = File(downloadData.download!!.downloadedPath)
                val file = tempFile
                Glide.with((activity)!!).load(downloadData.download!!.thumbnail)
                    .into(holder.imgVideo)
                val strRename: String? =
                    renameVideoPref.getString(downloadData.download!!.id.toString(), "")
                if (strRename!!.isNotEmpty()) {
                    val desFile: File = File(strRename)
                    if (desFile.exists()) {
                        tempFile = desFile
                    }
                }
                holder.edtSearch.setText(downloadData.download!!.url)
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
                                arrayOf(documentFile.toString()), null
                            ) { path: String?, uri: Uri? ->
                                downloadsViewModel!!.viewContent(
                                    downloadData.download!!.downloadedPath, (context)!!
                                )
                            }
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
                holder.imgMore.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        val popup: PopupMenu = PopupMenu(activity, holder.imgMore)
                        popup.menuInflater.inflate(R.menu.menu_download, popup.menu)
                        popup.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.menu_share -> shareFile(downloadData.download!!.downloadedPath)
                                R.id.menu_rename -> renameFile(downloadData, documentFile)
                                R.id.menu_edit_video -> {
                                    val intent = Intent(Intent.ACTION_EDIT)
                                    intent.setDataAndType(Uri.parse(downloadData.download!!.downloadedPath), getMimeType(Uri.parse(downloadData.download!!.downloadedPath)))
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    try {
                                        val choose = Intent.createChooser(intent, "Edit with")
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
                                    } catch (e: java.lang.Exception) {
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
                                    (activity as MainActivity?)!!.navView.selectedItemId = R.id.navHome
                                    WebConnect(holder.edtSearch, (activity as MainActivity)).connect()
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
                                    txtNO.setOnClickListener { dialog.dismiss() }
                                    txtOK.setOnClickListener {
                                        downloadsViewModel!!.startDelete(
                                            downloadList[position].id.toLong(), dialog.context
                                        )
                                        downloadList.removeAt(position)
                                        notifyItemRemoved(position)
                                        notifyItemChanged(position)
                                        dialog.dismiss()
                                    }
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
                            true
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            popup.setForceShowIcon(true)
                        }
                        popup.show()
                    }
                })
                holder.imgCancel.visibility = View.GONE
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
                                val strDescFailed: String =
                                    "Failed " + downloadData.download!!.downloadedPercent + "% " + getStringSizeLengthFile(
                                        downloadData.download!!.downloadedSize
                                    ) + "/" + getStringSizeLengthFile(
                                        downloadData.download!!.totalSize
                                    )
                                holder.downloadTextSize.text = strDescFailed
                            }
                            WorkInfo.State.RUNNING -> {}
                            WorkInfo.State.ENQUEUED -> {
                                holder.imgCancel.visibility = View.VISIBLE
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                downloaded = true
                                holder.imgCancel.visibility = View.VISIBLE
                                holder.imgCancel.visibility = View.GONE
                                holder.downloadProgressBar.visibility = View.INVISIBLE
                                holder.downloadProgressBar1.visibility = View.INVISIBLE
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
                                holder.downloadTextSize.text = strDescComplete
                                holder.txtDuration.text =
                                    Utils.formatDuration(downloadData.download!!.duration)
                            }
                            WorkInfo.State.CANCELLED -> {
                                val strDesc2: String =
                                    "Cancelled " + downloadData.download!!.downloadedPercent + "% " + getStringSizeLengthFile(
                                        downloadData.download!!.downloadedSize
                                    ) + "/" + getStringSizeLengthFile(
                                        downloadData.download!!.totalSize
                                    )
                                holder.downloadTextSize.text = strDesc2
                                holder.imgCancel.visibility = View.GONE
                            }
                            WorkInfo.State.BLOCKED -> {}
                            else -> {}
                        }
                    } as (WorkInfo.State?) -> Unit)
                )
                if (isSelectedMode) {
                    holder.imgCancel.visibility = View.GONE
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
                progressViewHolder = itemHolder
                val downloadInfo: DownloadProgress = progressList!![position]
                if (downloadInfo.line.contains("download waiting.....")) {
                    progressViewHolder!!.downloadProgressBar1.progressDrawable =
                        CustomProgressBarDrawable(requireContext().getColor(R.color.primary_green))
                    progressViewHolder!!.downloadProgressBar1.visibility = View.VISIBLE
                    progressViewHolder!!.downloadProgressBar.visibility = View.GONE
                } else {
                    progressViewHolder!!.downloadProgressBar1.visibility = View.GONE
                    progressViewHolder!!.downloadProgressBar.visibility = View.VISIBLE
                    progressViewHolder!!.downloadProgressBar.progress = downloadInfo.progress
                }
                val result = Utils.extractPercentageAndMB(downloadInfo.line)
                if (result != null) {
                    val (percentage, sizeInMB, speedInKiB) = result
                    val mb = String.format("%.2f MB/S", speedInKiB / 1024)
                    val text = "$mb+${speedInKiB}KB/S"
                    val builder = SpannableStringBuilder(text)
                    val start = 0
                    val end = mb.length
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.setSpan(
                        ForegroundColorSpan(Color.parseColor("#00a69c")),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    progressViewHolder!!.downloadSize.text = "$percentage %/$sizeInMB MB"
                    progressViewHolder!!.downloadSpeed.text = builder
                } else {
                    progressViewHolder!!.downloadSize.text = "0.00%/0.00MB"
                    Log.d(TAG, "onBindViewHolder: ${downloadInfo.size}")
                    progressViewHolder!!.downloadSpeed.text = "0.00KB/S+0.00MB/S"
                }
                progressViewHolder!!.imgSelect.visibility = View.GONE
                progressViewHolder!!.imgMore.visibility = View.GONE
                progressViewHolder!!.downloadVideoName.text = downloadInfo.name
                Glide.with(requireContext()).load(downloadInfo.thumbnail)
                    .into(progressViewHolder!!.imgVideo)
                progressViewHolder!!.imgCancel.setOnClickListener {
                    val cancelIntent = Intent(context, CancelReceiver::class.java)
                    cancelIntent.putExtra("taskId", downloadInfo.taskId)
                    cancelIntent.putExtra("notificationId", downloadInfo.taskId)
                    activity!!.sendBroadcast(cancelIntent)
                    CoroutineScope(Dispatchers.IO).launch {
                        downloadProgress!!.delete(downloadInfo)
                    }
                }
                if (downloadInfo.progress == 100) {
                    CoroutineScope(Dispatchers.IO).launch {
                        downloadProgress!!.delete(downloadInfo)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return downloadList.size + progressList!!.size
        }

        internal inner class ViewHolder constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            var downloadVideoName: TextView
            var downloadProgressBar: ProgressBar
            var downloadProgressBar1: ProgressBar
            var imgVideo: ImageView
            var downloadTextSpeed: TextView
            var downloadTextSize: TextView
            var imgCancel: ImageView
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
                imgCancel = itemView.findViewById(R.id.imgCancel)
                downloadTextSize = itemView.findViewById(R.id.download_size)
                downloadTextSpeed = itemView.findViewById(R.id.download_speed)
                imgVideo = itemView.findViewById(R.id.imgVideo)
                downloadVideoName = itemView.findViewById(R.id.downloadVideoName)
                downloadProgressBar = itemView.findViewById(R.id.downloadProgressBar)
                downloadProgressBar1 = itemView.findViewById(R.id.downloadProgressBar1)
            }
        }

        inner class ProgressViewHolder constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            var downloadProgressBar: ProgressBar
            var downloadProgressBar1: ProgressBar
            var imgVideo: ImageView
            var downloadSize: TextView
            var downloadSpeed: TextView
            var downloadVideoName: TextView
            var imgMore: ImageView
            var imgCancel: ImageView
            var imgSelect: ImageView

            init {
                imgSelect = itemView.findViewById(R.id.imgSelect)
                imgMore = itemView.findViewById(R.id.imgMore)
                imgCancel = itemView.findViewById(R.id.imgCancel)
                downloadSpeed = itemView.findViewById(R.id.download_speed)
                downloadSize = itemView.findViewById(R.id.download_size)
                imgVideo = itemView.findViewById(R.id.imgVideo)
                downloadVideoName = itemView.findViewById(R.id.downloadVideoName)
                downloadProgressBar = itemView.findViewById(R.id.downloadProgressBar)
                downloadProgressBar1 = itemView.findViewById(R.id.downloadProgressBar1)
            }
        }
    }

    fun renameFile(downloadData: DownloadData, file: DocumentFile?) {
        val dialog = Dialog((activity)!!)
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

    private fun enablingWiFiDisplay() {
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

    fun getMimeType(uri: Uri): String? {
        var mimeType: String? = null
        mimeType = if ((ContentResolver.SCHEME_CONTENT == uri.scheme)) {
            val cr: ContentResolver = requireActivity().contentResolver
            cr.getType(uri)
        } else {
            val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
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