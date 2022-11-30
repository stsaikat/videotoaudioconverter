package com.simplerapps.phonic.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import com.simplerapps.phonic.Range
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.common.ProgressListener
import com.simplerapps.phonic.tools.AudioTranscoder
import java.io.FileDescriptor
import java.nio.ByteBuffer

class AudioConverter(
    private val context: Context,
    private val uri: Uri,
    private val outputUri: Uri,
    private val listener: ProgressListener,
    private val trim: Range?
) {

    private var extractor: MediaExtractor = MediaExtractor()
    private var sourcePFD: ParcelFileDescriptor? = null
    private lateinit var sourceFD: FileDescriptor
    private var desPFD: ParcelFileDescriptor? = null
    private lateinit var desFD: FileDescriptor
    private lateinit var muxer: MediaMuxer
    private val buffer = ByteBuffer.allocate(1024 * 1024)
    private var durationUs = 0L

    init {
        trim?.let {
            durationUs = (trim.to - trim.from) * 1000L
        }
    }

    fun convert() {
        sourcePFD = context.contentResolver.openFileDescriptor(uri, "r")
        desPFD = context.contentResolver.openFileDescriptor(outputUri, "w")

        if (sourcePFD == null || desPFD == null) {
            onConversionFailed()
            return
        }

        sourceFD = sourcePFD!!.fileDescriptor
        desFD = desPFD!!.fileDescriptor

        if (!sourceFD.valid() || !desFD.valid()) {
            onConversionFailed()
            return
        }

        extractor.setDataSource(sourceFD)

        var trackNo = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mimeType = format.getString(MediaFormat.KEY_MIME)!!
            if (mimeType.startsWith("audio/")) {

                if (!mimeType.contains("mp4a")) {
                    release()
                    startTranscoderFlow()
                    return
                }

                FileInfoManager.mimeType = mimeType
                extractor.selectTrack(i)
                trackNo = i
                if (trim == null) {
                    durationUs = format.getLong(MediaFormat.KEY_DURATION)
                }
                break
            }
        }

        if (trackNo == -1) {
            onConversionFailed("No Audio Found in this video!")
            return
        }

        muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MediaMuxer(desFD, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } else {
            val path = outputUri.path
            if (path == null) {
                onConversionFailed()
                return
            }
            MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }


        val bufferInfo = MediaCodec.BufferInfo()
        bufferInfo.offset = 0

        val muxerIndex = try {
            muxer.addTrack(extractor.getTrackFormat(trackNo))
        }
        catch (e: IllegalStateException) {
            e.printStackTrace()
            onConversionFailed("This format isn't supported!")
            return
        }

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
            } else {
                extractor.sampleTime - trim.from * 1000
            }
            listener.onProgress(
                ((bufferInfo.presentationTimeUs * 100) / durationUs).toInt()
            )
            muxer.writeSampleData(muxerIndex, buffer, bufferInfo)

            extractor.advance()
        }

        release()
        listener.onFinish(outputUri.toString())
    }

    private fun onConversionFailed(message: String = "Failed to convert! Please try again!") {
        release()
        listener.onFailed(message)
    }

    private fun release() {
        sourcePFD?.close()
        desPFD?.close()

        buffer.clear()

        try {
            muxer.stop()
            muxer.release()
        }
        catch (e: UninitializedPropertyAccessException) {
            e.printStackTrace()
            // muxer isn't started so no need to stop or release
        }
        catch (e: IllegalStateException) {
            e.printStackTrace()
            // muxer isn't started so no need to stop or release
        }

        extractor.release()
    }

    private fun startTranscoderFlow() {
        val audioTranscoder = AudioTranscoder(
            context,
            uri,
            outputUri,
            listener,
            trim
        )
        audioTranscoder.process()
    }
}