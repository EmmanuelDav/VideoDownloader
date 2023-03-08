package com.kunkunapp.allvideodowloader.browser;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.kunkunapp.allvideodowloader.activities.MainActivity;
import com.kunkunapp.allvideodowloader.model.VidInfoItem;
import com.kunkunapp.allvideodowloader.R;
import com.kunkunapp.allvideodowloader.utils.PermissionInterceptor;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import kotlin.Unit;

public abstract class VideoList {
    private static final String TAG = VideoList.class.getCanonicalName();
    public int selectedVideo = 0;
    private Activity activity;
    private RecyclerView view;
    ImageView imgVideo;
    EditText txtTitle;
    TextView txtDownload;
    BottomSheetDialog bottomSheetDialog;
    VideoInfo videoInfo;
    private VideoFormat videoFormat;
    private VidInfoItem.VidFormatItem headerItem;


    abstract void onItemClicked(VidInfoItem.VidFormatItem vidFormatItem);

    VideoList(Activity activity, RecyclerView view, ImageView imgVideo, EditText txtTitle, TextView txtDownload, BottomSheetDialog bottomSheetDialog, VideoInfo v) {
        this.activity = activity;
        this.view = view;
        this.imgVideo = imgVideo;
        this.txtTitle = txtTitle;
        this.txtDownload = txtDownload;
        this.bottomSheetDialog = bottomSheetDialog;
        this.videoInfo = v;
        selectedVideo = 0;
        VideoListAdapter videoListAdapter = new VideoListAdapter();
        videoListAdapter.fill(videoInfo);
        view.setAdapter(videoListAdapter);
        view.setLayoutManager(new GridLayoutManager(activity, 3));
        view.setHasFixedSize(true);
        txtDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XXPermissions.with(activity)
                        .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                        .interceptor(new PermissionInterceptor())
                        .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
                                if (!all) {
                                    return;
                                }

                                onItemClicked(headerItem);
                            }

                            @Override
                            public void onDenied(List<String> permissions, boolean never) {
                                OnPermissionCallback.super.onDenied(permissions, never);
                                Log.d(TAG, "onDenied: =====");
                            }
                        });
            }
        });
    }

    void recreateVideoList(RecyclerView view, ImageView imgVideo, EditText txtTitle, TextView txtDownload, BottomSheetDialog bottomSheetDialog, VideoInfo videoInfo) {
        this.view = view;
        this.imgVideo = imgVideo;
        this.txtTitle = txtTitle;
        this.txtDownload = txtDownload;
        this.bottomSheetDialog = bottomSheetDialog;
        this.videoInfo = videoInfo;
        selectedVideo = 0;
        VideoListAdapter videoListAdapter = new VideoListAdapter();
        videoListAdapter.fill(videoInfo);
        view.setAdapter(videoListAdapter);
        view.setLayoutManager(new GridLayoutManager(activity, 3));
        view.setHasFixedSize(true);

        txtDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XXPermissions.with(activity)
                        .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                        .interceptor(new PermissionInterceptor())
                        .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
                                if (!all) {
                                    return;
                                }
                                onItemClicked(headerItem);
                            }

                            @Override
                            public void onDenied(List<String> permissions, boolean never) {
                                OnPermissionCallback.super.onDenied(permissions, never);
                                Log.d(TAG, "onDenied: =====");

                            }
                        });
            }
        });
    }


    class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.VideoItem> {
        List<VidInfoItem> items = Collections.emptyList();
        VideoInfo mVideoInfo;

        public void fill(VideoInfo vidInfo) {
            if (vidInfo == null) {
                items = Collections.emptyList();
            } else {
                List<VidInfoItem> itemList = new ArrayList<>();
                vidInfo.getFormats().forEach(videoFormat1 -> {
                    itemList.add(new VidInfoItem.VidFormatItem(vidInfo, videoFormat1.getFormatId()));
                });
                items = itemList;
            }
            notifyDataSetChanged();
        }

        @Override
        public VideoItem onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(activity);
            return (new VideoItem(inflater.inflate(R.layout.video_found_item_lay, parent,
                    false)));
        }

        @Override
        public void onBindViewHolder(VideoItem holder, int position) {
            VidInfoItem item = items.get(position);
            if (item instanceof VidInfoItem.VidFormatItem) {
                headerItem = (VidInfoItem.VidFormatItem) items.get(selectedVideo);
                 mVideoInfo = headerItem.getVidInfo();
                if (position == 0) {
                    imgVideo.setVisibility(View.VISIBLE);
                    if (mVideoInfo.getTitle() != null && mVideoInfo.getTitle().length() > 0) {
                        txtTitle.setVisibility(View.VISIBLE);
                        txtTitle.setText(mVideoInfo.getTitle());
                    } else {
                        txtTitle.setVisibility(View.INVISIBLE);
                    }
                    Glide.with(activity)
                            .load(mVideoInfo.getThumbnail())
                            .thumbnail(0.5f)
                            .into(imgVideo);
                }


                String sizeFormatted = Formatter.formatShortFileSize(activity, Long.parseLong(String.valueOf(mVideoInfo.getFormats().get(position).getFileSizeApproximate())));
                holder.videoFoundSize.setText(sizeFormatted);
                holder.name.setText(mVideoInfo.getFulltitle());
                videoFormat = mVideoInfo.getFormats().get(selectedVideo);

                try {
                    holder.txtQuality.setText(BrowserWindow.convertSolution(mVideoInfo.getFormats().get(position).getFormatId()));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                if (selectedVideo == position) {
                    holder.imgSelected.setVisibility(View.VISIBLE);
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        holder.llMain.setBackgroundDrawable(ContextCompat.getDrawable(activity, R.drawable.bg_round_quality_select));
                    } else {
                        holder.llMain.setBackground(ContextCompat.getDrawable(activity, R.drawable.bg_round_quality_select));
                    }
                } else {
                    holder.imgSelected.setVisibility(View.GONE);
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        holder.llMain.setBackgroundDrawable(ContextCompat.getDrawable(activity, R.drawable.bg_round_quality_unselect));
                    } else {
                        holder.llMain.setBackground(ContextCompat.getDrawable(activity, R.drawable.bg_round_quality_unselect));
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            if (videoInfo.getFormats().size() == 0) {
                imgVideo.setVisibility(View.GONE);
                txtTitle.setVisibility(View.GONE);
                bottomSheetDialog.dismiss();
            }
            if (videoInfo.getFormats().size() >= 7) {
                return 7;
            }
            return videoInfo.getFormats().size();
        }

        class VideoItem extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView txtQuality, videoFoundSize;
            TextView name;
            ImageView imgSelected;
            LinearLayout llMain;

            VideoItem(View itemView) {
                super(itemView);
                llMain = itemView.findViewById(R.id.llMain);
                imgSelected = itemView.findViewById(R.id.imgSelected);
                txtQuality = itemView.findViewById(R.id.txtQuality);
                videoFoundSize = itemView.findViewById(R.id.videoFoundSize);
                name = itemView.findViewById(R.id.videoFoundName);

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (v == itemView) {
                    selectedVideo = getAdapterPosition();
                    notifyDataSetChanged();
                }
            }
        }
    }
}
