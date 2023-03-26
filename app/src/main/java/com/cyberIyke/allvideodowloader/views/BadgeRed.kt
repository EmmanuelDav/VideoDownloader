package com.cyberIyke.allvideodowloader.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class BadgeRed : AppCompatTextView {
    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
    }

    fun setNumber(number: Int) {
        if (number > 0) {
            show()
        } else {
            hide()
        }
        text = number.toString()
    }

    fun hide() {
        visibility = GONE
    }

    fun show() {
        visibility = VISIBLE
    }
}