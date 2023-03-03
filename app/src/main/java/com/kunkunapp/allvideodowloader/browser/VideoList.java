package com.kunkunapp.allvideodowloader.browser;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.kunkunapp.allvideodowloader.MyApp;
import com.kunkunapp.allvideodowloader.activities.MainActivity;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;
import com.kunkunapp.allvideodowloader.R;
import com.kunkunapp.allvideodowloader.utils.PermissionInterceptor;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public abstract class VideoList {
    private static final String TAG = VideoList.class.getCanonicalName();
    public int selectedVideo = 0;
    private Activity activity;
    private RecyclerView view;
    private List<Video> videos;
    ImageView imgVideo;
    EditText txtTitle;
    TextView txtDownload;
    BottomSheetDialog bottomSheetDialog;

    public class Video {
        String size;
        String type;
        String link;
        String name;
        String page;
        String website;
        boolean chunked = false;
        boolean checked = false;
        boolean expanded = false;
        boolean audio;
    }

    abstract void onItemDeleted();

    abstract void onVideoPlayed(String url);

    VideoList(Activity activity, RecyclerView view, ImageView imgVideo, EditText txtTitle, TextView txtDownload, BottomSheetDialog bottomSheetDialog) {
        this.activity = activity;
        this.view = view;
        this.imgVideo = imgVideo;
        this.txtTitle = txtTitle;
        this.txtDownload = txtDownload;
        this.bottomSheetDialog = bottomSheetDialog;
        selectedVideo = 0;
        VideoListAdapter videoListAdapter = new VideoListAdapter();
        view.setAdapter(videoListAdapter);
        view.setLayoutManager(new GridLayoutManager(activity, 3));
        view.setHasFixedSize(true);
        videos = Collections.synchronizedList(new ArrayList<Video>());
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
                                startDownload(videoListAdapter);
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

    void recreateVideoList(RecyclerView view, ImageView imgVideo, EditText txtTitle, TextView txtDownload, BottomSheetDialog bottomSheetDialog) {
        this.view = view;
        this.imgVideo = imgVideo;
        this.txtTitle = txtTitle;
        this.txtDownload = txtDownload;
        this.bottomSheetDialog = bottomSheetDialog;
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
                                startDownload(videoListAdapter);
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

    void addItem(String size, String type, String link, String name, String page,
                 boolean chunked, String website, boolean audio) {
        Video video = new Video();
        video.size = size;
        video.type = type;
        video.link = link;
        video.name = name;
        video.page = page;
        video.chunked = chunked;
        video.website = website;
        video.audio = audio;
        videos.clear();
        if (!audio) {
            boolean duplicate = false;
            for (ListIterator<Video> iterator = videos.listIterator(); iterator.hasNext(); ) {
                Video v = iterator.next();
                if (v.link.equals(video.link)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                videos.add(video);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        view.getAdapter().notifyDataSetChanged();
                    }
                });
            }

        }
    }

    int getSize() {
        return videos.size();
    }

    void deleteAllItems() {
        /*for (int i = 0; i < videos.size(); ) {
            videos.remove(i);
        }*/
        videos.clear();
        ((VideoListAdapter) view.getAdapter()).expandedItem = -1;
        view.getAdapter().notifyDataSetChanged();
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
                if (videos.get(position).name != null && videos.get(position).name.length() > 0) {
                    txtTitle.setVisibility(View.VISIBLE);
                    txtTitle.setText(videos.get(position).name);
                } else {
                    txtTitle.setVisibility(View.INVISIBLE);
                }
                Glide.with(activity)
                        .load(videos.get(position).link)
                        .thumbnail(0.5f)
                        .into(imgVideo);
            }

            Video video = videos.get(position);
            if (video.size != null) {
                String sizeFormatted = Formatter.formatShortFileSize(activity,
                        Long.parseLong(video.size));
                holder.videoFoundSize.setText(sizeFormatted);
            } else holder.videoFoundSize.setText(" ");

            holder.name.setText(video.name);
            Log.d(TAG, "onBindViewHolder: link: " + video.name);
            if (video.name.toLowerCase().endsWith(".mp4")) {
                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(video.link);
                    int width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    int height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    retriever.release();
                    String resolution = width + "x" + height;
                    holder.txtQuality.setText(BrowserWindow.convertSolution(resolution));
                } catch (IllegalArgumentException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                holder.txtQuality.setText("unknown");
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
            if (videos.size() == 0) {
                imgVideo.setVisibility(View.GONE);
                txtTitle.setVisibility(View.GONE);
                bottomSheetDialog.dismiss();
            }
            if (videos.size() >= 5) {
                return 5;
            }
            return videos.size();
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
                /*if (v == download) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        new DownloadPermissionHandler(activity) {
                            @Override
                            public void onPermissionGranted() {
                                startDownload();
                            }
                        }.checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                PermissionRequestCodes.DOWNLOADS);
                    } else {
                        startDownload();
                    }
                }*/
            }


        }
    }

    public boolean indexExists(final List list, final int index) {
        return index >= 0 && index < list.size();
    }

    public void startDownload(VideoListAdapter videoListAdapter) {
        Log.d(TAG, "startDownload: ");
        if (indexExists(videos, selectedVideo)) {
            FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(activity)
                    .setDownloadConcurrentLimit(10000)
                    .build();

            Video video = videos.get(selectedVideo);
            Fetch fetch = Fetch.Impl.getInstance(fetchConfiguration);

            SharedPreferences prefs = activity.getSharedPreferences("settings", 0);
            String strDownloadLocation = prefs.getString("downloadLocation", "/storage/emulated/0/Download/Videodownloader");

            File dir = new File(strDownloadLocation);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String strName = txtTitle.getText().toString().trim().replaceAll("[^\\w ()'!\\[\\]\\-]", "");
            strName = strName.trim();
            if (strName.length() > 127) {//allowed filename length is 127
                strName = strName.substring(0, 127);
            } else if (strName.equals("")) {
                strName = "video";
            }

            final String filePath = dir.getAbsolutePath() + "/" + strName + "." + video.type;
            if (new File(filePath).exists()) {
                Toast.makeText(activity, "Please enter other name, file already exist.", Toast.LENGTH_SHORT).show();
                return;
            }
            Request request = new Request(video.link, filePath);
            request.setPriority(Priority.HIGH);
            request.setTag(video.page);
            boolean wifiOn = prefs.getBoolean(activity.getString(R.string.wifiON), false);
            if (wifiOn) {
                request.setNetworkType(NetworkType.WIFI_ONLY);
            } else {
                request.setNetworkType(NetworkType.ALL);
            }
            request.addHeader("clientKey", "SD78DF93_3947&MVNGHE1WONG");
            fetch.enqueue(request, request1 -> {
                LayoutInflater inflater = activity.getLayoutInflater();
                View layout = inflater.inflate(R.layout.toast_download,
                        (ViewGroup) activity.findViewById(R.id.toast_layout_root));
                Toast toast = new Toast(activity);
                toast.setGravity(Gravity.BOTTOM, 0, 250);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(layout);
                toast.show();

                ((MainActivity) activity).downloadCount = (((MainActivity) activity).downloadCount + 1);
                ((MainActivity) activity).badgeDownload.setNumber(((MainActivity) activity).downloadCount);

                bottomSheetDialog.dismiss();

                boolean isRatingDisplay = prefs.getBoolean("is_rating_display", false);
                if (!isRatingDisplay) {
                    ReviewManager reviewManager = ReviewManagerFactory.create(activity);
                    Task<ReviewInfo> requestRate = reviewManager.requestReviewFlow();
                    requestRate.addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            ReviewInfo reviewInfo = task.getResult();
                            Task<Void> flow = reviewManager.launchReviewFlow(activity, reviewInfo);
                            flow.addOnCompleteListener(task1 -> {
                                prefs.edit().putBoolean("is_rating_display", true);
                            });
                        }
                    });
                }
            }, error -> {
                Toast.makeText(activity, "Download Failed", Toast.LENGTH_LONG).show();
            });

            return;
        }

        /*if (indexExists(videos, selectedVideo)) {
            Video video = videos.get(selectedVideo);
            DownloadQueues queues = DownloadQueues.load(activity);
            queues.insertToTop(video.size, video.type, video.link, video.name, video
                    .page, video.chunked, video.website);
            queues.save(activity);
            DownloadVideo topVideo = queues.getTopVideo();
            Intent downloadService = MyApp.getInstance().getDownloadService();
            DownloadManager.stop();
            downloadService.putExtra("link", topVideo.link);
            downloadService.putExtra("name", topVideo.name);
            downloadService.putExtra("type", topVideo.type);
            downloadService.putExtra("size", topVideo.size);
            downloadService.putExtra("page", topVideo.page);
            downloadService.putExtra("chunked", topVideo.chunked);
            downloadService.putExtra("website", topVideo.website);
            MyApp.getInstance().startService(downloadService);
            videos.remove(selectedVideo);
            videoListAdapter.expandedItem = -1;
            selectedVideo = 0;
            videoListAdapter.notifyDataSetChanged();
            onItemDeleted();
            Toast.makeText(activity, "Download is started", Toast.LENGTH_LONG).show();
        }*/
    }

    private String getValidName(String name, String type) {
        name = name.replaceAll("[^\\w ()'!\\[\\]\\-]", "");
        name = name.trim();
        if (name.length() > 127) {//allowed filename length is 127
            name = name.substring(0, 127);
        } else if (name.equals("")) {
            name = "video";
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(MyApp.getInstance().getApplicationContext().getString(R.string.app_name)), name + "." + type);
        StringBuilder nameBuilder = new StringBuilder(name);
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String time = sdf1.format(timestamp);
        nameBuilder = new StringBuilder(name);
        nameBuilder.append(time);
        file = new File(Environment.getExternalStoragePublicDirectory(MyApp.getInstance().getApplicationContext().getString(R.string.app_name)), nameBuilder + "." + type);
        return nameBuilder.toString();
    }
}
