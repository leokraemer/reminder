package de.leo.smartTrigger.datacollector.jitai.manage

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.*
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.*
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.EVERYWHERE
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import de.leo.smartTrigger.datacollector.ui.notifications.NotificationService
import org.jetbrains.anko.intentFor


/**
 * Created by Leo on 13.11.2017.
 */
open class NaturalTriggerJitai(var id: Int,
                               val context: Context,
                               val naturalTriggerModel: NaturalTriggerModel) {
    companion object {
        //all_conditions_for_jitai_met_reaching_out_to_user
        const val CONDITION_MET = "condition_met"
        //"the_notification_was_correct"
        const val NOTIFICATION_SUCCESS = "correct moment"
        //"the_notification_was_wrong"
        const val NOTIFICATION_FAIL = "wrong moment"
        //sooze for 15 minutes
        const val NOTIFICATION_SNOOZE = "snooze"
        const val NOTIFICATION_SNOOZE_FINISHED = "snooze finished"
        //"The_notification_timed_out_(conditions_not_met_any_more)"
        const val NOTIFICATION_NOT_VALID_ANY_MORE = "timed out"
        //notification not sent not to bud the user
        const val TOO_FREQUENT_NOTIFICATIONS = "too many"
        //when the user deletes the notification without classifying
        const val NOTIFICATION_DELETED = "dismissed"
        const val SURVEY_ABORD = "survey abort"

        //for notification trigger
        const val NOTIFICATION_TRIGGER = "notification "
        const val NOTIFICATION_TRIGGER_YES = NOTIFICATION_TRIGGER + "yes"
        const val NOTIFICATION_TRIGGER_NO = NOTIFICATION_TRIGGER + "no"
        const val NOTIFICATION_TRIGGER_DELETE = NOTIFICATION_TRIGGER + "delete"
        const val NOTIFICATION_FULL_SCREEN = NOTIFICATION_TRIGGER + "fullscreen"
    }

    var active = true

    internal val db: JitaiDatabase by lazy { JitaiDatabase.getInstance(context) }
    private val userName: String by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.user_name), null)
    }

    val message: String
        get() = naturalTriggerModel.goal
    val goal: String
        get() = naturalTriggerModel.message

    val wifiTrigger: WifiTrigger?
    val timeTrigger: TimeTrigger?

    val geofenceTrigger: GeofenceTrigger?

    val weatherTrigger: WeatherTrigger? = null

    val activitTrigger: ActivityTrigger?

    init {
        timeTrigger = TimeTrigger(naturalTriggerModel.beginTime!!
                                      .rangeTo(naturalTriggerModel.endTime!!),
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

    fun check(sensorData: SensorDataSet): Boolean {
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

    private fun removeNotification(id: Int, timestamp: Long, sensorDataId: Long) {
        JitaiDatabase.getInstance(context).enterUserJitaiEvent(id,
                                                               timestamp,
                                                               userName,
                                                               NOTIFICATION_NOT_VALID_ANY_MORE,
                                                               sensorDataId,
                                                               -1,
                                                               -1,
                                                               "")
        val intent = context.intentFor<NotificationService>(JITAI_ID to id,
                                                            JITAI_EVENT to
                                                                NOTIFICATION_NOT_VALID_ANY_MORE,
                                                            JITAI_EVENT_SENSORDATASET_ID to sensorDataId)
        context.startService(intent)
    }

    internal open fun postNotification(id: Int, timestamp: Long, goal: String, message: String,
                                       sensorDataId: Long) {
        JitaiDatabase.getInstance(context).enterUserJitaiEvent(id,
                                                               timestamp,
                                                               userName,
                                                               CONDITION_MET,
                                                               sensorDataId,
                                                               -1,
                                                               -1,
                                                               "")
        val intent = context.intentFor<NotificationService>(JITAI_ID to id,
                                                            JITAI_EVENT to CONDITION_MET,
                                                            JITAI_GOAL to goal,
                                                            JITAI_MESSAGE to message,
                                                            JITAI_EVENT_SENSORDATASET_ID to sensorDataId)
        context.startService(intent)
    }

    internal fun nextUpdate(): Long {
        return listOf(activitTrigger, geofenceTrigger, timeTrigger, wifiTrigger, weatherTrigger)
            .map { it?.nextUpdate() ?: 0 }.max() ?: 0
    }
}