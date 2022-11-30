package com.simplerapps.phonic.common

interface ProgressListener {
    /**
     * get progress from 0 to 100
     */
    fun onProgress(progress: Int)
    fun onFinish(uri: String)
    fun onFailed(message: String)
}