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
    fun loiteringInside(timestamp: Long): Boolean {
        loiteringInside = enteredTimestamp + loiteringDelay < timestamp
        return loiteringInside
    }

    @Transient
    private var loiteringOutside = false

    /**
     * true when entered for at least [loiteringDelay] milliseconds
     */
    fun loiteringOutside(timestamp: Long): Boolean {
        loiteringOutside = exitedTimestamp + loiteringDelay < timestamp
        return loiteringOutside
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
            exitedTimestamp = Long.MAX_VALUE
            if (enteredTimestamp == Long.MAX_VALUE)
                enteredTimestamp = timestamp
            val loiteringBefore = loiteringInside
            if (loiteringBefore != loiteringInside(timestamp))
                stateChanged = true
        } else {
            if (entered)
                stateChanged = true
            entered = false
            enteredTimestamp = Long.MAX_VALUE
            loiteringInside = false
            if (exitedTimestamp == Long.MAX_VALUE)
                exitedTimestamp = timestamp
            val loiteringBefore = loiteringOutside
            if (loiteringBefore != loiteringOutside(timestamp))
                stateChanged = true
        }
        return stateChanged
    }

    /**
     * Checkwithout updating
     */
    fun checkCondition(location: Location, timestamp: Long): Boolean {
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
     * Return the current state of the geofence for the given timestamp
     */
    fun checkCondition(): Boolean {
        return enter && entered()
            || exit && exited()
            || dwellInside && loiteringInside
            || dwellOutside && loiteringOutside
    }
}