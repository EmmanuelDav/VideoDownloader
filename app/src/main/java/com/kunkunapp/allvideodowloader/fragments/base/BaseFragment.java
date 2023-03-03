package com.kunkunapp.allvideodowloader.fragments.base;

import androidx.fragment.app.Fragment;

import com.kunkunapp.allvideodowloader.MyApp;
import com.kunkunapp.allvideodowloader.activities.MainActivity;

public class BaseFragment extends Fragment {

    public MainActivity getBaseActivity() {
        return (MainActivity) getActivity();
    }

    public MyApp getMyApp() {
        return (MyApp) getActivity().getApplication();
    }
}
