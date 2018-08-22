package de.leo.fingerprint.datacollector.jitai.manage

import android.content.Context
import android.preference.PreferenceManager
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.*
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.ui.notifications.NotificationService
import org.jetbrains.anko.intentFor


/**
 * Created by Leo on 13.11.2017.
 */
abstract class Jitai(val context: Context) {
    companion object {
        //all_conditions_for_jitai_met_reaching_out_to_user
        const val CONDITION_MET = 1
        //"the_notification_was_correct"
        const val NOTIFICATION_SUCCESS = 2
        //positive classification by user action
        const val NOW = 3
        //"the_notification_was_wrong"
        const val NOTIFICATION_FAIL = 4
        //sooze for 15 minutes
        const val NOTIFICATION_SNOOZE = 12
        const val NOTIFICATION_SNOOZE_FINISHED = 13
        //"The_notification_timed_out_(conditions_not_met_any_more)"
        const val NOTIFICATION_NOT_VALID_ANY_MORE = 5
        //notification not sent not to bud the user
        const val TOO_FREQUENT_NOTIFICATIONS = 6
        //when the user deletes the notification without classifying
        const val NOTIFICATION_DELETED = 7
        const val JITAI_POSITIVE_PREDICTION = 8
        const val JITAI_NEGATIVE_PREDICTION = 9
        const val JITAI_CLASSIFIER_UNINITIALIZED = 10
        const val JITAI_CLASSIFIER_UPDATED = 11

        //for notification trigger
        const val NOTIFICATION_TRIGGER = 100000
        const val NOTIFICATION_TRIGGER_YES = NOTIFICATION_TRIGGER + 1
        const val NOTIFICATION_TRIGGER_NO = NOTIFICATION_TRIGGER + 2
        const val NOTIFICATION_TRIGGER_DELETE = NOTIFICATION_TRIGGER + 3
        const val NOTIFICATION_FULL_SCREEN = NOTIFICATION_TRIGGER + 4
    }

    var active = true
    abstract val goal: String
    abstract val message: String
    abstract var id: Int

    internal val db: JitaiDatabase by lazy { JitaiDatabase.getInstance(context) }
    private val userName: String by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.user_name), null)
    }


    abstract fun check(sensorData: SensorDataSet): Boolean

    private fun removeNotification(id: Int, timestamp: Long, sensorDataId: Long) {
        JitaiDatabase.getInstance(context).enterUserJitaiEvent(id,
                                                               timestamp,
                                                               userName,
                                                               NOTIFICATION_NOT_VALID_ANY_MORE,
                                                               sensorDataId)
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
                                                               sensorDataId)
        val intent = context.intentFor<NotificationService>(JITAI_ID to id,
                                                            JITAI_EVENT to CONDITION_MET,
                                                            JITAI_GOAL to goal,
                                                            JITAI_MESSAGE to message,
                                                            JITAI_EVENT_SENSORDATASET_ID to sensorDataId)
        context.startService(intent)
    }
}