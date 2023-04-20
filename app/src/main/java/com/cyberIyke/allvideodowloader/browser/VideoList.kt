package com.cyberIyke.allvideodowloader.browser

import android.app.Activity
import android.os.Build
import android.text.format.Formatter
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.browser.VideoList.VideoListAdapter.VideoItem
import com.cyberIyke.allvideodowloader.model.Format
import com.cyberIyke.allvideodowloader.model.VidInfoItem
import com.cyberIyke.allvideodowloader.model.VidInfoItem.VidFormatItem
import com.cyberIyke.allvideodowloader.utils.PermissionInterceptor
import com.cyberIyke.allvideodowloader.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hjq.permissions.*
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import java.util.function.Consumer

abstract class VideoList internal constructor(
    private val activity: Activity,
    private var view: RecyclerView,
    var imgVideo: ImageView,
    var txtTitle: EditText,
    var txtDownload: TextView,
    var bottomSheetDialog: BottomSheetDialog,
    var videoInfo: VideoInfo,
    val formats: MutableList<Format> = mutableListOf()
) {
    var selectedVideo: Int = 0
    private var headerItem: VidFormatItem? = null

    abstract fun onItemClicked(vidFormatItem: VidFormatItem?)

    init {
        selectedVideo = 0
        val videoListAdapter: VideoListAdapter = VideoListAdapter()
        videoListAdapter.fill(videoInfo)
        view.adapter = videoListAdapter
        view.layoutManager = GridLayoutManager(activity, 3)
        view.setHasFixedSize(true)
        txtDownload.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                XXPermissions.with(activity)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .interceptor(PermissionInterceptor())
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (!all) {
                                return
                            }
                            onItemClicked(headerItem)
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            super.onDenied(permissions, never)
                            Log.d(TAG, "onDenied: =====")
                        }
                    })
            }
        })
    }

    fun recreateVideoList(
        view: RecyclerView,
        imgVideo: ImageView,
        txtTitle: EditText,
        txtDownload: TextView,
        bottomSheetDialog: BottomSheetDialog,
        videoInfo: VideoInfo
    ) {
        this.view = view
        this.imgVideo = imgVideo
        this.txtTitle = txtTitle
        this.txtDownload = txtDownload
        this.bottomSheetDialog = bottomSheetDialog
        this.videoInfo = videoInfo
        selectedVideo = 0
        val videoListAdapter = VideoListAdapter()
        view.layoutManager = GridLayoutManager(activity, 3)
        view.adapter = videoListAdapter
        videoListAdapter.fill(videoInfo)
        view.setHasFixedSize(true)
        txtDownload.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                XXPermissions.with(activity)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .interceptor(PermissionInterceptor())
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (!all) {
                                return
                            }
                            onItemClicked(headerItem)
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            super.onDenied(permissions, never)
                            Log.d(TAG, "onDenied: =====")
                        }
                    })
            }
        })
    }

    internal inner class VideoListAdapter : RecyclerView.Adapter<VideoItem>() {
        var items: List<VidInfoItem> = emptyList()
        var mVideoInfo: VideoInfo? = null

        fun fill(vidInfo: VideoInfo?) {
            items = if (vidInfo == null) {
                emptyList()

            } else {
                val itemList: MutableList<VidInfoItem> = ArrayList()
                vidInfo.formats!!.forEach(Consumer { videoFormat1: VideoFormat ->
                    itemList.add(
                        VidFormatItem(
                            vidInfo,
                            videoFormat1.formatId!!
                        )
                    )
                })

                itemList
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): VideoItem {
            val inflater: LayoutInflater = LayoutInflater.from(activity)
            return (VideoItem(
                inflater.inflate(
                    R.layout.video_found_item_lay, parent,
                    false
                )
            ))
        }

        override fun onBindViewHolder(holder: VideoItem, position: Int) {
            if (position == 0) {
                if (items.size > 1) {
                    headerItem = items[selectedVideo] as VidFormatItem?
                    mVideoInfo = headerItem!!.vidInfo
                }
                imgVideo.visibility = View.VISIBLE
                if (videoInfo.title!!.isNotEmpty()) {
                    txtTitle.visibility = View.VISIBLE
                    txtTitle.setText(videoInfo.title)
                } else {
                    txtTitle.visibility = View.INVISIBLE
                }
                Glide.with(activity)
                    .load(if (videoInfo.thumbnail == null) videoInfo.url else videoInfo.thumbnail)
                    .thumbnail(0.5f)
                    .into(imgVideo)
            }

            if (items.size <= 1) {
                headerItem = items[0] as VidFormatItem?
                val items = formats[position]
                holder.videoFoundSize.text = items.encoding
                holder.txtQuality.text = items.format_id
            } else {
                val sizeFormatted: String = Formatter.formatShortFileSize(
                    activity,
                    mVideoInfo!!.formats!![position].fileSizeApproximate.toString()
                        .toLong()
                )
                val resolution = Utils.getNumbersFromString(BrowserWindow.convertSolution(mVideoInfo!!.formats!![position].formatId!!))
                holder.videoFoundSize.text =
                    if (sizeFormatted == "0 B") BrowserWindow.estimateVideoSize(
                        mVideoInfo!!.duration, resolution!!
                    ) else sizeFormatted
                holder.name.text = mVideoInfo!!.fulltitle
                try {
                    holder.txtQuality.text = BrowserWindow.convertSolution(
                        mVideoInfo!!.formats!![position].formatId!!
                    )
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }

            if (selectedVideo == position) {
                holder.imgSelected.visibility = View.VISIBLE
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    holder.llMain.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            activity, R.drawable.bg_round_quality_select
                        )
                    )
                } else {
                    holder.llMain.background = ContextCompat.getDrawable(
                        activity, R.drawable.bg_round_quality_select
                    )
                }
            } else {
                holder.imgSelected.visibility = View.GONE
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    holder.llMain.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            activity, R.drawable.bg_round_quality_unselect
                        )
                    )
                } else {
                    holder.llMain.background = ContextCompat.getDrawable(
                        activity, R.drawable.bg_round_quality_unselect
                    )
                }
            }
        }

        override fun getItemCount(): Int {
            if (videoInfo.formats!!.size == 0) {
                imgVideo.visibility = View.GONE
                txtTitle.visibility = View.GONE
                bottomSheetDialog.dismiss()
            }
            if (videoInfo.formats!!.size >= 7) {
                return 7
            }

            if (videoInfo.formats!!.size <= 1) {
                return formats.size
            }

            return videoInfo.formats!!.size
        }


        internal inner class VideoItem constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var txtQuality: TextView
            var videoFoundSize: TextView
            var name: TextView
            var imgSelected: ImageView
            var llMain: LinearLayout

            init {
                llMain = itemView.findViewById(R.id.llMain)
                imgSelected = itemView.findViewById(R.id.imgSelected)
                txtQuality = itemView.findViewById(R.id.txtQuality)
                videoFoundSize = itemView.findViewById(R.id.videoFoundSize)
                name = itemView.findViewById(R.id.videoFoundName)
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View) {
                if (v === itemView) {
                    selectedVideo = adapterPosition
                    notifyDataSetChanged()
                }
            }
        }
    }

    companion object {
        private val TAG: String = VideoList::class.java.canonicalName
    }
}