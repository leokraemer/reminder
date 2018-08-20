package de.leo.fingerprint.datacollector.datacollection.models

import com.google.android.gms.location.DetectedActivity


data class SensorDataSet @JvmOverloads constructor(
    //timestamp and user
    val time: Long,
    val userName: String,
    var recordingId: Int = -1,
    var id: Long = -1L,
    //google fit data
    var activity: List<DetectedActivity> = listOf(DetectedActivity(DetectedActivity.UNKNOWN, 0)),
    var totalStepsToday: Long? = null,

    //all have
    var ambientSound: Double? = null,

    //every phone has these
    var location: String? = null,
    var gps: android.location.Location? = null,
    var wifiInformation: List<WifiInfo>? = null,
    var bluetoothDevices: List<String>? = null,
    var screenState: Boolean = false,

    //via network
    var weather: Long? = null)