package com.simplerapps.phonic.datamodel

enum class Service(
    val serviceName: String,
    val serviceDetails: String? = null,
    val serviceId: Int
) {
    VIDEO_TO_AUDIO(
        serviceName = "Convert Video To Audio",
        serviceId = 0
    ),
    EDIT_AUDIO(
        serviceName = "Edit Audio",
        serviceId = 1
    ),
    MERGE_AUDIO(
        serviceName = "Merge Audio",
        serviceId = 2
    ),
    MY_FOLDER(
        serviceName = "My Folder",
        serviceId = 3
    );

    companion object {
        fun getServiceById(id: Int) : Service? {
            values().forEach {
                if (it.serviceId == id) return it
            }
            return null
        }
    }
}