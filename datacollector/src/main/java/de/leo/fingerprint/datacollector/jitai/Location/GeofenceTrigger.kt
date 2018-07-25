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
    private var state: Int = -1
    private var lastTime: Long = 0
    private val TIMEOUT = TimeUnit.MINUTES.toMillis(30)

    constructor(locations: List<MyGeofence>) : this() {
        this.locations = locations
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        //only one location -> no state checks necessary
        if (locations.size == 1) {
            val stateChanged = locations[0].update(sensorData.gps!!, sensorData.time)
            return stateChanged && locations[0].checkCondition()
        }

        var currentGeofence: Int = -1
        for (i in 0 until locations.size) {
            val stateChanged = locations[i].update(sensorData.gps!!, sensorData.time)
            if (stateChanged && locations[i].checkCondition())
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

    fun getCurrentLocation(): MyGeofence = if (state > -1) locations[state] else locations[0]

    private fun checkTimestamp(sensorDataTime: Long): Boolean {
        if (lastTime + TIMEOUT < sensorDataTime)
            return false
        return true
    }

    override fun toString(): String {
        return locations.fold("Geofences: ") { r, f -> r + f.name + " -> " }
            .trimEnd('-', '>', ' ')
    }
}