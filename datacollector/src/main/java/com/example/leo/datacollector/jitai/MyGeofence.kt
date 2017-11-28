package com.example.leo.datacollector.jitai

import android.location.Location
import com.google.android.gms.maps.model.LatLng

/**
 * Created by Leo on 17.11.2017.
 * @param latLng midpoint of the geofence
 * @param radius of the geofence
 */
class MyGeofence(val latLng: LatLng, val radius: Float) {
    val loc: Location

    init {
        loc = Location("")
        loc.latitude = this.latLng.latitude
        loc.longitude = this.latLng.latitude
    }

    fun checkInside(latLng: LatLng): Boolean {
        val loc = Location("")
        loc.latitude = latLng.latitude
        loc.longitude = latLng.latitude
        return checkInside(loc)
    }

    fun checkInside(location: Location): Boolean {
        if (loc.distanceTo(location) < radius)
            return true
        return false
    }
}