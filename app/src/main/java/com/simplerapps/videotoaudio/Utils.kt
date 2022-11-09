package com.simplerapps.videotoaudio

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

fun convertSecondsToHMmSs(seconds: Int): String {
    val s = seconds % 60
    val m = seconds / 60 % 60
    val h = seconds / (60 * 60) % 24
    return String.format("%d:%02d:%02d", h, m, s)
}

fun LogD(message: String, tag: String = "xyz") = Log.d(tag,message)

fun getDateTimeFromMillis(millis: Long): String {
    val dateFormat = "yyyyMMddHHmmss"
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
    val calendar = Calendar.getInstance()

    calendar.timeInMillis = millis
    return formatter.format(calendar.time)
}

fun getFileNameSerial() : String {
    val millis = System.currentTimeMillis()

    val dateFormat = "yyMMddHHmm"
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
    val calendar = Calendar.getInstance()

    calendar.timeInMillis = millis
    return formatter.format(calendar.time)
}