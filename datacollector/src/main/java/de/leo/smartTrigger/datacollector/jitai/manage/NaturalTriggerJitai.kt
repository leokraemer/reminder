package de.leo.smartTrigger.datacollector.jitai.manage

import android.content.Context
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.*
import de.leo.smartTrigger.datacollector.jitai.Location.GeofenceTrigger
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.EVERYWHERE
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel

open class NaturalTriggerJitai(override var id: Int,
                               context: Context,
                               val naturalTriggerModel: NaturalTriggerModel) : Jitai(context) {
    override val message: String
        get() = naturalTriggerModel.goal
    override val goal: String
        get() = naturalTriggerModel.message

    val wifiTrigger: WifiTrigger?
    val timeTrigger: TimeTrigger?

    val geofenceTrigger: GeofenceTrigger?

    val weatherTrigger: WeatherTrigger? = null

    val activitTrigger: ActivityTrigger?

    init {
        timeTrigger = TimeTrigger(naturalTriggerModel.beginTime!!.rangeTo(naturalTriggerModel
                                                                              .endTime!!),
                                  TimeTrigger.ALL_DAYS)
        geofenceTrigger = naturalTriggerModel.geofence?.let {
            if (it.name != EVERYWHERE)
                GeofenceTrigger(listOf(it))
            else null
        }
        wifiTrigger = naturalTriggerModel.wifi?.let { WifiTrigger(it) }
        activitTrigger = if (naturalTriggerModel.activity.isNotEmpty()) {
            DatabaseBackedActivityTrigger(naturalTriggerModel.activity.map {
                DetectedActivity(it, 0)
            }, naturalTriggerModel.timeInActivity)
        } else null
    }

    override fun check(sensorData: SensorDataSet): Boolean {
        //Log.d(goal, "${sensorData.time}, ${sensorData.activity.firstOrNull()?.toString()}")
        //update all triggers to trigger as early as possible
        val activityTriggered = activitTrigger == null || activitTrigger.check(context, sensorData)
        val geofenceTriggered =
            geofenceTrigger == null || geofenceTrigger.check(context, sensorData)
        val wifiTriggered = wifiTrigger == null || wifiTrigger.check(context, sensorData)
        val weatherTriggered = weatherTrigger == null || weatherTrigger.check(context, sensorData)
        val timeTriggered = timeTrigger == null || timeTrigger.check(context, sensorData)

        if (activityTriggered && geofenceTriggered && wifiTriggered && weatherTriggered && timeTriggered) {
            Log.d("hit", "$naturalTriggerModel ${sensorData.time}, ${sensorData.activity}")
            postNotification(id, sensorData.time, goal, message, sensorData.id)
            geofenceTrigger?.reset(sensorData)
            wifiTrigger?.reset(sensorData)
            activitTrigger?.reset(sensorData)
            return true
        } else {
            //removeNotification(id, sensorData.time, sensorData.id)
            return false
        }
    }
}