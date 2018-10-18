package de.leo.smartTrigger.datacollector.jitai;

import android.content.Context
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 16.11.2017.
 */

/** For rssiThreshold default see according to android.net.wifi.WifiInfo.MIN_RSSI*/
data class WifiTrigger(val wifi: MyWifiGeofence) : Trigger {

    override fun reset(sensorData: SensorDataSet) {
        //noop
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        sensorData.wifiInformation?.let {
            return wifi.updateAndCheck(sensorData.time, it)
        }
        return false
    }

    override fun toString(): String {
        return "${wifi.name}, ${wifi.bssid}, enter:${wifi.enter}, exit:${wifi.exit}, " +
            "dwellIn:${wifi.dwellInside}, dwellOut:${wifi.dwellOutside}, "
    }
}
