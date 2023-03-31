package com.cyberIyke.allvideodowloader.views.cardstack

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.view.animation.AccelerateDecelerateInterpolator

abstract class AnimatorAdapter(protected var mCardStackView: CardStackView) {
    protected var mSet: AnimatorSet? = null
    private fun initAnimatorSet() {
        mSet = AnimatorSet()
        mSet!!.interpolator = AccelerateDecelerateInterpolator()
        mSet!!.duration = duration.toLong()
    }

    fun itemClick(viewHolder: CardStackView.ViewHolder, position: Int) {
        if (mSet != null && mSet!!.isRunning) return
        initAnimatorSet()
        if (mCardStackView.selectPosition == position) {
            onItemCollapse(viewHolder)
        } else {
            onItemExpand(viewHolder, position)
        }
        if (mCardStackView.childCount == 1) mSet!!.end()
    }

    protected abstract fun itemExpandAnimatorSet(
        viewHolder: CardStackView.ViewHolder,
        position: Int
    )

    protected abstract fun itemCollapseAnimatorSet(viewHolder: CardStackView.ViewHolder?)
    private fun onItemExpand(viewHolder: CardStackView.ViewHolder, position: Int) {
        val preSelectPosition = mCardStackView.selectPosition
        val preSelectViewHolder = mCardStackView.getViewHolder(preSelectPosition)
        preSelectViewHolder?.onItemExpand(false)
        mCardStackView.selectPosition = position
        itemExpandAnimatorSet(viewHolder, position)
        mSet!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                mCardStackView.setScrollEnable(false)
                preSelectViewHolder?.onAnimationStateChange(
                    CardStackView.Companion.ANIMATION_STATE_START,
                    false
                )
                viewHolder.onAnimationStateChange(
                    CardStackView.Companion.ANIMATION_STATE_START,
                    true
                )
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                viewHolder.onItemExpand(true)
                preSelectViewHolder?.onAnimationStateChange(
                    CardStackView.Companion.ANIMATION_STATE_END,
                    false
                )
                viewHolder.onAnimationStateChange(CardStackView.Companion.ANIMATION_STATE_END, true)
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                preSelectViewHolder?.onAnimationStateChange(
                    CardStackView.Companion.ANIMATION_STATE_CANCEL,
                    false
                )
                viewHolder.onAnimationStateChange(
                    CardStackView.Companion.ANIMATION_STATE_CANCEL,
                    true
                )
            }
        })
        mSet!!.start()
    }

    private fun onItemCollapse(viewHolder: CardStackView.ViewHolder) {
        itemCollapseAnimatorSet(viewHolder)
        mSet!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                viewHolder.onItemExpand(false)
                mCardStackView.setScrollEnable(true)
                viewHolder.onAnimationStateChange(
                    CardStackView.Companion.ANIMATION_STATE_START,
                    false
                )
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                mCardStackView.selectPosition = CardStackView.Companion.DEFAULT_SELECT_POSITION
                viewHolder.onAnimationStateChange(
                    CardStackView.Companion.ANIMATION_STATE_END,
                    false
                )
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                viewHolder.onAnimationStateChange(
                    CardStackView.Companion.ANIMATION_STATE_CANCEL,
                    false
                )
            }
        })
        mSet!!.start()
    }

    protected fun getCollapseStartTop(collapseShowItemCount: Int): Int {
        return (mCardStackView.overlapGapsCollapse
                * (mCardStackView.numBottomShow - collapseShowItemCount - (mCardStackView.numBottomShow - if (mCardStackView.childCount - mCardStackView.selectPosition > mCardStackView.numBottomShow) mCardStackView.numBottomShow else mCardStackView.childCount - mCardStackView.selectPosition - 1)))
    }

    val duration: Int
        get() = mCardStackView.duration

    companion object {
        const val ANIMATION_DURATION = 400
    }
}