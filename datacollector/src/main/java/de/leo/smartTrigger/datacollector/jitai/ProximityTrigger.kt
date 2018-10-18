package de.leo.smartTrigger.datacollector.jitai

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet

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
        if (maxDistance.isNaN()) {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            maxDistance = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY).maximumRange.toDouble()
        }
        val values = db!!.getProximity(sensorData.time)
        if (values.isEmpty())
            return false
        return !(values.first().second < maxDistance) xor near
    }

    override fun reset(sensorData: SensorDataSet) {
        //noop
    }
}