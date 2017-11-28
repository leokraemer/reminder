package com.example.leo.datacollector.jitai

import com.example.leo.datacollector.models.SensorDataSet
import java.util.*

/**
 * Created by Leo on 13.11.2017.
 */
class Jitai(val name: String, val message: String, val triggers: List<Trigger>) {
    var geofence: String? = null
    var weather: String? = null
    var activity: String? = null
    var time: Date? = null

    fun check(sensorData: SensorDataSet) {
        if (triggers.foldRight(true, { trigger, acc -> acc && trigger.check(sensorData) })) {
            postNotification()
        }
    }

    private fun postNotification() {
    }

    fun getGeogenceString(): String {
        return geofence ?: ""
    }
}