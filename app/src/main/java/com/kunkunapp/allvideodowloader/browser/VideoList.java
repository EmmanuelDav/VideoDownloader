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
import com.kunkunapp.allvideodowloader.viewModel.VidInfoViewModel;
import com.kunkunapp.allvideodowloader.R;
import com.kunkunapp.allvideodowloader.utils.PermissionInterceptor;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
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

    abstract  void onItemClicked(VideoInfo VideoFormat, VideoFormat videoFormat);

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
                                onItemClicked(videoInfo,videoFormat);
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
                                onItemClicked(videoInfo,videoFormat);
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
        int expandedItem = -1;

        @Override
        public VideoItem onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(activity);
            return (new VideoItem(inflater.inflate(R.layout.video_found_item_lay, parent,
                    false)));
        }

        @Override
        public void onBindViewHolder(VideoItem holder, int position) {
            if (position == 0) {
                imgVideo.setVisibility(View.VISIBLE);
                if (videoInfo.getTitle() != null && videoInfo.getTitle().length() > 0) {
                    txtTitle.setVisibility(View.VISIBLE);
                    txtTitle.setText(videoInfo.getTitle());
                } else {
                    txtTitle.setVisibility(View.INVISIBLE);
                }
                Glide.with(activity)
                        .load(videoInfo.getThumbnail())
                        .thumbnail(0.5f)
                        .into(imgVideo);
            }

            String sizeFormatted = Formatter.formatShortFileSize(activity, Long.parseLong(String.valueOf(videoInfo.getFormats().get(position).getFileSizeApproximate())));
            holder.videoFoundSize.setText(sizeFormatted);
            holder.name.setText(videoInfo.getFulltitle());
            videoFormat = videoInfo.getFormats().get(selectedVideo);



            try {
                holder.txtQuality.setText(BrowserWindow.convertSolution(videoInfo.getFormats().get(position).getFormatId()));
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

    public void startDownload(VidInfoViewModel model) {
        VideoInfo mVideoInfo = videoInfo;
        SharedPreferences prefs = activity.getSharedPreferences("settings", 0);
        String strDownloadLocation = prefs.getString("downloadLocation", "/storage/emulated/0/Download/Videodownloader");
        File dir = new File(strDownloadLocation);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        model.startDownload(mVideoInfo,videoFormat,strDownloadLocation,activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_download, (ViewGroup) activity.findViewById(R.id.toast_layout_root));
        Toast toast = new Toast(activity);
        toast.setGravity(Gravity.BOTTOM, 0, 250);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
        ((MainActivity) activity).downloadCount = (((MainActivity) activity).downloadCount + 1);
        ((MainActivity) activity).badgeDownload.setNumber(((MainActivity) activity).downloadCount);
        bottomSheetDialog.dismiss();
    }

}
