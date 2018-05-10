package de.leo.fingerprint.datacollector.jitai

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.database.TABLE_REALTIME_LIGHT
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 11.01.2018.
 */
class DimmerThanTrigger(val threshold: Double, val interval: Long) :
        Trigger {
    @Transient
    var db: JitaiDatabase? = null

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (db == null)
            db = JitaiDatabase.getInstance(context)
        return !(db!!.getSensorValues(sensorData.time - interval, sensorData.time,
                                      TABLE_REALTIME_LIGHT)
                .any { v -> v.second > threshold })
    }
}


class BrighterThanTrigger(val threshold: Double, val interval: Long) :
        Trigger {
    @Transient
    var db: JitaiDatabase? = null

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (db == null)
            db = JitaiDatabase.getInstance(context)
        return db!!.getSensorValues(sensorData.time - interval, sensorData.time,
                                    TABLE_REALTIME_LIGHT)
                .any { v -> v.second > threshold }
    }
}