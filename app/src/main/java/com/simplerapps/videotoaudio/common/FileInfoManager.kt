package com.simplerapps.videotoaudio.common

import android.content.SharedPreferences
import android.net.Uri

object FileInfoManager {
    var fileName: String? = null
    var fileUri: Uri? = null
    var fileSize: Long = -1
    var mimeType: String = "audio/mp4a"
    var fileExtension: String? = null
    var savedFileUri: Uri? = null
    var savedFileName: String? = null
    const val cacheName: String = "audio.m4a"
}