package com.cyberIyke.allvideodowloader.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.cyberIyke.allvideodowloader.R

class Badge : AppCompatTextView {
    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
    }

    var number: Int
        get() = text.toString().toInt()
        set(number) {
            text = number.toString()
        }

    fun tabSelected(isSelected: Boolean) {
        background = if (isSelected) {
            setTextColor(ContextCompat.getColor(context, R.color.white))
            ContextCompat.getDrawable(context, R.drawable.ic_tab_selected)
        } else {
            setTextColor(ContextCompat.getColor(context, R.color.text_2))
            ContextCompat.getDrawable(context, R.drawable.ic_tab_unselected)
        }
    }

    fun hide() {
        visibility = GONE
    }

    fun show() {
        visibility = VISIBLE
    }
}