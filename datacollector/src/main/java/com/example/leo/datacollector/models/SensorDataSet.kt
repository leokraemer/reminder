package com.example.leo.datacollector.models

import com.google.android.gms.location.DetectedActivity
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime


data class SensorDataSet @JvmOverloads constructor(
        //timestamp and user
        val time: Long,
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

        //every phone has these
        var location: String? = null,
        var gps: android.location.Location? = null,
        var wifiName: String? = null,
        var bluetoothDevices: List<String>? = null,

        //accelerometer raw
        var raw_acc_x : Float = 0F,
        var raw_acc_y : Float = 0F,
        var raw_acc_z : Float = 0F,

        //accelerometer
        var acc_x : Float = 0F,
        var acc_y : Float = 0F,
        var acc_z : Float = 0F,

        //Gyroscope
        var gyro_x : Float = 0F,
        var gyro_y : Float = 0F,
        var gyro_z : Float = 0F,

        //magnetometer
        var mag_x : Float = 0F,
        var mag_y : Float = 0F,
        var mag_z : Float = 0F,

        //Orientation
        var azimuth : Float = 0F,
        var pitch : Float = 0F,
        var roll : Float = 0F,

        //seldom present
        var airPressure: Float? = null,
        var humidityPercent: Float? = null,
        var temperature: Float? = null,
        var proximity: Float? = null,

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