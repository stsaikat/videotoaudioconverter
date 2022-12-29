package com.simplerapps.phonic

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.fragment.app.FragmentManager
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