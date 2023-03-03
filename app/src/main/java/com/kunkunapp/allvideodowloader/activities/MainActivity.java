package com.kunkunapp.allvideodowloader.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.gyf.immersionbar.ImmersionBar;
import com.kunkunapp.allvideodowloader.BuildConfig;
import com.kunkunapp.allvideodowloader.MyApp;
import com.kunkunapp.allvideodowloader.R;
import com.kunkunapp.allvideodowloader.adapters.ShortcutAdapter;
import com.kunkunapp.allvideodowloader.adapters.SuggestionAdapter;
import com.kunkunapp.allvideodowloader.browser.BrowserManager;
import com.kunkunapp.allvideodowloader.database.AppDatabase;
import com.kunkunapp.allvideodowloader.database.AppExecutors;
import com.kunkunapp.allvideodowloader.database.ShortcutTable;
import com.kunkunapp.allvideodowloader.fragments.AllDownloadFragment;
import com.kunkunapp.allvideodowloader.fragments.SettingsFragment;
import com.kunkunapp.allvideodowloader.helper.WebConnect;
import com.kunkunapp.allvideodowloader.interfaces.ShortcutListner;
import com.kunkunapp.allvideodowloader.utils.ThemeSettings;
import com.kunkunapp.allvideodowloader.utils.Utils;
import com.kunkunapp.allvideodowloader.views.Badge;
import com.kunkunapp.allvideodowloader.views.BadgeRed;
import com.kunkunapp.allvideodowloader.views.NotificationBadge;
import com.kunkunapp.allvideodowloader.views.NotificationBadgeRed;
import com.kunkunapp.allvideodowloader.webservice.Gossip;
import com.kunkunapp.allvideodowloader.webservice.Result;
import com.kunkunapp.allvideodowloader.webservice.RetrofitClient;
import com.kunkunapp.allvideodowloader.webservice.SearchModel;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2core.DownloadBlock;
import com.tonyodev.fetch2core.Func;

