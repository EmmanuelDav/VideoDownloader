package com.kunkunapp.allvideodowloader.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kunkunapp.allvideodowloader.R;
import com.kunkunapp.allvideodowloader.activities.MainActivity;
import com.kunkunapp.allvideodowloader.fragments.base.BaseFragment;


import java.io.File;

public class DownloadsCompFragment extends BaseFragment implements MainActivity.OnBackPressedListener{
    private static final String COMPLETE = "downloadsCompleted";
    private View view;

    private ViewPager pager;
    private FloatingActionButton deleteAllBtn;
    private DownloadsCompletedFragment downloadsComplete;


    @Override
    public void onDestroy() {
        Fragment fragment;
        if ((fragment = getFragmentManager().findFragmentByTag(COMPLETE)) != null) {
            getFragmentManager().beginTransaction().remove(fragment).commit();
        }
        super.onDestroy();
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);

        if (view == null) {
            view = inflater.inflate(R.layout.downloads_lay, container, false);

            getBaseActivity().setOnBackPressedListener(this);

            pager = view.findViewById(R.id.progress_pager);
            pager.setAdapter(new PagerAdapter());

            //deleteAllBtn = view.findViewById(R.id.deleteAllBtn);
//            deleteAllBtn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    new AlertDialog.Builder(getContext())
//                            .setMessage("Do you want to delete all downloads?")
//                            .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    deleteAll();
//                                    //downloadsComplete.getAdapter().updateList();
//                                }
//                            })
//                            .create()
//                            .show();
//                }
//            });

            downloadsComplete = new DownloadsCompletedFragment();

           // downloadsComplete.setOnNumDownloadsCompletedChangeListener(this);

            getFragmentManager().beginTransaction().add(pager.getId(), downloadsComplete,
                    COMPLETE).commit();

        }

        return view;
    }

    private void deleteAll() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                if (new File(dir, child).delete()) {
                    //nada
                }

            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pager.setCurrentItem(0);
    }

    @Override
    public void onBackpressed() {
        getBaseActivity().getBrowserManager().unhideCurrentWindow();
        getFragmentManager().beginTransaction().remove(this).commit();
    }



    class PagerAdapter extends androidx.viewpager.widget.PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return downloadsComplete;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }
    }
}
