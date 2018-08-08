package de.leo.fingerprint.datacollector.jitai

/**
 * Created by Leo on 17.11.2017.
 * @param latLng midpoint of the geofence
 * @param radius of the geofence in meters
 */
abstract class MyAbstractGeofence(open var id: Int = -1,
                                  open var name: String,
                                  open val enter: Boolean,
                                  open val exit: Boolean,
                                  open val dwellInside: Boolean,
                                  open val dwellOutside: Boolean,
                                  open val loiteringDelay: Long,
                                  open val imageResId: Int) {

    init {
        if (!(exit || enter || dwellInside || dwellOutside))
            throw IllegalStateException("Geofence can never enter a positive state, because " +
                                            "enter, exit and dwell are false")
    }

    @Transient
    var enteredTimestamp: Long = Long.MAX_VALUE

    @Transient
    var exitedTimestamp: Long = Long.MAX_VALUE

    /**
     * true when the last status update happened inside the geofence
     */
    @Transient
    var entered = false
        protected set

    /**
     * true when the last status update happened outside of the geofence
     */
    @Transient
    var exited = !entered
        get() = !entered
    //do not use
        private set

    @Transient
    var loiteringInside = false
        protected set

    @Transient
    var loiteringOutside = false
        protected set

    /**
     * Update the state of the geofence.
     * Returns if the state changed.
     */
    internal fun update(timestamp: Long, vararg args: Any): Boolean {
        var stateChanged = false
        if (checkInside(*args)) {
            //inside
            if (!entered)
                stateChanged = true
            entered = true
            exitedTimestamp = Long.MAX_VALUE
            if (enteredTimestamp == Long.MAX_VALUE)
                enteredTimestamp = timestamp
            val loiteringBefore = loiteringInside
            loiteringInside = enteredTimestamp + loiteringDelay <= timestamp
            if (loiteringBefore != loiteringInside) {
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
            loiteringOutside = exitedTimestamp + loiteringDelay <= timestamp
            if (loiteringBefore != loiteringOutside) {
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
    internal fun updateAndCheck(timestamp: Long, vararg args: Any): Boolean {
        val stateChanged = update(timestamp, *args)
        return stateChanged && checkCondition()
    }

    internal fun checkCondition(timestamp: Long, vararg args: Any): Boolean {
        var inside = false
        var outside = false
        var loiteringInside = false
        var loiteringOutside = false
        if (checkInside(*args)) {
            inside = true
            if (enteredTimestamp + loiteringDelay <= timestamp)
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
        return enter && entered
            || exit && exited
            || dwellInside && loiteringInside
            || dwellOutside && loiteringOutside
    }

    abstract fun checkInside(vararg args: Any): Boolean
}