package de.leo.fingerprint.datacollector.jitai;

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 16.11.2017.
 */

/** For rssiThreshold default see according to android.net.wifi.WifiInfo.MIN_RSSI*/
data class WifiTrigger(val wifi: MyWifiGeofence) : Trigger {

    override fun reset() {
        //noop
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        sensorData.wifiInformation?.let {
            return it.any { wifi.checkCondition(sensorData.time, it.BSSID, it.rssi) }
        }
        return false
    }
}
