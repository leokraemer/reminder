package de.leo.smartTrigger.datacollector.datacollection.models

import android.net.wifi.ScanResult

data class WifiInfo(val BSSID: String,
                    val rssi: Int,
                    val SSID: String,
                    val IP: String,
                    val networkId: Int) {
    constructor(scanResult: ScanResult) : this(scanResult.BSSID,
                                               scanResult.level,
                                               scanResult.SSID,
                                               "0.0.0.0",
                                               -1)

    constructor(wifiInfo: android.net.wifi.WifiInfo) : this(wifiInfo.bssid,
                                                            wifiInfo.rssi,
                                                            wifiInfo.ssid,
                                                            wifiInfo.ipAddress.toString(),
                                                            wifiInfo.networkId)
}