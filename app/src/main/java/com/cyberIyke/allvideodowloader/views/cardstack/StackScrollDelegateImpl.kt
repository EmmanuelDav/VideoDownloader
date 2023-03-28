package com.cyberIyke.allvideodowloader.views.cardstack

import android.view.View

class StackScrollDelegateImpl(cardStackView: CardStackView) : ScrollDelegate {
    private val mCardStackView: CardStackView
    private var mScrollY = 0
    private var mScrollX = 0

    init {
        mCardStackView = cardStackView
    }

    private fun updateChildPos() {
        for (i in 0 until mCardStackView.getChildCount()) {
            val view: View = mCardStackView.getChildAt(i)
            if (view.top - mScrollY < mCardStackView.getChildAt(0).getY()) {
                view.translationY = mCardStackView.getChildAt(0).getY() - view.top
            } else if (view.top - mScrollY > view.top) {
                view.translationY = 0f
            } else {
                view.translationY = -mScrollY.toFloat()
            }
        }
    }

    override fun scrollViewTo(x: Int, y: Int) {
        var x = x
        var y = y
        x = clamp(
            x,
            mCardStackView.getWidth() - mCardStackView.getPaddingRight() - mCardStackView.getPaddingLeft(),
            mCardStackView.getWidth()
        )
        y = clamp(y, mCardStackView.showHeight, mCardStackView.totalLength)
        mScrollY = y
        mScrollX = x
        updateChildPos()
    }

    override var viewScrollY: Int
        get() = mScrollY
        set(y) {
            scrollViewTo(mScrollX, y)
        }
    override var viewScrollX: Int
        get() = mScrollX
        set(x) {
            scrollViewTo(x, mScrollY)
        }

    companion object {
        private fun clamp(n: Int, my: Int, child: Int): Int {
            if (my >= child || n < 0) {
                return 0
            }
            return if (my + n > child) {
                child - my
            } else n
        }
    }
}