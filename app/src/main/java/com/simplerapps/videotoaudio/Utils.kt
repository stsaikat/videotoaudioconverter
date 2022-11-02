package com.simplerapps.videotoaudio

fun convertSecondsToHMmSs(seconds: Int): String {
    val s = seconds % 60
    val m = seconds / 60 % 60
    val h = seconds / (60 * 60) % 24
    return String.format("%d:%02d:%02d", h, m, s)
}