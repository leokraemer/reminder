package de.leo.fingerprint.datacollector.jitai

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 13.11.2017.
 */
interface Trigger {
    /**
     * Return true if the conditions for this trigger are met.
     */
    fun check(context : Context, sensorData : SensorDataSet) : Boolean

    /**
     * Reset the state of any stateful trigger to the initial state.
     */
    fun reset()
}