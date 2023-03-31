package com.cyberIyke.allvideodowloader.views

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.helper.OrientationDetector
import java.io.IOException

class CustomVideoView @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    SurfaceView(mContext, attrs, defStyleAttr), CustomMediaController.MediaPlayerControl,
    OrientationDetector.OrientationChangeListener {
    private val videoTag = "UniversalVideoView"

    // settable by the client
    private var mUri: Uri? = null

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private var mCurrentState = STATE_IDLE
    private var mTargetState = STATE_IDLE

    // All the stuff we need for playing and showing a video
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mAudioSession = 0
    private var mVideoWidth = 0
    private var mVideoHeight = 0
    private var mSurfaceWidth = 0
    private var mSurfaceHeight = 0
    private var mMediaController: CustomMediaController? = null
    private var mOnCompletionListener: OnCompletionListener? = null
    private var mOnPreparedListener: OnPreparedListener? = null
    private var mCurrentBufferPercentage = 0
    private var mOnErrorListener: OnErrorListener? = null
    private var mOnInfoListener: OnInfoListener? = null
    private var mSeekWhenPrepared // recording the seek position while preparing
            = 0
    private var mCanPause = false
    private var mCanSeekBack = false
    private var mCanSeekForward = false
    private var mPreparedBeforeStart = false
    private var mFitXY: Boolean
    private var mAutoRotation: Boolean
    private var mVideoViewLayoutWidth = 0
    private var mVideoViewLayoutHeight = 0
    private var mOrientationDetector: OrientationDetector? = null
    private var videoViewCallback: VideoViewCallback? = null
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mFitXY) {
            onMeasureFitXY(widthMeasureSpec, heightMeasureSpec)
        } else {
            onMeasureKeepAspectRatio(widthMeasureSpec, heightMeasureSpec)
        }
    }

    private fun onMeasureFitXY(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(mVideoWidth, widthMeasureSpec)
        val height = getDefaultSize(mVideoHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    private fun onMeasureKeepAspectRatio(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = getDefaultSize(mVideoWidth, widthMeasureSpec)
        var height = getDefaultSize(mVideoHeight, heightMeasureSpec)
        if (mVideoWidth > 0 && mVideoHeight > 0) {
            val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
            val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
            val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize
                height = heightSpecSize

                // for compatibility, we adjust size based on aspect ratio
                if (mVideoWidth * height < width * mVideoHeight) {
                    width = height * mVideoWidth / mVideoHeight
                } else if (mVideoWidth * height > width * mVideoHeight) {
                    height = width * mVideoHeight / mVideoWidth
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize
                height = width * mVideoHeight / mVideoWidth
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize
                width = height * mVideoWidth / mVideoHeight
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = mVideoWidth
                height = mVideoHeight
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize
                    width = height * mVideoWidth / mVideoHeight
                }
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize
                    height = width * mVideoHeight / mVideoWidth
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }
        setMeasuredDimension(width, height)
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = CustomVideoView::class.java.name
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            info.className = CustomVideoView::class.java.name
        }
    }

    fun resolveAdjustedSize(desiredSize: Int, measureSpec: Int): Int {
        return getDefaultSize(desiredSize, measureSpec)
    }

    private fun initVideoView() {
        mVideoWidth = 0
        mVideoHeight = 0
        holder.addCallback(mSHCallback)
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        mCurrentState = STATE_IDLE
        mTargetState = STATE_IDLE
    }

    override fun onOrientationChanged(screenOrientation: Int, direction: OrientationDetector.Direction) {
        if (!mAutoRotation) {
            return
        }
        if (direction === OrientationDetector.Direction.PORTRAIT) {
            setFullscreen(false, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        } else if (direction === OrientationDetector.Direction.REVERSE_PORTRAIT) {
            setFullscreen(false, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
        } else if (direction === OrientationDetector.Direction.LANDSCAPE) {
            setFullscreen(true, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        } else if (direction === OrientationDetector.Direction.REVERSE_LANDSCAPE) {
            setFullscreen(true, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
        }
    }

    fun setFitXY(fitXY: Boolean) {
        mFitXY = fitXY
    }

    fun setAutoRotation(auto: Boolean) {
        mAutoRotation = auto
    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    fun setVideoPath(path: String?) {
        setVideoURI(Uri.parse(path))
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    fun setVideoURI(uri: Uri?) {
        setVideoURI(uri, null)
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     * Note that the cross domain redirection is allowed by default, but that can be
     * changed with key/value pairs through the headers parameter with
     * "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     * to disallow or allow cross domain redirection.
     */
    fun setVideoURI(uri: Uri?, headers: Map<String?, String?>?) {
        mUri = uri
        mSeekWhenPrepared = 0
        openVideo()
        requestLayout()
        invalidate()
    }

    fun stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
            mMediaPlayer = null
            mCurrentState = STATE_IDLE
            mTargetState = STATE_IDLE
        }
    }

    private fun openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return
        }
        val am = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false)
        try {
            mMediaPlayer = MediaPlayer()
            if (mAudioSession != 0) {
                mMediaPlayer!!.audioSessionId = mAudioSession
            } else {
                mAudioSession = mMediaPlayer!!.audioSessionId
            }
            mMediaPlayer!!.setOnPreparedListener(mPreparedListener)
            mMediaPlayer!!.setOnVideoSizeChangedListener(mSizeChangedListener)
            mMediaPlayer!!.setOnCompletionListener(mCompletionListener)
            mMediaPlayer!!.setOnErrorListener(mErrorListener)
            mMediaPlayer!!.setOnInfoListener(mInfoListener)
            mMediaPlayer!!.setOnBufferingUpdateListener(mBufferingUpdateListener)
            mCurrentBufferPercentage = 0
            mMediaPlayer!!.setDataSource(mContext, mUri!!)
            mMediaPlayer!!.setDisplay(mSurfaceHolder)
            mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mMediaPlayer!!.setScreenOnWhilePlaying(true)
            mMediaPlayer!!.prepareAsync()


            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING
            attachMediaController()
        } catch (ex: IOException) {
            Log.w(videoTag, "Unable to open content: $mUri", ex)
            mCurrentState = STATE_ERROR
            mTargetState = STATE_ERROR
            mErrorListener.onError(mMediaPlayer, MEDIA_ERROR_UNKNOWN, 0)
        }
    }

    fun setMediaController(controller: CustomMediaController?) {
        mMediaController?.hide()
        mMediaController = controller
        attachMediaController()
    }

    private fun attachMediaController() {
        if (mMediaPlayer != null && mMediaController != null) {
            mMediaController!!.setMediaPlayer(this)
            mMediaController!!.setEnabled(isInPlaybackState)
            mMediaController!!.hide()
        }
    }

    var mSizeChangedListener: OnVideoSizeChangedListener =
        OnVideoSizeChangedListener { mp, width, height ->
            mVideoWidth = mp.videoWidth
            mVideoHeight = mp.videoHeight
            Log.d(
                videoTag,
                String.format("onVideoSizeChanged width=%d,height=%d", mVideoWidth, mVideoHeight)
            )
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                holder.setFixedSize(mVideoWidth, mVideoHeight)
                requestLayout()
            }
        }
    var mPreparedListener: OnPreparedListener = OnPreparedListener { mp ->
        mCurrentState = STATE_PREPARED
        mCanSeekForward = true
        mCanSeekBack = mCanSeekForward
        mCanPause = mCanSeekBack
        mPreparedBeforeStart = true
        mMediaController?.hideLoading()
        if (mOnPreparedListener != null) {
            mOnPreparedListener!!.onPrepared(mMediaPlayer)
        }
        mMediaController?.setEnabled(true)
        mVideoWidth = mp.videoWidth
        mVideoHeight = mp.videoHeight
        val seekToPosition =
            mSeekWhenPrepared // mSeekWhenPrepared may be changed after seekTo() call
        if (seekToPosition != 0) {
            seekTo(seekToPosition)
        }
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            holder.setFixedSize(mVideoWidth, mVideoHeight)
            if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                // We didn't actually change the size (it was already at the size
                // we need), so we won't get a "surface changed" callback, so
                // start the video here instead of in the callback.
                if (mTargetState == STATE_PLAYING) {
                    start()
                    mMediaController?.show()
                } else if (((mMediaController != null) && !isPlaying &&
                            (seekToPosition != 0 || currentPosition > 0))
                ) {

                    // Show the media controls when we're paused into a video and make 'em stick.
                    mMediaController!!.show(0)
                }
            }
        } else {
            // We don't know the video size yet, but should start anyway.
            // The video size might be reported to us later.
            if (mTargetState == STATE_PLAYING) {
                start()
            }
        }
    }
    private val mCompletionListener: OnCompletionListener = OnCompletionListener {
        mCurrentState = STATE_PLAYBACK_COMPLETED
        mTargetState = STATE_PLAYBACK_COMPLETED
        if (mMediaController != null) {
            val a = mMediaPlayer!!.isPlaying
            val b = mCurrentState
            mMediaController!!.showComplete()
            Log.d(videoTag, String.format("a=%s,b=%d", a, b))
        }
        if (mOnCompletionListener != null) {
            mOnCompletionListener!!.onCompletion(mMediaPlayer)
        }
    }
    private val mInfoListener: OnInfoListener =
        OnInfoListener { mp, what, extra ->
            var handled = false
            when (what) {
                MEDIA_INFO_BUFFERING_START -> {
                    Log.d(videoTag, "onInfo MediaPlayer.MEDIA_INFO_BUFFERING_START")
                    if (videoViewCallback != null) {
                        videoViewCallback!!.onBufferingStart(mMediaPlayer)
                    }
                    mMediaController?.showLoading()
                    handled = true
                }
                MEDIA_INFO_BUFFERING_END -> {
                    Log.d(videoTag, "onInfo MediaPlayer.MEDIA_INFO_BUFFERING_END")
                    if (videoViewCallback != null) {
                        videoViewCallback!!.onBufferingEnd(mMediaPlayer)
                    }
                    mMediaController?.hideLoading()
                    handled = true
                }
                else -> {}
            }
            if (mOnInfoListener != null) {
                mOnInfoListener!!.onInfo(mp, what, extra) || handled
            } else handled
        }
    private val mErrorListener: OnErrorListener =
        OnErrorListener { mp, frameworkErr, implErr ->
            Log.d(videoTag, "Error: $frameworkErr,$implErr")
            mCurrentState = STATE_ERROR
            mTargetState = STATE_ERROR
            mMediaController?.showError()
            mOnErrorListener!!.onError(mMediaPlayer, frameworkErr, implErr)
        }
    private val mBufferingUpdateListener: OnBufferingUpdateListener =
        OnBufferingUpdateListener { mp, percent -> mCurrentBufferPercentage = percent }

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    fun setOnPreparedListener(l: OnPreparedListener?) {
        mOnPreparedListener = l
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    fun setOnCompletionListener(l: OnCompletionListener?) {
        mOnCompletionListener = l
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    fun setOnErrorListener(l: OnErrorListener?) {
        mOnErrorListener = l
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    fun setOnInfoListener(l: OnInfoListener?) {
        mOnInfoListener = l
    }

    var mSHCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(
            holder: SurfaceHolder, format: Int,
            w: Int, h: Int
        ) {
            mSurfaceWidth = w
            mSurfaceHeight = h
            val isValidState = mTargetState == STATE_PLAYING
            val hasValidSize = mVideoWidth == w && mVideoHeight == h
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared)
                }
                start()
            }
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            mSurfaceHolder = holder
            openVideo()
            enableOrientationDetect()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null
            mMediaController?.hide()
            release(true)
            disableOrientationDetect()
        }
    }

    init {
        val a = mContext.obtainStyledAttributes(attrs, R.styleable.CustomVideoView, 0, 0)
        mFitXY = a.getBoolean(R.styleable.CustomVideoView_uvv_fitXY, false)
        mAutoRotation = a.getBoolean(R.styleable.CustomVideoView_uvv_autoRotation, false)
        a.recycle()
        initVideoView()
    }

    private fun enableOrientationDetect() {
        if (mAutoRotation && mOrientationDetector == null) {
            mOrientationDetector = OrientationDetector(mContext)
            mOrientationDetector!!.setOrientationChangeListener(this@CustomVideoView)
            mOrientationDetector!!.enable()
        }
    }

    private fun disableOrientationDetect() {
        mOrientationDetector?.disable()
    }

    /*
     * release the media player in any state
     */
    private fun release(cleartargetstate: Boolean) {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
            mMediaPlayer = null
            mCurrentState = STATE_IDLE
            if (cleartargetstate) {
                mTargetState = STATE_IDLE
            }
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (isInPlaybackState && mMediaController != null) {
            toggleMediaControlsVisibility()
        }
        return false
    }

    override fun onTrackballEvent(ev: MotionEvent): Boolean {
        if (isInPlaybackState && mMediaController != null) {
            toggleMediaControlsVisibility()
        }
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isKeyCodeSupported =
            keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_MUTE && keyCode != KeyEvent.KEYCODE_MENU && keyCode != KeyEvent.KEYCODE_CALL && keyCode != KeyEvent.KEYCODE_ENDCALL
        if (isInPlaybackState && isKeyCodeSupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            ) {
                if (mMediaPlayer!!.isPlaying) {
                    pause()
                    mMediaController!!.show()
                } else {
                    start()
                    mMediaController!!.hide()
                }
                return true
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mMediaPlayer!!.isPlaying) {
                    start()
                    mMediaController!!.hide()
                }
                return true
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
            ) {
                if (mMediaPlayer!!.isPlaying) {
                    pause()
                    mMediaController!!.show()
                }
                return true
            } else {
                toggleMediaControlsVisibility()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun toggleMediaControlsVisibility() {
        if (mMediaController?.isShowing == true) {
            mMediaController?.hide()
        } else {
            mMediaController?.show()
        }
    }

    override fun start() {
        if (!mPreparedBeforeStart && mMediaController != null) {
            mMediaController!!.showLoading()
        }
        if (isInPlaybackState) {
            mMediaPlayer!!.start()
            mCurrentState = STATE_PLAYING
            if (videoViewCallback != null) {
                videoViewCallback!!.onStart(mMediaPlayer)
            }
        }
        mTargetState = STATE_PLAYING
    }

    override fun pause() {
        if (isInPlaybackState && mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.pause()
            mCurrentState = STATE_PAUSED
            if (videoViewCallback != null) {
                videoViewCallback!!.onPause(mMediaPlayer)
            }
        }
        mTargetState = STATE_PAUSED
    }

    fun suspend() {
        release(false)
    }

    fun resume() {
        openVideo()
    }

    override val duration: Int
        get() = if (isInPlaybackState) {
            mMediaPlayer!!.duration
        } else -1
    override val currentPosition: Int
        get() {
            return if (isInPlaybackState) {
                mMediaPlayer!!.currentPosition
            } else 0
        }

    override fun seekTo(msec: Int) {
        mSeekWhenPrepared = if (isInPlaybackState) {
            mMediaPlayer!!.seekTo(msec)
            0
        } else {
            msec
        }
    }

    override val isPlaying: Boolean
        get() = isInPlaybackState && mMediaPlayer!!.isPlaying
    override val bufferPercentage: Int
        get() {
            return if (mMediaPlayer != null) {
                mCurrentBufferPercentage
            } else 0
        }
    private val isInPlaybackState: Boolean
        private get() = ((mMediaPlayer != null) && (
                mCurrentState != STATE_ERROR) && (
                mCurrentState != STATE_IDLE) && (
                mCurrentState != STATE_PREPARING))

    override fun canPause(): Boolean {
        return mCanPause
    }

    override fun canSeekBackward(): Boolean {
        return mCanSeekBack
    }

    override fun canSeekForward(): Boolean {
        return mCanSeekForward
    }

    override fun closePlayer() {
        release(true)
    }

    override fun setFullscreen(fullscreen: Boolean) {
        val screenOrientation =
            if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setFullscreen(fullscreen, screenOrientation)
    }

    override fun setFullscreen(fullscreen: Boolean, screenOrientation: Int) {
        val activity = mContext as Activity
        if (fullscreen) {
            if (mVideoViewLayoutWidth == 0 && mVideoViewLayoutHeight == 0) {
                val params = layoutParams
                mVideoViewLayoutWidth = params.width
                mVideoViewLayoutHeight = params.height
            }
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            activity.requestedOrientation = screenOrientation
        } else {
            val params = layoutParams
            params.width = mVideoViewLayoutWidth
            params.height = mVideoViewLayoutHeight
            layoutParams = params
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            activity.requestedOrientation = screenOrientation
        }
        mMediaController!!.toggleButtons(fullscreen)
        if (videoViewCallback != null) {
            videoViewCallback!!.onScaleChange(fullscreen)
        }
    }

    interface VideoViewCallback {
        fun onScaleChange(isFullscreen: Boolean)
        fun onPause(mediaPlayer: MediaPlayer?)
        fun onStart(mediaPlayer: MediaPlayer?)
        fun onBufferingStart(mediaPlayer: MediaPlayer?)
        fun onBufferingEnd(mediaPlayer: MediaPlayer?)
    }

    fun setVideoViewCallback(callback: VideoViewCallback?) {
        videoViewCallback = callback
    }

    companion object {
        // all possible internal states
        private const val STATE_ERROR = -1
        private const val STATE_IDLE = 0
        private const val STATE_PREPARING = 1
        private const val STATE_PREPARED = 2
        private const val STATE_PLAYING = 3
        private const val STATE_PAUSED = 4
        private const val STATE_PLAYBACK_COMPLETED = 5
    }
}