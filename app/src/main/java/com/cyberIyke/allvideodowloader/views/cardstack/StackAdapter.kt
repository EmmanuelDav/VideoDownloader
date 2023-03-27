package com.cyberIyke.allvideodowloader.views.cardstack

import android.content.Context
import android.view.LayoutInflater
import com.cyberIyke.allvideodowloader.browser.BrowserWindow

abstract class StackAdapter<T>(val context: Context?) :
    CardStackView.Adapter<CardStackView.ViewHolder?>() {
    val layoutInflater: LayoutInflater
    private val mData: MutableList<T>

    init {
        layoutInflater = LayoutInflater.from(context)
        mData = ArrayList<Any?>()
    }

    fun updateData(data: List<T>?) {
        setData(data)
        notifyDataSetChanged()
    }

    fun setData(data: MutableList<BrowserWindow?>?) {
        mData.clear()
        if (data != null) {
            mData.addAll(data)
        }
        notifyDataSetChanged()
    }

    public override fun onBindViewHolder(holder: CardStackView.ViewHolder?, position: Int) {
        val data = getItem(position)
        bindView(data, position, holder)
    }

    abstract fun bindView(data: T, position: Int, holder: CardStackView.ViewHolder?)
    override val itemCount: Int
        get() = mData.size

    fun getItem(position: Int): T {
        return mData[position]
    }
}