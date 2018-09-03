package de.leo.smartTrigger.datacollector.jitai;

import android.content.Context
import com.google.android.gms.location.DetectedActivity
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 16.11.2017.
 */

class DatabaseBackedActivityTrigger(override val activities: List<DetectedActivity>,
                                    override val duration: Long,
                                    val percentageThreshold: Double = 90.0 / 100.0) :
    ActivityTrigger(activities, duration) {

    init {
        lastTime = 0L
    }

    override fun reset() {
        lastTime = Long.MAX_VALUE
    }

    //in percent
    override val confidenceThreshold = 20
    private lateinit var db: JitaiDatabase

    private val errorMargin = DataCollectorService.UPDATE_DELAY

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (lastTime == Long.MAX_VALUE)
            lastTime = sensorData.time
        //check if the trigger was already reset
        if (sensorData.time - lastTime < duration)
            return false
        if (!::db.isInitialized) db = JitaiDatabase.getInstance(context)
        val end = sensorData.time
        val begin = sensorData.time - duration
        val firstPositiveMustBeEarlierThan = begin + errorMargin
        //get relevant samples
        val pastSensorData = db.getSensorDataSets(begin, end)
        val positiveMatches = pastSensorData.filter { sensorDataSet ->
            sensorDataSet.activity.any { outer ->
                activities.any { inner ->
                    inner.type == outer.type && outer.confidence >= confidenceThreshold
                }
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
