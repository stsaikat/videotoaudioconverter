package com.simplerapps.phonic.common

import android.net.Uri
import com.simplerapps.phonic.utils.TrimRange

class FileInfoManager {
    companion object {
        @Volatile var fileName: String? = null
        @Volatile var fileUri: Uri? = null
        @Volatile var fileSize: Long = -1
        @Volatile var mimeType: String = "audio/mp4a"
        @Volatile var fileExtension: String? = null
        @Volatile var savedFileUri: Uri? = null
        @Volatile var savedFileName: String? = null
        @Volatile var trim: TrimRange? = null
        @Volatile var volume: Int = 100
        @Volatile var fadeInMs = 0
        @Volatile var fadeOutMs = 0
        const val cacheName: String = "audio.m4a"
    }
}