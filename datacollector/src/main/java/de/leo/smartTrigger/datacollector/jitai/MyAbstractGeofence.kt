package de.leo.smartTrigger.datacollector.jitai

import android.support.annotation.VisibleForTesting
import android.util.Log
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase

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

    @Transient
    var lastTimestamp: Long = 0

    /**
     * Update the state of the geofence.
     * Returns if the state changed.
     */
    internal fun update(timestamp: Long, vararg args: Any): Boolean {
        var stateChanged = false
        if (checkInside(*args)) {
            //inside
            if (!entered) {
                stateChanged = true
                Log.d("TAG", "entering $name")
            }
            entered = true
            exitedTimestamp = Long.MAX_VALUE
            loiteringOutside = false
            if (enteredTimestamp == Long.MAX_VALUE)
                enteredTimestamp = timestamp
            //loiteringbefore is invalid, if the update interval is smaller than loiteringDelay
            val loiteringBefore = loiteringInside && lastTimestamp - timestamp > loiteringDelay
            loiteringInside = enteredTimestamp + loiteringDelay <= timestamp
            if (loiteringBefore != loiteringInside && dwellInside) {
                stateChanged = true
                //reset to hit again after loiteringDelay millis
                enteredTimestamp = timestamp
                Log.d("TAG", "loitering in $name for $loiteringDelay millis")
            }
        } else {
            //outside
            if (entered) {
                stateChanged = true
                Log.d("TAG", "exiting $name")
            }
            entered = false
            enteredTimestamp = Long.MAX_VALUE
            loiteringInside = false
            if (exitedTimestamp == Long.MAX_VALUE)
                exitedTimestamp = timestamp
            val loiteringBefore = loiteringOutside && lastTimestamp - timestamp > loiteringDelay
            loiteringOutside = exitedTimestamp + loiteringDelay <= timestamp
            if (loiteringBefore != loiteringOutside && dwellOutside) {
                stateChanged = true
                //rest to hit again after loiteringDelay millis
                exitedTimestamp = timestamp
                Log.d("TAG", "loitering outside $name for $loiteringDelay millis")
            }
        }
        lastTimestamp = timestamp
        return stateChanged
    }

    lateinit var db: JitaiDatabase
    /**
     * Update the geofences state and return the new state. Main API function.
     */
    internal fun updateAndCheck(timestamp: Long, vararg args: Any): Boolean {
        val stateChanged = update(timestamp, *args)
        return stateChanged && checkCondition()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MyAbstractGeofence

        if (id != other.id) return false
        if (name != other.name) return false
        if (enter != other.enter) return false
        if (exit != other.exit) return false
        if (dwellInside != other.dwellInside) return false
        if (dwellOutside != other.dwellOutside) return false
        if (loiteringDelay != other.loiteringDelay) return false
        if (imageResId != other.imageResId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + enter.hashCode()
        result = 31 * result + exit.hashCode()
        result = 31 * result + dwellInside.hashCode()
        result = 31 * result + dwellOutside.hashCode()
        result = 31 * result + loiteringDelay.hashCode()
        result = 31 * result + imageResId
        return result
    }
}