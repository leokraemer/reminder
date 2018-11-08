package de.leo.smartTrigger.datacollector.jitai

import android.content.Context
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 11.01.2018.
 */
class LouderThanSoundTrigger(val threshold: Double, val interval: Long) :
    Trigger {


    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        val db = JitaiDatabase.getInstance(context)
        if (sensorData.ambientSound!! >= threshold)
            return true
        return db.getSoundData(sensorData.time - interval, sensorData.time)
            .any { v -> v.second >= threshold }
    }

    override fun reset(sensorData: SensorDataSet) {
        //noop
    }

    //wants to be checked again immediately
    override fun nextUpdate(): Long = 0
}


class LessLoudThanSoundTrigger(val threshold: Double, val interval: Long) :
    Trigger {

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        val db = JitaiDatabase.getInstance(context)
        if (sensorData.ambientSound!! < threshold)
            return !(db.getSoundData(sensorData.time - interval, sensorData.time)
                .any { v -> v.second > threshold })
        return false
    }

    override fun reset(sensorData: SensorDataSet) {
        //noop
    }

    //wants to be checked again immediately
    override fun nextUpdate(): Long = 0
}