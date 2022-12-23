package com.simplerapps.phonic.tools

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import com.simplerapps.phonic.LogD
import com.simplerapps.phonic.TrimRange
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
    private val trim: TrimRange? = null,
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

    private var allInputExtracted = false
    private var allInputDecoded = false
    private var allOutputEncoded = false

    private var encoderOutputAvailable = true
    private var decoderOutputAvailable = !allInputDecoded

    private val bufferInfo = MediaCodec.BufferInfo()
    private var trackIndex = -1

    init {
        trim?.let {
            durationUs = (trim.to - trim.from) * 1000L
        }
        lastMuxerPresentationUs = 0
    }

    private fun initSources(): Boolean {
        sourcePFD = context.contentResolver.openFileDescriptor(inUri, "r")
        desPFD = context.contentResolver.openFileDescriptor(outUri, "w")

        if (sourcePFD == null || desPFD == null) {
            return false
        }

        sourceFD = sourcePFD!!.fileDescriptor
        desFD = desPFD!!.fileDescriptor

        if (!sourceFD.valid() || !desFD.valid()) {
            return false
        }

        extractor.setDataSource(sourceFD)

        return true
    }

    private fun getInputAudioFormat(): MediaFormat? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mimeType = format.getString(MediaFormat.KEY_MIME)!!
            if (mimeType.startsWith("audio/")) {
                FileInfoManager.mimeType = mimeType
                extractor.selectTrack(i)
                return format
            }
        }

        return null
    }

    private fun initDecoder(audioFormat: MediaFormat) : Boolean {
        channels = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        decoder = MediaCodec.createDecoderByType(audioFormat.getString(MediaFormat.KEY_MIME)!!)
        decoder.configure(audioFormat, null, null, 0)
        decoder.start()

        return true
    }

    private fun initEncoder(audioFormat: MediaFormat) : Boolean {
        encoder = MediaCodec.createEncoderByType(audioFormat.getString(MediaFormat.KEY_MIME)!!)
        encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        return true
    }

    private fun initDecoderEncoder(audioFormat: MediaFormat): Boolean {
        if (trim == null) {
            durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION)
        }

        initDecoder(audioFormat)
        val outputFormat = getOutputFormat(audioFormat)
        initEncoder(outputFormat)

        return true
    }

    private fun initMuxer(): Boolean {
        muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MediaMuxer(desFD, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } else {
            val path = outUri.path ?: return false
            MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        return true
    }

    private fun initDecodeEncodeState() {
        allInputExtracted = false
        allInputDecoded = false
        allOutputEncoded = false

        trackIndex = -1


        trim?.let {
            while (extractor.sampleTime < it.from * 1000) {
                extractor.advance()
            }
        }
    }

    private fun feedInputToDecoder() {
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
    }

    private fun muxEncodedBuffer(buffer: ByteBuffer): Boolean {
        return try {
            muxer.writeSampleData(trackIndex, buffer, bufferInfo)
            lastMuxerPresentationUs = bufferInfo.presentationTimeUs
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    private fun getCurrentProgress() : Int = ((bufferInfo.presentationTimeUs * 100) / durationUs).toInt()

    private fun drainEncoderAndMux(bufferId: Int) {
        if (bufferId >= 0) {
            val encodedBuffer = encoder.getOutputBuffer(bufferId)!!
            muxEncodedBuffer(encodedBuffer)
            encoder.releaseOutputBuffer(bufferId, false)

            listener.onProgress(getCurrentProgress())

            // Are we finished here?
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                allOutputEncoded = true
                return
            }
        } else if (bufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
            encoderOutputAvailable = false
        } else if (bufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            trackIndex = muxer.addTrack(encoder.outputFormat)
            muxer.start()
        }
    }

    private fun transcodeBuffer(fromBuffer: ByteBuffer, toBuffer: ByteBuffer) {
        if (volume != null && abs(volume - 100) > 10) {
            val shortSamples = fromBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
            val size = shortSamples.remaining()

            for (i in 0 until size) {
                var sample = shortSamples.get()

                // adjust volume
                sample = (sample * volume / 100f).toInt().toShort()

                // Put processed sample into encoder's buffer
                toBuffer.putShort(sample)
            }
        } else {
            toBuffer.put(fromBuffer)
        }
    }

    private fun feedEncoder(buffer: ByteBuffer?) {
        val inBufferId = encoder.dequeueInputBuffer(timeoutUs)
        val inBuffer = encoder.getInputBuffer(inBufferId)

        if (buffer != null && inBuffer != null) {
            transcodeBuffer(buffer, inBuffer)
        }

        // Feed encoder
        encoder.queueInputBuffer(
            inBufferId,
            bufferInfo.offset,
            bufferInfo.size,
            bufferInfo.presentationTimeUs,
            bufferInfo.flags
        )
    }

    private fun drainDecoderAndFeedEncoder() {
        if (!allInputDecoded) {
            val decoderOutBufferId = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (decoderOutBufferId >= 0) {
                val outBuffer = decoder.getOutputBuffer(decoderOutBufferId)
                feedEncoder(outBuffer)
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

    private fun runTranscodeFlow() {

        initDecodeEncodeState()

        while (!allOutputEncoded) {

            feedInputToDecoder()

            encoderOutputAvailable = true
            decoderOutputAvailable = !allInputDecoded

            while (encoderOutputAvailable || decoderOutputAvailable) {

                val encoderOutBufferId = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                drainEncoderAndMux(encoderOutBufferId)
                if (allOutputEncoded) break
                if (encoderOutBufferId != MediaCodec.INFO_TRY_AGAIN_LATER) continue

                // Get output from decoder and feed it to encoder
                drainDecoderAndFeedEncoder()
            }
        }
    }

    fun process() {

        if (!initSources()) {
            onConversionFailed()
            return
        }

        val inputAudioFormat = getInputAudioFormat()

        if (inputAudioFormat == null) {
            onConversionFailed("No Audio Found!")
            return
        }

        if (!initDecoderEncoder(inputAudioFormat)) {
            onConversionFailed()
            return
        }

        if (!initMuxer()) {
            onConversionFailed()
            return
        }

        runTranscodeFlow()
        release()

        listener.onFinish(outUri.toString())
    }

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
            } catch (e: NullPointerException) {
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