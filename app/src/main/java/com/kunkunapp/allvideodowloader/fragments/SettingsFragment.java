package com.kunkunapp.allvideodowloader.fragments;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.kunkunapp.allvideodowloader.BuildConfig;
import com.kunkunapp.allvideodowloader.R;
import com.kunkunapp.allvideodowloader.activities.IntroActivity;
import com.kunkunapp.allvideodowloader.activities.MainActivity;
import com.kunkunapp.allvideodowloader.fragments.base.BaseFragment;
import com.kunkunapp.allvideodowloader.helper.AdController;
import com.kunkunapp.allvideodowloader.helper.WebConnect;
import com.kunkunapp.allvideodowloader.utils.HistorySQLite;
import com.kunkunapp.allvideodowloader.utils.Utils;
import com.kunkunapp.allvideodowloader.views.SwitchButton;

import java.io.File;
import java.util.ArrayList;


public class SettingsFragment extends BaseFragment implements MainActivity.OnBackPressedListener, View.OnClickListener {
    private View view;
    private String searchEngine;
    TextView txtSelectedSearchEngine;
    TextView txtDownloadLocation;
    private String strDownloadLocation;
    SharedPreferences prefs;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);

        if (view == null) {
            view = inflater.inflate(R.layout.fragment_settings, container, false);

            getBaseActivity().setOnBackPressedListener(this);
            prefs = getActivity().getSharedPreferences("settings", 0);

            strDownloadLocation = prefs.getString("downloadLocation", "/storage/emulated/0/Download/Videodownloader");
            txtDownloadLocation = view.findViewById(R.id.txtDownloadLocation);
            if (!strDownloadLocation.endsWith("/")) {
                strDownloadLocation = strDownloadLocation + "/";
            }
            txtDownloadLocation.setText(strDownloadLocation);

            searchEngine = prefs.getString("searchEngine", "Google");
            txtSelectedSearchEngine = view.findViewById(R.id.txtSelectedSearchEngine);
            txtSelectedSearchEngine.setText(searchEngine);
            //Back
            ImageView btnSettingsBack = view.findViewById(R.id.backBtn);
            btnSettingsBack.setOnClickListener(this);

            // Switch wifi only switch
            SwitchButton wifiSwitch = view.findViewById(R.id.wifiSwitch);
            boolean wifiOn = prefs.getBoolean(getString(R.string.wifiON), false);
            wifiSwitch.setChecked(wifiOn);
            wifiSwitch.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(SwitchButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean(getString(R.string.wifiON), isChecked).commit();
                }
            });

            // Switch ad blocker switch
            SwitchButton adBlockerSwitch = view.findViewById(R.id.adBlockerSwitch);
            boolean adBlockOn = prefs.getBoolean(getString(R.string.adBlockON), true);
            adBlockerSwitch.setChecked(adBlockOn);
            adBlockerSwitch.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(SwitchButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean(getString(R.string.adBlockON), isChecked).commit();
                }
            });

            LinearLayout llDownloadLocation = view.findViewById(R.id.llDownloadLocation);
            llDownloadLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectFolder();
                }
            });
            LinearLayout llSearchEngine = view.findViewById(R.id.llSearchEngine);
            llSearchEngine.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeSearchEngine();
                }
            });
            TextView txtClearCache = view.findViewById(R.id.txtClearCache);
            txtClearCache.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearCache();
                }
            });
            TextView txtClearHistory = view.findViewById(R.id.txtClearHistory);
            txtClearHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearHistory();
                }
            });
            TextView txtClearCookies = view.findViewById(R.id.txtClearCookies);
            txtClearCookies.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearCookies();
                }
            });
            TextView txtHowToDownload = view.findViewById(R.id.txtHowToDownload);
            txtHowToDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(getActivity(), IntroActivity.class));
                }
            });

            TextView txtPrivacyPolicy = view.findViewById(R.id.txtPrivacyPolicy);
            txtPrivacyPolicy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().onBackPressed();
                    getBaseActivity().isEnableSuggetion = false;
                    getBaseActivity().navView.setSelectedItemId(R.id.navHome);
                    new WebConnect(view.findViewById(R.id.edtSearch), getBaseActivity()).connect();
                }
            });
            TextView txtVersion = view.findViewById(R.id.txtVersion);
            txtVersion.setText(BuildConfig.VERSION_NAME);

            /*admob*/
            LinearLayout adContainer = view.findViewById(R.id.banner_container);
            AdController.loadBannerAd(getActivity(), adContainer);
            AdController.loadInterAd(getActivity());

        }
        return view;
    }

    public void selectFolder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose folder to save videos");

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_selectfolder, null);

        ListView lvDirectories = (ListView) dialogView.findViewById(R.id.lvDirectories);
        String path = Environment.getExternalStorageDirectory().toString();
        ((TextView) dialogView.findViewById(R.id.tvJamesBond)).setText(path);

        final ArrayList<String> items = listFolders(path);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, items);


        lvDirectories.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String dest = ((ListView) dialogView.findViewById(R.id.lvDirectories)).getItemAtPosition(i).toString().trim();
                String path;
                if (dest.compareTo("...") == 0) {
                    int lastSlash = ((TextView) dialogView.findViewById(R.id.tvJamesBond)).getText().toString().lastIndexOf("/");
                    path = ((TextView) dialogView.findViewById(R.id.tvJamesBond)).getText().toString().substring(0, lastSlash);
                } else {
                    path = ((TextView) dialogView.findViewById(R.id.tvJamesBond)).getText().toString() + "/" + dest;
                }
                items.clear();
                items.addAll(listFolders(path));
                ((TextView) dialogView.findViewById(R.id.tvJamesBond)).setText(path);
                adapter.notifyDataSetChanged();
            }
        });

        lvDirectories.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        builder.setView(dialogView);

        // Add the buttons
        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String location = ((TextView) dialogView.findViewById(R.id.tvJamesBond)).getText().toString();
                prefs.edit().putString("downloadLocation", location).commit();
                strDownloadLocation = location;
                if (!strDownloadLocation.endsWith("/")) {
                    strDownloadLocation = strDownloadLocation + "/";
                }
                txtDownloadLocation.setText(strDownloadLocation);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        // Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public ArrayList<String> listFolders(String path) {
        ArrayList<String> result = new ArrayList<String>();
        File f = new File(path);
        File[] files = f.listFiles();
        Log.d("TEST PATH1", path);
        Log.d("TEST PATH1", Environment.getExternalStorageDirectory().toString());
        if (path.compareTo(Environment.getExternalStorageDirectory().toString()) != 0) {
            result.add("...");
        }
        for (File inFile : files) {
            if (inFile.isDirectory()) {
                result.add(inFile.getName());
            }
        }

        return result;
    }

    private void clearCache() {
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirmation);

        TextView txtTitle = dialog.findViewById(R.id.txtTitle);
        TextView txtDesc = dialog.findViewById(R.id.txtDesc);
        txtTitle.setText("Clear cache");
        txtDesc.setText("Would you like to clear all the browsing cache?");
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
                new WebView(getActivity()).clearCache(true);
            }
        });
        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void clearCookies() {
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirmation);

        TextView txtTitle = dialog.findViewById(R.id.txtTitle);
        TextView txtDesc = dialog.findViewById(R.id.txtDesc);
        txtTitle.setText("Clear cookies");
        txtDesc.setText("Would you like to clear all the browsing cookies?");
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
                WebStorage.getInstance().deleteAllData();
            }
        });
        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void deleteCache() {
        try {
            File dir = getActivity().getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private void clearHistory() {
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirmation);

        TextView txtTitle = dialog.findViewById(R.id.txtTitle);
        TextView txtDesc = dialog.findViewById(R.id.txtDesc);
        txtTitle.setText("Clear history");
        txtDesc.setText("Would you like to clear all the browsing history?");
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
                new HistorySQLite(getActivity()).clearHistory();
            }
        });
        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    private void changeSearchEngine() {
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_search_engine);

        LinearLayout llGoogle = dialog.findViewById(R.id.llGoogle);
        LinearLayout llBing = dialog.findViewById(R.id.llBing);
        LinearLayout llAsk = dialog.findViewById(R.id.llAsk);
        LinearLayout llYahoo = dialog.findViewById(R.id.llYahoo);
        LinearLayout llBaidu = dialog.findViewById(R.id.llBaidu);
        LinearLayout llYandex = dialog.findViewById(R.id.llYandex);

        ImageView imgGoogle = dialog.findViewById(R.id.imgGoogle);
        ImageView imgBing = dialog.findViewById(R.id.imgBing);
        ImageView imgAsk = dialog.findViewById(R.id.imgAsk);
        ImageView imgYahoo = dialog.findViewById(R.id.imgYahoo);
        ImageView imgBaidu = dialog.findViewById(R.id.imgBaidu);
        ImageView imgYandex = dialog.findViewById(R.id.imgYandex);

        TextView txtSelect = dialog.findViewById(R.id.txtSelect);

        final SharedPreferences prefs = getActivity().getSharedPreferences("settings", 0);
        searchEngine = prefs.getString("searchEngine", "Google");

        txtSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtSelectedSearchEngine.setText(searchEngine);
                prefs.edit().putString("searchEngine", searchEngine).commit();
                dialog.dismiss();
            }
        });
        changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex);

        llGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEngine = "Google";
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex);
            }
        });
        llBing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEngine = "Bing";
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex);
            }
        });
        llAsk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEngine = "Ask";
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex);
            }
        });
        llYahoo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEngine = "Yahoo";
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex);
            }
        });
        llBaidu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEngine = "Baidu";
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex);
            }
        });
        llYandex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEngine = "Yandex";
                changeSelection(imgGoogle, imgBing, imgAsk, imgYahoo, imgBaidu, imgYandex);
            }
        });
        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void changeSelection(ImageView imgGoogle,
                                 ImageView imgBing,
                                 ImageView imgAsk,
                                 ImageView imgYahoo,
                                 ImageView imgBaidu,
                                 ImageView imgYandex) {
        imgGoogle.setSelected(false);
        imgBing.setSelected(false);
        imgAsk.setSelected(false);
        imgYahoo.setSelected(false);
        imgBaidu.setSelected(false);
        imgYandex.setSelected(false);

        switch (searchEngine) {
            case "Google":
                imgGoogle.setSelected(true);
                break;
            case "Bing":
                imgBing.setSelected(true);
                break;
            case "Ask":
                imgAsk.setSelected(true);
                break;
            case "Yahoo":
                imgYahoo.setSelected(true);
                break;
            case "Baidu":
                imgBaidu.setSelected(true);
                break;
            case "Yandex":
                imgYandex.setSelected(true);
                break;
        }
    }

    @Override
    public void onBackpressed() {
        getBaseActivity().transStatusBar(true);
        getBaseActivity().getBrowserManager().unhideCurrentWindow();
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.backBtn) {
            getActivity().onBackPressed();
        }
    }

}
