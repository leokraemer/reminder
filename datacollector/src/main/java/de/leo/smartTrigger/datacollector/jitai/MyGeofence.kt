package de.leo.smartTrigger.datacollector.jitai

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MyGeofence

        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (radius != other.radius) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + radius.hashCode()
        return result
    }


}