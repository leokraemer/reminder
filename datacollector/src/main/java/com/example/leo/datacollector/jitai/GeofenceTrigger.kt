package com.example.leo.datacollector.jitai

import com.example.leo.datacollector.models.SensorDataSet

/**
 * Created by Leo on 16.11.2017.
 */
class GeofenceTrigger(val geofence: MyGeofence) : Trigger {
    override fun check(sensorData: SensorDataSet): Boolean = geofence.checkInside(sensorData.gps!!)
}