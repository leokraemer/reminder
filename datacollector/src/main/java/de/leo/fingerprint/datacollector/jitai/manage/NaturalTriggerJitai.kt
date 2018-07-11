package de.leo.fingerprint.datacollector.jitai.manage

import android.content.Context
import com.google.android.gms.location.DetectedActivity
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.jitai.ActivityTrigger
import de.leo.fingerprint.datacollector.jitai.Location.GeofenceTrigger
import de.leo.fingerprint.datacollector.jitai.TimeTrigger
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel

class NaturalTriggerJitai(context: Context, val naturalTriggerModel: NaturalTriggerModel) : Jitai
                                                                                            (context) {

    init {
        goal = naturalTriggerModel.goal
        message = naturalTriggerModel.message
        timeTrigger = TimeTrigger(naturalTriggerModel.beginTime.rangeTo(naturalTriggerModel.endTime),
                                  TimeTrigger.ALL_DAYS)
        geofenceTrigger = naturalTriggerModel.geofence?.let { GeofenceTrigger(listOf(it)) }
        activitTrigger = naturalTriggerModel.activity?.map {
            ActivityTrigger(
                DetectedActivity(it, 0),
                naturalTriggerModel.geofence!!.loiteringDelay.toLong()
                           )
        }
    }

    override fun check(sensorData: SensorDataSet) {
        if ((timeTrigger == null || timeTrigger!!.check(context, sensorData))
            && (geofenceTrigger == null || geofenceTrigger!!.check(context, sensorData))
            && (weatherTrigger == null || weatherTrigger!!.check(context, sensorData))
            && (activitTrigger == null || activitTrigger!!.isEmpty()
                || activitTrigger!!.any { it.check(context, sensorData) })
        ) {
            postNotification(id, sensorData.time, goal, message, sensorData.id)
            geofenceTrigger?.reset()
            activitTrigger?.forEach { it.reset() }
        } else {
            //removeNotification(id, sensorData.time, sensorData.id)
        }
    }
}