package com.kunkunapp.allvideodowloader.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.kunkunapp.allvideodowloader.R;
import com.kunkunapp.allvideodowloader.database.ShortcutTable;
import com.kunkunapp.allvideodowloader.interfaces.ShortcutListner;

import java.util.ArrayList;
import java.util.List;

public class ShortcutAdapter extends RecyclerView.Adapter<ShortcutAdapter.ViewHolder> {
    Context context;
    List<ShortcutTable> shortcutArrayList = new ArrayList<>();
    ShortcutListner shortcutListner;
    public boolean selectionMode = false;

    public ShortcutAdapter(Context context, ShortcutListner shortcutListner) {
        this.context = context;
        this.shortcutListner = shortcutListner;
    }

    public void setShortcutArrayList(List<ShortcutTable> shortcutArrayList) {
        this.shortcutArrayList = shortcutArrayList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_shortcut, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShortcutTable shortcut = shortcutArrayList.get(position);

        holder.txtTitle.setText(shortcut.strTitle);
        if (position == 0) {
            holder.imgShortCut.setVisibility(View.VISIBLE);
            holder.imgLogoFix.setVisibility(View.GONE);
            holder.llSiteLogo.setVisibility(View.GONE);

            Glide.with(context)
                    .load(shortcut.imgLogo)
                    .placeholder(R.drawable.ic_default)
                    .into(holder.imgShortCut);
        } else if (shortcut.imgLogo != R.drawable.ic_default) {
            holder.imgLogoFix.setVisibility(View.VISIBLE);
            holder.imgShortCut.setVisibility(View.GONE);
            holder.llSiteLogo.setVisibility(View.GONE);

            Glide.with(context)
                    .load(shortcut.imgLogo)
                    .placeholder(R.drawable.ic_default)
                    .into(holder.imgLogoFix);
        } else {
            holder.imgLogoFix.setVisibility(View.GONE);
            holder.imgShortCut.setVisibility(View.GONE);
            holder.llSiteLogo.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .asBitmap()
                    .load("https://www.google.com/s2/favicons?sz=64&domain_url=" + shortcut.strURL)
                    .placeholder(R.drawable.ic_default)
                    .error(R.drawable.ic_default)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            holder.llSiteLogo.getBackground().setColorFilter(getDominantColor(resource), PorterDuff.Mode.SRC_ATOP);
                            holder.imgLogo.setImageBitmap(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });

        }

        if (selectionMode && position != 0) {
            holder.imgRemove.setVisibility(View.VISIBLE);
        } else {
            holder.imgRemove.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shortcutListner != null && !selectionMode) {
                    shortcutListner.shortcutClick(shortcut);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (position == 0) {
                    return false;
                }
                if (selectionMode) {
                    selectionMode = false;
                    notifyDataSetChanged();
                    return false;
                }
                selectionMode = true;
                notifyDataSetChanged();
                return false;
            }
        });
        holder.imgRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shortcutListner != null) {
                    shortcutListner.shortcutRemoveClick(shortcut);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return shortcutArrayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llSiteLogo;
        ImageView imgLogo, imgLogoFix;
        ImageView imgShortCut;
        TextView txtTitle;
        ImageView imgRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            llSiteLogo = itemView.findViewById(R.id.llSiteLogo);
            imgRemove = itemView.findViewById(R.id.imgRemove);
            imgShortCut = itemView.findViewById(R.id.imgShortCut);
            imgLogo = itemView.findViewById(R.id.imgLogo);
            imgLogoFix = itemView.findViewById(R.id.imgLogoFix);
            txtTitle = itemView.findViewById(R.id.txtTitle);
        }
    }

    public int getDominantColor(Bitmap bitmap) {
        if (null == bitmap) return Color.TRANSPARENT;

        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        int alphaBucket = 0;

        boolean hasAlpha = bitmap.hasAlpha();
        int pixelCount = bitmap.getWidth() * bitmap.getHeight();
        int[] pixels = new int[pixelCount];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int y = 0, h = bitmap.getHeight(); y < h; y++) {
            for (int x = 0, w = bitmap.getWidth(); x < w; x++) {
                int color = pixels[x + y * w]; // x + y * width
                redBucket += (color >> 16) & 0xFF; // Color.red
                greenBucket += (color >> 8) & 0xFF; // Color.greed
                blueBucket += (color & 0xFF); // Color.blue
                if (hasAlpha) alphaBucket += (color >>> 50); // Color.alpha
            }
        }

        return Color.argb(
                (hasAlpha) ? (alphaBucket / pixelCount) : 255,
                redBucket / pixelCount,
                greenBucket / pixelCount,
                blueBucket / pixelCount);
    }

}
