package com.kunkunapp.allvideodowloader.browser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.kunkunapp.allvideodowloader.MyApp;
import com.kunkunapp.allvideodowloader.R;
import com.kunkunapp.allvideodowloader.activities.MainActivity;
import com.kunkunapp.allvideodowloader.fragments.DownloadPathDialogFragment;
import com.kunkunapp.allvideodowloader.fragments.base.BaseFragment;
import com.kunkunapp.allvideodowloader.utils.HistorySQLite;
import com.kunkunapp.allvideodowloader.utils.PermissionInterceptor;
import com.kunkunapp.allvideodowloader.utils.Utils;
import com.kunkunapp.allvideodowloader.utils.VisitedPage;
import com.kunkunapp.allvideodowloader.viewModel.VidInfoViewModel;
import com.kunkunapp.allvideodowloader.views.CustomMediaController;
import com.kunkunapp.allvideodowloader.views.CustomVideoView;
import com.kunkunapp.allvideodowloader.model.VidInfoItem;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class BrowserWindow extends BaseFragment implements View.OnClickListener, MainActivity.OnBackPressedListener, DownloadPathDialogFragment.DialogListener {

    private static final String TAG = BrowserWindow.class.getCanonicalName();
    private String url;
    private View view;
    private TouchableWebView page;
    private SSLSocketFactory defaultSSLSF;

    private FrameLayout videoFoundTV;
    private CustomVideoView videoFoundView;
    private ImageView videosFoundHUD;
    private ImageView imgDetacting;

    private View foundVideosWindow;
    private VideoList videoList;
    private ImageView foundVideosClose;

    private ProgressBar loadingPageProgress;

    private int orientation;
    private boolean loadedFirsTime;

    private List<String> blockedWebsites;
    private BottomSheetDialog dialog;

    private Activity activity;
    private InterstitialAd mInterstitialAd;
    private Context context;
    private boolean isVisible = false;

    private VidInfoViewModel viewModel;
    VideoInfo mVideoInfo;

    private static int OPEN_DIRECTORY_REQUEST_CODE = 42069;


    public BrowserWindow(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onClick(View v) {
        if (v == videosFoundHUD) {
            if (videoList != null) {
                XXPermissions.with(activity)
                        .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                        .interceptor(new PermissionInterceptor())
                        .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
                                if (!all) {
                                    return;
                                }
                                dialog.show();
                            }

                            @Override
                            public void onDenied(List<String> permissions, boolean never) {
                                OnPermissionCallback.super.onDenied(permissions, never);
                                Log.d(TAG, "onDenied: =====");
                            }
                        });
            } else {
                showGuide();
            }
        } else if (v == foundVideosClose) {
            dialog.dismiss();
        }
    }

    private void showGuide() {
        final Dialog guide = new Dialog(getContext());
        guide.setContentView(R.layout.dialog_guide_download);

        TextView txtGotIt = guide.findViewById(R.id.txtGotIt);
        txtGotIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                guide.dismiss();
            }
        });

        guide.show();
        guide.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        guide.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = getArguments();
        url = data.getString("url");
        defaultSSLSF = HttpsURLConnection.getDefaultSSLSocketFactory();
        blockedWebsites = Arrays.asList(getResources().getStringArray(R.array.blocked_sites));
        setRetainInstance(true);
        context = getContext();
    }

    private void createVideosFoundTV() {
        videoFoundTV = view.findViewById(R.id.videoFoundTV);
        videoFoundView = view.findViewById(R.id.videoFoundView);
        CustomMediaController mediaFoundController = view.findViewById(R.id.mediaFoundController);
        mediaFoundController.setFullscreenEnabled();
        videoFoundView.setMediaController(mediaFoundController);
        videoFoundTV.setVisibility(View.GONE);
    }

    private void createVideosFoundHUD() {
        videosFoundHUD = view.findViewById(R.id.videosFoundHUD);
        videosFoundHUD.setOnClickListener(this);
    }

    private void createFoundVideosWindow() {
        dialog = new BottomSheetDialog(activity, R.style.CustomBottomSheetDialogTheme);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.bottom_download_options);
        RecyclerView qualities = dialog.findViewById(R.id.qualities_rv);
        ImageView imgVideo = dialog.findViewById(R.id.imgVideo);
        EditText txtTitle = dialog.findViewById(R.id.txtTitle);
        TextView txtDownload = dialog.findViewById(R.id.txtDownload);
        ImageView dismiss = dialog.findViewById(R.id.dismiss);
        assert dismiss != null;
        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        qualities.setLayoutManager(new GridLayoutManager(activity, 3));
        qualities.setHasFixedSize(true);
        foundVideosWindow = view.findViewById(R.id.foundVideosWindow);
        viewModel.getVidFormats().observe(getViewLifecycleOwner(), videoInfo -> {

            if (videoInfo == null || videoInfo.getFormats() == null)  {
                return;
            }

            videoInfo.getFormats().removeIf(it -> !it.getExt().contains("mp4") || it.getFormat().contains("audio"));
            Set<String> namesAlreadySeen = new HashSet<>();
            videoInfo.getFormats().removeIf(p -> !namesAlreadySeen.add(convertSolution(p.getFormat())));

            mVideoInfo = videoInfo;
            if (videoList != null) {
                videoList.recreateVideoList(qualities, imgVideo, txtTitle, txtDownload, dialog, videoInfo);
            } else {
                videoList = new VideoList(activity, qualities, imgVideo, txtTitle, txtDownload, dialog, videoInfo) {
                    @Override
                    public void onItemClicked(VidInfoItem.VidFormatItem vidFormatItem) {
                        viewModel.selectedItem = vidFormatItem;
                        new DownloadPathDialogFragment().show(getChildFragmentManager(),
                                "download_location_chooser_dialog"
                        );
                    }
                };
            }
            updateFoundVideosBar();
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        if (view == null || getResources().getConfiguration().orientation != orientation) {
            int visibility = View.VISIBLE;
            if (view != null) {
                visibility = view.getVisibility();
            }
            view = inflater.inflate(R.layout.browser_lay, container, false);
            viewModel = new ViewModelProvider(this).get(VidInfoViewModel.class);

            view.setVisibility(visibility);
            if (page == null) {
                page = view.findViewById(R.id.page);
            } else {
                View page1 = view.findViewById(R.id.page);
                ((ViewGroup) view).removeView(page1);
                ((ViewGroup) page.getParent()).removeView(page);
                ((ViewGroup) view).addView(page);
                ((ViewGroup) view).bringChildToFront(view.findViewById(R.id.videosFoundHUD));
                ((ViewGroup) view).bringChildToFront(view.findViewById(R.id.foundVideosWindow));
            }
            loadingPageProgress = view.findViewById(R.id.loadingPageProgress);
            loadingPageProgress.setVisibility(View.GONE);
            imgDetacting = view.findViewById(R.id.imgDetacting);

            ObjectAnimator rotate = new ObjectAnimator().ofFloat(imgDetacting, "rotation", 0f, 360f);
            rotate.setDuration(1000);
            rotate.setRepeatCount(999999999);
            rotate.start();

            createVideosFoundHUD();
            createVideosFoundTV();
            createFoundVideosWindow();
            webViewLightDark();
        }

        return view;
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        if (!loadedFirsTime) {
            page.getSettings().setJavaScriptEnabled(true);
            page.getSettings().setDomStorageEnabled(true);
            page.getSettings().setAllowUniversalAccessFromFileURLs(true);
            page.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            page.setWebViewClient(new WebViewClient() {//it seems not setting webclient, launches
                //default browser instead of opening the page in webview
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    if (blockedWebsites.contains(Utils.Companion.getBaseDomain(request.getUrl().toString()))) {
                        Log.d("vdd", "URL : " + request.getUrl().toString());
                        Dialog dialog = new Dialog(getActivity());
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        dialog.setContentView(R.layout.dialog_youtube_not_supported);
                        TextView txtGotIt = dialog.findViewById(R.id.txtGotIt);
                        txtGotIt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        return true;
                    }
                    return super.shouldOverrideUrlLoading(view, request);
                }

                @Override
                public void onPageStarted(final WebView webview, final String url, Bitmap favicon) {
                    //  videoList.deleteAllItems();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            EditText urlBox = getBaseActivity().findViewById(R.id.inputURLText);
                            getBaseActivity().isEnableSuggetion = false;
                            urlBox.setText(url);
                            BrowserWindow.this.url = url;
                            viewModel.fetchInfo(url);
                        }
                    });
                    view.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
                    loadingPageProgress.setVisibility(View.VISIBLE);
                    super.onPageStarted(webview, url, favicon);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    loadingPageProgress.setVisibility(View.GONE);
                }

                @Override
                public void onLoadResource(final WebView view, final String url) {
                    Log.d("fb :", "URL: " + url);
                    final String viewUrl = view.getUrl();
                    final String title = view.getTitle();

                    new VideoContentSearch(activity, url, viewUrl, title) {
                        @Override
                        public void onStartInspectingURL() {
                            Utils.Companion.disableSSLCertificateChecking();
                        }

                        @Override
                        public void onFinishedInspectingURL(boolean finishedAll) {
                            HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSLSF);
                        }

                        @Override
                        public void onVideoFound(String size, String type, String link, String name, String page, boolean chunked, String website, boolean audio) {
                            if (size != null && isNumber(size)) {
                                long numericSize = Long.parseLong(size);
                                if (numericSize > 700000) {
                                    if (link.contains("mp4")) {
                                        SortedSet<String> hashMap = new TreeSet<>();
                                        hashMap.add(link);
                                        viewModel.fetchInfo(hashMap.last());
                                    }
                                    Log.d(TAG, "onVideoFound: link > "+link+"  website >"+website +" page >"+page +" size >"+size + " name >"+name);

                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            imgDetacting.setVisibility(View.VISIBLE);

                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    imgDetacting.setVisibility(View.GONE);
                                                    updateFoundVideosBar();
                                                }
                                            }, 1000);
                                        }
                                    });
                                }
                            }
                        }
                    }.start();
                }

                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                    if (activity != null) {
                        Log.d("VDDebug", "Url: " + url);
                        if (activity.getSharedPreferences("settings", 0).getBoolean(getString(R
                                .string.adBlockON), true)
                                && (url.contains("ad") || url.contains("banner") || url.contains("pop")) || url.contains("banners")
                                && getBaseActivity().getBrowserManager().checkUrlIfAds(url)) {
                            Log.d("VDDebug", "Ads detected: " + url);
                            return new WebResourceResponse(null, null, null);
                        }
                    }
                    return super.shouldInterceptRequest(view, url);
                }

                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getBaseActivity() !=
                            null) {
                        if (MyApp.getInstance().getSharedPreferences("settings", 0).getBoolean(getString
                                (R.string.adBlockON), true)
                                && (request.getUrl().toString().contains("ad") ||
                                request.getUrl().toString().contains("banner") ||
                                request.getUrl().toString().contains("banners") ||
                                request.getUrl().toString().contains("pop"))
                                && getBaseActivity().getBrowserManager().checkUrlIfAds(request.getUrl()
                                .toString())) {
                            Log.i("VDInfo", "Ads detected: " + request.getUrl().toString());
                            return new WebResourceResponse(null, null, null);
                        } else return null;
                    } else {
                        return shouldInterceptRequest(view, request.getUrl().toString());
                    }
                }


            });
            page.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    loadingPageProgress.setProgress(newProgress);
                }

                @Override
                public void onReceivedIcon(WebView view, Bitmap icon) {
                    super.onReceivedIcon(view, icon);
                }

                @Override
                public void onReceivedTitle(WebView view, String title) {
                    super.onReceivedTitle(view, title);
                    //  videoList.deleteAllItems();
                    updateFoundVideosBar();

                    VisitedPage vp = new VisitedPage();
                    vp.title = title;
                    vp.link = view.getUrl();
                    HistorySQLite db = new HistorySQLite(activity);
                    db.addPageToHistory(vp);
                    db.close();
                }

                @Override
                public Bitmap getDefaultVideoPoster() {
                    return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
                }


            });

            page.loadUrl(url);
            loadedFirsTime = true;
        } else {
            EditText urlBox = getBaseActivity().findViewById(R.id.inputURLText);
            getBaseActivity().isEnableSuggetion = false;
            urlBox.setText(url);
        }
    }

    boolean isNumber(String string) {
        try {
            int amount = Integer.parseInt(string);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void webViewLightDark() {

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_YES:
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(page.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
                }
                break;

            case Configuration.UI_MODE_NIGHT_NO:
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(page.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
                }
                break;

            default:
                //

        }

    }

    @Override
    public void onDestroy() {
        page.stopLoading();
        page.destroy();
        super.onDestroy();
    }

    private void updateFoundVideosBar() {
        if (mVideoInfo != null && mVideoInfo.getFormats().size() > 0) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RequestOptions options = new RequestOptions()
                            .skipMemoryCache(true)
                            .centerInside()
                            .placeholder(R.drawable.ic_download_det)
                            .transform(new CircleCrop());

                    Glide.with(activity)
                            .load(R.drawable.ic_download_det)
                            .apply(options)
                            .into(videosFoundHUD);

                    ObjectAnimator animY = ObjectAnimator.ofFloat(videosFoundHUD, "translationY", -100f, 0f);
                    animY.setDuration(1000);
                    animY.setInterpolator(new BounceInterpolator());
                    animY.setRepeatCount(2);
                    animY.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation, boolean isReverse) {
                            super.onAnimationEnd(animation, isReverse);
                            Glide.with(activity)
                                    .load(R.drawable.ic_download_enable)
                                    .into(videosFoundHUD);
                        }
                    });
                    animY.start();
                }
            });

        } else {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Glide.with(activity)
                            .load(R.drawable.ic_download_dis)
                            .into(videosFoundHUD);
                    if (foundVideosWindow.getVisibility() == View.VISIBLE)
                        foundVideosWindow.setVisibility(View.GONE);
                }
            });
        }
    }

    private void updateVideoPlayer(String url) {
        videoFoundTV.setVisibility(View.VISIBLE);
        Uri uri = Uri.parse(url);
        videoFoundView.setVideoURI(uri);
        videoFoundView.start();
    }

    @Override
    public void onBackpressed() {
        if (foundVideosWindow.getVisibility() == View.VISIBLE && !videoFoundView.isPlaying() && videoFoundTV.getVisibility() == View.GONE) {
            foundVideosWindow.setVisibility(View.GONE);
        } else if (videoFoundView.isPlaying() || videoFoundTV.getVisibility() == View.VISIBLE) {
            videoFoundView.closePlayer();
            videoFoundTV.setVisibility(View.GONE);
        } else if (page.canGoBack()) {
            page.goBack();
        } else {
            getBaseActivity().getBrowserManager().closeWindow(BrowserWindow.this);
        }
    }


    public WebView getWebView() {
        return this.page;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (page != null) page.onPause();
        Log.d("debug", "onPause: ");
    }

    public void onResumeForce() {
        page.onResume();
        Log.d("debug", "onResume: ");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getBaseActivity().navView.getSelectedItemId() == R.id.navHome && !getBaseActivity().isDisableOnResume) {
            if (page != null)
                page.onResume();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getBaseActivity().isDisableOnResume = false;
            }
        }, 500);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;
    }

    public static String convertSolution(String str) {
        String str2;
        String[] split = str.split("[^\\d]+");
        if (split.length >= 2) {
            str2 = split[1];
        } else {
            if (split.length == 1) {
                str2 = split[0];
            } else if (!str.matches(".*\\d.*")) {
                return str;
            } else {
                return "720P";
            }
        }
        try {
            long parseLong = Long.parseLong(str2);
            if (parseLong < 240) {
                return "144P";
            }
            if (parseLong < 360) {
                return "240P";
            }
            if (parseLong < 480) {
                return "360P";
            }
            if (parseLong < 720) {
                return "480P";
            }
            if (parseLong < 1080) {
                return "720P";
            }
            if (parseLong < 1440) {
                return "1080P";
            }
            if (parseLong < 2160) {
                return "1440P";
            }
            return parseLong < 4320 ? "4K" : "8K";
        } catch (NumberFormatException unused) {
        }
        return str2;
    }

    @Override
    public void onOk(@NonNull DownloadPathDialogFragment dialog) {
        String path = PreferenceManager.getDefaultSharedPreferences(context).getString(getString(R.string.download_location_key), null);
        if (path == null) {
            Toast.makeText(context, R.string.invalid_download_location, Toast.LENGTH_SHORT).show();
        }
        removeDialog();
        viewModel.startDownload(viewModel.selectedItem, path, activity);
    }

    @Override
    public void onFilePicker(@NonNull DownloadPathDialogFragment dialog) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                activity.getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
                setDefaultDownloadLocation(uri.toString());
                removeDialog();
                viewModel.startDownload(viewModel.selectedItem, uri.toString(), activity);
            }
        }
    }

    private void setDefaultDownloadLocation(String path) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if ((prefs.getString(getString(R.string.download_location_key), null) == null)) {
            prefs.edit().putString(getString(R.string.download_location_key), path).apply();
        }
    }

    private void removeDialog() {
        LayoutInflater inflater = activity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_download, (ViewGroup) activity.findViewById(R.id.toast_layout_root));
        Toast toast = new Toast(activity);
        toast.setGravity(Gravity.BOTTOM, 0, 250);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
        ((MainActivity) activity).downloadCount = (((MainActivity) activity).downloadCount + 1);
        ((MainActivity) activity).badgeDownload.setNumber(((MainActivity) activity).downloadCount);
        dialog.dismiss();
    }
}
