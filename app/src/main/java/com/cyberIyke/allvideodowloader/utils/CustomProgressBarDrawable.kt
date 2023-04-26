package com.cyberIyke.allvideodowloader.utils

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator


class CustomProgressBarDrawable(var color: Int) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var position = 0f
    private val dotSizeMultiplier = 4.5f
    private var animationFraction = 2f
    private val animator: ValueAnimator

    init {
        paint.color = color
        paint.style = Paint.Style.FILL
        animator = ValueAnimator.ofFloat(0f, 1f)
        animator.interpolator = LinearInterpolator()
        animator.duration = 1000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { valueAnimator ->
            animationFraction = valueAnimator.animatedValue as Float
            invalidateSelf()
        }
        animator.start()
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val barHeight = bounds.height() * 0.25f
        val barWidth = bounds.width() * 0.8f

        val progressPosition = bounds.left + bounds.width() * 0.2f
        position = if (animationFraction < 0.5f) {
            progressPosition + animationFraction * (bounds.width() * 0.6f * 2f)
        } else {
            progressPosition + (1f - animationFraction) * (bounds.width() * 0.6f * 2f)
        }

        // Draw the progress bar
        paint.color = Color.WHITE
        canvas.drawRect(
            progressPosition,
            bounds.centerY() - barHeight / 2f,
            progressPosition + bounds.width() * 0.6f,
            bounds.centerY() + barHeight / 2f,
            paint
        )

        // Draw the progress indicator as a dot
        paint.color = color
        canvas.drawCircle(
            position,
            bounds.centerY().toFloat(),
            barHeight * dotSizeMultiplier,
            paint
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }
}