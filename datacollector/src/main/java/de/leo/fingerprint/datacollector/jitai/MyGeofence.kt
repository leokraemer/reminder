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

    @Transient
    private var loc: Location? = null

    fun getLocation(): Location {
        if (loc == null) {
            loc = Location(name)
            loc!!.latitude = latitude
            loc!!.longitude = longitude
        }
        return loc!!
    }

    fun checkInside(location: Location): Boolean {
        if (loc == null) {
            loc = Location(name)
            loc!!.latitude = latitude
            loc!!.longitude = longitude
        }
        if (loc!!.distanceTo(location) < radius)
            return true
        return false
    }
}