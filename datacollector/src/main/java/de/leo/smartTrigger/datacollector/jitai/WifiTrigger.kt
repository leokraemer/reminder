package de.leo.smartTrigger.datacollector.jitai;

import android.content.Context
import android.net.wifi.WifiInfo
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 16.11.2017.
 */

/** For rssiThreshold default see according to android.net.wifi.WifiInfo.MIN_RSSI*/
data class WifiTrigger(var wifi: MyWifiGeofence) : Trigger {

    override fun reset(sensorData: SensorDataSet) {
        if (wifi.dwellOutside || wifi.dwellInside)
            wifi = wifi.copy()
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean =
        wifi.updateAndCheck(sensorData.time, sensorData.wifiInformation ?: emptyList<WifiInfo>())


    override fun toString(): String {
        return "${wifi.name}, ${wifi.bssid}, enter:${wifi.enter}, exit:${wifi.exit}, " +
            "dwellIn:${wifi.dwellInside}, dwellOut:${wifi.dwellOutside}, "
    }

    //wants to be checked again immediately
    override fun nextUpdate(): Long = 0
}