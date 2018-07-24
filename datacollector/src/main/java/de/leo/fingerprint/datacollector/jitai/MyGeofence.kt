package de.leo.fingerprint.datacollector.jitai

import android.location.Location

/**
 * Created by Leo on 17.11.2017.
 * @param latLng midpoint of the geofence
 * @param radius of the geofence in meters
 */
data class MyGeofence(var id: Int = -1,
                      var name: String,
                      val latitude: Double = 0.0,
                      val longitude: Double = 0.0,
                      val radius: Float = 0.0F,
                      val enter: Boolean = false,
                      val exit: Boolean = false,
                      val dwell: Boolean = false,
                      val loiteringDelay: Int,
                      val imageResId: Int) {

    var enteredTimestamp: Long = Long.MAX_VALUE

    @Transient
    private val loc: Location = getLocation()

    fun getLocation(): Location {
        loc.latitude = latitude
        loc.longitude = longitude
        return loc
    }

    fun checkCondition(location: Location): Boolean {
        if (enter)
            if (loc.distanceTo(location) < radius)
                return true
        if (exit)
            if (loc.distanceTo(location) > radius)
                return true
        if (dwell) {
            if (loc.distanceTo(location) < radius) {
                if(enteredTimestamp == Long.MAX_VALUE)
                    enteredTimestamp = System.currentTimeMillis()
                if (enteredTimestamp + loiteringDelay < System.currentTimeMillis())
                    return true
            } else {
                enteredTimestamp = Long.MAX_VALUE
            }
        }
        return false
    }
}