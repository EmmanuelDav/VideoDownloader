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
import com.cyberIyke.allvideodowloader.browser.VideoList
import com.cyberIyke.allvideodowloader.browser.VideoList.VideoListAdapter.VideoItem
import com.cyberIyke.allvideodowloader.model.VidInfoItem
import com.cyberIyke.allvideodowloader.model.VidInfoItem.VidFormatItem
import com.cyberIyke.allvideodowloader.utils.PermissionInterceptor
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
    var videoInfo: VideoInfo
) {
    var selectedVideo: Int = 0
    private var videoFormat: VideoFormat? = null
    private var headerItem: VidFormatItem? = null
    abstract fun onItemClicked(vidFormatItem: VidFormatItem?)

    init {
        selectedVideo = 0
        val videoListAdapter: VideoListAdapter = VideoListAdapter()
        videoListAdapter.fill(videoInfo)
        view.setAdapter(videoListAdapter)
        view.setLayoutManager(GridLayoutManager(activity, 3))
        view.setHasFixedSize(true)
        txtDownload.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View) {
                XXPermissions.with(activity)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .interceptor(PermissionInterceptor())
                    .request(object : OnPermissionCallback {
                        public override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (!all) {
                                return
                            }
                            onItemClicked(headerItem)
                        }

                        public override fun onDenied(permissions: List<String>, never: Boolean) {
                            super@OnPermissionCallback.onDenied(permissions, never)
                            Log.d(VideoList.Companion.TAG, "onDenied: =====")
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
        val videoListAdapter: VideoListAdapter = VideoListAdapter()
        videoListAdapter.fill(videoInfo)
        view.setAdapter(videoListAdapter)
        view.setLayoutManager(GridLayoutManager(activity, 3))
        view.setHasFixedSize(true)
        txtDownload.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View) {
                XXPermissions.with(activity)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .interceptor(PermissionInterceptor())
                    .request(object : OnPermissionCallback {
                        public override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (!all) {
                                return
                            }
                            onItemClicked(headerItem)
                        }

                        public override fun onDenied(permissions: List<String>, never: Boolean) {
                            super@OnPermissionCallback.onDenied(permissions, never)
                            Log.d(VideoList.Companion.TAG, "onDenied: =====")
                        }
                    })
            }
        })
    }

    internal inner class VideoListAdapter constructor() : RecyclerView.Adapter<VideoItem>() {
        var items: List<VidInfoItem> = emptyList()
        var mVideoInfo: VideoInfo? = null
        fun fill(vidInfo: VideoInfo?) {
            if (vidInfo == null) {
                items = emptyList()
            } else {
                val itemList: MutableList<VidInfoItem> = ArrayList()
                vidInfo.getFormats().forEach(Consumer({ videoFormat1: VideoFormat ->
                    itemList.add(
                        VidFormatItem(
                            vidInfo,
                            videoFormat1.getFormatId()
                        )
                    )
                }))
                items = itemList
            }
            notifyDataSetChanged()
        }

        public override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): VideoList.VideoListAdapter.VideoItem {
            val inflater: LayoutInflater = LayoutInflater.from(activity)
            return (VideoItem(
                inflater.inflate(
                    R.layout.video_found_item_lay, parent,
                    false
                )
            ))
        }

        public override fun onBindViewHolder(holder: VideoItem, position: Int) {
            val item: VidInfoItem = items.get(position)
            if (item is VidFormatItem) {
                headerItem = items.get(selectedVideo) as VidFormatItem?
                mVideoInfo = headerItem!!.vidInfo
                if (position == 0) {
                    imgVideo.setVisibility(View.VISIBLE)
                    if (mVideoInfo!!.getTitle() != null && mVideoInfo!!.getTitle().length > 0) {
                        txtTitle.setVisibility(View.VISIBLE)
                        txtTitle.setText(mVideoInfo!!.getTitle())
                    } else {
                        txtTitle.setVisibility(View.INVISIBLE)
                    }
                    Glide.with(activity)
                        .load(if (mVideoInfo!!.getThumbnail() == null) videoInfo.getUrl() else videoInfo.getThumbnail())
                        .thumbnail(0.5f)
                        .into(imgVideo)
                }
                val sizeFormatted: String = Formatter.formatShortFileSize(
                    activity,
                    mVideoInfo!!.getFormats().get(position).getFileSizeApproximate().toString()
                        .toLong()
                )
                holder.videoFoundSize.setText(if ((sizeFormatted == "0 B")) "Unknown" else sizeFormatted)
                holder.name.setText(mVideoInfo!!.getFulltitle())
                videoFormat = mVideoInfo!!.getFormats().get(selectedVideo)
                try {
                    holder.txtQuality.setText(
                        BrowserWindow.Companion.convertSolution(
                            mVideoInfo!!.getFormats().get(position).getFormatId()
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
                if (selectedVideo == position) {
                    holder.imgSelected.setVisibility(View.VISIBLE)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        holder.llMain.setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                activity, R.drawable.bg_round_quality_select
                            )
                        )
                    } else {
                        holder.llMain.setBackground(
                            ContextCompat.getDrawable(
                                activity, R.drawable.bg_round_quality_select
                            )
                        )
                    }
                } else {
                    holder.imgSelected.setVisibility(View.GONE)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        holder.llMain.setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                activity, R.drawable.bg_round_quality_unselect
                            )
                        )
                    } else {
                        holder.llMain.setBackground(
                            ContextCompat.getDrawable(
                                activity, R.drawable.bg_round_quality_unselect
                            )
                        )
                    }
                }
            }
        }

        public override fun getItemCount(): Int {
            if (videoInfo.getFormats().size == 0) {
                imgVideo.setVisibility(View.GONE)
                txtTitle.setVisibility(View.GONE)
                bottomSheetDialog.dismiss()
            }
            if (videoInfo.getFormats().size >= 7) {
                return 7
            }
            return videoInfo.getFormats().size
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

            public override fun onClick(v: View) {
                if (v === itemView) {
                    selectedVideo = getAdapterPosition()
                    notifyDataSetChanged()
                }
            }
        }
    }

    companion object {
        private val TAG: String = VideoList::class.java.getCanonicalName()
    }
}