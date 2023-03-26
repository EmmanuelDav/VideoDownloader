package com.cyberIyke.allvideodowloader.fragmentsimport

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

class AllDownloadFragment constructor() : Fragment() {
    private var view: View? = null
    var imgCast: ImageView? = null
    var downloadsList: RecyclerView? = null
    var downloadAdapter: DownloadAdapter? = null
    var downloadsViewModel: DownloadsViewModel? = null
    var selectedList: ArrayList<DownloadData> = ArrayList()
    var isSelectedMode: Boolean = false
    private var llBottom: LinearLayout? = null
    private var rlTopSelected: RelativeLayout? = null
    var downloadInterface: DownloadInterface? = null
    var txtSelectedCount: TextView? = null
    var imgCancel: ImageView? = null
    var llDeleteSelected: LinearLayout? = null
    var llSelectAll: LinearLayout? = null
    var renameVideoPref: RenameVideoPref? = null
    var progressReceiver: BroadcastReceiver? = null
    public override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        view = inflater.inflate(R.layout.fragment_all_download, container, false)
        downloadsViewModel = ViewModelProvider(this).get(
            DownloadsViewModel::class.java
        )
        renameVideoPref = RenameVideoPref(getActivity())
        downloadsList = view.findViewById(R.id.downloadsList)
        downloadAdapter = DownloadAdapter()
        downloadsList.setLayoutManager(LinearLayoutManager(getActivity()))
        downloadsList.setAdapter(downloadAdapter)
        llSelectAll = view.findViewById(R.id.llSelectAll)
        llDeleteSelected = view.findViewById(R.id.llDeleteSelected)
        imgCancel = view.findViewById(R.id.imgCancel)
        txtSelectedCount = view.findViewById(R.id.txtSelectedCount)
        rlTopSelected = view.findViewById(R.id.rlTopSelected)
        llBottom = view.findViewById(R.id.llBottom)
        imgCast = view.findViewById(R.id.imgCast)
        imgCast.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                enablingWiFiDisplay()
            }
        })
        imgCancel.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                unSelectAll()
            }
        })
        llDeleteSelected.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                val dialog: Dialog = Dialog((getActivity())!!)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.dialog_confirmation)
                val txtTitle: TextView = dialog.findViewById(R.id.txtTitle)
                val txtDesc: TextView = dialog.findViewById(R.id.txtDesc)
                txtTitle.setText("Confirm")
                txtDesc.setText("Are you sure you want to delete this video?")
                val txtNO: TextView = dialog.findViewById(R.id.txtNO)
                val txtOK: TextView = dialog.findViewById(R.id.txtOK)
                txtNO.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
                        dialog.dismiss()
                    }
                })
                txtOK.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
                        dialog.dismiss()
                        for (download: DownloadData in selectedList) {
                            val file: DocumentFile? = DocumentFile.fromSingleUri(
                                getActivity()!!.getApplicationContext(), Uri.parse(
                                    download.download!!.downloadedPath
                                )
                            )
                            if (file!!.exists()) {
                                file.delete()
                                //fetch.remove((int) download.download.getId());;
                            }
                        }
                        downloadAdapter!!.notifyDataSetChanged()
                        onResume()
                        unSelectAll()
                    }
                })
                dialog.show()
                dialog.getWindow()!!
                    .setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                    )
                dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        })
        llSelectAll.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                selectedList.clear()
                selectedList.addAll(downloadAdapter!!.downloads)
                txtSelectedCount.setText(selectedList.size.toString() + " selected")
                downloadAdapter!!.notifyDataSetChanged()
            }
        })
        progressReceiver = object : BroadcastReceiver() {
            public override fun onReceive(context: Context?, intent: Intent) {
                if ((intent.getAction() == "DOWNLOAD_PROGRESS")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val downloadInfoArrayList: ArrayList<DownloadInfo>? =
                            intent.getParcelableArrayListExtra(
                                "downloadList",
                                DownloadInfo::class.java
                            )
                        downloadAdapter!!.loadProgress(downloadInfoArrayList)
                    }
                    if (downloadInterface != null) {
                        downloadInterface!!.loading()
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(progressReceiver, IntentFilter("DOWNLOAD_PROGRESS"))
        return view
    }

    fun unSelectAll() {
        txtSelectedCount!!.setText("0 selected")
        isSelectedMode = false
        selectedList.clear()
        downloadAdapter!!.notifyDataSetChanged()
        (getActivity() as MainActivity?)!!.navView.setVisibility(View.VISIBLE)
        llBottom!!.setVisibility(View.GONE)
        rlTopSelected!!.setVisibility(View.GONE)
    }

    public override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver((progressReceiver)!!)
    }

    public override fun onResume() {
        super.onResume()
        downloadsViewModel!!.allDownloads.observe(
            getViewLifecycleOwner(),
            Observer({ downloads: List<Download>? ->
                val list: ArrayList<Download> = ArrayList(downloads)
                for (download: Download in list) {
                    val file: DocumentFile? = DocumentFile.fromSingleUri(
                        getActivity()!!.getApplicationContext(),
                        Uri.parse(download.downloadedPath)
                    )
                    val strRename: String = renameVideoPref!!.getString(download.id.toString(), "")
                    if (strRename.length > 0) {
                        val desFile: File = File(strRename)
                        if (desFile.exists()) {
                            downloadAdapter!!.addDownload(download)
                        }
                    } else if (file!!.exists()) {
                        downloadAdapter!!.addDownload(download)
                    }
                    downloadAdapter!!.notifyDataSetChanged()
                }
                if (downloadInterface != null) {
                    downloadInterface!!.notLoading()
                }
            })
        )
    }

    inner class DownloadAdapter constructor() : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
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
                Collections.sort(downloads, object : Comparator<DownloadData?> {
                    public override fun compare(o1: DownloadData, o2: DownloadData): Int {
                        return o2.download!!.timestamp.toString()
                            .compareTo(o1.download!!.timestamp.toString())
                    }
                })
                notifyItemInserted(downloads.indexOf(downloadData))
            } else {
                data!!.download = download
                notifyItemChanged(dataPosition)
            }
        }

        public override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val inflater: LayoutInflater = LayoutInflater.from(parent.getContext())
            if (viewType == Companion.VIEW_TYPE_DOWNLOAD) {
                val view: View = inflater.inflate(R.layout.item_download, parent, false)
                return ProgressViewHolder(view)
            } else {
                val view: View = inflater.inflate(R.layout.item_download, parent, false)
                return AllDownloadFragment.DownloadAdapter.ViewHolder(view)
            }
        }

        public override fun getItemViewType(position: Int): Int {
            if (position < downloadList!!.size) {
                return Companion.VIEW_TYPE_DOWNLOAD
            } else {
                return Companion.VIEW_TYPE_ITEM
            }
        }

        public override fun onBindViewHolder(itemHolder: RecyclerView.ViewHolder, position: Int) {
            if (itemHolder is AllDownloadFragment.DownloadAdapter.ViewHolder) {
                val downloadData: DownloadData
                val holder: AllDownloadFragment.DownloadAdapter.ViewHolder = itemHolder
                downloadData = downloads.get(position - downloadList!!.size)
                val documentFile: DocumentFile? = DocumentFile.fromSingleUri(
                    (getContext())!!, Uri.parse(
                        downloadData.download!!.downloadedPath
                    )
                )
                var tempFile: File = File(downloadData.download!!.downloadedPath)
                val file: File = tempFile
                Glide.with((getActivity())!!).load(downloadData.download!!.downloadedPath)
                    .into(holder.imgVideo)
                val strRename: String =
                    renameVideoPref!!.getString(downloadData.download!!.id.toString(), "")
                if (strRename.length > 0) {
                    val desFile: File = File(strRename)
                    if (desFile.exists()) {
                        tempFile = desFile
                    }
                }
                holder.downloadVideoName.setText(downloadData.download!!.name)
                holder.imgVideo.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
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
                            txtSelectedCount!!.setText(selectedList.size.toString() + " selected")
                            notifyDataSetChanged()
                            return
                        }
                        if (downloaded) {
                            MediaScannerConnection.scanFile(
                                getActivity(),
                                arrayOf(documentFile.toString()),
                                null,
                                OnScanCompletedListener({ path: String?, uri: Uri? ->
                                    downloadsViewModel!!.viewContent(
                                        downloadData.download!!.downloadedPath, (getContext())!!
                                    )
                                })
                            )
                        }
                    }
                })
                holder.imgVideo.setOnLongClickListener(object : OnLongClickListener {
                    public override fun onLongClick(v: View?): Boolean {
                        if (!isSelectedMode) {
                            selectedList.add(downloadData)
                            isSelectedMode = true
                            txtSelectedCount!!.setText(selectedList.size.toString() + " selected")
                            downloadAdapter!!.notifyDataSetChanged()
                            (getActivity() as MainActivity?)!!.navView.setVisibility(View.GONE)
                            llBottom!!.setVisibility(View.VISIBLE)
                            rlTopSelected!!.setVisibility(View.VISIBLE)
                        }
                        return false
                    }
                })
                holder.llContent.setOnLongClickListener(object : OnLongClickListener {
                    public override fun onLongClick(v: View?): Boolean {
                        if (!isSelectedMode) {
                            selectedList.add(downloadData)
                            isSelectedMode = true
                            txtSelectedCount!!.setText(selectedList.size.toString() + " selected")
                            downloadAdapter!!.notifyDataSetChanged()
                            (getActivity() as MainActivity?)!!.navView.setVisibility(View.GONE)
                            llBottom!!.setVisibility(View.VISIBLE)
                            rlTopSelected!!.setVisibility(View.VISIBLE)
                        }
                        return false
                    }
                })
                holder.imgSelect.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
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
                        txtSelectedCount!!.setText(selectedList.size.toString() + " selected")
                        notifyDataSetChanged()
                    }
                })
                holder.llContent.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
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
                            txtSelectedCount!!.setText(selectedList.size.toString() + " selected")
                            notifyDataSetChanged()
                            return
                        }
                        if (downloaded) {
                            MediaScannerConnection.scanFile(
                                getActivity(),
                                arrayOf(documentFile.toString()),
                                null,
                                object : OnScanCompletedListener {
                                    public override fun onScanCompleted(path: String?, uri: Uri?) {
                                        downloadsViewModel!!.viewContent(
                                            downloadData.download!!.downloadedPath,
                                            (getContext())!!
                                        )
                                    }
                                })
                        }
                    }
                })
                holder.imgCancel.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
                        //  fetch.remove(downloadData.download.getId());
                    }
                })
                holder.imgResume.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
