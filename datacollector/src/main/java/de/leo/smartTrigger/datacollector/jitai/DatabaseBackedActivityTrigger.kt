package de.leo.smartTrigger.datacollector.jitai;

import android.content.Context
import com.google.android.gms.location.DetectedActivity
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import java.util.*

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

    private lateinit var db: JitaiDatabase

    private val errorMargin = DataCollectorService.UPDATE_DELAY
    private var pastSensorData = ArrayDeque<SensorDataSet>()

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (lastTime == Long.MAX_VALUE)
            lastTime = sensorData.time
        //check if the trigger was already reset
        if (sensorData.time - lastTime < duration)
            return false

        val end = sensorData.time
        val begin = sensorData.time - duration
        val firstPositiveMustBeEarlierThan = begin + errorMargin
        if (pastSensorData.isEmpty()) {
            //get relevant samples from db if uninitialized
            if (!::db.isInitialized) db = JitaiDatabase.getInstance(context)
            pastSensorData.addAll(db.getSensorDataSets(begin, end))
        } else {
            while (pastSensorData.peek()?.let { it.time < begin } == true)
            //pop data if it expired
                pastSensorData.pop()
            //add online data
            pastSensorData.addLast(sensorData)
        }
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
        return positiveMatches.size > 0
            && positiveMatches.size >= (pastSensorData.size * percentageThreshold)
    }
}
