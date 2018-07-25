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

    init {
        if (!(exit || enter || dwell))
            throw IllegalStateException("Geofence can never enter a positive state, because " +
                                            "enter, exit and dwell are false")
    }

    @Transient
    var enteredTimestamp: Long = Long.MAX_VALUE

    @delegate:Transient
    val location: Location by lazy {
        val location = Location(name)
        location.longitude = longitude
        location.latitude = latitude
        location
    }
    /**
     * true when the last status update happened inside the geofence
     */
    @Transient
    private var entered = false

    fun entered() = entered

    /**
     * true when the last status update happened outside of the geofence
     */
    fun exited() = !entered

    @Transient
    private var loitering = false

    /**
     * true when entered for at least [loiteringDelay] milliseconds
     */
    fun loitering(timestamp: Long): Boolean {
        loitering = enteredTimestamp + loiteringDelay < timestamp
        return loitering
    }

    /**
     * Update the state of the geofence
     * Returns if the state changed
     */
    fun update(location: Location, timestamp: Long): Boolean {
        var stateChanged = false
        if (this.location.distanceTo(location) <= radius) {
            if (!entered)
                stateChanged = true
            entered = true
            if (enteredTimestamp == Long.MAX_VALUE)
                enteredTimestamp = timestamp
            val loiteringBefore = loitering
            if (loiteringBefore != loitering(timestamp))
                stateChanged = true
        } else {
            if (entered)
                stateChanged = true
            entered = false
            loitering = false
            enteredTimestamp = Long.MAX_VALUE
        }
        return stateChanged
    }

    /**
     * Checkwithout updating
     */
    fun checkCondition(location: Location, timestamp: Long): Boolean {
        var inside = false
        var outside = false
        var loitering = false
        if (this.location.distanceTo(location) <= radius) {
            inside = true
            if (enteredTimestamp + loiteringDelay < timestamp)
                loitering = true
        } else {
            outside = true
        }
        return enter && inside || exit && outside || dwell && loitering
    }

    /**
     * Return the current state of the geofence for the given timestamp
     */
    fun checkCondition(): Boolean {
        return enter && entered() || exit && exited() || dwell && loitering
    }
}