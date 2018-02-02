package com.example.leo.datacollector.jitai

import android.location.Location
import com.google.android.gms.maps.model.LatLng

/**
 * Created by Leo on 17.11.2017.
 * @param latLng midpoint of the geofence
 * @param radius of the geofence
 */
class MyGeofence private constructor() {
    constructor(id: Int,
                name: String,
                latitude: Double,
                longitude: Double,
                radius: Float) : this() {
        this.id = id
        this.name = name
        this.latitude = latitude
        this.longitude = longitude
        this.radius = radius
    }

    lateinit var name: String
        private set
    var id: Int = -1
        private set
    var latitude: Double = 0.0
        private set
    var longitude: Double = 0.0
        private set
    var radius: Float = 0.0F
        private set

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