package com.cyberIyke.allvideodowloader.fragments.base;

import androidx.fragment.app.Fragment;

import com.cyberIyke.allvideodowloader.MyApp;
import com.cyberIyke.allvideodowloader.activities.MainActivity;

public class BaseFragment extends Fragment {

    public MainActivity getBaseActivity() {
        return (MainActivity) getActivity();
    }

    public MyApp getMyApp() {
        return (MyApp) getActivity().getApplication();
    }
}
