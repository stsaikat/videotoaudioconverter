package com.simplerapps.phonic.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.simplerapps.phonic.common.FileInfoManager
import java.io.FileDescriptor
import java.nio.ByteBuffer

class VideoToAudioConverter(
    private val context: Context,
    private val uri: Uri,
    private val outputUri: Uri,
    private val listener: Listener
) {

    private var extractor: MediaExtractor = MediaExtractor()
    private var sourceFD: FileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")!!.fileDescriptor
    private var desFD: FileDescriptor = context.contentResolver.openFileDescriptor(outputUri, "w")!!.fileDescriptor
    private lateinit var muxer: MediaMuxer
    private val buffer = ByteBuffer.allocate(1024 * 1024)
    private var durationUs = 0L

    init {
        extractor.setDataSource(sourceFD)
    }

    fun convert() {
        var trackNo = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mimeType = format.getString(MediaFormat.KEY_MIME)!!
            if (mimeType.startsWith("audio/")) {
                FileInfoManager.mimeType = mimeType
                extractor.selectTrack(i)
                trackNo = i
                durationUs = format.getLong(MediaFormat.KEY_DURATION)
                break
            }
        }

        muxer = MediaMuxer(desFD, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val bufferInfo = MediaCodec.BufferInfo()
        bufferInfo.offset = 0
        val muxerIndex = muxer.addTrack(extractor.getTrackFormat(trackNo))
        muxer.start()

        while (true) {
            bufferInfo.size = extractor.readSampleData(buffer, bufferInfo.offset)
            if (bufferInfo.size < 0) {
                bufferInfo.size = 0
                break
            }

            bufferInfo.presentationTimeUs = extractor.sampleTime
            listener.onProgress(((bufferInfo.presentationTimeUs * 100) / durationUs).toInt())
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
    }
}