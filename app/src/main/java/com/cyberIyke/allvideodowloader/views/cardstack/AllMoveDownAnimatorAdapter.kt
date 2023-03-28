package com.cyberIyke.allvideodowloader.views.cardstack

import android.animation.ObjectAnimator
import android.view.View

class AllMoveDownAnimatorAdapter(cardStackView: CardStackView?) : AnimatorAdapter(cardStackView!!) {
     override fun itemExpandAnimatorSet(viewHolder: CardStackView.ViewHolder, position: Int) {
        val itemView = viewHolder.itemView
        itemView.clearAnimation()
        val oa = ObjectAnimator.ofFloat(
            itemView,
            View.Y,
            itemView.y,
            mCardStackView.scrollY + mCardStackView.paddingTop.toFloat()
        )
        mSet!!.play(oa)
        var collapseShowItemCount = 0
        for (i in 0 until mCardStackView.childCount) {
            var childTop: Int
            if (i == mCardStackView.selectPosition) continue
            val child: View = mCardStackView.getChildAt(i)
            child.clearAnimation()
            if (i > mCardStackView.selectPosition && collapseShowItemCount < mCardStackView.numBottomShow) {
                childTop =
                    mCardStackView.showHeight - getCollapseStartTop(collapseShowItemCount) + mCardStackView.getScrollY()
                val oAnim = ObjectAnimator.ofFloat(child, View.Y, child.y, childTop.toFloat())
                mSet!!.play(oAnim)
                collapseShowItemCount++
            } else {
                val oAnim = ObjectAnimator.ofFloat(
                    child,
                    View.Y,
                    child.y,
                    mCardStackView.showHeight + mCardStackView.scrollY.toFloat()
                )
                mSet!!.play(oAnim)
            }
        }
    }

     override fun itemCollapseAnimatorSet(viewHolder: CardStackView.ViewHolder?) {
        var childTop: Int = mCardStackView.getPaddingTop()
        for (i in 0 until mCardStackView.getChildCount()) {
            val child: View = mCardStackView.getChildAt(i)
            child.clearAnimation()
            val lp = child.layoutParams as CardStackView.LayoutParams
            childTop += lp.topMargin
            if (i != 0) {
                childTop -= mCardStackView.overlapGaps * 2
                val oAnim = ObjectAnimator.ofFloat(child, View.Y, child.y, childTop.toFloat())
                mSet!!.play(oAnim)
            } else {
                val oAnim = ObjectAnimator.ofFloat(child, View.Y, child.y, childTop.toFloat())
                mSet!!.play(oAnim)
            }
            childTop += lp.mHeaderHeight
        }
    }
}