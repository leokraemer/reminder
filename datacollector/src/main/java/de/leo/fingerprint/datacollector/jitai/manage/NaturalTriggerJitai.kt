package de.leo.fingerprint.datacollector.jitai.manage

import android.content.Context
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.jitai.ActivityTrigger
import de.leo.fingerprint.datacollector.jitai.Location.GeofenceTrigger
import de.leo.fingerprint.datacollector.jitai.TimeTrigger
import de.leo.fingerprint.datacollector.jitai.WifiTrigger
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.EVERYWHERE
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.everywhere_geofence
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel

open class NaturalTriggerJitai(context: Context, val naturalTriggerModel: NaturalTriggerModel) : Jitai
                                                                                            (context) {

    val wifiTrigger: WifiTrigger?

    init {
        goal = naturalTriggerModel.goal
        message = naturalTriggerModel.message
        timeTrigger = TimeTrigger(naturalTriggerModel.beginTime.rangeTo(naturalTriggerModel.endTime),
                                  TimeTrigger.ALL_DAYS)
        geofenceTrigger = naturalTriggerModel.geofence?.let {
            if (it.name != EVERYWHERE)
                GeofenceTrigger(listOf(it))
            else null
        }
        wifiTrigger = naturalTriggerModel.wifi?.let { WifiTrigger(it) }
        activitTrigger = naturalTriggerModel.activity.map {
            ActivityTrigger(DetectedActivity(it, 0), naturalTriggerModel.timeInActivity)
        }
    }

    override fun check(sensorData: SensorDataSet): Boolean {
        Log.d(goal, "${sensorData.time}, ${sensorData.activity.firstOrNull()?.toString()}")
        //update all triggers to trigger as early as possible
        val activityTriggered = activitTrigger == null || activitTrigger!!.isEmpty()
            || activitTrigger!!.any { it.check(context, sensorData) }
        val geofenceTriggered =
            geofenceTrigger == null || geofenceTrigger!!.check(context, sensorData)
        val wifiTriggered = wifiTrigger == null || wifiTrigger.check(context, sensorData)
        val weatherTriggered = weatherTrigger == null || weatherTrigger!!.check(context, sensorData)
        val timeTriggered = timeTrigger == null || timeTrigger!!.check(context, sensorData)

        if (activityTriggered && geofenceTriggered && wifiTriggered && weatherTriggered && timeTriggered) {
            postNotification(id, sensorData.time, goal, message, sensorData.id)
            geofenceTrigger?.reset()
            activitTrigger?.forEach { it.reset() }
            return true
        } else {
            //removeNotification(id, sensorData.time, sensorData.id)
            return false
        }
    }
}