package com.example.leo.datacollector.models

import com.google.android.gms.location.DetectedActivity


data class SensorDataSet @JvmOverloads constructor(
        //timestamp and user
        val time: Long,
        val userName: String,
        var recordingId: Int = -1,
        var id : Long = -1L,
        //google fit data
        var activity: DetectedActivity = DetectedActivity(DetectedActivity.UNKNOWN, 0),
        var totalStepsToday: Long? = null,

        //all have
        var ambientSound: Double? = null,

        //every phone has these
        var location: String? = null,
        var gps: android.location.Location? = null,
        var wifiName: String? = null,
        var bluetoothDevices: List<String>? = null,
        var screenState: Boolean = false,

        //via network
        var weather: Long? = null
                                                  ) {
    fun getActivityString() = when (activity.type) {
        0 -> "IN_VEHICLE"
        1 -> "ON_BICYCLE"
        2 -> "ON_FOOT"
        3 -> "STILL"
        4 -> "UNKNOWN"
        5 -> "TILTING"
        7 -> "WALKING"
        8 -> "RUNNING"
        else -> "UNKNOWN"
    }
}