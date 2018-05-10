package de.leo.fingerprint.datacollector.jitai

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 25.02.2018.
 */
class AskingTrigger(val jitaiId: Int, val validity: Long) : Trigger {
    val preconditions: List<Trigger> = mutableListOf()

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (preconditions.all { check(context, sensorData) }) {
            val events = JitaiDatabase.getInstance(context).getJitaiTriggerEvents(jitaiId)
            val latestYes = events.lastOrNull { it.event == Jitai.NOTIFICATION_TRIGGER_YES }
            val latestNo = events.lastOrNull { it.event == Jitai.NOTIFICATION_TRIGGER_NO }
            val yesTime = latestYes?.timestamp ?: -1
            val noTime = latestNo?.timestamp ?: 0
            if (noTime < yesTime && yesTime + validity > sensorData.time )
                return true
        }
        return false
    }
}