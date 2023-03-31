package com.cyberIyke.allvideodowloader.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.webservice.Result

class SuggestionAdapter constructor(var suggetionListner: SuggetionListner?) :
    RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    private var resultList: List<Result>? = ArrayList()

    fun resultList(resultList: List<Result>?) {
        this.resultList = resultList
        if (this.resultList == null) {
            this.resultList = ArrayList()
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SuggestionAdapter.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_suggestion, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SuggestionAdapter.ViewHolder, position: Int) {
        val result: Result = resultList!!.get(position)
        if (result.key != null) {
            holder.txtTitle.text = result.key
            val strSubTitle: String = "Search for \"" + result.key + "\""
            holder.txtSubTitle.text = strSubTitle
            holder.itemView.setOnClickListener {
                if (suggetionListner != null) {
                    suggetionListner!!.onSuggetion(result.key)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return resultList!!.size
    }

    inner class ViewHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var txtTitle: TextView
        var txtSubTitle: TextView

        init {
            txtTitle = itemView.findViewById(R.id.txtTitle)
            txtSubTitle = itemView.findViewById(R.id.txtSubTitle)
        }
    }

    open interface SuggetionListner {
        fun onSuggetion(str: String?)
    }
}