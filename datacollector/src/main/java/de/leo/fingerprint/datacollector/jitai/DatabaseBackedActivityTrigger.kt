package de.leo.fingerprint.datacollector.jitai;

import android.content.Context
import com.google.android.gms.location.DetectedActivity
import de.leo.fingerprint.datacollector.datacollection.DataCollectorService
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 16.11.2017.
 */

class DatabaseBackedActivityTrigger(override val activity: DetectedActivity,
                                    override val duration: Long,
                                    val percentageThreshold: Double = 90.0 / 100.0) :
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

    private val errorMargin = DataCollectorService.UPDATE_DELAY

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        //check if the trigger was already reset
        if (sensorData.time - lastTime <= duration)
            return false
        if (!::db.isInitialized) db = JitaiDatabase.getInstance(context)
        val end = sensorData.time
        val begin = sensorData.time - duration
        val firstPositiveMustBeEarlierThan = begin + errorMargin
        //get relevant samples
        val pastSensorData = db.getSensorDataSets(begin, end)
        val positiveMatches = pastSensorData.filter { sensorDataSet ->
            sensorDataSet.activity.any {
                it.type == this.activity.type && it.confidence >= confidenceThreshold
            }
        }
        val firstPositiveTimestamp = positiveMatches.firstOrNull()?.time ?: 0
        if (firstPositiveTimestamp >= firstPositiveMustBeEarlierThan)
        //not enough data
            return false
        //test for percentage
        return positiveMatches.size >= (pastSensorData.size * percentageThreshold)
    }
}
