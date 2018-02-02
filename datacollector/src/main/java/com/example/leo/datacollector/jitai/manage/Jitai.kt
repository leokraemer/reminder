package com.example.leo.datacollector.jitai.manage

import android.content.Context
import android.util.Log
import com.example.leo.datacollector.activityDetection.MATCH
import com.example.leo.datacollector.activityDetection.NO_MATCH
import com.example.leo.datacollector.activityDetection.createFingerprint
import com.example.leo.datacollector.activityDetection.nominal
import com.example.leo.datacollector.database.*
import com.example.leo.datacollector.jitai.JitaiEvent
import com.example.leo.datacollector.jitai.Location.GeofenceTrigger
import com.example.leo.datacollector.jitai.TimeTrigger
import com.example.leo.datacollector.jitai.WeatherTrigger
import com.example.leo.datacollector.models.SensorDataSet
import com.example.leo.datacollector.notifications.NotificationService
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.uiThread
import weka.classifiers.functions.LibSVM
import weka.core.SelectedTag
import weka.filters.Filter.useFilter


/**
 * Created by Leo on 13.11.2017.
 */
class Jitai(val context: Context) {
    companion object {
        //all_conditions_for_jitai_met_reaching_out_to_user
        const val CONDITION_MET = 1
        //"the_notification_was_correct"
        const val NOTIFICATION_SUCCESS = 2
        //positive classification by user action
        const val NOW = 3
        //"the_notification_was_wrong"
        const val NOTIFICATION_FAIL = 4
        //"The_notification_timed_out_(conditions_not_met_any_more)"
        const val NOTIFICATION_NOT_VALID_ANY_MORE = 5
        //notification not sent not to bud the user
        const val TOO_FREQUENT_NOTIFICATIONS = 6
        //when the user deletes the notification without classifying
        const val NOTIFICATION_DELETED = 7
        const val JITAI_POSITIVE_PREDICTION = 8
        const val JITAI_NEGATIVE_PREDICTION = 9
    }

    var active = true
    var goal: String = ""
    var message: String = ""
    var id: Int = -1
    var timeTrigger: TimeTrigger? = null

    var geofenceTrigger: GeofenceTrigger? = null

    var weatherTrigger: WeatherTrigger? = null

    @Transient
    private var svm: LibSVM? = null

    @Transient
    private var db: JitaiDatabase? = null

    @Transient
    private var events: MutableList<JitaiEvent>? = null

    fun check(sensorData: SensorDataSet) {
        //check sensor only if it exists, otherwise true
        if ((timeTrigger == null || timeTrigger!!.check(context, sensorData))
                && (geofenceTrigger == null || geofenceTrigger!!.check(context, sensorData))
                && (weatherTrigger == null || weatherTrigger!!.check(context, sensorData)) &&
                checkSVM(sensorData)) {
            if (db == null) db = JitaiDatabase.getInstance(context)
            val newEvents = db!!.getJitaiEventsForTraining(id)
            //update classifier only on significant change
            if (events == null || events!!.size < newEvents.size - 2) {
                events = newEvents
                updateClassfier(sensorData)
            }
            postNotification(id, sensorData.time, goal, message, sensorData.id)
        } else {
            //removeNotification(id, sensorData.time, sensorData.id)
        }
    }

    private fun updateClassfier(sensorData: SensorDataSet) {
        if (events!!.size == 0) {
            val fingerprint = createFingerprint(this, context, sensorData, MATCH)
            svm = LibSVM()
            svm!!.buildClassifier(fingerprint)
        } else if (events!!.size > 0) {
            val time = System.currentTimeMillis()
            Log.d("svm start train", time.toString())
            val sensorData = db!!.getSensorDataset(events!!)
            if (sensorData.size > 0) {
                doAsync {
                    val fingerprints = createFingerprint(this@Jitai, context, sensorData)
                    val svm = LibSVM() //defult is rbf kernel
                    Log.d("svm instances done",
                          (System.currentTimeMillis() - time).toString())
                    svm!!.buildClassifier(fingerprints)
                    Log.d("svm rebuilding done",
                          (System.currentTimeMillis() - time).toString())
                    uiThread {
                        this@Jitai.svm = svm
                    }
                }
            } else {
                Log.d("svm rebuild cancelled", (System.currentTimeMillis() - time)
                        .toString())
            }
        }
    }

    private fun checkSVM(sensorData: SensorDataSet): Boolean {
        if (svm != null) {
            val fingerprint = createFingerprint(this, context, sensorData, NO_MATCH)
            val prediction = svm!!.classifyInstance(fingerprint[0])
            if (nominal[prediction.toInt()] == MATCH) {
                db!!.enterJitaiEvent(id, sensorData.time, JITAI_POSITIVE_PREDICTION, sensorData.id)
                Log.d("prediction", "match")
                return true
            } else {
                db!!.enterJitaiEvent(id, sensorData.time, JITAI_NEGATIVE_PREDICTION, sensorData.id)
                /*if (Math.random() < 0.3) {
                    Log.d("prediction", "no_match - still posting notification, because random")
                    return true
                } else {*/
                    Log.d("prediction", "no_match")
                    return false
                //}
            }
        }
        //send initial event
        return true
    }

    private fun removeNotification(id: Int, timestamp: Long, sensorDataId: Long) {
        JitaiDatabase.getInstance(context).enterJitaiEvent(id,
                                                           timestamp,
                                                           NOTIFICATION_NOT_VALID_ANY_MORE,
                                                           sensorDataId)
        val intent = context.intentFor<NotificationService>(JITAI_ID to id,
                                                            JITAI_EVENT to
                                                                    NOTIFICATION_NOT_VALID_ANY_MORE,
                                                            JITAI_EVENT_SENSORDATASET_ID to sensorDataId)
        context.startService(intent)
    }

    private fun postNotification(id: Int, timestamp: Long, goal: String, message: String,
                                 sensorDataId: Long) {
        JitaiDatabase.getInstance(context).enterJitaiEvent(id,
                                                           timestamp,
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