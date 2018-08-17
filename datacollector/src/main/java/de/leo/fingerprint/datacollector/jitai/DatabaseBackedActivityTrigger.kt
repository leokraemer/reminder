package de.leo.fingerprint.datacollector.jitai;

import android.content.Context
import com.google.android.gms.location.DetectedActivity
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 16.11.2017.
 */

class DatabaseBackedActivityTrigger(override val activity: DetectedActivity,
                                    override val duration: Long) :
    ActivityTrigger(activity, duration) {

    init {
        lastTime = 0L
    }

    override fun reset() {
        lastTime = System.currentTimeMillis()
    }

    //in percent
    override val confidenceThreshold = 20
    private lateinit var db: JitaiDatabase

    private val percentageThreshold = 90 / 100

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        //check if the trigger was already reset
        if (sensorData.time - lastTime <= duration)
            return false
        if (!::db.isInitialized) db = JitaiDatabase.getInstance(context)
        val pastSensorData = db.getSensorDataSets(sensorData.time, sensorData.time - duration)
        val positiveMatches = pastSensorData.filter { sensorDataSet ->
            sensorDataSet.activity.any {
                it.type == this.activity.type && it.confidence >= confidenceThreshold
            }
        }
        return positiveMatches.count() >= (pastSensorData.count() * percentageThreshold)
    }
}
