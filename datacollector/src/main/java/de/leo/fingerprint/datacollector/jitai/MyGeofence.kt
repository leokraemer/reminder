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
                      val dwellInside: Boolean = false,
                      val dwellOutside: Boolean = false,
                      val loiteringDelay: Int,
                      val imageResId: Int) {

    init {
        if (!(exit || enter || dwellInside || dwellOutside))
            throw IllegalStateException("Geofence can never enter a positive state, because " +
                                            "enter, exit and dwell are false")
    }

    @Transient
    var enteredTimestamp: Long = Long.MAX_VALUE

    @Transient
    var exitedTimestamp: Long = Long.MAX_VALUE

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

    fun loiteringInside() = loiteringInside
    fun loiteringOutside() = loiteringOutside

    @Transient
    private var loiteringInside = false

    /**
     * true when entered for at least [loiteringDelay] milliseconds
     */
    private fun loiteringInside(timestamp: Long): Boolean {
        loiteringInside = enteredTimestamp + loiteringDelay < timestamp
        return loiteringInside
    }

    @Transient
    private var loiteringOutside = false

    /**
     * true when entered for at least [loiteringDelay] milliseconds
     */
    private fun loiteringOutside(timestamp: Long): Boolean {
        loiteringOutside = exitedTimestamp + loiteringDelay < timestamp
        return loiteringOutside
    }

    /**
     * Update the state of the geofence.
     * Returns if the state changed.
     */
    internal fun update(location: Location, timestamp: Long): Boolean {
        var stateChanged = false
        if (this.location.distanceTo(location) <= radius) {
            //inside
            if (!entered)
                stateChanged = true
            entered = true
            exitedTimestamp = Long.MAX_VALUE
            if (enteredTimestamp == Long.MAX_VALUE)
                enteredTimestamp = timestamp
            val loiteringBefore = loiteringInside
            if (loiteringBefore != loiteringInside(timestamp)) {
                stateChanged = true
                //reset to hit again after loiteringDelay millis
                enteredTimestamp = timestamp
            }
        } else {
            //outside
            if (entered)
                stateChanged = true
            entered = false
            enteredTimestamp = Long.MAX_VALUE
            loiteringInside = false
            if (exitedTimestamp == Long.MAX_VALUE)
                exitedTimestamp = timestamp
            val loiteringBefore = loiteringOutside
            if (loiteringBefore != loiteringOutside(timestamp)) {
                stateChanged = true
                //rest to hit again after loiteringDelay millis
                exitedTimestamp = timestamp
            }
        }
        return stateChanged
    }

    /**
     * Update the geofences state and return the new state. Main API function.
     */
    fun updateAndCheck(location: Location, timestamp: Long): Boolean {
        val stateChanged = update(location, timestamp)
        return stateChanged && checkCondition()
    }

    private fun checkCondition(location: Location, timestamp: Long): Boolean {
        var inside = false
        var outside = false
        var loiteringInside = false
        var loiteringOutside = false
        if (this.location.distanceTo(location) <= radius) {
            inside = true
            if (enteredTimestamp + loiteringDelay < timestamp)
                loiteringInside = true
        } else {
            outside = true
        }
        return enter && inside
            || exit && outside
            || dwellInside && loiteringInside
            || dwellOutside && loiteringOutside
    }

    /**
     * Check current state. Use in conjunction with [update]. Use [updateAndCheck] for simple
     * access.
     */
    internal fun checkCondition(): Boolean {
        return enter && entered()
            || exit && exited()
            || dwellInside && loiteringInside
            || dwellOutside && loiteringOutside
    }
}