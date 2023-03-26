package com.cyberIyke.allvideodowloader.views

import android.content.*
import android.os.*
import android.util.AttributeSet
import android.view.*
import android.view.View.OnClickListener
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import com.cyberIyke.allvideodowloader.R
import java.util.*

class CustomMediaController : FrameLayout {
    private var mPlayer: MediaPlayerControl? = null
    private var mContext: Context? = null
    private var mProgress: ProgressBar? = null
    private var mEndTime: TextView? = null
    private var mCurrentTime: TextView? = null
    private var mTitle: TextView? = null
    var isShowing = true
        private set
    private var mDragging = false
    private var mScalable = false
    var isFullScreen = false
        private set
    var mFormatBuilder: StringBuilder? = null
    var mFormatter: Formatter? = null
    private var mTurnButton: ImageButton? = null
    private var mScaleButton: ImageButton? = null
    private var mBackButton: View? = null
    private var loadingLayout: ViewGroup? = null
    private var errorLayout: ViewGroup? = null
    private var mTitleLayout: View? = null
    private var mControlLayout: View? = null
    private var mCenterPlayButton: View? = null

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        mContext = context
        val a = mContext!!.obtainStyledAttributes(attrs, R.styleable.CustomMediaController)
        mScalable = a.getBoolean(R.styleable.CustomMediaController_uvv_scalable, false)
        a.recycle()
        init(context)
    }

    constructor(context: Context?) : super(context!!) {
        init(context)
    }

    private fun init(context: Context?) {
        mContext = context
        val inflater =
            mContext!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val viewRoot = inflater.inflate(R.layout.video_player_controller_lay, this)
        viewRoot.setOnTouchListener(mTouchListener)
        initControllerView(viewRoot)
    }

    private fun initControllerView(v: View) {
        mTitleLayout = v.findViewById(R.id.title_part)
        mControlLayout = v.findViewById(R.id.control_layout)
        loadingLayout = v.findViewById<View>(R.id.loading_layout) as ViewGroup
        errorLayout = v.findViewById<View>(R.id.error_layout) as ViewGroup
        mTurnButton = v.findViewById<View>(R.id.turn_button) as ImageButton
        mScaleButton = v.findViewById<View>(R.id.scale_button) as ImageButton
        mCenterPlayButton = v.findViewById(R.id.center_play_btn)
        mBackButton = v.findViewById(R.id.back_btn)
        if (mTurnButton != null) {
            mTurnButton!!.requestFocus()
            mTurnButton!!.setOnClickListener(mPauseListener)
        }
        if (mScalable) {
            if (mScaleButton != null) {
                mScaleButton!!.visibility = VISIBLE
                mScaleButton!!.setOnClickListener(mScaleListener)
            }
        } else {
            if (mScaleButton != null) {
                mScaleButton!!.visibility = GONE
            }
        }
        if (mCenterPlayButton != null) {
            mCenterPlayButton!!.setOnClickListener(mCenterPlayListener)
        }
        if (mBackButton != null) {
            mBackButton!!.setOnClickListener(mBackListener)
        }
        val bar = v.findViewById<View>(R.id.seekbar)
        mProgress = bar as ProgressBar
        if (mProgress != null) {
            if (mProgress is SeekBar) {
                val seeker = mProgress as SeekBar
                seeker.setOnSeekBarChangeListener(mSeekListener)
            }
            mProgress!!.max = 1000
        }
        mEndTime = v.findViewById<View>(R.id.duration) as TextView
        mCurrentTime = v.findViewById<View>(R.id.has_played) as TextView
        mTitle = v.findViewById<View>(R.id.title) as TextView
        mFormatBuilder = StringBuilder()
        mFormatter = Formatter(mFormatBuilder, Locale.getDefault())
    }

    fun setMediaPlayer(player: MediaPlayerControl?) {
        mPlayer = player
        updatePausePlay()
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private fun disableUnsupportedButtons() {
        try {
            if (mTurnButton != null && mPlayer != null && !mPlayer!!.canPause()) {
                mTurnButton!!.isEnabled = false
            }
        } catch (ex: IncompatibleClassChangeError) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }
    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     *
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    @JvmOverloads
    fun show(timeout: Int = CustomMediaController.Companion.S_DEFAULT_TIMEOUT) { //只负责上下两条bar的显示,不负责中央loading,error,playBtn的显示.
        if (!isShowing) {
            setProgress()
            if (mTurnButton != null) {
                mTurnButton!!.requestFocus()
            }
            disableUnsupportedButtons()
            isShowing = true
        }
        updatePausePlay()
        updateBackButton()
        if (visibility != VISIBLE) {
            visibility = VISIBLE
        }
        if (mTitleLayout!!.visibility != VISIBLE) {
            mTitleLayout!!.visibility = VISIBLE
        }
        if (mControlLayout!!.visibility != VISIBLE) {
            mControlLayout!!.visibility = VISIBLE
        }

        // cause the progress bar to be updated even if mShowing
        // was already true. This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(CustomMediaController.Companion.SHOW_PROGRESS)
        val msg = mHandler.obtainMessage(CustomMediaController.Companion.FADE_OUT)
        if (timeout != 0) {
            mHandler.removeMessages(CustomMediaController.Companion.FADE_OUT)
            mHandler.sendMessageDelayed(msg, timeout.toLong())
        }
    }

    fun hide() {
        if (isShowing) {
            mHandler.removeMessages(CustomMediaController.Companion.SHOW_PROGRESS)
            mTitleLayout!!.visibility = GONE
            mControlLayout!!.visibility = GONE
            isShowing = false
        }
    }

    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            var msg = msg
            val pos: Int
            when (msg.what) {
                CustomMediaController.Companion.FADE_OUT -> hide()
                CustomMediaController.Companion.SHOW_PROGRESS -> {
                    pos = setProgress()
                    if (!mDragging && isShowing && mPlayer != null && mPlayer!!.isPlaying) {
                        msg = obtainMessage(CustomMediaController.Companion.SHOW_PROGRESS)
                        sendMessageDelayed(msg, 1000L - pos % 1000)
                    }
                }
                CustomMediaController.Companion.SHOW_LOADING -> {
                    show()
                    showCenterView(R.id.loading_layout)
                }
                CustomMediaController.Companion.SHOW_COMPLETE -> showCenterView(R.id.center_play_btn)
                CustomMediaController.Companion.SHOW_ERROR -> {
                    show()
                    showCenterView(R.id.error_layout)
                }
                CustomMediaController.Companion.HIDE_LOADING, CustomMediaController.Companion.HIDE_ERROR, CustomMediaController.Companion.HIDE_COMPLETE -> {
                    hide()
                    hideCenterView()
                }
                else -> {}
            }
        }
    }

    private fun showCenterView(resId: Int) {
        if (resId == R.id.loading_layout) {
            if (loadingLayout!!.visibility != VISIBLE) {
                loadingLayout!!.visibility = VISIBLE
            }
            if (mCenterPlayButton!!.visibility == VISIBLE) {
                mCenterPlayButton!!.visibility = GONE
            }
            if (errorLayout!!.visibility == VISIBLE) {
                errorLayout!!.visibility = GONE
            }
        } else if (resId == R.id.center_play_btn) {
            if (mCenterPlayButton!!.visibility != VISIBLE) {
                mCenterPlayButton!!.visibility = VISIBLE
            }
            if (loadingLayout!!.visibility == VISIBLE) {
                loadingLayout!!.visibility = GONE
            }
            if (errorLayout!!.visibility == VISIBLE) {
                errorLayout!!.visibility = GONE
            }
        } else if (resId == R.id.error_layout) {
            if (errorLayout!!.visibility != VISIBLE) {
                errorLayout!!.visibility = VISIBLE
            }
            if (mCenterPlayButton!!.visibility == VISIBLE) {
                mCenterPlayButton!!.visibility = GONE
            }
            if (loadingLayout!!.visibility == VISIBLE) {
                loadingLayout!!.visibility = GONE
            }
        }
    }

    private fun hideCenterView() {
        if (mCenterPlayButton!!.visibility == VISIBLE) {
            mCenterPlayButton!!.visibility = GONE
        }
        if (errorLayout!!.visibility == VISIBLE) {
            errorLayout!!.visibility = GONE
        }
        if (loadingLayout!!.visibility == VISIBLE) {
            loadingLayout!!.visibility = GONE
        }
    }

    fun reset() {
        mCurrentTime!!.text = "00:00"
        mEndTime!!.text = "00:00"
        mProgress!!.progress = 0
        mTurnButton!!.setImageResource(R.drawable.ic_play)
        visibility = VISIBLE
        hideLoading()
    }

    private fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        mFormatBuilder!!.setLength(0)
        return if (hours > 0) {
            mFormatter!!.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter!!.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    private fun setProgress(): Int {
        if (mPlayer == null || mDragging) {
            return 0
        }
        val position = mPlayer!!.currentPosition
        val duration = mPlayer!!.duration
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                val pos = 1000L * position / duration
                mProgress!!.progress = pos.toInt()
            }
            val percent = mPlayer!!.bufferPercentage
            mProgress!!.secondaryProgress = percent * 10
        }
        if (mEndTime != null) mEndTime!!.text = stringForTime(duration)
        if (mCurrentTime != null) mCurrentTime!!.text = stringForTime(position)
        return position
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                show(0) // show until hide is called
                handled = false
            }
            MotionEvent.ACTION_UP -> if (!handled) {
                handled = false
                show(CustomMediaController.Companion.S_DEFAULT_TIMEOUT) // start timeout
            }
            MotionEvent.ACTION_CANCEL -> hide()
            else -> {}
        }
        return true
    }

    var handled = false
    private val mTouchListener: OnTouchListener = object : OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN && isShowing) {
                hide()
                handled = true
                return true
            }
            return false
        }
    }

    override fun onTrackballEvent(ev: MotionEvent): Boolean {
        show(CustomMediaController.Companion.S_DEFAULT_TIMEOUT)
        return false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val uniqueDown = (event.repeatCount == 0
                && event.action == KeyEvent.ACTION_DOWN)
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume()
                show(CustomMediaController.Companion.S_DEFAULT_TIMEOUT)
                if (mTurnButton != null) {
                    mTurnButton!!.requestFocus()
                }
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer!!.isPlaying) {
                mPlayer!!.start()
                updatePausePlay()
                show(CustomMediaController.Companion.S_DEFAULT_TIMEOUT)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
            || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
        ) {
            if (uniqueDown && mPlayer!!.isPlaying) {
                mPlayer!!.pause()
                updatePausePlay()
                show(CustomMediaController.Companion.S_DEFAULT_TIMEOUT)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event)
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide()
            }
            return true
        }
        show(CustomMediaController.Companion.S_DEFAULT_TIMEOUT)
        return super.dispatchKeyEvent(event)
    }

    private val mPauseListener = OnClickListener {
        if (mPlayer != null) {
            doPauseResume()
            show(CustomMediaController.Companion.S_DEFAULT_TIMEOUT)
        }
    }
    private val mScaleListener: OnClickListener = object : OnClickListener {
        override fun onClick(v: View) {
            isFullScreen = !isFullScreen
            updateScaleButton()
            updateBackButton()
            mPlayer!!.setFullscreen(isFullScreen)
        }
    }
    private val mBackListener: OnClickListener = object : OnClickListener {
        override fun onClick(v: View) {
            if (isFullScreen) {
                isFullScreen = false
                updateScaleButton()
                updateBackButton()
                mPlayer!!.setFullscreen(false)
            }
        }
    }
    private val mCenterPlayListener = OnClickListener {
        hideCenterView()
        mPlayer!!.start()
    }

    private fun updatePausePlay() {
        if (mPlayer != null && mPlayer!!.isPlaying) {
            mTurnButton!!.setImageResource(R.drawable.ic_play)
        } else {
            mTurnButton!!.setImageResource(R.drawable.ic_stop)
        }
    }

    fun updateScaleButton() {
        if (isFullScreen) {
            mScaleButton!!.setImageResource(R.drawable.ic_zoom_in)
        } else {
            mScaleButton!!.setImageResource(R.drawable.ic_zoom_out)
        }
    }

    fun toggleButtons(isFullScreen: Boolean) {
        this.isFullScreen = isFullScreen
        updateScaleButton()
        updateBackButton()
    }

    fun updateBackButton() {
        mBackButton!!.visibility = if (isFullScreen) VISIBLE else INVISIBLE
    }

    private fun doPauseResume() {
        if (mPlayer!!.isPlaying) {
            mPlayer!!.pause()
        } else {
            mPlayer!!.start()
        }
        updatePausePlay()
    }

    private val mSeekListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        var newPosition = 0
        var change = false
        override fun onStartTrackingTouch(bar: SeekBar) {
            if (mPlayer == null) {
                return
            }
            show(3600000)
            mDragging = true
            mHandler.removeMessages(CustomMediaController.Companion.SHOW_PROGRESS)
        }

        override fun onProgressChanged(bar: SeekBar, progress: Int, fromuser: Boolean) {
            if (mPlayer == null || !fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return
            }
            val duration = mPlayer!!.duration.toLong()
            val newposition = duration * progress / 1000L
            newPosition = newposition.toInt()
            change = true
        }

        override fun onStopTrackingTouch(bar: SeekBar) {
            if (mPlayer == null) {
                return
            }
            if (change) {
                mPlayer!!.seekTo(newPosition)
                if (mCurrentTime != null) {
                    mCurrentTime!!.text = stringForTime(newPosition)
                }
            }
            mDragging = false
            setProgress()
            updatePausePlay()
            show(CustomMediaController.Companion.S_DEFAULT_TIMEOUT)

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            isShowing = true
            mHandler.sendEmptyMessage(CustomMediaController.Companion.SHOW_PROGRESS)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        if (mTurnButton != null) {
            mTurnButton!!.isEnabled = enabled
        }
        if (mProgress != null) {
            mProgress!!.isEnabled = enabled
        }
        if (mScalable) {
            mScaleButton!!.isEnabled = enabled
        }
        mBackButton!!.isEnabled = true
    }

    fun showLoading() {
        mHandler.sendEmptyMessage(CustomMediaController.Companion.SHOW_LOADING)
    }

    fun hideLoading() {
        mHandler.sendEmptyMessage(CustomMediaController.Companion.HIDE_LOADING)
    }

    fun showError() {
        mHandler.sendEmptyMessage(CustomMediaController.Companion.SHOW_ERROR)
    }

    fun hideError() {
        mHandler.sendEmptyMessage(CustomMediaController.Companion.HIDE_ERROR)
    }

    fun showComplete() {
        mHandler.sendEmptyMessage(CustomMediaController.Companion.SHOW_COMPLETE)
    }

    fun hideComplete() {
        mHandler.sendEmptyMessage(CustomMediaController.Companion.HIDE_COMPLETE)
    }

    fun setTitle(titile: String?) {
        mTitle!!.text = titile
    }

    fun setFullscreenEnabled() {
        mScaleButton!!.visibility = if (isFullScreen) VISIBLE else GONE
    }

    fun setOnErrorView(resId: Int) {
        errorLayout!!.removeAllViews()
        val inflater = LayoutInflater.from(mContext)
        inflater.inflate(resId, errorLayout, true)
    }

    fun setOnErrorView(onErrorView: View?) {
        errorLayout!!.removeAllViews()
        errorLayout!!.addView(onErrorView)
    }

    fun setOnLoadingView(resId: Int) {
        loadingLayout!!.removeAllViews()
        val inflater = LayoutInflater.from(mContext)
        inflater.inflate(resId, loadingLayout, true)
    }

    fun setOnLoadingView(onLoadingView: View?) {
        loadingLayout!!.removeAllViews()
        loadingLayout!!.addView(onLoadingView)
    }

    fun setOnErrorViewClick(onClickListener: OnClickListener?) {
        errorLayout!!.setOnClickListener(onClickListener)
    }

    interface MediaPlayerControl {
        fun start()
        fun pause()
        val duration: Int
        val currentPosition: Int
        fun seekTo(pos: Int)
        val isPlaying: Boolean
        val bufferPercentage: Int
        fun canPause(): Boolean
        fun canSeekBackward(): Boolean
        fun canSeekForward(): Boolean
        fun closePlayer()
        fun setFullscreen(fullscreen: Boolean)

        /***
         *
         * @param fullscreen
         * @param screenOrientation valid only fullscreen=true.values should be one of
         * ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
         * ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
         * ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
         * ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
         */
        fun setFullscreen(fullscreen: Boolean, screenOrientation: Int)
    }

    companion object {
        private const val S_DEFAULT_TIMEOUT = 3000
        private const val FADE_OUT = 1
        private const val SHOW_PROGRESS = 2
        private const val SHOW_LOADING = 3
        private const val HIDE_LOADING = 4
        private const val SHOW_ERROR = 5
        private const val HIDE_ERROR = 6
        private const val SHOW_COMPLETE = 7
        private const val HIDE_COMPLETE = 8
    }
}