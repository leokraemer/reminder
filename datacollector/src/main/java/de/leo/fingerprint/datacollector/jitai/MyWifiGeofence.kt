package de.leo.fingerprint.datacollector.jitai

import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.LocationSelection
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.WIFI_CODE

/**
 * Created by Leo on 17.11.2017.
 * @param latLng midpoint of the geofence
 * @param radius of the geofence in meters
 */
class MyWifiGeofence(id: Int = -1,
                     val bssid: String,
                     val rssi: Int = -126,
                     enter: Boolean,
                     exit: Boolean,
                     dwellInside: Boolean,
                     dwellOutside: Boolean,
                     loiteringDelay: Long) : MyAbstractGeofence(id = id,
                                                                name = LocationSelection.WIFI,
                                                                enter = enter,
                                                                exit = exit,
                                                                dwellInside = dwellInside,
                                                                dwellOutside = dwellOutside,
                                                                loiteringDelay = loiteringDelay,
                                                                imageResId = WIFI_CODE) {
    override fun checkInside(vararg args: Any) = checkIfInside(args[0] as String, args[1] as Int)


    private fun checkIfInside(bssid: String, rssi: Int) =
        this.bssid == bssid && this.rssi <= rssi

    fun copy(id: Int = this.id,
             bssid: String = this.bssid,
             rssi: Int = this.rssi,
             enter: Boolean = this.enter,
             exit: Boolean = this.exit,
             dwellInside: Boolean = this.dwellInside,
             dwellOutside: Boolean = this.dwellOutside,
             loiteringDelay: Long = this.loiteringDelay) = MyWifiGeofence(id,
                                                                          bssid,
                                                                          rssi,
                                                                          enter,
                                                                          exit,
                                                                          dwellInside,
                                                                          dwellOutside,
                                                                          loiteringDelay)
}