import java.io.File;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener {

    private static final String DOWNLOAD = "Downloads";
    private static final String HISTORY = "History";
    private static final String SETTING = "Settings";
    private static final String TAG = MainActivity.class.getCanonicalName();
    private EditText searchTextBar;
    private BrowserManager browserManager;
    private Uri appLinkData;
    private FragmentManager manager;
    public BottomNavigationView navView;
    public Badge badge;
    public int downloadCount = 0;
    public BadgeRed badgeDownload;
    private ImageView appSettingsBtn;
    private LinearLayout searchView;
    private Activity activity;
    private ImageView imgMore;
    private ImageView imgTitle;
    private ImageView imgBlur;
    private ImageView howToUseBtn;
    private LinearLayout llOption;
    private RelativeLayout mainContent;
    private RecyclerView rvShortcut;
    private ShortcutAdapter shortcutAdapter;
    private RelativeLayout tabContainer;
    private AllDownloadFragment allDownloadFragment;
    ImageView appSettingsBtn2;
    ImageView imgMore2;
    public EditText inputURLText;
    public RelativeLayout homeContainer;
    public SuggestionAdapter suggestionAdapter;
    public boolean isEnableSuggetion = false;
    private Fetch fetch;
    public boolean isDisableOnResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        transStatusBar(true);

        Intent appLinkIntent = getIntent();
        appLinkData = appLinkIntent.getData();

        manager = this.getSupportFragmentManager();
        // This is for creating browser manager fragment
        if ((browserManager = (BrowserManager) this.getSupportFragmentManager().findFragmentByTag("BM")) == null) {
            browserManager = new BrowserManager(MainActivity.this);
            manager.beginTransaction().add(browserManager, "BM").commit();
        }

        // Bottom navigation
        navView = findViewById(R.id.bottomNavigationView);
        navView.setItemIconTintList(null);
        badgeDownload = NotificationBadgeRed.getBadge(navView, 1);
        badgeDownload.setNumber(0);
        badge = NotificationBadge.getBadge(navView, 2);
        badge.setNumber(1);
        badge.tabSelected(false);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        setUPBrowserToolbarView();
        initViews();

    }

    private void initViews() {
        allDownloadFragment = new AllDownloadFragment();
        homeContainer = findViewById(R.id.homeContainer);
        inputURLText = findViewById(R.id.inputURLText);
        appSettingsBtn2 = findViewById(R.id.appSettingsBtn2);
        imgMore2 = findViewById(R.id.imgMore2);
        tabContainer = findViewById(R.id.tabContainer);
        mainContent = findViewById(R.id.mainContent);
        howToUseBtn = findViewById(R.id.howToUseBtn);
        imgTitle = findViewById(R.id.imgTitle);
        llOption = findViewById(R.id.llOption);
        imgMore = findViewById(R.id.imgMore);
        imgBlur = findViewById(R.id.imgBlur);
        rvShortcut = findViewById(R.id.rvShortcut);
        appSettingsBtn = findViewById(R.id.appSettingsBtn);
        searchView = findViewById(R.id.searchView);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(0, Utils.Companion.getStatusBarHeight(this), 0, 0);
        params.addRule(RelativeLayout.ABOVE, R.id.bottomNavigationView);
        mainContent.setLayoutParams(params);
        tabContainer.setLayoutParams(params);

        appSettingsBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsClicked();
            }
        });
        appSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsClicked();
            }
        });

        imgMore2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPopupButtonClick(imgMore2);
            }
        });
        imgMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPopupButtonClick(imgMore);
            }
        });
        howToUseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, IntroActivity.class);
                startActivity(intent);
            }
        });

        final SharedPreferences prefs = getSharedPreferences("settings", 0);
        if (!prefs.getBoolean("first_shortcut", false)) {
            prefs.edit().putBoolean("first_shortcut", true).apply();
            insertDefaultShortcut();
        }
        shortcutAdapter = new ShortcutAdapter(this, new ShortcutListner() {
            @Override
            public void shortcutClick(ShortcutTable shortcutTable) {
                if (shortcutTable.strTitle.equalsIgnoreCase(getString(R.string.add_shortcut))) {
                    addShortcut();
                } else {
                    isEnableSuggetion = false;
                    searchTextBar.setText(shortcutTable.strURL);
                    browserManager.newWindow(shortcutTable.strURL);
                }
            }

            @Override
            public void shortcutRemoveClick(ShortcutTable shortcutTable) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().delete(shortcutTable);
                    }
                });
            }
        });
        rvShortcut.setLayoutManager(new GridLayoutManager(this, 4));
        rvShortcut.setAdapter(shortcutAdapter);

        fetchShortcut();

        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setDownloadConcurrentLimit(3)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);
        fetch.getDownloads(new Func<List<Download>>() {
            @Override
            public void call(@NonNull List<Download> downloads) {

            }
        }).addListener(new FetchListener() {
            @Override
            public void onAdded(@NonNull Download download) {

            }

            @Override
            public void onQueued(@NonNull Download download, boolean b) {

            }

            @Override
            public void onWaitingNetwork(@NonNull Download download) {

            }

            @Override
            public void onCompleted(@NonNull Download download) {
                if (isFinishing() || isDestroyed())
                    return;


                try {
                    File file = new File(download.getFile());
                    MediaScannerConnection.scanFile(MainActivity.this,
                            new String[]{file.toString()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {

                                }
                            });

                    View customSnackView = getLayoutInflater().inflate(R.layout.toast_success_download, (ViewGroup) activity.findViewById(R.id.toast_layout_root));
                    TextView txtTitle = customSnackView.findViewById(R.id.txtTitle);
                    txtTitle.setText(file.getName());

                    TextView txtPlay = customSnackView.findViewById(R.id.txtPlay);
                    txtPlay.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openFile(file);
                        }
                    });

                    final Snackbar snackbar = Snackbar.make(navView, "", Snackbar.LENGTH_LONG);
                    snackbar.getView().setBackgroundColor(Color.TRANSPARENT);
                    Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
                    snackbarLayout.addView(customSnackView, 0);
                    snackbar.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(@NonNull Download download, @NonNull Error error, @Nullable Throwable throwable) {

            }

            @Override
            public void onDownloadBlockUpdated(@NonNull Download download, @NonNull DownloadBlock downloadBlock, int i) {

            }

            @Override
            public void onStarted(@NonNull Download download, @NonNull List<? extends DownloadBlock> list, int i) {

            }

            @Override
            public void onProgress(@NonNull Download download, long l, long l1) {

            }

            @Override
            public void onPaused(@NonNull Download download) {

            }

            @Override
            public void onResumed(@NonNull Download download) {

            }

            @Override
            public void onCancelled(@NonNull Download download) {

            }

            @Override
            public void onRemoved(@NonNull Download download) {

            }

            @Override
            public void onDeleted(@NonNull Download download) {

            }
        });
    }

    public void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
            String mime = getContentResolver().getType(uri);

            // Open file with user selected app
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addShortcut() {
        BottomSheetDialog mBottomSheetDialog = new BottomSheetDialog(MainActivity.this, R.style.CustomBottomSheetDialogTheme);
        mBottomSheetDialog.setContentView(R.layout.dialog_add_shortcut);

        EditText edtName = mBottomSheetDialog.findViewById(R.id.edtName);
        EditText edtURL = mBottomSheetDialog.findViewById(R.id.edtURL);

        edtURL.setText(inputURLText.getText().toString());

        TextView txtCancel = mBottomSheetDialog.findViewById(R.id.txtCancel);
        TextView txtOK = mBottomSheetDialog.findViewById(R.id.txtOK);
        txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
            }
        });
        txtOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strName = edtName.getText().toString().trim();
                String strURL = edtURL.getText().toString().trim();

                if (strName.length() == 0) {
                    Toast.makeText(activity, "Please enter website name!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (strURL.length() == 0) {
                    Toast.makeText(activity, "Please enter URL of website!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (strURL.startsWith("http://") || strURL.startsWith("https://")) {

                } else {
                    strURL = "https://" + strURL;
                }

               /* if (!Patterns.WEB_URL.matcher(strURL).matches()) {
                    Toast.makeText(activity, "Please enter valid URL of website!", Toast.LENGTH_SHORT).show();
                    return;
                }*/
                mBottomSheetDialog.dismiss();
                String finalStrURL = strURL;
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_default, strName, finalStrURL));
                    }
                });
            }
        });
        mBottomSheetDialog.show();
    }

    private void insertDefaultShortcut() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_add_shortcut, getString(R.string.add_shortcut), ""));
                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_facebook, getString(R.string.facebook), "https://www.facebook.com/"));
                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_instagram_, getString(R.string.instagram), "https://www.instagram.com/"));
                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_linkedin, getString(R.string.linkedin), "https://www.linkedin.com/"));

                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_pinterest, getString(R.string.pinterest), "https://in.pinterest.com/"));
                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_tiktok, getString(R.string.tiktok), "https://www.tiktok.com/"));
                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_dailymotion, getString(R.string.dailymotion), "https://www.dailymotion.com/"));
                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_vimeo, getString(R.string.vimeo), "https://vimeo.com/"));

                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_buzz_video, getString(R.string.buzz_video), "https://www.buzzvideo.com/"));
                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_imdb, getString(R.string.imdb), "https://www.imdb.com/"));
                AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().insert(new ShortcutTable(R.drawable.ic_vlive, getString(R.string.vlive), "https://www.vlive.tv/"));
            }
        });
    }

    private void fetchShortcut() {
        AppDatabase.Companion.getDatabase(this).shortcutDao().getAllShortcut().observe(this, new Observer<List<ShortcutTable>>() {
            @Override
            public void onChanged(List<ShortcutTable> shortcutTables) {
                if (isFinishing() || isDestroyed())
                    return;

                shortcutAdapter.setShortcutArrayList(shortcutTables);
            }
        });
    }

    public void onPopupButtonClick(View button) {
        PopupMenu popup = new PopupMenu(this, button);
        popup.getMenuInflater().inflate(R.menu.menu_home, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_new_tab:
                        addTabDialog();
                        break;
                    case R.id.menu_add_shortcut:
                        addShortcut();
                        break;
                    case R.id.menu_copy:
                        String strLink = searchTextBar.getText().toString().trim();
                        if (strLink.length() > 0) {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("label", strLink);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(MainActivity.this, "Copied", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.menu_history:
                        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                        startActivityForResult(intent, 151);
                        break;
                    case R.id.menu_feedback:
                        Intent intentFeed = new Intent(MainActivity.this, FeedbackActivity.class);
                        startActivity(intentFeed);
                        break;
                }
                return true;
            }
        });
        popup.setForceShowIcon(true);
        popup.show();
    }

    public void addTabDialog() {
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_tab);


        BottomNavigationView bottomNavigationView = dialog.findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                dialog.dismiss();
                switch (item.getItemId()) {
                    case R.id.navHomeTab:
                        navView.setSelectedItemId(R.id.navHome);
                        return true;
                    case R.id.navDownloadTab:
                        navView.setSelectedItemId(R.id.navDownload);
                        return true;
                    case R.id.navTabsTab:
                        navView.setSelectedItemId(R.id.navTabs);
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });

        Badge badgeDialog = NotificationBadge.getBadge(bottomNavigationView, 2);
        badgeDialog.setNumber(badge.getNumber());
        badgeDialog.tabSelected(false);
        ImageView howToUseBtn = dialog.findViewById(R.id.howToUseBtn);

        howToUseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(MainActivity.this, IntroActivity.class);
                startActivity(intent);
            }
        });
        ImageView appSettingsBtn = dialog.findViewById(R.id.appSettingsBtn);
        appSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                settingsClicked();
            }
        });
        ImageView imgMore = dialog.findViewById(R.id.imgMore);
        imgMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPopupButtonChild(imgMore);
            }
        });
        EditText edtSearch = dialog.findViewById(R.id.edtSearch);
        edtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                isDisableOnResume = true;
                dialog.dismiss();
                Utils.Companion.hideSoftKeyboard(MainActivity.this, edtSearch.getWindowToken());
                navView.setSelectedItemId(R.id.navHome);
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    isEnableSuggetion = false;
                    suggestionAdapter.setResultList(null);
                    new WebConnect(edtSearch, MainActivity.this).connect();
                } else if (actionId == EditorInfo.IME_ACTION_GO) {
                    isEnableSuggetion = false;
                    suggestionAdapter.setResultList(null);
                    new WebConnect(edtSearch, MainActivity.this).connect();
                }
                return false;
            }
        });
        ImageView searchBtn = dialog.findViewById(R.id.searchBtn);
        RecyclerView rvShortcut = dialog.findViewById(R.id.rvShortcut);
        rvShortcut.setLayoutManager(new GridLayoutManager(MainActivity.this, 4));

        ShortcutAdapter shortcutAdapter = new ShortcutAdapter(MainActivity.this, new ShortcutListner() {
            @Override
            public void shortcutClick(ShortcutTable shortcutTable) {
                isDisableOnResume = true;
                dialog.dismiss();
                if (shortcutTable.strTitle.equalsIgnoreCase(getString(R.string.add_shortcut))) {
                    addShortcut();
                } else {
                    isEnableSuggetion = false;
                    navView.setSelectedItemId(R.id.navHome);
                    edtSearch.setText(shortcutTable.strURL);
                    Utils.Companion.hideSoftKeyboard(MainActivity.this, edtSearch.getWindowToken());
                    new WebConnect(edtSearch, MainActivity.this).connect();
                }
            }

            @Override
            public void shortcutRemoveClick(ShortcutTable shortcutTable) {

            }
        });
        rvShortcut.setAdapter(shortcutAdapter);
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                List<ShortcutTable> shortcutTableList = AppDatabase.Companion.getDatabase(MainActivity.this).shortcutDao().getAllShortcutList();
                if (shortcutTableList != null && shortcutAdapter != null)
                    shortcutAdapter.setShortcutArrayList(shortcutTableList);
            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDisableOnResume = true;
                dialog.dismiss();
                isEnableSuggetion = false;
                navView.setSelectedItemId(R.id.navHome);
                Utils.Companion.hideSoftKeyboard(MainActivity.this, edtSearch.getWindowToken());
                new WebConnect(edtSearch, MainActivity.this).connect();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void onPopupButtonChild(View button) {
        PopupMenu popup = new PopupMenu(this, button);
        popup.getMenuInflater().inflate(R.menu.menu_new_tab, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_add_shortcut:
                        addShortcut();
                        break;
                    case R.id.menu_copy:
                        String strLink = searchTextBar.getText().toString().trim();
                        if (strLink.length() > 0) {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("label", strLink);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(MainActivity.this, "Copied", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.menu_history:
                        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                        startActivityForResult(intent, 151);
                        break;
                    case R.id.menu_feedback:
                        Intent intentFeed = new Intent(MainActivity.this, FeedbackActivity.class);
                        startActivity(intentFeed);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 151 && data != null) {
            String link = data.getStringExtra("link");
            if (link != null && link.length() > 0) {
                isEnableSuggetion = false;
                searchTextBar.setText(link);
                new WebConnect(searchTextBar, MainActivity.this).connect();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ThemeSettings.getInstance(this).save(this);
    }

    private void setUPBrowserToolbarView() {
        RecyclerView rvSuggetion = findViewById(R.id.rvSuggetion);
        suggestionAdapter = new SuggestionAdapter(new SuggestionAdapter.SuggetionListner() {
            @Override
            public void onSuggetion(String str) {
                Log.d(TAG, "onSuggetion: selected: " + str);
                Utils.Companion.hideSoftKeyboard(MainActivity.this, searchTextBar.getWindowToken());
                isEnableSuggetion = false;
                searchTextBar.setText(str);
                new WebConnect(searchTextBar, MainActivity.this).connect();
                suggestionAdapter.setResultList(null);
            }
        });
        rvSuggetion.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        rvSuggetion.setAdapter(suggestionAdapter);

        ImageView btnSearch = findViewById(R.id.searchBtn);
        searchTextBar = findViewById(R.id.inputURLText);
        TextWatcher searchViewTextWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (searchTextBar.getText().toString().trim().length() > 0) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm.isAcceptingText() && isEnableSuggetion) {
                        fetchSearchList(searchTextBar.getText().toString().trim());
                    }
                    isEnableSuggetion = true;
                } else {
                    suggestionAdapter.setResultList(null);
                }
            }
        };
        searchTextBar.addTextChangedListener(searchViewTextWatcher);
        searchTextBar.setOnEditorActionListener(this);
        btnSearch.setOnClickListener(this);

    }

    Call<SearchModel> searchModelCall;

    private void fetchSearchList(String str) {
        if (searchModelCall != null) {
            searchModelCall.cancel();
        }
        suggestionAdapter.setResultList(null);
        Log.d(TAG, "fetchSearchList: " + str);
        searchModelCall = RetrofitClient.getInstance().getApi().getSearchResult("json", 5, str);
        searchModelCall.enqueue(new Callback<SearchModel>() {
            @Override
            public void onResponse(Call<SearchModel> call, Response<SearchModel> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getGossip() != null && response.body().getGossip().getResults() != null) {
                    SearchModel searchModel = response.body();

                    Gossip gossip = searchModel.getGossip();
                    List<Result> resultList = gossip.getResults();
                    if (searchTextBar.getText().toString().trim().length() > 0) {
                        suggestionAdapter.setResultList(resultList);
                    } else {
                        suggestionAdapter.setResultList(null);
                    }
                }
            }

            @Override
            public void onFailure(Call<SearchModel> call, Throwable t) {
                suggestionAdapter.setResultList(null);
            }
        });
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navHome:
                    badge.tabSelected(false);
                    tabContainer.setVisibility(View.GONE);
                    transStatusBar(true);
                    homeClicked();
                    return true;
                case R.id.navDownload:
                    downloadCount = 0;
                    badgeDownload.setNumber(downloadCount);
                    badge.tabSelected(false);
                    transStatusBar(false);
                    downloadClicked();
                    //searchView.setVisibility(View.GONE);
                    tabContainer.setVisibility(View.GONE);
                    return true;
                case R.id.navTabs:
                    badge.tabSelected(true);
                    transStatusBar(false);
                    tabContainer.setVisibility(View.VISIBLE);
                    tabContainer.removeAllViews();
                    tabContainer.addView(getBrowserManager().getTabMain());
                    browserManager.pauseCurrentWindow();
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.searchBtn:
                isEnableSuggetion = false;
                suggestionAdapter.setResultList(null);
                new WebConnect(searchTextBar, this).connect();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            isEnableSuggetion = false;
            suggestionAdapter.setResultList(null);
            new WebConnect(searchTextBar, this).connect();
        } else if (actionId == EditorInfo.IME_ACTION_GO) {
            isEnableSuggetion = false;
            suggestionAdapter.setResultList(null);
            new WebConnect(searchTextBar, this).connect();
        }
        return handled;
    }

    public void transStatusBar(boolean isTrans) {
        if (isTrans) {
            ImmersionBar.with(MainActivity.this)
                    .transparentStatusBar()
                    .navigationBarColor(R.color.white)
                    .statusBarDarkFont(true)
                    .navigationBarDarkIcon(true)
                    .init();
        } else {
            ImmersionBar.with(MainActivity.this)
                    .statusBarColor(R.color.white)
                    .navigationBarColor(R.color.white)
                    .statusBarDarkFont(true)
                    .navigationBarDarkIcon(true)
                    .init();
        }
    }

    public void showTopMenu() {
        imgTitle.setVisibility(View.VISIBLE);
        imgBlur.setVisibility(View.VISIBLE);
        llOption.setVisibility(View.VISIBLE);

        appSettingsBtn2.setVisibility(View.GONE);
        imgMore2.setVisibility(View.GONE);
    }

    public void hideTopMenu() {
        imgBlur.setVisibility(View.GONE);
        imgTitle.setVisibility(View.GONE);
        llOption.setVisibility(View.GONE);

        appSettingsBtn2.setVisibility(View.VISIBLE);
        imgMore2.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (suggestionAdapter.getItemCount() > 0) {
            suggestionAdapter.setResultList(null);
            return;
        }
        if (allDownloadFragment.isSelectedMode) {
            allDownloadFragment.unSelectAll();
            return;
        }

        if (navView.getSelectedItemId() == R.id.navDownload) {
            navView.setSelectedItemId(R.id.navHome);
            return;
        }
        if (navView.getSelectedItemId() == R.id.navTabs) {
            navView.setSelectedItemId(R.id.navHome);
            return;
        }

        if (manager.findFragmentByTag(DOWNLOAD) != null || manager.findFragmentByTag(HISTORY) != null) {
            MyApp.getInstance().getOnBackPressedListener().onBackpressed();
            browserManager.resumeCurrentWindow();
            navView.setSelectedItemId(R.id.navHome);
        } else if (manager.findFragmentByTag(SETTING) != null) {
            MyApp.getInstance().getOnBackPressedListener().onBackpressed();
            browserManager.resumeCurrentWindow();
            navView.setVisibility(View.VISIBLE);
            navView.setSelectedItemId(R.id.navHome);
        } else if (MyApp.getInstance().getOnBackPressedListener() != null) {
            MyApp.getInstance().getOnBackPressedListener().onBackpressed();
        } else if (shortcutAdapter.selectionMode) {
            shortcutAdapter.selectionMode = false;
            shortcutAdapter.notifyDataSetChanged();
        } else {
            Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_confirmation);

            TextView txtTitle = dialog.findViewById(R.id.txtTitle);
            TextView txtDesc = dialog.findViewById(R.id.txtDesc);
            txtTitle.setText("Exit from app");
            txtDesc.setText("Are you sure you want to exit?");
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
                    finish();
                }
            });
            dialog.show();
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public BrowserManager getBrowserManager() {
        return browserManager;
    }

    public interface OnBackPressedListener {
        void onBackpressed();
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        MyApp.getInstance().setOnBackPressedListener(onBackPressedListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (appLinkData != null) {
            browserManager.newWindow(appLinkData.toString());
        }
    }

    public void browserClicked() {
        browserManager.unhideCurrentWindow();
    }

    public void downloadClicked() {
        closeHistory();
        if (manager.findFragmentByTag(DOWNLOAD) == null) {
            browserManager.hideCurrentWindow();
            browserManager.pauseCurrentWindow();
            manager.beginTransaction().add(R.id.mainContent, allDownloadFragment, DOWNLOAD).commit();
        }
    }

    public void settingsClicked() {
        if (manager.findFragmentByTag(SETTING) == null) {
            transStatusBar(false);
            browserManager.hideCurrentWindow();
            browserManager.pauseCurrentWindow();
            navView.setVisibility(View.GONE);
            manager.beginTransaction().add(R.id.mainContent, new SettingsFragment(), SETTING).commit();
        }
    }

    public void homeClicked() {
        browserManager.unhideCurrentWindow();
        browserManager.resumeCurrentWindow();
        closeDownloads();
        closeHistory();
    }

    private void closeDownloads() {
        Fragment fragment = manager.findFragmentByTag(DOWNLOAD);
        if (fragment != null) {
            manager.beginTransaction().remove(fragment).commit();
        }
    }

    private void closeHistory() {
        Fragment fragment = manager.findFragmentByTag(HISTORY);
        if (fragment != null) {
            manager.beginTransaction().remove(fragment).commit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        onRequestPermissionsResultCallback.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
    }

    private ActivityCompat.OnRequestPermissionsResultCallback onRequestPermissionsResultCallback;

    public void setOnRequestPermissionsResultListener(ActivityCompat
                                                              .OnRequestPermissionsResultCallback
                                                              onRequestPermissionsResultCallback) {
        this.onRequestPermissionsResultCallback = onRequestPermissionsResultCallback;
    }

}
