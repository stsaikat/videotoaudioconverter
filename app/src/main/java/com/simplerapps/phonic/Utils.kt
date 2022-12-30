package com.simplerapps.phonic

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.FragmentManager
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.service.InfoActivity
import com.simplerapps.phonic.servicechooser.Service
import com.simplerapps.phonic.share.ProcessResultDialog
import java.text.SimpleDateFormat
import java.util.*

fun convertSecondsToHMmSs(seconds: Int): String {
    val s = seconds % 60
    val m = seconds / 60 % 60
    val h = seconds / (60 * 60) % 24
    return String.format("%d:%02d:%02d", h, m, s)
}

fun LogD(message: String, tag: String = "xyz") = Log.d(tag, message)

fun getDateTimeFromMillis(millis: Long): String {
    val dateFormat = "yyyyMMddHHmmss"
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
    val calendar = Calendar.getInstance()

    calendar.timeInMillis = millis
    return formatter.format(calendar.time)
}

fun getFileCreateTime(): String {
    val millis = System.currentTimeMillis()

    val dateFormat = "yyMMddHHmm"
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
    val calendar = Calendar.getInstance()

    calendar.timeInMillis = millis
    return formatter.format(calendar.time)
}

fun Activity.shareAudioFile(uri: Uri, name: String? = null) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "audio/*"
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    startActivity(Intent.createChooser(intent, "Share $name using"))
}

fun showInfoDialog(
    supportFragmentManager: FragmentManager,
    title: String? = null,
    message: String? = null
) {
    val processResultDialog = ProcessResultDialog(title, message)
    processResultDialog.show(supportFragmentManager, null)
}

data class TrimRange(var fromMs: Int, var toMs: Int)

fun getFormattedTrimTimeText(time: Int): String {
    val ss = (time % 1000) / 100
    val seconds = time / 1000
    val s = seconds % 60
    val m = (seconds / 60) % 60
    val h = (seconds / (60 * 60)) % 24
    return String.format("%02d:%02d:%02d.%01d", h, m, s, ss)
}

fun Activity.goToInfoActivity(withService: Service) {
    val intent = Intent(this, InfoActivity::class.java)
    intent.putExtra(InfoActivity.SERVICE_ID, withService.serviceId)
    startActivity(intent)
}

fun Activity.processChosenAudioUri(uri: Uri) {
    FileInfoManager.fileUri = uri
    contentResolver.query(uri, null, null, null, null)?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

        it.moveToFirst()

        FileInfoManager.fileName = getMp4RemovedName(it.getString(nameIndex))
        FileInfoManager.fileSize = it.getLong(sizeIndex)
    }

    goToInfoActivity(Service.EDIT_AUDIO)
}

fun getMp4RemovedName(name: String) : String {
    return name.removeSuffix(".mp4").removeSuffix(".m4a").removeSuffix(".mp3")
}