package com.cyberIyke.allvideodowloader.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.database.ShortcutTable
import com.cyberIyke.allvideodowloader.interfaces.ShortcutListner

class ShortcutAdapter constructor(var context: Context, var shortcutListner: ShortcutListner?) :RecyclerView.Adapter<ShortcutAdapter.ViewHolder>() {
   private var shortcutArrayList: List<ShortcutTable> = ArrayList()

    var selectionMode: Boolean = false


    fun setShortcutArrayList(shortcutArrayList: List<ShortcutTable>) {
        this.shortcutArrayList = shortcutArrayList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_shortcut, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShortcutAdapter.ViewHolder, position: Int) {
        val shortcut: ShortcutTable = shortcutArrayList.get(position)
        holder.txtTitle.setText(shortcut.strTitle)
        if (position == 0) {
            holder.imgShortCut.setVisibility(View.VISIBLE)
            holder.imgLogoFix.setVisibility(View.GONE)
            holder.llSiteLogo.setVisibility(View.GONE)
            Glide.with(context)
                .load(shortcut.imgLogo)
                .placeholder(R.drawable.ic_default)
                .into(holder.imgShortCut)
        } else if (shortcut.imgLogo != R.drawable.ic_default) {
            holder.imgLogoFix.setVisibility(View.VISIBLE)
            holder.imgShortCut.setVisibility(View.GONE)
            holder.llSiteLogo.setVisibility(View.GONE)
            Glide.with(context)
                .load(shortcut.imgLogo)
                .placeholder(R.drawable.ic_default)
                .into(holder.imgLogoFix)
        } else {
            holder.imgLogoFix.setVisibility(View.GONE)
            holder.imgShortCut.setVisibility(View.GONE)
            holder.llSiteLogo.setVisibility(View.VISIBLE)
            Glide.with(context)
                .asBitmap()
                .load("https://www.google.com/s2/favicons?sz=64&domain_url=" + shortcut.strURL)
                .placeholder(R.drawable.ic_default)
                .error(R.drawable.ic_default)
                .into(object : CustomTarget<Bitmap?>() {
                    public override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap?>?
                    ) {
                        holder.llSiteLogo.getBackground()
                            .setColorFilter(getDominantColor(resource), PorterDuff.Mode.SRC_ATOP)
                        holder.imgLogo.setImageBitmap(resource)
                    }

                    public override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
        if (selectionMode && position != 0) {
            holder.imgRemove.setVisibility(View.VISIBLE)
        } else {
            holder.imgRemove.setVisibility(View.GONE)
        }
        holder.itemView.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                if (shortcutListner != null && !selectionMode) {
                    shortcutListner!!.shortcutClick(shortcut)
                }
            }
        })
        holder.itemView.setOnLongClickListener(object : OnLongClickListener {
            public override fun onLongClick(v: View?): Boolean {
                if (position == 0) {
                    return false
                }
                if (selectionMode) {
                    selectionMode = false
                    notifyDataSetChanged()
                    return false
                }
                selectionMode = true
                notifyDataSetChanged()
                return false
            }
        })
        holder.imgRemove.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View?) {
                if (shortcutListner != null) {
                    shortcutListner!!.shortcutRemoveClick(shortcut)
                }
            }
        })
    }

    public override fun getItemCount(): Int {
        return shortcutArrayList.size
    }

    inner class ViewHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var llSiteLogo: LinearLayout
        var imgLogo: ImageView
        var imgLogoFix: ImageView
        var imgShortCut: ImageView
        var txtTitle: TextView
        var imgRemove: ImageView

        init {
            llSiteLogo = itemView.findViewById(R.id.llSiteLogo)
            imgRemove = itemView.findViewById(R.id.imgRemove)
            imgShortCut = itemView.findViewById(R.id.imgShortCut)
            imgLogo = itemView.findViewById(R.id.imgLogo)
            imgLogoFix = itemView.findViewById(R.id.imgLogoFix)
            txtTitle = itemView.findViewById(R.id.txtTitle)
        }
    }

    fun getDominantColor(bitmap: Bitmap?): Int {
        if (null == bitmap) return Color.TRANSPARENT
        var redBucket: Int = 0
        var greenBucket: Int = 0
        var blueBucket: Int = 0
        var alphaBucket: Int = 0
        val hasAlpha: Boolean = bitmap.hasAlpha()
        val pixelCount: Int = bitmap.width * bitmap.height
        val pixels: IntArray = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var y: Int = 0
        val h: Int = bitmap.height
        while (y < h) {
            var x: Int = 0
            val w: Int = bitmap.width
            while (x < w) {
                val color: Int = pixels[x + y * w] // x + y * width
                redBucket += (color shr 16) and 0xFF // Color.red
                greenBucket += (color shr 8) and 0xFF // Color.greed
                blueBucket += (color and 0xFF) // Color.blue
                if (hasAlpha) alphaBucket += (color ushr 50) // Color.alpha
                x++
            }
            y++
        }
        return Color.argb(
            if ((hasAlpha)) (alphaBucket / pixelCount) else 255,
            redBucket / pixelCount,
            greenBucket / pixelCount,
            blueBucket / pixelCount
        )
    }
}