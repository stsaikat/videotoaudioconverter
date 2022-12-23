package com.simplerapps.phonic.tools

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import com.simplerapps.phonic.LogD
import com.simplerapps.phonic.Range
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.common.ProgressListener
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class AudioTranscoder(
    private val context: Context,
    private val inUri: Uri,
    private val outUri: Uri,
    private val listener: ProgressListener,
    private val trim: Range? = null,
    private val volume: Int? = null
) {

    private var extractor: MediaExtractor = MediaExtractor()
    private var sourcePFD: ParcelFileDescriptor? = null
    private lateinit var sourceFD: FileDescriptor
    private var desPFD: ParcelFileDescriptor? = null
    private lateinit var desFD: FileDescriptor
    private lateinit var muxer: MediaMuxer
    private val buffer = ByteBuffer.allocate(1024 * 1024)
    private var durationUs = 0L
    private lateinit var decoder: MediaCodec
    private lateinit var encoder: MediaCodec
    private val timeoutUs = 10000L
    private var channels = 0
    private var lastMuxerPresentationUs = 0L

    init {
        trim?.let {
            durationUs = (trim.to - trim.from) * 1000L
        }
        lastMuxerPresentationUs = 0
    }

    fun process() {

        sourcePFD = context.contentResolver.openFileDescriptor(inUri, "r")
        desPFD = context.contentResolver.openFileDescriptor(outUri, "w")

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
                FileInfoManager.mimeType = mimeType
                extractor.selectTrack(i)
                trackNo = i
                if (trim == null) {
                    durationUs = format.getLong(MediaFormat.KEY_DURATION)
                }
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
                decoder.configure(format, null, null, 0)
                decoder.start()

                val outputFormat = getOutputFormat(decoder.outputFormat)

                encoder = MediaCodec.createEncoderByType(outputFormat.getString(MediaFormat.KEY_MIME)!!)
                encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                encoder.start()

                break
            }
        }

        if (trackNo == -1) {
            onConversionFailed("No Audio Found!")
            return
        }

        muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MediaMuxer(desFD, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } else {
            val path = outUri.path
            if (path == null) {
                onConversionFailed()
                return
            }
            MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        var allInputExtracted = false
        var allInputDecoded = false
        var allOutputEncoded = false

        val bufferInfo = MediaCodec.BufferInfo()
        var trackIndex = -1

        trim?.let {
            while (extractor.sampleTime < it.from * 1000) {
                extractor.advance()
            }
        }

        LogD("${extractor.sampleTime}")

        while (!allOutputEncoded) {

            // Feed input to decoder
            if (!allInputExtracted) {
                val inBufferId = decoder.dequeueInputBuffer(timeoutUs)
                if (inBufferId >= 0) {
                    val buffer = decoder.getInputBuffer(inBufferId)!!
                    val sampleSize = extractor.readSampleData(buffer, 0)

                    val endFlag = if (trim != null) {
                        extractor.sampleTime > trim.to * 1000
                    } else {
                        false
                    }

                    if (sampleSize >= 0 && !endFlag) {
                        decoder.queueInputBuffer(
                            inBufferId, 0, sampleSize,
                            if (trim == null) {
                                extractor.sampleTime
                            } else {
                                extractor.sampleTime - trim.from * 1000
                            },
                            extractor.sampleFlags
                        )

                        extractor.advance()
                    } else {
                        decoder.queueInputBuffer(
                            inBufferId, 0, 0,
                            0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        allInputExtracted = true
                    }
                }
            }

            var encoderOutputAvailable = true
            var decoderOutputAvailable = !allInputDecoded

            while (encoderOutputAvailable || decoderOutputAvailable) {

                // Drain Encoder & mux first
                val encoderOutBufferId = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (encoderOutBufferId >= 0) {

                    val encodedBuffer = encoder.getOutputBuffer(encoderOutBufferId)!!

                    try {
                        muxer.writeSampleData(trackIndex, encodedBuffer, bufferInfo)
                        lastMuxerPresentationUs = bufferInfo.presentationTimeUs
                    }
                    catch (e: IllegalStateException) {
                        LogD("${bufferInfo.presentationTimeUs} $lastMuxerPresentationUs $durationUs")
                    }


                    encoder.releaseOutputBuffer(encoderOutBufferId, false)

                    listener.onProgress(
                        ((bufferInfo.presentationTimeUs * 100) / durationUs).toInt()
                    )

                    // Are we finished here?
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        allOutputEncoded = true
                        break
                    }
                } else if (encoderOutBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    encoderOutputAvailable = false
                } else if (encoderOutBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    trackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                }

                if (encoderOutBufferId != MediaCodec.INFO_TRY_AGAIN_LATER)
                    continue

                // Get output from decoder and feed it to encoder
                if (!allInputDecoded) {
                    val decoderOutBufferId = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    if (decoderOutBufferId >= 0) {
                        val outBuffer = decoder.getOutputBuffer(decoderOutBufferId)
                        val inBufferId = encoder.dequeueInputBuffer(timeoutUs)
                        val inBuffer = encoder.getInputBuffer(inBufferId)

                        outBuffer?.let { mOutBuffer ->
                            if (volume != null && abs(volume - 100) > 10) {
                                val shortSamples = mOutBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                                val size = shortSamples.remaining()

                                for (i in 0 until size) {
                                    var sample = shortSamples.get()

                                    // adjust volume
                                    sample = (sample * volume / 100f).toInt().toShort()

                                    // Put processed sample into encoder's buffer
                                    inBuffer?.putShort(sample)
                                }
                            } else {
                                inBuffer?.put(mOutBuffer)
                            }
                        }


                        // Feed encoder
                        encoder.queueInputBuffer(
                            inBufferId,
                            bufferInfo.offset,
                            bufferInfo.size,
                            bufferInfo.presentationTimeUs,
                            bufferInfo.flags
                        )

                        decoder.releaseOutputBuffer(decoderOutBufferId, false)

                        // Did we get all output from decoder?
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            allInputDecoded = true
                        }

                    } else if (decoderOutBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        decoderOutputAvailable = false
                    }
                }
            }
        }

        release()
        listener.onFinish(outUri.toString())

        LogD("$mxSample $mnSample")
    }

    var mxSample = 0
    var mnSample = 1000000

    private fun release() {
        sourcePFD?.close()
        desPFD?.close()

        buffer.clear()

        decoder.stop()
        decoder.release()

        encoder.stop()
        encoder.release()

        try {
            muxer.stop()
            muxer.release()
        } catch (e: UninitializedPropertyAccessException) {
            e.printStackTrace()
            // muxer isn't started so no need to stop or release
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            // muxer isn't started so no need to stop or release
        }

        extractor.release()
    }

    private fun onConversionFailed(message: String = "Failed to convert! Please try again!") {
        release()
        listener.onFailed(message)
    }

    private fun getOutputFormat(inputFormat: MediaFormat): MediaFormat {
        val outputFormat = MediaFormat()
        outputFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm")
        outputFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        outputFormat.setInteger(
            MediaFormat.KEY_SAMPLE_RATE,
            inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        )
        outputFormat.setInteger(
            MediaFormat.KEY_BIT_RATE,
            try {
                inputFormat.getInteger(MediaFormat.KEY_BIT_RATE)
            }
            catch (e: NullPointerException) {
                2000000
            }
        )
        outputFormat.setInteger(
            MediaFormat.KEY_CHANNEL_COUNT,
            inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        )
        outputFormat.setInteger(
            MediaFormat.KEY_MAX_INPUT_SIZE,
            1048576
        ) // Needs to be large enough to avoid BufferOverflowException
        return outputFormat
    }
}