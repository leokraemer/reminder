package de.leo.smartTrigger.datacollector.jitai

import android.content.Context
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.manage.Jitai

/**
 * Created by Leo on 25.02.2018.
 */
class AskingTrigger(val jitaiId: Int, val validity: Long) : Trigger {
    override fun reset(sensorData: SensorDataSet) {
        //noop
    }

    val preconditions: List<Trigger> = mutableListOf()

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (preconditions.all { check(context, sensorData) }) {
            val events = JitaiDatabase.getInstance(context).getJitaiEvents(jitaiId)
            val latestYes = events.lastOrNull { it.eventType == Jitai.NOTIFICATION_TRIGGER_YES }
            val latestNo = events.lastOrNull { it.eventType == Jitai.NOTIFICATION_TRIGGER_NO }
            val yesTime = latestYes?.timestamp ?: -1
            val noTime = latestNo?.timestamp ?: 0
            if (noTime < yesTime && yesTime + validity > sensorData.time)
                return true
        }
        return false
    }
}