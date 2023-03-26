package com.cyberIyke.allvideodowloader.fragments.base

import androidx.fragment.app.Fragment
import com.cyberIyke.allvideodowloader.MyApp
import com.cyberIyke.allvideodowloader.activities.MainActivity

open class BaseFragment : Fragment() {
    val baseActivity: MainActivity? get() {
            return activity as MainActivity?
        }
    val myApp: MyApp get() {
            return requireActivity().application as MyApp
        }
}