package com.simplerapps.videotoaudio.servicechooser

enum class Service(
    val serviceName: String,
    val serviceDetails: String? = null,
    val serviceId: Int
) {
    VIDEO_TO_AUDIO(
        serviceName = "Convert Video To Audio",
        serviceId = 0
    );
}