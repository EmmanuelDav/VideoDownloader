package com.kunkunapp.allvideodowloader.fragments;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.kunkunapp.allvideodowloader.database.Download;
import com.kunkunapp.allvideodowloader.helper.RenameVideoPref;
import com.kunkunapp.allvideodowloader.viewModel.DownloadsViewModel;

import com.kunkunapp.allvideodowloader.BuildConfig;
import com.kunkunapp.allvideodowloader.R;
import com.kunkunapp.allvideodowloader.activities.MainActivity;
import com.kunkunapp.allvideodowloader.helper.WebConnect;
import com.kunkunapp.allvideodowloader.utils.Utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class AllDownloadFragment extends Fragment {
    private static final String TAG = AllDownloadFragment.class.getCanonicalName();
    private View view;
    ImageView imgCast;
    RecyclerView downloadsList;
    DownloadAdapter downloadAdapter;
    DownloadsViewModel downloadsViewModel;

    private static final long UNKNOWN_REMAINING_TIME = -1;
    private static final long UNKNOWN_DOWNLOADED_BYTES_PER_SECOND = 0;
    public ArrayList<DownloadData> selectedList = new ArrayList<>();
    public boolean isSelectedMode = false;
    private LinearLayout llBottom;
    private RelativeLayout rlTopSelected;
    TextView txtSelectedCount;
    ImageView imgCancel;
    LinearLayout llDeleteSelected;
    LinearLayout llSelectAll;
    RenameVideoPref renameVideoPref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_all_download, container, false);
        downloadsViewModel = new ViewModelProvider(this).get(DownloadsViewModel.class);
        renameVideoPref = new RenameVideoPref(getActivity());
        downloadAdapter = new DownloadAdapter();
        downloadsList = view.findViewById(R.id.downloadsList);
        downloadsList.setLayoutManager(new LinearLayoutManager(getActivity()));
        downloadsList.setAdapter(downloadAdapter);

        llSelectAll = view.findViewById(R.id.llSelectAll);
        llDeleteSelected = view.findViewById(R.id.llDeleteSelected);
        imgCancel = view.findViewById(R.id.imgCancel);
        txtSelectedCount = view.findViewById(R.id.txtSelectedCount);
        rlTopSelected = view.findViewById(R.id.rlTopSelected);
        llBottom = view.findViewById(R.id.llBottom);
        imgCast = view.findViewById(R.id.imgCast);
        imgCast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enablingWiFiDisplay();
            }
        });
        imgCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unSelectAll();
            }
        });
        llDeleteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new Dialog(getActivity());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_confirmation);
                TextView txtTitle = dialog.findViewById(R.id.txtTitle);
                TextView txtDesc = dialog.findViewById(R.id.txtDesc);
                txtTitle.setText("Confirm");
                txtDesc.setText("Are you sure you want to delete this video?");
                TextView txtNO = dialog.findViewById(R.id.txtNO);
                TextView txtOK = dialog.findViewById(R.id.txtOK);
                txtNO.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                txtOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        for (DownloadData download : selectedList) {
                            DocumentFile file = DocumentFile.fromSingleUri(getActivity().getApplicationContext(), Uri.parse(download.download.downloadedPath));
                            if (file.exists()) {
                                file.delete();
                                //fetch.remove((int) download.download.getId());
                            }
                        }
                        unSelectAll();
                    }
                });
                dialog.show();
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        llSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedList.clear();
                selectedList.addAll(downloadAdapter.downloads);
                txtSelectedCount.setText(selectedList.size() + " selected");
                downloadAdapter.notifyDataSetChanged();
            }
        });
        return view;
    }

    public void unSelectAll() {
        txtSelectedCount.setText("0 selected");
        isSelectedMode = false;
        selectedList.clear();
        downloadAdapter.notifyDataSetChanged();

        ((MainActivity) getActivity()).navView.setVisibility(View.VISIBLE);
        llBottom.setVisibility(View.GONE);
        rlTopSelected.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        downloadsViewModel.getAllDownloads().observe(getViewLifecycleOwner(), downloads -> {
            final ArrayList<Download> list = new ArrayList<>(downloads);
            for (Download download : list) {
                DocumentFile file = DocumentFile.fromSingleUri(getActivity().getApplicationContext(), Uri.parse(download.downloadedPath));
                String strRename = renameVideoPref.getString(String.valueOf(download.getId()), "");
                if (strRename.length() > 0) {
                    File desFile = new File(strRename);
                    if (desFile.exists()) {
                        downloadAdapter.addDownload(download);
                    }
                } else if (file.exists()) {
                    downloadAdapter.addDownload(download);
                }
            }
        });
    }


    private class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {
        private List<DownloadData> downloads = new ArrayList<>();
        boolean downloaded = false;

        public void addDownload(@NonNull final Download download) {
            boolean found = false;
            DownloadData data = null;
            int dataPosition = -1;
            for (int i = 0; i < downloads.size(); i++) {
                final DownloadData downloadData = downloads.get(i);
                if (downloadData.id == download.getId()) {
                    data = downloadData;
                    dataPosition = i;
                    found = true;
                    break;
                }
            }
            if (!found) {
                final DownloadData downloadData = new DownloadData();
                downloadData.id = (int) download.getId();
                downloadData.download = download;
                downloads.add(downloadData);
                notifyItemInserted(downloads.size() - 1);
            } else {
                data.download = download;
                notifyItemChanged(dataPosition);
            }
        }

        public void update(@NonNull final Download download, long eta, long downloadedBytesPerSecond) {
            for (int position = 0; position < downloads.size(); position++) {
                final DownloadData downloadData = downloads.get(position);
                if (downloadData.id == download.getId()) {
//                    switch (download.getStatus()) {
//                        case REMOVED:
//                        case DELETED: {
//                            downloads.remove(position);
//                            notifyItemRemoved(position);
//                            break;
//                        }
//                        default: {
//
//                        }
//                    }

                    downloadData.download = download;
                    downloadData.eta = eta;
                    downloadData.downloadedBytesPerSecond = downloadedBytesPerSecond;
                    notifyItemChanged(position);
                    return;
                }
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DownloadData downloadData = downloads.get(position);
            DocumentFile documentFile =  DocumentFile.fromSingleUri(getContext(), Uri.parse(downloadData.download.downloadedPath));
            File tempFile = new File(downloadData.download.getDownloadedPath());
            File file = tempFile;
//            downloadsViewModel.getLoadState().observe(getViewLifecycleOwner(), downloadState -> {
//                switch (downloadState) {
//                    case INIT:
//                        downloaded = true;
//                        holder.imgCancel.setVisibility(View.VISIBLE);
//                        holder.imgPause.setVisibility(View.VISIBLE);
//                        holder.imgResume.setVisibility(View.GONE);
//
//                        holder.imgCancel.setVisibility(View.GONE);
//                        holder.imgPause.setVisibility(View.GONE);
//                        holder.imgResume.setVisibility(View.GONE);
//                        holder.downloadProgressBar.setVisibility(View.GONE);
//                        holder.txtDuration.setVisibility(View.VISIBLE);
//                        holder.imgMore.setVisibility(View.VISIBLE);
//
//                        String dateString = new SimpleDateFormat("MMMM dd yyyy").format(new Date(downloadData.download.getTimestamp()));
//                        String strDescComplete = Utils.Companion.getStringSizeLengthFile(downloadData.download.getTotalSize()) + "  " + dateString;
//                        holder.downloadProgressText.setText(strDescComplete);
//
//                        if (documentFile.exists()) {
//                            String duration = null;
//                            try {
//                                duration = Utils.Companion.convertSecondsToHMmSs(getFileDuration(downloadData.download.downloadedPath,getContext()));
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                            if (duration != null)
//                                holder.txtDuration.setText(duration);
//                        }
//                        break;
//                    case CANCELED:
//                        String strDesc2 = "Cancelled " + downloadData.download.getDownloadedPercent() + "% " + Utils.Companion.getStringSizeLengthFile(downloadData.download.getDownloadedSize()) + "/" + Utils.Companion.getStringSizeLengthFile(downloadData.download.getTotalSize());
//                        holder.downloadProgressText.setText(strDesc2);
//                        holder.imgCancel.setVisibility(View.GONE);
//                        holder.imgPause.setVisibility(View.GONE);
//                        holder.imgResume.setVisibility(View.VISIBLE);
//                        break;
//
//                    case COMPLETED:
////                        holder.imgCancel.setVisibility(View.GONE);
////                        holder.imgPause.setVisibility(View.GONE);
////                        holder.imgResume.setVisibility(View.GONE);
////                        holder.downloadProgressBar.setVisibility(View.GONE);
////                        holder.txtDuration.setVisibility(View.VISIBLE);
////                        holder.imgMore.setVisibility(View.VISIBLE);
////
////                        String dateString = new SimpleDateFormat("MMMM dd yyyy").format(new Date(downloadData.download.getTimestamp()));
////                        String strDescComplete = Utils.Companion.getStringSizeLengthFile(downloadData.download.getTotalSize()) + "  " + dateString;
////                        holder.downloadProgressText.setText(strDescComplete);
////
////                        if (file.exists()) {
////                            String duration = null;
////                            try {
////                                duration = getFileDuration(file);
////                            } catch (IOException e) {
////                                e.printStackTrace();
////                            }
////                            if (duration != null)
////                                holder.txtDuration.setText(duration);
////                        }
//
//                        break;
//                    case DOWNLOADING:
//
//                    case FAILED:
//
//                        holder.imgCancel.setVisibility(View.VISIBLE);
//                        holder.imgPause.setVisibility(View.GONE);
//                        holder.imgResume.setVisibility(View.VISIBLE);
//                        String strDescFailed = "Failed " + downloadData.download.getDownloadedPercent() + "% " + Utils.Companion.getStringSizeLengthFile(downloadData.download.getDownloadedSize()) + "/" + Utils.Companion.getStringSizeLengthFile(downloadData.download.getTotalSize());
//                        holder.downloadProgressText.setText(strDescFailed);
//                        break;
//
//                }
//
//            });
            Glide.with(getActivity())
                    .load(downloadData.download.getDownloadedPath())
                    .into(holder.imgVideo);

            int progress = (int) downloadData.download.getDownloadedPercent();
            if (progress == -1) { // Download progress is undermined at the moment.
                progress = 0;
            }

            String strRename = renameVideoPref.getString(String.valueOf(downloadData.download.getId()), "");
            if (strRename.length() > 0) {
                File desFile = new File(strRename);
                if (desFile.exists()) {
                    tempFile = desFile;
                }
            }
            holder.downloadVideoName.setText(downloadData.download.getName());
            String strDesc = progress + "% " + Utils.Companion.getStringSizeLengthFile(downloadData.download.getDownloadedSize()) + "/" + Utils.Companion.getStringSizeLengthFile(downloadData.download.getTotalSize());
            holder.downloadProgressText.setText(strDesc);
            holder.downloadProgressBar.setProgress(progress);
            holder.edtSearch.setText(downloadData.download.getName());
            holder.imgVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isSelectedMode) {
                        boolean isContain = false;
                        for (DownloadData data : selectedList) {
                            if (data.download.getId() == downloadData.download.getId()) {
                                isContain = true;
                            }
                        }

                        if (isContain) {
                            selectedList.remove(downloadData);
                        } else {
                            selectedList.add(downloadData);
                        }
                        txtSelectedCount.setText(selectedList.size() + " selected");
                        notifyDataSetChanged();
                        return;
                    }
                    if (downloaded) {
                        MediaScannerConnection.scanFile(getActivity(),
                                new String[]{documentFile.toString()}, null,
                                (path, uri) -> downloadsViewModel.viewContent(downloadData.download.downloadedPath, getContext()));
                    }
                }
            });
            holder.imgVideo.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!isSelectedMode) {
                        selectedList.add(downloadData);
                        isSelectedMode = true;
                        txtSelectedCount.setText(selectedList.size() + " selected");
                        downloadAdapter.notifyDataSetChanged();

                        ((MainActivity) getActivity()).navView.setVisibility(View.GONE);
                        llBottom.setVisibility(View.VISIBLE);
                        rlTopSelected.setVisibility(View.VISIBLE);
                    }
                    return false;
                }
            });
            holder.llContent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!isSelectedMode) {
                        selectedList.add(downloadData);
                        isSelectedMode = true;
                        txtSelectedCount.setText(selectedList.size() + " selected");
                        downloadAdapter.notifyDataSetChanged();

                        ((MainActivity) getActivity()).navView.setVisibility(View.GONE);
                        llBottom.setVisibility(View.VISIBLE);
                        rlTopSelected.setVisibility(View.VISIBLE);
                    }
                    return false;
                }
            });
            holder.imgSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isContain = false;
                    for (DownloadData data : selectedList) {
                        if (data.download.getId() == downloadData.download.getId()) {
                            isContain = true;
                        }
                    }

                    if (isContain) {
                        selectedList.remove(downloadData);
                    } else {
                        selectedList.add(downloadData);
                    }
                    txtSelectedCount.setText(selectedList.size() + " selected");
                    notifyDataSetChanged();
                }
            });
            holder.llContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isSelectedMode) {
                        boolean isContain = false;
                        for (DownloadData data : selectedList) {
                            if (data.download.getId() == downloadData.download.getId()) {
                                isContain = true;
                            }
                        }

                        if (isContain) {
                            selectedList.remove(downloadData);
                        } else {
                            selectedList.add(downloadData);
                        }
                        txtSelectedCount.setText(selectedList.size() + " selected");
                        notifyDataSetChanged();
                        return;
                    }
                    if (downloaded) {
                        MediaScannerConnection.scanFile(getActivity(),
                                new String[]{documentFile.toString()}, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        downloadsViewModel.viewContent(downloadData.download.downloadedPath, getContext());
                                    }
                                });
                    }
                }
            });
            holder.imgCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //  fetch.remove(downloadData.download.getId());
                }
            });
            holder.imgResume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    if (status == Status.FAILED || status == Status.CANCELLED) {
//                        //fetch.retry(downloadData.download.getId());
//                    } else {
//                      //  fetch.resume(downloadData.download.getId());
//                    }
                }
            });
            holder.imgPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //fetch.pause((int) downloadData.download.getId());
                }
            });
            holder.imgMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(getActivity(), holder.imgMore);
                    popup.getMenuInflater().inflate(R.menu.menu_download, popup.getMenu());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menu_share:
                                    shareFile(downloadData.download.downloadedPath);
                                    break;
                                case R.id.menu_rename:
                                     renameFile(downloadData, documentFile);
                                    break;
                                case R.id.menu_edit_video:

                                    Uri videoURI = FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".fileprovider", file);
                                    Intent intent = new Intent(Intent.ACTION_EDIT);
                                    //intent.setDataAndType(videoURI, "video/*");
                                    intent.setDataAndType(videoURI, getMimeType(videoURI));
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP);
                                    try {
                                        Intent choose = Intent.createChooser(intent, "Edit with");
                                        startActivityForResult(choose, 1005);
                                    } catch (ActivityNotFoundException e) {
                                        try {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.kunkun.videoeditor.videomaker")));
                                        } catch (ActivityNotFoundException exception) {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.kunkun.videoeditor.videomaker")));
                                        }
                                    } catch (Exception e) {
                                        try {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.kunkun.videoeditor.videomaker")));
                                        } catch (ActivityNotFoundException exception) {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.kunkun.videoeditor.videomaker")));
                                        }
                                    }
                                    break;
                                case R.id.menu_open_link:
                                    getActivity().onBackPressed();
                                    ((MainActivity) getActivity()).isEnableSuggetion = false;
                                    ((MainActivity) getActivity()).navView.setSelectedItemId(R.id.navHome);
                                    new WebConnect(holder.edtSearch, ((MainActivity) getActivity())).connect();
                                    break;
                                case R.id.menu_delete:
                                    Dialog dialog = new Dialog(getActivity());
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                    dialog.setContentView(R.layout.dialog_confirmation);

                                    TextView txtTitle = dialog.findViewById(R.id.txtTitle);
                                    TextView txtDesc = dialog.findViewById(R.id.txtDesc);
                                    txtTitle.setText("Confirm");
                                    txtDesc.setText("Are you sure you want to delete this video?");
                                    TextView txtNO = dialog.findViewById(R.id.txtNO);
                                    TextView txtOK = dialog.findViewById(R.id.txtOK);
                                    txtNO.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            dialog.dismiss();
                                        }
                                    });
                                    txtOK.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                           downloadsViewModel.startDelete(downloads.get(position).id,dialog.getContext());
                                           dialog.dismiss();
                                           onResume();
                                        }
                                    });
                                    dialog.show();
                                    dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                    break;
                            }
                            return true;
                        }
                    });
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        popup.setForceShowIcon(true);
                    }
                    popup.show();
                }
            });