//                    if (status == Status.FAILED || status == Status.CANCELLED) {
//                        //fetch.retry(downloadData.download.getId());
//                    } else {
//                      //  fetch.resume(downloadData.download.getId());
//                    }
                    }
                })
                holder.imgPause.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
                        //fetch.pause((int) downloadData.download.getId());
                    }
                })
                holder.imgMore.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(v: View?) {
                        val popup: PopupMenu = PopupMenu(getActivity(), holder.imgMore)
                        popup.getMenuInflater().inflate(R.menu.menu_download, popup.getMenu())
                        popup.setOnMenuItemClickListener(object :
                            PopupMenu.OnMenuItemClickListener {
                            public override fun onMenuItemClick(item: MenuItem): Boolean {
                                when (item.getItemId()) {
                                    R.id.menu_share -> shareFile(downloadData.download!!.downloadedPath)
                                    R.id.menu_rename -> renameFile(downloadData, documentFile)
                                    R.id.menu_edit_video -> {
                                        val videoURI: Uri = FileProvider.getUriForFile(
                                            (getActivity())!!,
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
                                        getActivity()!!.onBackPressed()
                                        (getActivity() as MainActivity?)!!.isEnableSuggetion = false
                                        (getActivity() as MainActivity?)!!.navView.setSelectedItemId(
                                            R.id.navHome
                                        )
                                        WebConnect(
                                            holder.edtSearch,
                                            (getActivity() as MainActivity?)
                                        ).connect()
                                    }
                                    R.id.menu_delete -> {
                                        val dialog: Dialog = Dialog((getActivity())!!)
                                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                        dialog.setContentView(R.layout.dialog_confirmation)
                                        val txtTitle: TextView = dialog.findViewById(R.id.txtTitle)
                                        val txtDesc: TextView = dialog.findViewById(R.id.txtDesc)
                                        txtTitle.setText("Confirm")
                                        txtDesc.setText("Are you sure you want to delete this video?")
                                        val txtNO: TextView = dialog.findViewById(R.id.txtNO)
                                        val txtOK: TextView = dialog.findViewById(R.id.txtOK)
                                        txtNO.setOnClickListener(object : View.OnClickListener {
                                            public override fun onClick(v: View?) {
                                                dialog.dismiss()
                                            }
                                        })
                                        txtOK.setOnClickListener(object : View.OnClickListener {
                                            public override fun onClick(v: View?) {
                                                downloadsViewModel!!.startDelete(
                                                    downloads.get(
                                                        position
                                                    ).id.toLong(), dialog.getContext()
                                                )
                                                dialog.dismiss()
                                                onResume()
                                            }
                                        })
                                        dialog.show()
                                        dialog.getWindow()!!.setLayout(
                                            WindowManager.LayoutParams.MATCH_PARENT,
                                            WindowManager.LayoutParams.MATCH_PARENT
                                        )
                                        dialog.getWindow()!!.setBackgroundDrawable(
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
                holder.imgCancel.setVisibility(View.GONE)
                holder.imgPause.setVisibility(View.GONE)
                holder.imgResume.setVisibility(View.GONE)
                holder.txtDuration.setVisibility(View.GONE)
                holder.imgMore.setVisibility(View.GONE)
                holder.imgSelect.setVisibility(View.GONE)
                holder.downloadProgressBar.setVisibility(View.VISIBLE)
                downloadsViewModel!!.loadState.observe(
                    getViewLifecycleOwner(),
                    Observer({ state: WorkInfo.State ->
                        Log.d(AllDownloadFragment.Companion.TAG, "onBindViewHolder: " + state)
                        when (state) {
                            WorkInfo.State.FAILED -> {
                                holder.imgCancel.setVisibility(View.VISIBLE)
                                holder.imgPause.setVisibility(View.GONE)
                                holder.imgResume.setVisibility(View.VISIBLE)
                                val strDescFailed: String =
                                    "Failed " + downloadData.download!!.downloadedPercent + "% " + getStringSizeLengthFile(
                                        downloadData.download!!.downloadedSize
                                    ) + "/" + getStringSizeLengthFile(
                                        downloadData.download!!.totalSize
                                    )
                                holder.downloadProgressText.setText(strDescFailed)
                            }
                            WorkInfo.State.RUNNING -> return@observe
                            WorkInfo.State.ENQUEUED -> {
                                holder.imgCancel.setVisibility(View.VISIBLE)
                                holder.imgPause.setVisibility(View.VISIBLE)
                                holder.imgResume.setVisibility(View.GONE)
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                downloaded = true
                                holder.imgCancel.setVisibility(View.VISIBLE)
                                holder.imgPause.setVisibility(View.VISIBLE)
                                holder.imgResume.setVisibility(View.GONE)
                                holder.imgCancel.setVisibility(View.GONE)
                                holder.imgPause.setVisibility(View.GONE)
                                holder.imgResume.setVisibility(View.GONE)
                                holder.downloadProgressBar.setVisibility(View.GONE)
                                holder.txtDuration.setVisibility(View.VISIBLE)
                                holder.imgMore.setVisibility(View.VISIBLE)
                                val dateString: String = SimpleDateFormat("MMMM dd yyyy").format(
                                    Date(
                                        downloadData.download!!.timestamp
                                    )
                                )
                                val strDescComplete: String = getStringSizeLengthFile(
                                    downloadData.download!!.downloadedSize
                                ) + "  " + dateString
                                holder.downloadProgressText.setText(strDescComplete)
                                if (documentFile!!.exists()) {
                                    var duration: String? = null
                                    try {
                                        duration = convertSecondsToHMmSs(
                                            getFileDuration(
                                                downloadData.download!!.downloadedPath, getContext()
                                            )
                                        )
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                    if (duration != null) holder.txtDuration.setText(duration)
                                }
                            }
                            WorkInfo.State.CANCELLED -> {
                                val strDesc2: String =
                                    "Cancelled " + downloadData.download!!.downloadedPercent + "% " + getStringSizeLengthFile(
                                        downloadData.download!!.downloadedSize
                                    ) + "/" + getStringSizeLengthFile(
                                        downloadData.download!!.totalSize
                                    )
                                holder.downloadProgressText.setText(strDesc2)
                                holder.imgCancel.setVisibility(View.GONE)
                                holder.imgPause.setVisibility(View.GONE)
                                holder.imgResume.setVisibility(View.VISIBLE)
                            }
                            WorkInfo.State.BLOCKED -> return@observe
                            else -> {}
                        }
                    })
                )
                if (isSelectedMode) {
                    holder.imgCancel.setVisibility(View.GONE)
                    holder.imgPause.setVisibility(View.GONE)
                    holder.imgResume.setVisibility(View.GONE)
                    holder.imgMore.setVisibility(View.GONE)
                    holder.imgSelect.setVisibility(View.VISIBLE)
                    var isContain: Boolean = false
                    for (data: DownloadData in selectedList) {
                        if (data.download!!.id == downloadData.download!!.id) {
                            isContain = true
                        }
                    }
                    if (isContain) {
                        Glide.with((getActivity())!!).load(R.drawable.ic_box_selected)
                            .into(holder.imgSelect)
                    } else {
                        Glide.with((getActivity())!!).load(R.drawable.ic_box_unselect)
                            .into(holder.imgSelect)
                    }
                }
            } else if (itemHolder is ProgressViewHolder) {
                headerViewHolder = itemHolder
                val downloadInfo: DownloadInfo = downloadList!!.get(position)
                headerViewHolder!!.downloadProgressBar.setProgress(downloadInfo.progress)
                headerViewHolder!!.downloadProgressText.setText(downloadInfo.line)
                headerViewHolder!!.imgSelect.setVisibility(View.GONE)
                headerViewHolder!!.downloadVideoName.setText(downloadInfo.name)
                originalHeight = headerViewHolder!!.itemView.getLayoutParams().height
            }
        }

        public override fun getItemCount(): Int {
            return downloads.size + downloadList!!.size
        }

        public override fun loading() {
            Log.d(AllDownloadFragment.Companion.TAG, "loading: loading")
            if (headerViewHolder != null) {
                headerViewHolder!!.itemView.setVisibility(View.VISIBLE)
                headerViewHolder!!.itemView.getLayoutParams().height = -2
            }
        }

        public override fun notLoading() {
            Log.d(AllDownloadFragment.Companion.TAG, "loading: not loading")
            if (headerViewHolder != null) {
                headerViewHolder!!.itemView.setVisibility(View.GONE)
                headerViewHolder!!.itemView.getLayoutParams().height = 0
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

        internal inner class ProgressViewHolder constructor(itemView: View) :
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

        companion object {
            private val VIEW_TYPE_DOWNLOAD: Int = 0
            private val VIEW_TYPE_ITEM: Int = 1
        }
    }

    fun renameFile(downloadData: DownloadData, file: DocumentFile?) {
        val dialog: Dialog = Dialog((getActivity())!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_rename)
        val edtName: EditText = dialog.findViewById(R.id.edtName)
        edtName.setText(downloadData.download!!.name)
        val txtNO: TextView = dialog.findViewById(R.id.txtNO)
        val txtOK: TextView = dialog.findViewById(R.id.txtOK)
        txtNO.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                dialog.dismiss()
            }
        })
        txtOK.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                val strName: String = edtName.getText().toString().trim({ it <= ' ' })
                    .replace("[^\\w ()'!\\[\\]\\-]".toRegex(), "")
                if (strName.isEmpty() || strName.length == 0) {
                    Toast.makeText(getActivity(), "Please enter video name", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                try {
                    val os: OutputStream? = getContext()!!.getContentResolver().openOutputStream(
                        file!!.getUri()
                    )
                    os!!.write(edtName.getText().toString().toByteArray())
                    os.close()
                } catch (exception: Exception) {
                }
                onResume()
                dialog.dismiss()
            }
        })
        dialog.show()
        dialog.getWindow()!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    class DownloadData constructor() {
        var id: Int = 0
        var download: Download? = null
        var eta: Long = -1
        var downloadedBytesPerSecond: Long = 0
        public override fun hashCode(): Int {
            return id
        }

        public override fun toString(): String {
            if (download == null) {
                return ""
            }
            return download.toString()
        }

        public override fun equals(obj: Any?): Boolean {
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
            Toast.makeText(getActivity(), "Device not supported", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(file: String) {
        val fileUri: DocumentFile? = DocumentFile.fromSingleUri((getContext())!!, Uri.parse(file))
        val msg: StringBuilder = StringBuilder()
        msg.append(getResources().getString(R.string.share_message))
        msg.append("\n")
        msg.append("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)
        if (fileUri != null) {
            val shareIntent: Intent = Intent()
            shareIntent.setAction(Intent.ACTION_SEND)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.setType("*/*")
            shareIntent.putExtra(Intent.EXTRA_TEXT, msg.toString())
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(file))
            try {
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(getActivity(), "No App Available", Toast.LENGTH_SHORT).show()
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
        if ((ContentResolver.SCHEME_CONTENT == uri.getScheme())) {
            val cr: ContentResolver = getActivity()!!.getContentResolver()
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
        private val TAG: String = AllDownloadFragment::class.java.getCanonicalName()
    }
}