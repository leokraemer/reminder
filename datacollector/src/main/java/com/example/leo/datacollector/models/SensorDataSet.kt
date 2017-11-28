package com.example.leo.datacollector.models

import com.google.android.gms.location.DetectedActivity
import org.threeten.bp.LocalDateTime


data class SensorDataSet @JvmOverloads constructor(
        //timestamp and user
        val time: LocalDateTime,
        val userName: String,
        var recordingId: Int = -1,
        //google fit data
        var activity: DetectedActivity = DetectedActivity(DetectedActivity.UNKNOWN, 0),
        var stepsSinceLast: Long? = null,

        //all have
        var ambientSound: Double? = null,

        //most smartphones have these sensors
        /**
         * off == false
         * on == true
         */
        var screenState: Boolean? = null,
        var ambientLight: Float? = null,

        //avery phone has these
        var location: String? = null,
        var gps: android.location.Location? = null,
        var wifiName: String? = null,
        var bluetoothDevices: List<String>? = null,

        //seldom present
        var airPressure: Float? = null,
        var humidityPercent: Float? = null,
        var temperature: Float? = null,

        //via network
        var weather: String? = null
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