package com.example.leo.datacollector.jitai

import android.content.Context
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.models.SensorDataSet

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
}