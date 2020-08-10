package com.mayburger.videotrimmer.trimmer.utils

import android.content.Context
import android.net.Uri
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.sink.DefaultDataSink
import com.otaliastudios.transcoder.source.UriDataSource
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.otaliastudios.transcoder.strategy.size.FractionResizer
import com.otaliastudios.transcoder.strategy.size.PassThroughResizer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Future

data class VideoTranscodeResultData(val resultPath: String, val originalPath: String)

object VideoTranscode {
    private val multipleTranscodeResult: MutableList<VideoTranscodeResultData> = mutableListOf()
    private var multipleTranscodeProgress: MutableList<Double> = mutableListOf()
    private val videoStrategy = DefaultVideoStrategy.Builder()
            .addResizer(FractionResizer(1f))
            .addResizer(PassThroughResizer())
            .frameRate(DefaultVideoStrategy.DEFAULT_FRAME_RATE)
            .build()

    private var transcode: Future<Void>? = null

    fun cancel() {
        transcode?.cancel(true)
        multipleTranscodeProgress.clear()
        multipleTranscodeResult.clear()
    }

    fun transcode(context: Context, path: String, listener: VideoTranscoderListener) {
        val outputDir = context.filesDir.absolutePath + File.separator
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "MP4_transcode_$timeStamp.mp4"
        val filePath: String = outputDir + fileName
        val tempFile = File(filePath)
        tempFile.parentFile.mkdirs()

        val sink = DefaultDataSink(tempFile.absolutePath)

        val transcoderBuilder = Transcoder.into(sink)

        transcode = transcoderBuilder
                .addDataSource(UriDataSource(context, Uri.parse(path)))
                .setVideoTrackStrategy(videoStrategy)
                .setListener(object : TranscoderListener {
                    override fun onTranscodeCompleted(successCode: Int) {
                        listener.onTranscodeCompleted(VideoTranscodeResultData(
                                resultPath = tempFile.absolutePath,
                                originalPath = path), successCode)
                    }

                    override fun onTranscodeProgress(progress: Double) {
                        listener.onTranscodeProgress(progress)
                    }

                    override fun onTranscodeCanceled() {
                        listener.onTranscodeCanceled()
                    }

                    override fun onTranscodeFailed(exception: Throwable) {
                        listener.onTranscodeCanceled()
                    }
                }).transcode()
    }
}

interface BaseTranscodeListener {
    /**
     * Called to notify progress.
     *
     * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
     */
    fun onTranscodeProgress(progress: Double)

    /**
     * Called when transcode canceled.
     */
    fun onTranscodeCanceled()

    /**
     * Called when transcode failed.
     * @param exception the failure exception
     */
    fun onTranscodeFailed(exception: Throwable)
}

interface VideoTranscoderListener : BaseTranscodeListener {
    /**
     * Called when transcode completed. The success code can be either
     * [Transcoder.SUCCESS_TRANSCODED] or [Transcoder.SUCCESS_NOT_NEEDED].
     *
     * @param successCode the success code
     */
    fun onTranscodeCompleted(resultData: VideoTranscodeResultData, successCode: Int)
}
