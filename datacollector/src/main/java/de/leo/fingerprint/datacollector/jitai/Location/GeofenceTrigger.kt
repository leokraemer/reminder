package de.leo.fingerprint.datacollector.jitai.Location

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import de.leo.fingerprint.datacollector.jitai.Trigger
import java.util.concurrent.TimeUnit

/**
 * Created by Leo on 16.11.2017.
 */
class GeofenceTrigger() : Trigger {
    override fun reset() {
        state = 0
    }

    private lateinit var locations: List<MyGeofence>
    private var state: Int = 0
    private var lastTime: Long = 0
    private val timestamp = System.currentTimeMillis()
    private val TIMEOUT = TimeUnit.MINUTES.toMillis(30)

    constructor(locations: List<MyGeofence>) : this() {
        this.locations = locations
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        //only one location -> no state checks necessary
        if (locations.size == 1)
            return locations[0].checkInside(sensorData.gps!!)

        var currentGeofence: Int = -1
        for (i in 0 until locations.size) {
            if (locations[i].checkInside(sensorData.gps!!))
                currentGeofence = i
        }
        //not inside a geofence -> dont update timeout
        if (currentGeofence == -1)
            return false
        //only update timeout on transition
        if (currentGeofence != state)
            lastTime = sensorData.time

        //check if we actually advanced
        if (currentGeofence == state + 1) {
            state++
            if (state == locations.size - 1)
                return checkTimestamp(sensorData.time)
        } else {
            if (currentGeofence == state) {
                //we stay in the current state
                //if in final state return true
                if (state == locations.size - 1)
                    return checkTimestamp(sensorData.time)
                return false
            } else {
                state = 0
            }
        }
        return false
    }

    fun getCurrentLocation(): MyGeofence = locations[state]

    private fun checkTimestamp(sensorDataTime: Long): Boolean {
        if (timestamp + TIMEOUT < sensorDataTime)
            return false
        return true
    }

    override fun toString(): String {
        return locations.fold("Geofences: ", { r, f ->
            r + f.name + " -> "
        }).trimEnd('-', '>', ' ')
    }
}