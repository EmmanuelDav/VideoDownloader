package com.cyberIyke.allvideodowloader.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberIyke.allvideodowloader.R;
import com.cyberIyke.allvideodowloader.webservice.Result;

import java.util.ArrayList;
import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
    List<Result> resultList = new ArrayList<>();
    SuggetionListner suggetionListner;

    public SuggestionAdapter(SuggetionListner suggetionListner) {
        this.suggetionListner = suggetionListner;
    }

    public void setResultList(List<Result> resultList) {
        this.resultList = resultList;
        if (this.resultList == null){
            this.resultList = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_suggestion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Result result = resultList.get(position);

        if (result != null && result.getKey() != null) {
            holder.txtTitle.setText(result.getKey());
            String strSubTitle = "Search for \"" + result.getKey() + "\"";
            holder.txtSubTitle.setText(strSubTitle);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (suggetionListner != null) {
                        suggetionListner.onSuggetion(result.getKey());
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return resultList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtSubTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtSubTitle = itemView.findViewById(R.id.txtSubTitle);
        }
    }

    public interface SuggetionListner {
        void onSuggetion(String str);
    }
}
