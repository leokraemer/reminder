package de.leo.fingerprint.datacollector.jitai;

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.datacollection.models.WifiInfo

/**
 * Created by Leo on 16.11.2017.
 */

/** For rssiThreshold default see according to android.net.wifi.WifiInfo.MIN_RSSI*/
data class WifiTrigger(val wifi: WifiInfo, var rssiThreshold: Int = -126) : Trigger {

    override fun reset() {
        //noop
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        sensorData.wifiInformation?.let {
            return it.any { it.BSSID == wifi.BSSID && rssiThreshold <= it.rssi }
        }
        return false
    }
}
