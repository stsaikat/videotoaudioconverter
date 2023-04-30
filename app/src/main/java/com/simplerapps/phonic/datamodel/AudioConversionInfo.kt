package com.simplerapps.phonic.datamodel

import android.net.Uri
import com.simplerapps.phonic.common.ProgressListener
import com.simplerapps.phonic.utils.TrimRange
import kotlin.math.abs

data class AudioConversionInfo(
    var uri: Uri = Uri.EMPTY,
    var outputUri: Uri = Uri.EMPTY,
    var listener: ProgressListener? = null,
    var trim: TrimRange?,
    var volume: Int = 100,
    var fadeInMs: Int = 0,
    var fadeOutMs: Int = 0
) {
    fun isTranscodeNeeded() : Boolean {
        if (isVolumeChangeNeeded()) return true
        if (isFadeNeeded()) return true
        return false
    }

    fun isVolumeChangeNeeded() : Boolean {
        if (abs(volume - 100) > 10) return true
        return false
    }

    fun isFadeNeeded() : Boolean {
        if (fadeInMs > 0) return true
        if (fadeOutMs > 0) return true
        return false
    }
}