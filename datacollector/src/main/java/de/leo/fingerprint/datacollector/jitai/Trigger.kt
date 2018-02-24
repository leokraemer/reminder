package de.leo.fingerprint.datacollector.jitai

import android.content.Context
import de.leo.fingerprint.datacollector.models.SensorDataSet

/**
 * Created by Leo on 13.11.2017.
 */
interface Trigger {
    /**
     * Return true if the conditions for this trigger are met.
     */
    fun check(context : Context, sensorData : SensorDataSet) : Boolean
}