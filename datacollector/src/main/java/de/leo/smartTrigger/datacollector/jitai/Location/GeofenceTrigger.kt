package de.leo.smartTrigger.datacollector.jitai.Location

import android.content.Context
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.MyGeofence
import de.leo.smartTrigger.datacollector.jitai.Trigger
import java.util.concurrent.TimeUnit

/**
 * Created by Leo on 16.11.2017.
 */
open class GeofenceTrigger() : Trigger {
    override fun reset(sensorData: SensorDataSet) {
        state = 0
        locations = locations.map { if (it.dwellInside || it.dwellOutside) it.copy() else it }
    }

    private lateinit var locations: List<MyGeofence>
    private var state: Int = -1
    private var lastTime: Long = 0
    private val TIMEOUT = TimeUnit.HOURS.toMillis(24)
    private lateinit var db: JitaiDatabase

    constructor(locations: List<MyGeofence>) : this() {
        this.locations = locations
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        //only one locationName -> no state checks necessary
        if (locations.size == 1) {
            val statechanged = locations[0].update(sensorData.time, sensorData.gps!!)
            val returnval = locations[0].checkCondition()
            if (!::db.isInitialized) db = JitaiDatabase.getInstance(context)
            if (statechanged)
                db.enterGeofenceEvent(sensorData.time, locations[0].id, locations[0].name,
                                      "${locations[0].entered}," +
                                          "${locations[0].exited}," +
                                          "${locations[0].loiteringInside}," +
                                          "${locations[0].loiteringOutside}")
            return returnval && statechanged
        }

        //########################currently unused code, because paths are not enabled in the
        //######################## trigger creation
        var currentGeofence: Int = -1
        for (i in 0 until locations.size) {
            if (locations[i].updateAndCheck(sensorData.time, sensorData.gps!!))
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