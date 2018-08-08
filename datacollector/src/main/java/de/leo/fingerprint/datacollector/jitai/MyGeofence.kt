package de.leo.fingerprint.datacollector.jitai

import android.location.Location

/**
 * Created by Leo on 17.11.2017.
 * @param latLng midpoint of the geofence
 * @param radius of the geofence in meters
 */
class MyGeofence(id: Int = -1,
                 name: String,
                 val latitude: Double = 0.0,
                 val longitude: Double = 0.0,
                 val radius: Float = 0.0F,
                 enter: Boolean,
                 exit: Boolean,
                 dwellInside: Boolean,
                 dwellOutside: Boolean,
                 loiteringDelay: Long,
                 imageResId: Int) : MyAbstractGeofence(id,
                                                       name,
                                                       enter,
                                                       exit,
                                                       dwellInside,
                                                       dwellOutside,
                                                       loiteringDelay,
                                                       imageResId) {
    override fun checkInside(vararg args: Any): Boolean =
        this.location.distanceTo(args[0] as Location) <= radius

    @delegate:Transient
    val location: Location by lazy {
        val location = Location(name)
        location.longitude = longitude
        location.latitude = latitude
        location
    }

    fun copy(id: Int = this.id,
             name: String = this.name,
             latitude: Double = this.latitude,
             longitude: Double = this.longitude,
             radius: Float = this.radius,
             enter: Boolean = this.enter,
             exit: Boolean = this.exit,
             dwellInside: Boolean = this.dwellInside,
             dwellOutside: Boolean = this.dwellOutside,
             loiteringDelay: Long = this.loiteringDelay,
             imageResId: Int = this.imageResId) = MyGeofence(id,
                                                             name,
                                                             latitude,
                                                             longitude,
                                                             radius,
                                                             enter,
                                                             exit,
                                                             dwellInside,
                                                             dwellOutside,
                                                             loiteringDelay,
                                                             imageResId)

}