package com.cyberIyke.allvideodowloader.helper

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.util.Log
import android.view.OrientationEventListener

class OrientationDetector constructor(private val context: Context) {
    private var orientationEventListener: OrientationEventListener? = null
    private var rotationThreshold: Int = 20
    private var holdingTime: Long = 0
    private var lastCalcTime: Long = 0
    private var lastDirection: OrientationDetector.Direction = OrientationDetector.Direction.PORTRAIT

    private var currentOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT //初始为竖屏
    private var listener: OrientationChangeListener? = null
    fun setOrientationChangeListener(listener: OrientationChangeListener?) {
        this.listener = listener
    }

    fun enable() {
        if (orientationEventListener == null) {
            orientationEventListener =
                object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI) {
                    public override fun onOrientationChanged(orientation: Int) {
                        val currDirection: OrientationDetector.Direction? =
                            calcDirection(orientation)
                        if (currDirection == null) {
                            return
                        }
                        if (currDirection != lastDirection) {
                            resetTime()
                            lastDirection = currDirection
                        } else {
                            calcHoldingTime()
                            if (holdingTime > OrientationDetector.Companion.HOLDING_THRESHOLD) {
                                if (currDirection == OrientationDetector.Direction.LANDSCAPE) {
                                    if (currentOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                        Log.d(
                                            OrientationDetector.Companion.TAG,
                                            "switch to SCREEN_ORIENTATION_LANDSCAPE"
                                        )
                                        currentOrientation =
                                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        if (listener != null) {
                                            listener!!.onOrientationChanged(
                                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                                                currDirection
                                            )
                                        }
                                    }
                                } else if (currDirection == OrientationDetector.Direction.PORTRAIT) {
                                    if (currentOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                                        Log.d(
                                            OrientationDetector.Companion.TAG,
                                            "switch to SCREEN_ORIENTATION_PORTRAIT"
                                        )
                                        currentOrientation =
                                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        if (listener != null) {
                                            listener!!.onOrientationChanged(
                                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                                                currDirection
                                            )
                                        }
                                    }
                                } else if (currDirection == OrientationDetector.Direction.REVERSE_PORTRAIT) {
                                    if (currentOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                                        Log.d(
                                            OrientationDetector.Companion.TAG,
                                            "switch to SCREEN_ORIENTATION_REVERSE_PORTRAIT"
                                        )
                                        currentOrientation =
                                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                                        if (listener != null) {
                                            listener!!.onOrientationChanged(
                                                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
                                                currDirection
                                            )
                                        }
                                    }
                                } else if (currDirection == OrientationDetector.Direction.REVERSE_LANDSCAPE && currentOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                                    Log.d(
                                        OrientationDetector.Companion.TAG,
                                        "switch to SCREEN_ORIENTATION_REVERSE_LANDSCAPE"
                                    )
                                    currentOrientation =
                                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                                    if (listener != null) {
                                        listener!!.onOrientationChanged(
                                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
                                            currDirection
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
        orientationEventListener!!.enable()
    }

    private fun calcHoldingTime() {
        val current: Long = System.currentTimeMillis()
        if (lastCalcTime == 0L) {
            lastCalcTime = current
        }
        holdingTime += current - lastCalcTime
        lastCalcTime = current
    }

    private fun resetTime() {
        lastCalcTime = 0
        holdingTime = lastCalcTime
    }

    private fun calcDirection(orientation: Int): OrientationDetector.Direction? {
        if ((orientation <= rotationThreshold
                    || orientation >= 360 - rotationThreshold)
        ) {
            return OrientationDetector.Direction.PORTRAIT
        } else if (Math.abs(orientation - 180) <= rotationThreshold) {
            return OrientationDetector.Direction.REVERSE_PORTRAIT
        } else if (Math.abs(orientation - 90) <= rotationThreshold) {
            return OrientationDetector.Direction.REVERSE_LANDSCAPE
        } else if (Math.abs(orientation - 270) <= rotationThreshold) {
            return OrientationDetector.Direction.LANDSCAPE
        }
        return null
    }

    fun setInitialDirection(direction: OrientationDetector.Direction) {
        lastDirection = direction
    }

    fun disable() {
        if (orientationEventListener != null) {
            orientationEventListener!!.disable()
        }
    }

    fun setThresholdDegree(degree: Int) {
        rotationThreshold = degree
    }

    open interface OrientationChangeListener {
        /***
         * @param screenOrientation ActivityInfo.SCREEN_ORIENTATION_PORTRAIT or ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
         * or ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE or ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
         * @param direction         PORTRAIT or REVERSE_PORTRAIT when screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
         * LANDSCAPE or REVERSE_LANDSCAPE when screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE.
         */
        fun onOrientationChanged(screenOrientation: Int, direction: OrientationDetector.Direction)
    }

    enum class Direction {
        PORTRAIT, REVERSE_PORTRAIT, LANDSCAPE, REVERSE_LANDSCAPE
    }

    companion object {
        private val TAG: String = "OrientationDetector"
        private val HOLDING_THRESHOLD: Int = 1500
    }
}