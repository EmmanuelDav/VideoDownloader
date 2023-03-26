package com.cyberIyke.allvideodowloader.browser

import android.content.*
import android.view.View.OnTouchListener
import android.webkit.WebView

class TouchableWebView constructor(context: Context?, attrs: AttributeSet?) : WebView(
    (context)!!, attrs
), OnTouchListener {
    var clickX: Float = 0f
        private set
    var clickY: Float = 0f
        private set

    init {
        setOnTouchListener(this)
    }

    public override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            clickX = event.getX()
            clickY = event.getY()
        }
        return false
    }
}