//            holder.imgCancel.setVisibility(View.GONE);
//            holder.imgPause.setVisibility(View.GONE);
//            holder.imgResume.setVisibility(View.GONE);
              holder.txtDuration.setVisibility(View.GONE);
//            holder.imgMore.setVisibility(View.GONE);
           holder.imgSelect.setVisibility(View.GONE);
//            holder.downloadProgressBar.setVisibility(View.VISIBLE);

//            switch (status) {
//                case CANCELLED: {

//                }
//                case COMPLETED: {

//                }
//                case FAILED: {

//                }
//                case PAUSED: {
//                    String strDesc2 = "Paused " + progress + "% " + Utils.Companion.getStringSizeLengthFile(downloadData.download.getDownloadedSize()) + "/" + Utils.Companion.getStringSizeLengthFile(downloadData.download.getTotalSize());
//                    holder.downloadProgressText.setText(strDesc2);
//                    holder.imgCancel.setVisibility(View.VISIBLE);
//                    holder.imgPause.setVisibility(View.GONE);
//                    holder.imgResume.setVisibility(View.VISIBLE);
//                    break;
//                }
//                case DOWNLOADING:
//                case QUEUED: {
//                    holder.imgCancel.setVisibility(View.VISIBLE);
//                    holder.imgPause.setVisibility(View.VISIBLE);
//                    holder.imgResume.setVisibility(View.GONE);
//                    break;
//                }
//                case ADDED: {
//
//                    break;
//                }
//                default: {
//                    break;
//                }
//            }

            if (isSelectedMode) {
                holder.imgCancel.setVisibility(View.GONE);
                holder.imgPause.setVisibility(View.GONE);
                holder.imgResume.setVisibility(View.GONE);
                holder.imgMore.setVisibility(View.GONE);

                holder.imgSelect.setVisibility(View.VISIBLE);

                boolean isContain = false;
                for (DownloadData data : selectedList) {
                    if (data.download.getId() == downloadData.download.getId()) {
                        isContain = true;
                    }
                }
                if (isContain) {
                    Glide.with(getActivity())
                            .load(R.drawable.ic_box_selected)
                            .into(holder.imgSelect);
                } else {
                    Glide.with(getActivity())
                            .load(R.drawable.ic_box_unselect)
                            .into(holder.imgSelect);
                }
            }
        }

        @Override
        public int getItemCount() {
            return downloads.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView downloadVideoName;
            ProgressBar downloadProgressBar;
            ImageView imgVideo;
            TextView downloadProgressText;
            ImageView imgCancel;
            ImageView imgPause;
            ImageView imgResume;
            ImageView imgMore;
            TextView txtDuration;
            EditText edtSearch;
            LinearLayout llContent;
            ImageView imgSelect;


            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                imgSelect = itemView.findViewById(R.id.imgSelect);
                llContent = itemView.findViewById(R.id.llContent);
                edtSearch = itemView.findViewById(R.id.edtSearch);
                txtDuration = itemView.findViewById(R.id.txtDuration);
                imgMore = itemView.findViewById(R.id.imgMore);
                imgResume = itemView.findViewById(R.id.imgResume);
                imgPause = itemView.findViewById(R.id.imgPause);
                imgCancel = itemView.findViewById(R.id.imgCancel);
                downloadProgressText = itemView.findViewById(R.id.downloadProgressText);
                imgVideo = itemView.findViewById(R.id.imgVideo);
                downloadVideoName = itemView.findViewById(R.id.downloadVideoName);
                downloadProgressBar = itemView.findViewById(R.id.downloadProgressBar);
            }
        }
    }

    public void renameFile(DownloadData downloadData, DocumentFile file) {
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_rename);
        EditText edtName = dialog.findViewById(R.id.edtName);
        edtName.setText(downloadData.download.getName());
        TextView txtNO = dialog.findViewById(R.id.txtNO);
        TextView txtOK = dialog.findViewById(R.id.txtOK);
        txtNO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        txtOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strName = edtName.getText().toString().trim().replaceAll("[^\\w ()'!\\[\\]\\-]", "");
                if (strName.isEmpty() || strName.length() == 0) {
                    Toast.makeText(getActivity(), "Please enter video name", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    OutputStream os = getContext().getContentResolver().openOutputStream(file.getUri());
                    os.write(edtName.getText().toString().getBytes());
                    os.close();
                }catch (Exception exception){

                }
                onResume();
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public static class DownloadData {
        public int id;
        @Nullable
        public Download download;
        long eta = -1;
        long downloadedBytesPerSecond = 0;

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            if (download == null) {
                return "";
            }
            return download.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof DownloadData && ((DownloadData) obj).id == id;
        }
    }

    public void enablingWiFiDisplay() {
        try {
            startActivity(new Intent("android.settings.WIFI_DISPLAY_SETTINGS"));
            return;
        } catch (ActivityNotFoundException activitynotfoundexception) {
            activitynotfoundexception.printStackTrace();
        }

        try {
            startActivity(new Intent("android.settings.CAST_SETTINGS"));
            return;
        } catch (Exception exception1) {
            Toast.makeText(getActivity(), "Device not supported", Toast.LENGTH_LONG).show();
        }
    }

    private void shareFile(final String file) {
      DocumentFile fileUri = DocumentFile.fromSingleUri(getContext(), Uri.parse(file));
        StringBuilder msg = new StringBuilder();
        msg.append(getResources().getString(R.string.share_message));
        msg.append("\n");
        msg.append("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);

        if (fileUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_TEXT, msg.toString());
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(file));
            try {
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), "No App Available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    long getFileDuration(String file, Context context) throws IOException {
        long result = 0;
        MediaMetadataRetriever retriever = null;
        FileInputStream inputStream = null;

        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, Uri.parse(file));
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            result = Long.parseLong(time );
            retriever.release();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (retriever != null) {
                retriever.release();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public String getMimeType(Uri uri) {
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = getActivity().getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }
}