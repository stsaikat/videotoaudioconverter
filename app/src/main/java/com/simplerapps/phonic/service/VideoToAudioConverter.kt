package com.simplerapps.phonic.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import com.simplerapps.phonic.Range
import com.simplerapps.phonic.common.FileInfoManager
import java.io.FileDescriptor
import java.nio.ByteBuffer

class VideoToAudioConverter(
    private val context: Context,
    private val uri: Uri,
    private val outputUri: Uri,
    private val listener: Listener,
    private val trim: Range?
) {

    private var extractor: MediaExtractor = MediaExtractor()
    private var sourceFD: FileDescriptor =
        context.contentResolver.openFileDescriptor(uri, "r")!!.fileDescriptor
    private var desFD: FileDescriptor =
        context.contentResolver.openFileDescriptor(outputUri, "w")!!.fileDescriptor
    private lateinit var muxer: MediaMuxer
    private val buffer = ByteBuffer.allocate(1024 * 1024)
    private var durationUs = 0L

    init {
        extractor.setDataSource(sourceFD)
        trim?.let {
            durationUs = (trim.to - trim.from) * 1000L
        }
    }

    fun convert() {
        if (!sourceFD.valid() || !desFD.valid()) {
            listener.onFailed("Failed to convert! Please try again!")
            return
        }

        var trackNo = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mimeType = format.getString(MediaFormat.KEY_MIME)!!
            if (mimeType.startsWith("audio/")) {
                FileInfoManager.mimeType = mimeType
                extractor.selectTrack(i)
                trackNo = i
                if (trim == null) {
                    durationUs = format.getLong(MediaFormat.KEY_DURATION)
                }
                break
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            muxer = MediaMuxer(desFD, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } else {
            val path = outputUri.path
            if (path == null) {
                listener.onFailed("Failed to convert! Please try again!")
                return
            }
            muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }


        val bufferInfo = MediaCodec.BufferInfo()
        bufferInfo.offset = 0
        val muxerIndex = muxer.addTrack(extractor.getTrackFormat(trackNo))
        muxer.start()

        trim?.let {
            while (extractor.sampleTime < it.from * 1000) {
                extractor.advance()
            }
        }

        while (true) {
            bufferInfo.size = extractor.readSampleData(buffer, bufferInfo.offset)
            if (bufferInfo.size < 0) {
                bufferInfo.size = 0
                break
            }

            if (trim != null) {
                if (extractor.sampleTime > trim.to * 1000) {
                    break
                }
            }

            bufferInfo.presentationTimeUs = if (trim == null) {
                extractor.sampleTime
            }
            else {
                extractor.sampleTime - trim.from * 1000
            }
            listener.onProgress(
                ((bufferInfo.presentationTimeUs * 100) / durationUs).toInt()
            )
            muxer.writeSampleData(muxerIndex, buffer, bufferInfo)

            extractor.advance()
        }

        release()
    }

    private fun release() {
        buffer.clear()
        muxer.stop()
        muxer.release()
        extractor.release()

        listener.onFinish(outputUri.toString())
    }

    interface Listener {
        /**
         * get progress from 0 to 100
         */
        fun onProgress(progress: Int)
        fun onFinish(uri: String)
        fun onFailed(message: String)
    }
}