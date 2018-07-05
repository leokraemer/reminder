package de.leo.fingerprint.datacollector.datacollection.models

import android.net.wifi.ScanResult
import android.util.Log

/**
 * @return List<bssid, rssi, ssid, ip, networkId>
 */
fun deSerializeWifi(wifiNames: String): List<WifiInfo> {
    val wifis = wifiNames.replace("[", "", false).replace("]", "", false).split(",")
    wifis.toMutableList().remove("null")
    return wifis.map {
        try {
            val values = it.split(";")
            WifiInfo(values[0],
                                                                            values[1].toInt(),
                                                                            values[2],
                                                                            values[3],
                                                                            values[4].toInt())
        } catch (e: Exception) {
            Log.e("wifi deserilaisation", e.toString())
            WifiInfo("null",
                                                                            -100,
                                                                            "null",
                                                                            "null",
                                                                            -1)
        }
    }
}


fun serializeWifi(wifiInfo: android.net.wifi.WifiInfo): String =
    "[${wifiInfo?.bssid};${wifiInfo?.rssi};${wifiInfo?.ssid};${wifiInfo?.ipAddress};${wifiInfo?.networkId}]"

fun serializeWifi(scanResults: List<ScanResult>,
                  wifiInfo: android.net.wifi.WifiInfo?): String =
    scanResults.map {
        //replace the connected wifi with better information
        if (wifiInfo?.bssid == it.BSSID) {
            "[${wifiInfo?.bssid};${wifiInfo?.rssi};${wifiInfo?.ssid};${wifiInfo?.ipAddress};${wifiInfo?.networkId}]"
        } else {
            "[${it.BSSID};${it.level};${it.SSID};0.0.0.0;-1]"
        }
    }.toString()


fun serializeWifi(wifiInfo: WifiInfo): String =
    "[${wifiInfo.BSSID};${wifiInfo.rssi};${wifiInfo.SSID};${wifiInfo.IP};${wifiInfo.networkId}]"


data class WifiInfo(val BSSID: String, val rssi: Int, val SSID: String, val IP:
String, val networkId: Int) {
    constructor(scanResult: ScanResult) : this(scanResult.BSSID, scanResult.level, scanResult
        .SSID, "0.0.0.0", -1)
}

