package com.simplerapps.phonic.tools

import android.content.Context
import android.media.*
import android.os.Build
import android.os.ParcelFileDescriptor
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.datamodel.AudioConversionInfo
import com.simplerapps.phonic.utils.LogD
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioTranscoder(
    private val context: Context,
    private val audioConversionInfo: AudioConversionInfo
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
    private val timeoutUs = 100L
    private var channels = 0

    private var allInputExtracted = false
    private var allInputDecoded = false
    private var allOutputEncoded = false

    private var encoderOutputAvailable = true
    private var decoderOutputAvailable = !allInputDecoded

    private val bufferInfo = MediaCodec.BufferInfo()
    private var trackIndex = -1

    init {
        audioConversionInfo.trim?.let { trim ->
            durationUs = (trim.toMs - trim.fromMs) * 1000L
        }
    }

    private fun initSources(): Boolean {
        sourcePFD = context.contentResolver.openFileDescriptor(audioConversionInfo.uri, "r")
        desPFD = context.contentResolver.openFileDescriptor(audioConversionInfo.outputUri, "w")

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

    private fun initDecoder(audioFormat: MediaFormat): Boolean {
        channels = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        decoder = MediaCodec.createDecoderByType(audioFormat.getString(MediaFormat.KEY_MIME)!!)
        decoder.configure(audioFormat, null, null, 0)
        decoder.start()

        return true
    }

    private fun initEncoder(audioFormat: MediaFormat): Boolean {
        encoder = MediaCodec.createEncoderByType(audioFormat.getString(MediaFormat.KEY_MIME)!!)
        encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        return true
    }

    private fun initDecoderEncoder(audioFormat: MediaFormat): Boolean {
        if (audioConversionInfo.trim == null) {
            durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION)
        }

        initDecoder(audioFormat)

        val formatChangeWaitTimeStartMs = System.currentTimeMillis()

        while (true) {
            feedInputToDecoder()
            val bufferId = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (bufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val outputFormat = getOutputFormat(decoder.outputFormat)
                initEncoder(outputFormat)
                break
            }

            if (System.currentTimeMillis() - formatChangeWaitTimeStartMs > 10000L) {
                return false
            }
        }

        return true
    }

    private fun initMuxer(): Boolean {
        muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MediaMuxer(desFD, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } else {
            val path = audioConversionInfo.outputUri.path ?: return false
            MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        return true
    }

    private fun initDecodeEncodeState() {
        allInputExtracted = false
        allInputDecoded = false
        allOutputEncoded = false

        trackIndex = -1


        audioConversionInfo.trim?.let {
            while (extractor.sampleTime < it.fromMs * 1000) {
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

                val endFlag = if (audioConversionInfo.trim != null) {
                    extractor.sampleTime > audioConversionInfo.trim!!.toMs * 1000
                } else {
                    false
                }

                if (sampleSize >= 0 && !endFlag) {
                    decoder.queueInputBuffer(
                        inBufferId, 0, sampleSize,
                        if (audioConversionInfo.trim == null) {
                            extractor.sampleTime
                        } else {
                            extractor.sampleTime - audioConversionInfo.trim!!.fromMs * 1000
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
            true
        } catch (e: IllegalStateException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun getCurrentProgress(): Int =
        ((bufferInfo.presentationTimeUs * 100) / durationUs).toInt()

    private fun drainEncoderAndMux(bufferId: Int) {
        if (bufferId >= 0) {
            val encodedBuffer = encoder.getOutputBuffer(bufferId)!!
            muxEncodedBuffer(encodedBuffer)
            encoder.releaseOutputBuffer(bufferId, false)

            audioConversionInfo.listener?.onProgress(getCurrentProgress())

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

    private fun transcodeBuffer(fromBuffer: ByteBuffer, toBuffer: ByteBuffer, pts: Long) {
        if (audioConversionInfo.isVolumeChangeNeeded() || audioConversionInfo.isFadeNeeded()) {
            var volume = audioConversionInfo.volume.toFloat()
            if (pts < audioConversionInfo.fadeInMs * 1000) {
                volume *= (pts / 1000f) / audioConversionInfo.fadeInMs
            }
            else if (pts > durationUs - audioConversionInfo.fadeOutMs * 1000) {
                val rem = durationUs - pts
                volume *= (rem / 1000f) / audioConversionInfo.fadeOutMs
            }

            LogD("$pts $volume ${audioConversionInfo.volume}")

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
        val inBuffer = try {
            encoder.getInputBuffer(inBufferId)
        } catch (e: IllegalStateException) {
            null
        }

        if (buffer != null && inBuffer != null) {
            transcodeBuffer(buffer, inBuffer, bufferInfo.presentationTimeUs)
            // Feed encoder
            encoder.queueInputBuffer(
                inBufferId,
                bufferInfo.offset,
                bufferInfo.size,
                bufferInfo.presentationTimeUs,
                bufferInfo.flags
            )
        }
    }

    private fun drainDecoderAndFeedEncoder() {
        if (!allInputDecoded) {
            val bufferId = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (bufferId >= 0) {
                val outBuffer = decoder.getOutputBuffer(bufferId)
                feedEncoder(outBuffer)
                decoder.releaseOutputBuffer(bufferId, false)

                // Did we get all output from decoder?
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    allInputDecoded = true
                }

            } else if (bufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
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

        audioConversionInfo.listener?.onFinish(audioConversionInfo.outputUri.toString())
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
        audioConversionInfo.listener?.onFailed(message)
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