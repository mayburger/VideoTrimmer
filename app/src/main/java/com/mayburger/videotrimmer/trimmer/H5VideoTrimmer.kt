/*
 * MIT License
 *
 * Copyright (c) 2016 Knowledge, education for life.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.mayburger.videotrimmer.trimmer

import android.animation.Animator
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import com.mayburger.videotrimmer.R
import com.mayburger.videotrimmer.trimmer.interfaces.OnK4LVideoListener
import com.mayburger.videotrimmer.trimmer.interfaces.OnProgressVideoListener
import com.mayburger.videotrimmer.trimmer.interfaces.OnRangeSeekBarListener
import com.mayburger.videotrimmer.trimmer.interfaces.OnTrimVideoListener
import com.mayburger.videotrimmer.trimmer.utils.BackgroundExecutor
import com.mayburger.videotrimmer.trimmer.utils.TrimVideoUtils
import com.mayburger.videotrimmer.trimmer.utils.UiThreadExecutor
import com.mayburger.videotrimmer.trimmer.view.ProgressBarView
import com.mayburger.videotrimmer.trimmer.view.RangeSeekBarView
import com.mayburger.videotrimmer.trimmer.view.TimeLineView
import life.knowledge4.videotrimmer.view.Thumb
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

class H5VideoTrimmer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var mSeek: SeekBar? = null
    private var mRangeSeekBarView: RangeSeekBarView? = null
    private var mLinearVideo: RelativeLayout? = null
    private var mTimeInfoContainer: View? = null
    private var mVideoView: VideoView? = null
    private var mPlayView: ImageView? = null
    private var mTextSize: TextView? = null
    private var mTextTimeFrame: TextView? = null
    private var mTextTime: TextView? = null
    private var mTimeLineView: TimeLineView? = null
    private var headerProgress: ProgressBarView? = null
    private var footerProgress: ProgressBarView? = null
    private var mSrc: Uri? = null
    private var mFinalPath: String? = null
    private var mMaxDuration = 0
    private var mListeners: MutableList<OnProgressVideoListener?>? = null
    private var mOnTrimVideoListener: OnTrimVideoListener? = null
    private var mOnK4LVideoListener: OnK4LVideoListener? = null
    private var mDuration = 0
    private var mTimeVideo = 0
    private var mStartPosition = 0
    private var mEndPosition = 0
    private var mOriginSizeFile: Long = 0
    private var mResetSeekBar = true
    private val mMessageHandler =
        MessageHandler(this)

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.view_time_line, this, true)
        mSeek = findViewById<View>(R.id.handlerTop) as SeekBar
        headerProgress = findViewById<View>(R.id.headerProgress) as ProgressBarView
        footerProgress = findViewById<View>(R.id.footerProgress) as ProgressBarView
        mRangeSeekBarView = findViewById<View>(R.id.timeLineBar) as RangeSeekBarView
        mLinearVideo =
            findViewById<View>(R.id.layout_surface_view) as RelativeLayout
        mVideoView = findViewById<View>(R.id.video_loader) as VideoView
        mPlayView =
            findViewById<View>(R.id.icon_video_play) as ImageView
        mTimeInfoContainer = findViewById(R.id.timeText)
        mTextSize = findViewById<View>(R.id.textSize) as TextView
        mTextTimeFrame = findViewById<View>(R.id.textTimeSelection) as TextView
        mTextTime = findViewById<View>(R.id.textTime) as TextView
        mTimeLineView = findViewById<View>(R.id.timeLineView) as TimeLineView

        mSeek!!.max = 1000
        mSeek!!.secondaryProgress = 0

        setUpListeners()
        setUpMargins()
    }

    private fun setUpListeners() {
        mListeners = ArrayList()
        (mListeners as ArrayList<OnProgressVideoListener?>).add(OnProgressVideoListener { time, max, scale ->
            updateVideoProgress(
                time
            )
        })
        (mListeners as ArrayList<OnProgressVideoListener?>).add(headerProgress)
        (mListeners as ArrayList<OnProgressVideoListener?>).add(footerProgress)
        findViewById<View>(R.id.btCancel)
            .setOnClickListener { onCancelClicked() }
        findViewById<View>(R.id.btSave)
            .setOnClickListener { onSaveClicked() }
        val gestureDetector = GestureDetector(
            context,
            object : SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onClickVideoPlayPause()
                    return true
                }
            }
        )
        mVideoView!!.setOnErrorListener { mediaPlayer, what, extra ->
            if (mOnTrimVideoListener != null) mOnTrimVideoListener!!.onError("Something went wrong reason : $what")
            false
        }
        mVideoView!!.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        mRangeSeekBarView!!.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(
                rangeSeekBarView: RangeSeekBarView,
                index: Int,
                value: Float
            ) {
                // Do nothing
            }

            override fun onSeek(
                rangeSeekBarView: RangeSeekBarView,
                index: Int,
                value: Float
            ) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(
                rangeSeekBarView: RangeSeekBarView,
                index: Int,
                value: Float
            ) {
                initializePause(true)
                // Do nothing
            }

            override fun onSeekStop(
                rangeSeekBarView: RangeSeekBarView,
                index: Int,
                value: Float
            ) {
                onStopSeekThumbs()
            }
        })
        mRangeSeekBarView!!.addOnRangeSeekBarListener(headerProgress)
        mRangeSeekBarView!!.addOnRangeSeekBarListener(footerProgress)
        mSeek!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                onPlayerIndicatorSeekChanged(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStart()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStop(seekBar)
            }
        })
        mVideoView!!.setOnPreparedListener { mp -> onVideoPrepared(mp) }
        mVideoView!!.setOnCompletionListener { onVideoCompleted() }
    }

    private fun setUpMargins() {
//        int marge = mRangeSeekBarView.getThumbs().get(0).getWidthBitmap();
//        int widthSeek = mSeek.getThumb().getMinimumWidth() / 2;
//
//        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mSeek.getLayoutParams();
//        lp.setMargins(marge - widthSeek, 0, marge - widthSeek, 0);
//        mSeek.setLayoutParams(lp);
//
//        lp = (RelativeLayout.LayoutParams) mTimeLineView.getLayoutParams();
//        lp.setMargins(marge, 0, marge, 0);
//        mTimeLineView.setLayoutParams(lp);

//        lp = (RelativeLayout.LayoutParams) headerProgress.getLayoutParams();
//        lp.setMargins(marge, 0, marge, 0);
//        headerProgress.setLayoutParams(lp);
    }

    private fun onSaveClicked() {
        if (mStartPosition <= 0 && mEndPosition >= mDuration) {
            if (mOnTrimVideoListener != null) mOnTrimVideoListener!!.getResult(mSrc)
        } else {
            mPlayView!!.visibility = View.VISIBLE
            mVideoView!!.pause()
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, mSrc)
            val METADATA_KEY_DURATION =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLong()
            val file = File(mSrc.toString())
            println("IS FILE EX ${file}")
            if (mTimeVideo < MIN_TIME_FRAME) {
                if (METADATA_KEY_DURATION?.minus(mEndPosition) ?: 0 > MIN_TIME_FRAME - mTimeVideo) {
                    mEndPosition += MIN_TIME_FRAME - mTimeVideo
                } else if (mStartPosition > MIN_TIME_FRAME - mTimeVideo) {
                    mStartPosition -= MIN_TIME_FRAME - mTimeVideo
                }
            }

            //notify that video trimming started
            if (mOnTrimVideoListener != null) mOnTrimVideoListener!!.onTrimStarted()
            BackgroundExecutor.execute(
                object : BackgroundExecutor.Task("", 0L, "") {
                    override fun execute() {
                        try {
                            TrimVideoUtils.startTrim(
                                mSrc.toString(),
                                destinationPath!!,
                                mStartPosition.toLong(),
                                mEndPosition.toLong(),
                                mOnTrimVideoListener!!
                            )
                        } catch (e: Throwable) {
                            Thread.getDefaultUncaughtExceptionHandler()
                                .uncaughtException(Thread.currentThread(), e)
                        }
                    }
                }
            )
        }
    }

    private fun onClickVideoPlayPause() {
        if (mVideoView!!.isPlaying) {
            initializePause(true)
        } else {
            initializeResume()
        }
    }

    private fun initializePause(hideSeekbar: Boolean) {
        mPlayView!!.visibility = View.VISIBLE
        if (hideSeekbar) {
            mSeek?.hideWithCrossfade()
        }
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mVideoView!!.pause()
    }

    private fun initializeResume() {
        mPlayView!!.visibility = View.GONE
        mSeek?.showWithCrossfade()
        if (mResetSeekBar) {
            mResetSeekBar = false
            mVideoView!!.seekTo(mStartPosition)
        }
        mMessageHandler.sendEmptyMessage(SHOW_PROGRESS)
        mVideoView!!.start()
    }

    fun View.hideWithCrossfade() {
        this.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {

                }

                override fun onAnimationEnd(p0: Animator?) {
                    this@hideWithCrossfade.visibility = View.GONE
                }

                override fun onAnimationCancel(p0: Animator?) {

                }

                override fun onAnimationStart(p0: Animator?) {

                }
            })
    }

    fun View.showWithCrossfade() {
        if (this.visibility == View.GONE) {
            this.animate()
                .alpha(1f)
                .setDuration(300)
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {

                    }

                    override fun onAnimationEnd(p0: Animator?) {

                    }

                    override fun onAnimationCancel(p0: Animator?) {

                    }

                    override fun onAnimationStart(p0: Animator?) {
                        this@showWithCrossfade.visibility = View.VISIBLE
                    }
                })
        }
    }

    private fun onCancelClicked() {
        mVideoView!!.stopPlayback()
        if (mOnTrimVideoListener != null) {
            mOnTrimVideoListener!!.cancelAction()
        }
    }

    /**
     * Sets the path where the trimmed video will be saved
     * Ex: /storage/emulated/0/MyAppFolder/
     *
     * @param finalPath the full path
     */
    var destinationPath: String?
        get() {
            if (mFinalPath == null) {
                val folder = Environment.getExternalStorageDirectory()
                mFinalPath = folder.path + File.separator
                Log.d(TAG, "Using default path $mFinalPath")
            }
            return mFinalPath
        }
        set(finalPath) {
            mFinalPath = finalPath
            Log.d(TAG, "Setting custom path $mFinalPath")
        }

    private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {
        var duration = (mDuration * progress / 1000L).toInt()
        if (fromUser) {
            if (duration < mStartPosition) {
                setProgressBarPosition(mStartPosition)
                duration = mStartPosition
            } else if (duration > mEndPosition) {
                setProgressBarPosition(mEndPosition)
                duration = mEndPosition
            }
            setTimeVideo(duration)
        }
    }

    private fun onPlayerIndicatorSeekStart() {
        initializePause(false)
        //        mMessageHandler.removeMessages(SHOW_PROGRESS);
//        mVideoView.pause();
//        mPlayView.setVisibility(View.VISIBLE);
        notifyProgressUpdate(false)
    }

    private fun onPlayerIndicatorSeekStop(seekBar: SeekBar) {
        initializeResume()
        //        mMessageHandler.removeMessages(SHOW_PROGRESS);
//        mVideoView.pause();
//        mPlayView.setVisibility(View.VISIBLE);
        val duration = (mDuration * seekBar.progress / 1000L).toInt()
        mVideoView!!.seekTo(duration)
        setTimeVideo(duration)
        notifyProgressUpdate(false)
    }

    private fun onVideoPrepared(mp: MediaPlayer) {
        // Adjust the size of the video
        // so it fits on the screen
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
        val screenWidth = mLinearVideo!!.width
        val screenHeight = mLinearVideo!!.height
        val screenProportion =
            screenWidth.toFloat() / screenHeight.toFloat()
        val lp = mVideoView!!.layoutParams
        if (videoProportion > screenProportion) {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            lp.width = (videoProportion * screenHeight.toFloat()).toInt()
            lp.height = screenHeight
        }
        mVideoView!!.layoutParams = lp
        mPlayView!!.visibility = View.VISIBLE
        mDuration = mVideoView!!.duration
        setSeekBarPosition()
        setTimeFrames()
        setTimeVideo(0)
        if (mOnK4LVideoListener != null) {
            mOnK4LVideoListener!!.onVideoPrepared()
        }
    }

    private fun setSeekBarPosition() {
        if (mDuration >= mMaxDuration) {
            mStartPosition = mDuration / 2 - mMaxDuration / 2
            mEndPosition = mDuration / 2 + mMaxDuration / 2
            mRangeSeekBarView!!.setThumbValue(0, mStartPosition * 100 / mDuration.toFloat())
            mRangeSeekBarView!!.setThumbValue(1, mEndPosition * 100 / mDuration.toFloat())
        } else {
            mStartPosition = 0
            mEndPosition = mDuration
        }
        setProgressBarPosition(mStartPosition)
        mVideoView!!.seekTo(mStartPosition)
        mTimeVideo = mDuration
        mRangeSeekBarView!!.initMaxWidth()
    }

    private fun setTimeFrames() {
        val seconds = context.getString(R.string.short_seconds)
        mTextTimeFrame!!.text = String.format(
            "%s %s - %s %s",
            TrimVideoUtils.stringForTime(mStartPosition),
            seconds,
            TrimVideoUtils.stringForTime(mEndPosition),
            seconds
        )
    }

    private fun setTimeVideo(position: Int) {
        val seconds = context.getString(R.string.short_seconds)
        mTextTime!!.text = String.format(
            "%s %s",
            TrimVideoUtils.stringForTime(position),
            seconds
        )
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = (mDuration * value / 100L).toInt()
                mVideoView!!.seekTo(mStartPosition)
            }
            Thumb.RIGHT -> {
                mEndPosition = (mDuration * value / 100L).toInt()
            }
        }
        setProgressBarPosition(mStartPosition)
        setTimeFrames()
        mTimeVideo = mEndPosition - mStartPosition
    }

    private fun onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mVideoView!!.pause()
        mPlayView!!.visibility = View.VISIBLE
    }

    private fun onVideoCompleted() {
        mVideoView!!.seekTo(mStartPosition)
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0) return
        val position = mVideoView!!.currentPosition
        if (all) {
            for (item in mListeners!!) {
                item!!.updateProgress(position, mDuration, (position * 100 / mDuration).toFloat())
            }
        } else {
            mListeners!![1]
                ?.updateProgress(position, mDuration, (position * 100 / mDuration).toFloat())
        }
    }

    private fun updateVideoProgress(time: Int) {
        if (mVideoView == null) {
            return
        }
        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            mVideoView!!.pause()
            mPlayView!!.visibility = View.VISIBLE
            mResetSeekBar = true
            return
        }
        if (mSeek != null) {
            // use long to avoid overflow
            setProgressBarPosition(time)
        }
        setTimeVideo(time)
    }

    private fun setProgressBarPosition(position: Int) {
        if (mDuration > 0) {
            val pos = 1000L * position / mDuration
            mSeek?.progress = pos.toInt()
        }
    }

    /**
     * Set video information visibility.
     * For now this is for debugging
     *
     * @param visible whether or not the videoInformation will be visible
     */
    fun setVideoInformationVisibility(visible: Boolean) {
        mTimeInfoContainer!!.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Listener for events such as trimming operation success and cancel
     *
     * @param onTrimVideoListener interface for events
     */
    fun setOnTrimVideoListener(onTrimVideoListener: OnTrimVideoListener?) {
        mOnTrimVideoListener = onTrimVideoListener
    }

    /**
     * Listener for some [VideoView] events
     *
     * @param onK4LVideoListener interface for events
     */
    fun setOnK4LVideoListener(onK4LVideoListener: OnK4LVideoListener?) {
        mOnK4LVideoListener = onK4LVideoListener
    }

    /**
     * Cancel all current operations
     */
    fun destroy() {
        BackgroundExecutor.cancelAll("", true)
        UiThreadExecutor.cancelAll("")
    }

    /**
     * Set the maximum duration of the trimmed video.
     * The trimmer interface wont allow the user to set duration longer than maxDuration
     *
     * @param maxDuration the maximum duration of the trimmed video in seconds
     */
    fun setMaxDuration(maxDuration: Int) {
        mMaxDuration = maxDuration * 1000
    }

    /**
     * Sets the uri of the video to be trimmer
     *
     * @param videoURI Uri of the video
     */
    fun setVideoURI(videoURI: Uri?) {
        mSrc = videoURI
        if (mOriginSizeFile == 0L) {
            val file = File(mSrc!!.path)
            mOriginSizeFile = file.length()
            val fileSizeInKB = mOriginSizeFile / 1024
            if (fileSizeInKB > 1000) {
                val fileSizeInMB = fileSizeInKB / 1024
                mTextSize!!.text = String.format(
                    "%s %s",
                    fileSizeInMB,
                    context.getString(R.string.megabyte)
                )
            } else {
                mTextSize!!.text = String.format(
                    "%s %s",
                    fileSizeInKB,
                    context.getString(R.string.kilobyte)
                )
            }
        }
        mVideoView!!.setVideoURI(mSrc)
        mVideoView!!.requestFocus()
        mTimeLineView!!.setVideo(mSrc!!)
    }

    private class MessageHandler internal constructor(view: H5VideoTrimmer) : Handler() {
        private val mView: WeakReference<H5VideoTrimmer>
        override fun handleMessage(msg: Message) {
            val view = mView.get()
            if (view == null || view.mVideoView == null) {
                return
            }
            view.notifyProgressUpdate(true)
            if (view.mVideoView!!.isPlaying) {
                sendEmptyMessageDelayed(0, 10)
            }
        }

        init {
            mView = WeakReference(view)
        }
    }

    companion object {
        private val TAG = H5VideoTrimmer::class.java.simpleName
        private const val MIN_TIME_FRAME = 1000
        private const val SHOW_PROGRESS = 2
    }

    init {
        init(context)
    }
}