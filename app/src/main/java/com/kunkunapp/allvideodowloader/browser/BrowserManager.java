package com.kunkunapp.allvideodowloader.browser;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kunkunapp.allvideodowloader.R;
import com.kunkunapp.allvideodowloader.activities.IntroActivity;
import com.kunkunapp.allvideodowloader.adapters.ShortcutAdapter;
import com.kunkunapp.allvideodowloader.database.AppDatabase;
import com.kunkunapp.allvideodowloader.database.AppExecutors;
import com.kunkunapp.allvideodowloader.database.ShortcutAppDatabase;
import com.kunkunapp.allvideodowloader.database.ShortcutTable;
import com.kunkunapp.allvideodowloader.fragments.base.BaseFragment;
import com.kunkunapp.allvideodowloader.helper.WebConnect;
import com.kunkunapp.allvideodowloader.interfaces.ShortcutListner;
import com.kunkunapp.allvideodowloader.utils.Utils;
import com.kunkunapp.allvideodowloader.views.Badge;
import com.kunkunapp.allvideodowloader.views.NotificationBadge;
import com.kunkunapp.allvideodowloader.views.cardstack.CardStackView;
import com.kunkunapp.allvideodowloader.views.cardstack.StackAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class BrowserManager extends BaseFragment {

    private AdBlocker adBlock;
    public List<BrowserWindow> windowsList;
    private List<String> blockedWebsites;
    private Activity activity;
    private RecyclerView allWindows;
    private LinearLayout llCloseAll;
    CardStackView cardWindowTab;
    BrowserTabAdapter browserTabAdapter;

    public BrowserManager(Activity activity) {
        this.activity = activity;
    }

    RelativeLayout relativeLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d("debug", "Browser Manager added");
        windowsList = new ArrayList<>();
        relativeLayout = (RelativeLayout) LayoutInflater.from(getActivity()).inflate(R.layout.all_windows_popup, (ViewGroup) getActivity().findViewById(16908290), false);
        LinearLayout llAdd = relativeLayout.findViewById(R.id.llAdd);
        llAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTabDialog();
            }
        });
        llCloseAll = relativeLayout.findViewById(R.id.llCloseAll);
        llCloseAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeAllWindow();
            }
        });
        allWindows = relativeLayout.findViewById(R.id.rvRecent);
        allWindows.setLayoutManager(new LinearLayoutManager(getActivity()));
        this.allWindows.setAdapter(new AllWindowsAdapter());

        cardWindowTab = relativeLayout.findViewById(R.id.cardWindowTab);
        browserTabAdapter = new BrowserTabAdapter(getActivity());
        cardWindowTab.setItemExpendListener(new CardStackView.ItemExpendListener() {
            @Override
            public void onItemExpend(boolean expend) {

            }
        });
        cardWindowTab.setAdapter(browserTabAdapter);

        File file = new File(getActivity().getFilesDir(), "ad_filters.dat");
        try {
            if (file.exists()) {
                Log.d("debug", "file exists");
                FileInputStream fileInputStream = new FileInputStream(file);
                try (ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                    adBlock = (AdBlocker) objectInputStream.readObject();
                }
                fileInputStream.close();
            } else {
                adBlock = new AdBlocker();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                    objectOutputStream.writeObject(adBlock);
                }
                fileOutputStream.close();
            }
        } catch (IOException | ClassNotFoundException ignored) {
            //
        }
        updateAdFilters();
        blockedWebsites = Arrays.asList(getResources().getStringArray(R.array.blocked_sites));
    }

    public void addTabDialog() {
        Dialog dialog = new Dialog(getBaseActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_tab);


        BottomNavigationView bottomNavigationView = dialog.findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                dialog.dismiss();
                switch (item.getItemId()) {
                    case R.id.navHomeTab:
                        getBaseActivity().navView.setSelectedItemId(R.id.navHome);
                        return true;
                    case R.id.navDownloadTab:
                        getBaseActivity().navView.setSelectedItemId(R.id.navDownload);
                        return true;
                    case R.id.navTabsTab:
                        getBaseActivity().navView.setSelectedItemId(R.id.navTabs);
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });
        Badge badgeDialog = NotificationBadge.getBadge(bottomNavigationView, 2);
        badgeDialog.setNumber(getBaseActivity().badge.getNumber());
        badgeDialog.tabSelected(false);

        ImageView howToUseBtn = dialog.findViewById(R.id.howToUseBtn);
        howToUseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(getBaseActivity(), IntroActivity.class);
                startActivity(intent);
            }
        });
        ImageView appSettingsBtn = dialog.findViewById(R.id.appSettingsBtn);
        appSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                getBaseActivity().settingsClicked();
            }
        });
        ImageView imgMore = dialog.findViewById(R.id.imgMore);
        imgMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBaseActivity().onPopupButtonChild(imgMore);
            }
        });
        EditText edtSearch = dialog.findViewById(R.id.edtSearch);
        edtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                getBaseActivity().isDisableOnResume = true;
                dialog.dismiss();
                Utils.Companion.hideSoftKeyboard(getBaseActivity(), edtSearch.getWindowToken());
                getBaseActivity().navView.setSelectedItemId(R.id.navHome);
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    getBaseActivity().isEnableSuggetion = false;
                    getBaseActivity().suggestionAdapter.setResultList(null);
                    new WebConnect(edtSearch, getBaseActivity()).connect();
                } else if (actionId == EditorInfo.IME_ACTION_GO) {
                    getBaseActivity().isEnableSuggetion = false;
                    getBaseActivity().suggestionAdapter.setResultList(null);
                    new WebConnect(edtSearch, getBaseActivity()).connect();
                }
                return false;
            }
        });
        ImageView searchBtn = dialog.findViewById(R.id.searchBtn);
        RecyclerView rvShortcut = dialog.findViewById(R.id.rvShortcut);
        rvShortcut.setLayoutManager(new GridLayoutManager(getBaseActivity(), 4));

        ShortcutAdapter shortcutAdapter = new ShortcutAdapter(getBaseActivity(), new ShortcutListner() {
            @Override
            public void shortcutClick(ShortcutTable shortcutTable) {
                getBaseActivity().isDisableOnResume = true;
                dialog.dismiss();
                if (shortcutTable.strTitle.equalsIgnoreCase(getString(R.string.add_shortcut))) {
                    getBaseActivity().addShortcut();
                } else {
                    getBaseActivity().isEnableSuggetion = false;
                    getBaseActivity().navView.setSelectedItemId(R.id.navHome);
                    edtSearch.setText(shortcutTable.strURL);
                    Utils.Companion.hideSoftKeyboard(getBaseActivity(), edtSearch.getWindowToken());
                    new WebConnect(edtSearch, getBaseActivity()).connect();
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
                List<ShortcutTable> shortcutTableList =  ShortcutAppDatabase.getInstance(getBaseActivity()).shortcutDao().getAllShortcutList();
                if (shortcutTableList != null && shortcutAdapter != null)
                    shortcutAdapter.setShortcutArrayList(shortcutTableList);
            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBaseActivity().isDisableOnResume = true;
                dialog.dismiss();
                getBaseActivity().isEnableSuggetion = false;
                getBaseActivity().navView.setSelectedItemId(R.id.navHome);
                Utils.Companion.hideSoftKeyboard(getBaseActivity(), edtSearch.getWindowToken());
                new WebConnect(edtSearch, getBaseActivity()).connect();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void newWindow(String url) {
        if (blockedWebsites.contains(Utils.Companion.getBaseDomain(url))) {
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
        } else {
            //getBaseActivity().hideTopMenu();
            Bundle data = new Bundle();
            data.putString("url", url);
            BrowserWindow window = new BrowserWindow(activity);
            window.setArguments(data);
            getFragmentManager().beginTransaction()
                    .add(R.id.homeContainer, window, null)
                    .commit();
            windowsList.add(window);
            getBaseActivity().setOnBackPressedListener(window);
            if (windowsList.size() > 1) {
                window = windowsList.get(windowsList.size() - 2);
                if (window != null && window.getView() != null) {
                    window.getView().setVisibility(View.GONE);
                    //window.onPause();
                }
            }
            updateNumWindows();
            this.allWindows.getAdapter().notifyDataSetChanged();
            browserTabAdapter.setData(windowsList);

            for (int posWindow = 0; posWindow < windowsList.size(); posWindow++) {
                BrowserWindow windowTemp = windowsList.get(posWindow);
                windowTemp.onPause();
            }
            BrowserWindow windowCurrentTemp = windowsList.get(windowsList.size() - 1);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //windowCurrentTemp.onResume();
                }
            },1500);

        }
    }

    public void updateNumWindows() {
        if (windowsList.size() == 0) {
            updateNumWindows(1);
            getBaseActivity().showTopMenu();
        } else {
            getBaseActivity().hideTopMenu();
        }
        for (BrowserWindow window : this.windowsList) {
            updateNumWindows(this.windowsList.size());
        }
    }

    public void updateNumWindows(int num) {
        final String numWindowsString = String.valueOf(num);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                getBaseActivity().badge.setNumber(num);
            }
        });
    }

    public void closeWindow(BrowserWindow window) {
        final EditText inputURLText = getBaseActivity().findViewById(R.id.inputURLText);
        windowsList.remove(window);
        getFragmentManager().beginTransaction().remove(window).commit();
        if (!windowsList.isEmpty()) {
            BrowserWindow topWindow = windowsList.get(windowsList.size() - 1);
            if (topWindow != null && topWindow.getView() != null) {
                topWindow.onResume();
                topWindow.getView().setVisibility(View.VISIBLE);
            }
            if (topWindow != null) {
                getBaseActivity().isEnableSuggetion = false;
                inputURLText.setText(topWindow.getUrl());
                getBaseActivity().setOnBackPressedListener(topWindow);
            }
        } else {
            getBaseActivity().isEnableSuggetion = false;
            inputURLText.getText().clear();
            getBaseActivity().setOnBackPressedListener(null);
        }
        browserTabAdapter.setData(windowsList);
        updateNumWindows();
    }


    public void closeAllWindow() {
        if (!windowsList.isEmpty()) {
            for (Iterator<BrowserWindow> iterator = windowsList.iterator(); iterator.hasNext(); ) {
                BrowserWindow window = iterator.next();
                getFragmentManager().beginTransaction().remove(window).commit();
                iterator.remove();
            }
            getBaseActivity().setOnBackPressedListener(null);
        } else {
            getBaseActivity().setOnBackPressedListener(null);
        }
        windowsList.clear();
        allWindows.getAdapter().notifyDataSetChanged();
        browserTabAdapter.setData(windowsList);
        updateNumWindows();
    }

    public void hideCurrentWindow() {
        if (!windowsList.isEmpty()) {
            BrowserWindow topWindow = windowsList.get(windowsList.size() - 1);
            if (topWindow.getView() != null) {
                topWindow.getView().setVisibility(View.GONE);
            }
        }
    }

    public void unhideCurrentWindow() {
        if (!windowsList.isEmpty()) {
            BrowserWindow topWindow = windowsList.get(windowsList.size() - 1);
            if (topWindow.getView() != null) {
                topWindow.getView().setVisibility(View.VISIBLE);
                getBaseActivity().setOnBackPressedListener(topWindow);
            }
        } else {
            getBaseActivity().setOnBackPressedListener(null);
        }
    }

    public void pauseCurrentWindow() {
        if (!windowsList.isEmpty()) {
            BrowserWindow topWindow = windowsList.get(windowsList.size() - 1);
            if (topWindow.getView() != null) {
                topWindow.onPause();
            }
        }
    }

    public void resumeCurrentWindow() {
        if (!windowsList.isEmpty()) {
            BrowserWindow topWindow = windowsList.get(windowsList.size() - 1);
            if (topWindow.getView() != null) {
               // topWindow.onResume();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        topWindow.onResume();
                    }
                },500);
                getBaseActivity().setOnBackPressedListener(topWindow);
            }
        } else {
            getBaseActivity().setOnBackPressedListener(null);
        }
    }

    public void updateAdFilters() {
        adBlock.update(getContext());
    }

    public boolean checkUrlIfAds(String url) {
        return adBlock.checkThroughFilters(url);
    }

    public View getAllWindows() {
        return this.allWindows;
    }

    public View getTabMain() {
        return relativeLayout;
    }

    public void switchWindow(int index) {
        List<BrowserWindow> list = this.windowsList;
        BrowserWindow topWindow = list.get(list.size() - 1);
        if (topWindow.getView() != null) {
            topWindow.getView().setVisibility(View.GONE);
        }
        BrowserWindow window = this.windowsList.get(index);
        this.windowsList.remove(index);
        this.windowsList.add(window);
        if (window.getView() != null) {
            window.getView().setVisibility(View.VISIBLE);
            getBaseActivity().setOnBackPressedListener(window);
            getBaseActivity().isEnableSuggetion = false;
            getBaseActivity().inputURLText.setText(window.getUrl());
        }
        for (int posWindow = 0; posWindow < windowsList.size(); posWindow++) {
            BrowserWindow windowTemp = windowsList.get(posWindow);
            windowTemp.onPause();
        }
        BrowserWindow windowCurrentTemp = windowsList.get(windowsList.size() - 1);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                windowCurrentTemp.onResume();
            }
        },500);

        this.allWindows.getAdapter().notifyDataSetChanged();
        browserTabAdapter.setData(windowsList);
    }

    private class BrowserTabAdapter extends StackAdapter<BrowserWindow> {

        public BrowserTabAdapter(Context context) {
            super(context);
        }

        @Override
        protected CardStackView.ViewHolder onCreateView(ViewGroup parent, int viewType) {
            return new ColorItemViewHolder(getLayoutInflater().inflate(R.layout.all_windows_popup_item, parent, false));
        }

        @Override
        public void bindView(BrowserWindow data, int position, CardStackView.ViewHolder viewHolder) {
            if (viewHolder instanceof ColorItemViewHolder) {
                ColorItemViewHolder holder = (ColorItemViewHolder) viewHolder;

                WebView webView = data.getWebView();
                if (webView != null) {
                    if (webView.getTitle() == null || webView.getTitle().length() == 0) {
                        holder.windowTitle.setText("Home");
                    } else {
                        holder.windowTitle.setText(webView.getTitle());
                    }
                    if (webView.getFavicon() == null) {
                        Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon);
                        Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon);
                    } else {
                        holder.favicon.setImageBitmap(webView.getFavicon());
                        holder.imgLargeIcon.setImageBitmap(webView.getFavicon());
                    }
                } else {
                    holder.windowTitle.setText("Home");
                    Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon);
                    Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon);
                }
                holder.close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        closeWindow(windowsList.get(position));
                        notifyDataSetChanged();
                    }
                });
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //getBaseActivity().homeContainer.setVisibility(View.VISIBLE);
                        switchWindow(position);
                        getBaseActivity().navView.setSelectedItemId(R.id.navHome);
                    }
                });
            }
        }

        class ColorItemViewHolder extends CardStackView.ViewHolder {
            ImageView favicon;
            TextView windowTitle;
            ImageView close;
            ImageView imgLargeIcon;

            public ColorItemViewHolder(View view) {
                super(view);
                this.windowTitle = (TextView) itemView.findViewById(R.id.windowTitle);
                this.favicon = (ImageView) itemView.findViewById(R.id.favicon);
                this.close = (ImageView) itemView.findViewById(R.id.close);
                this.imgLargeIcon = itemView.findViewById(R.id.imgLargeIcon);
            }

            @Override
            public void onItemExpand(boolean b) {

            }
        }
    }

    private class AllWindowsAdapter extends RecyclerView.Adapter<WindowItem> {
        private AllWindowsAdapter() {
        }

        @Override
        public WindowItem onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WindowItem(LayoutInflater.from(getActivity()).inflate(R.layout.all_windows_popup_item, parent, false));
        }

        @Override
        public void onBindViewHolder(WindowItem holder, int position) {
            WebView webView = (windowsList.get(position)).getWebView();

            if (webView != null) {
                if (webView.getTitle() == null || webView.getTitle().length() == 0) {
                    holder.windowTitle.setText("Home");
                } else {
                    holder.windowTitle.setText(webView.getTitle());
                }
                if (webView.getFavicon() == null) {
                    Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon);
                    Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon);
                } else {
                    holder.favicon.setImageBitmap(webView.getFavicon());
                    holder.imgLargeIcon.setImageBitmap(webView.getFavicon());
                }
            } else {
                holder.windowTitle.setText("Home");
                Glide.with(activity).load(R.drawable.ic_home).into(holder.favicon);
                Glide.with(activity).load(R.drawable.ic_default).into(holder.imgLargeIcon);
            }
            holder.close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeWindow(windowsList.get(position));
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //getBaseActivity().homeContainer.setVisibility(View.VISIBLE);
                    switchWindow(position);
                    getBaseActivity().navView.setSelectedItemId(R.id.navHome);
                }
            });

        }

        @Override
        public int getItemCount() {
            return windowsList.size();
        }
    }

    public int getDominantColor(Bitmap bitmap) {
        if (null == bitmap) return Color.TRANSPARENT;

        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        int alphaBucket = 0;

        boolean hasAlpha = bitmap.hasAlpha();
        int pixelCount = bitmap.getWidth() * bitmap.getHeight();
        int[] pixels = new int[pixelCount];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int y = 0, h = bitmap.getHeight(); y < h; y++) {
            for (int x = 0, w = bitmap.getWidth(); x < w; x++) {
                int color = pixels[x + y * w]; // x + y * width
                redBucket += (color >> 16) & 0xFF; // Color.red
                greenBucket += (color >> 8) & 0xFF; // Color.greed
                blueBucket += (color & 0xFF); // Color.blue
                if (hasAlpha) alphaBucket += (color >>> 50); // Color.alpha
            }
        }

        return Color.argb(
                (hasAlpha) ? (alphaBucket / pixelCount) : 255,
                redBucket / pixelCount,
                greenBucket / pixelCount,
                blueBucket / pixelCount);
    }

    public class WindowItem extends RecyclerView.ViewHolder {
        ImageView favicon;
        TextView windowTitle;
        ImageView close;
        ImageView imgLargeIcon;

        WindowItem(View itemView) {
            super(itemView);
            this.windowTitle = (TextView) itemView.findViewById(R.id.windowTitle);
            this.favicon = (ImageView) itemView.findViewById(R.id.favicon);
            this.close = (ImageView) itemView.findViewById(R.id.close);
            this.imgLargeIcon = itemView.findViewById(R.id.imgLargeIcon);
        }
    }
}
