package com.example.leo.datacollector.jitai

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.models.SensorDataSet

/**
 * Created by Leo on 11.01.2018.
 */
class ProximityTrigger(val near: Boolean) : Trigger {
    var maxDistance: Double = Double.NaN
    @Transient
    var db: JitaiDatabase? = null

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (db == null) {
            db = JitaiDatabase.getInstance(context)
        }
        if (maxDistance == Double.NaN) {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            maxDistance = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY).maximumRange.toDouble()
        }
        val values = db!!.getProximity(sensorData.time)
        if (values.isEmpty())
            return false
        return !(values.first().second < maxDistance) xor near
    }
}