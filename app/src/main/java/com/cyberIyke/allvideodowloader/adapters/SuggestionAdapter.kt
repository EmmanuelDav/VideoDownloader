package com.cyberIyke.allvideodowloader.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cyberIyke.allvideodowloader.R

private class SuggestionAdapter constructor(var suggetionListner: SuggetionListner?) : RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    var resultList: List<Result>? = ArrayList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SuggestionAdapter.ViewHolder {
        return SuggestionAdapter.ViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false)
        )
    }

    public override fun onBindViewHolder(holder: SuggestionAdapter.ViewHolder, position: Int) {
        val result: Result = resultList!!.get(position)
        if (result != null && result.getKey() != null) {
            holder.txtTitle.setText(result.getKey())
            val strSubTitle: String = "Search for \"" + result.getKey() + "\""
            holder.txtSubTitle.setText(strSubTitle)
            holder.itemView.setOnClickListener(object : View.OnClickListener {
                public override fun onClick(v: View?) {
                    if (suggetionListner != null) {
                        suggetionListner!!.onSuggetion(result.getKey())
                    }
                }
            })
        }
    }

    public override fun getItemCount(): Int {
        return resultList!!.size
    }

    internal inner class ViewHolder constructor(itemView: View) :
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