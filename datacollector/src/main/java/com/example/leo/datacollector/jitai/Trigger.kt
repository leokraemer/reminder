package com.example.leo.datacollector.jitai

import com.example.leo.datacollector.models.SensorDataSet

/**
 * Created by Leo on 13.11.2017.
 */
interface Trigger {
    /**
     * Return true if the conditions for this trigger are met.
     */
    fun check(sensorData : SensorDataSet) : Boolean
}