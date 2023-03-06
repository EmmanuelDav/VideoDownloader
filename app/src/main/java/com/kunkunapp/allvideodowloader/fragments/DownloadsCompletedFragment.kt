package com.kunkunapp.allvideodowloader.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.kunkunapp.allvideodowloader.R
import com.kunkunapp.allvideodowloader.adapters.DownloadsAdapter
import com.kunkunapp.allvideodowloader.viewModel.DownloadsViewModel
import kotlinx.android.synthetic.main.fragment_downloads_list.*
import kotlinx.android.synthetic.main.fragment_downloads_list.view.*


class DownloadsCompletedFragment :Fragment() {

    private lateinit var downloadsViewModel: DownloadsViewModel

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_downloads_list, container, false)
            with(view.list) {
                adapter = DownloadsAdapter()
            }
            downloadsViewModel = ViewModelProvider(this).get(DownloadsViewModel::class.java)
            downloadsViewModel.allDownloads.observe(viewLifecycleOwner, Observer { downloads ->
                if (downloads.isEmpty()) {
                    llNoHistory.visibility = View.VISIBLE
                    list.visibility  = View.GONE
                } else{
                    list.visibility  = View.VISIBLE
                    llNoHistory.visibility = View.GONE
                }
                downloads?.let { (view.list.adapter as DownloadsAdapter).addItems(downloads) }
            })
        return view
    }
}