package com.cyberIyke.allvideodowloader.views.cardstack

class StackScrollDelegateImpl(private val mCardStackView: CardStackView) : ScrollDelegate {
    private var mScrollY = 0
    private var mScrollX = 0
    private fun updateChildPos() {
        for (i in 0 until mCardStackView.childCount) {
            val view = mCardStackView.getChildAt(i)
            if (view.top - mScrollY < mCardStackView.getChildAt(0).y) {
                view.translationY = mCardStackView.getChildAt(0).y - view.top
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
            mCardStackView.width - mCardStackView.paddingRight - mCardStackView.paddingLeft,
            mCardStackView.width
        )
        y = clamp(y, mCardStackView.showHeight, mCardStackView.totalLength)
        mScrollY = y
        mScrollX = x
        updateChildPos()
    }

    override fun setViewScrollY(y: Int) {
        scrollViewTo(mScrollX, y)
    }

    override fun setViewScrollX(x: Int) {
        scrollViewTo(x, mScrollY)
    }

    override fun getViewScrollY(): Int {
        return mScrollY
    }

    override fun getViewScrollX(): Int {
        return mScrollX
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