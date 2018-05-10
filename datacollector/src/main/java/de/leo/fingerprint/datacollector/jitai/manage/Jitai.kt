package de.leo.fingerprint.datacollector.jitai.manage

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import de.leo.fingerprint.datacollector.EntryActivity
import de.leo.fingerprint.datacollector.activityDetection.*
import de.leo.fingerprint.datacollector.database.*
import de.leo.fingerprint.datacollector.datacollection.database.*
import de.leo.fingerprint.datacollector.jitai.JitaiEvent
import de.leo.fingerprint.datacollector.jitai.Location.GeofenceTrigger
import de.leo.fingerprint.datacollector.jitai.TimeTrigger
import de.leo.fingerprint.datacollector.jitai.WeatherTrigger
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.jitai.activityDetection.*
import de.leo.fingerprint.datacollector.ui.notifications.NotificationService
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.uiThread
import org.w3c.dom.Attr
import weka.classifiers.Classifier
import weka.classifiers.Evaluation
import weka.classifiers.functions.LibSVM
import weka.classifiers.meta.AttributeSelectedClassifier
import weka.classifiers.meta.Bagging
import weka.classifiers.trees.J48
import weka.core.converters.ArffSaver
import java.io.File
import java.util.*


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
        const val JITAI_CLASSIFIER_UNINITIALIZED = 10
        const val JITAI_CLASSIFIER_UPDATED = 11

        //for notification trigger
        const val NOTIFICATION_TRIGGER = 100000
        const val NOTIFICATION_TRIGGER_YES = NOTIFICATION_TRIGGER + 1
        const val NOTIFICATION_TRIGGER_NO = NOTIFICATION_TRIGGER + 2
        const val NOTIFICATION_TRIGGER_DELETE = NOTIFICATION_TRIGGER + 3


        const val ARFF_PREFIX = "ex3_getSensordata_by_timestamp"
    }

    var numberOfDataPoints: Int = 1
    var active = true
    var goal: String = ""
    var message: String = ""
    var id: Int = -1
    var timeTrigger: TimeTrigger? = null

    var geofenceTrigger: GeofenceTrigger? = null

    var weatherTrigger: WeatherTrigger? = null

    @Transient
    private var db: JitaiDatabase? = null

    @Transient
    private var classifier: Classifier? = null
    @Transient
    private var svm: Classifier? = LibSVM()
    @Transient
    private var attribselected: Classifier? = AttributeSelectedClassifier()

    @Transient
    var evaluation: MutableList<Evaluation?> = mutableListOf()
    var evaluation1: MutableList<Evaluation?> = mutableListOf()
    var evaluation2: MutableList<Evaluation?> = mutableListOf()

    @Transient
    private var events: MutableList<JitaiEvent>? = null

    @Transient
    private var updatingClassifier = false

    fun check(sensorData: SensorDataSet) {
        if (db == null) db = JitaiDatabase.getInstance(context)
        if (classifier == null) {
            events = db!!.getJitaiEventsForTraining(id)
            updateClassfier(sensorData)
        }
        //check sensor only if it exists, otherwise true
        if ((timeTrigger == null || timeTrigger!!.check(context, sensorData))
            && (geofenceTrigger == null || geofenceTrigger!!.check(context, sensorData))
            && (weatherTrigger == null || weatherTrigger!!.check(context, sensorData)) &&
            checkSVM(sensorData)) {

            //val newEvents = db!!.getJitaiEventsForTraining(id)
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

    private fun getNewClassifier(): Classifier {
        val classifier = Bagging()
        return classifier
    }

    private fun updateClassfier(sensorData: SensorDataSet) {
        if (events?.isEmpty() == true) {
            val fingerprint = createFingerprint(
                this,
                context,
                sensorData,
                MATCH,
                FingerPrintAttributes(
                    numberOfDataPoints))
            classifier = getNewClassifier()
            classifier!!.buildClassifier(fingerprint)
        } else if (events != null && events!!.size > 0) {
            val time = System.currentTimeMillis()
            Log.d("$goal start train", time.toString())
            val relevantSensorDataSets = db!!.getSensorDatasetsFromAll(events!!, id)
            if (relevantSensorDataSets.size > 0) {
                //do not create two async tasks for the same classifier
                if (!updatingClassifier) {
                    updatingClassifier = true
                    doAsync {
                        val fingerprints = createFingerprint(
                            this@Jitai,
                            context,
                            relevantSensorDataSets,
                            FingerPrintAttributes(
                                numberOfDataPoints)
                                                                                                                     )
                        val classifier = getNewClassifier()
                        Log.d("$goal instances done",
                              (System.currentTimeMillis() - time).toString())
                        val eval = Evaluation(fingerprints)
                        try {
                            eval.crossValidateModel(classifier, fingerprints,
                                                    10, Random(1))
                            Log.i("$goal ${numberOfDataPoints} bagging", eval
                                .toSummaryString())
                            Log.i("$goal ${numberOfDataPoints} bagging", "confusion matrix")
                            eval.confusionMatrix()
                                .forEach { Log.i("$goal bagging", it.contentToString()) }
                        } catch (e: Exception) {
                            Log.i("$goal", e.toString())
                        }
                        svm = LibSVM()
                        val eval1 = Evaluation(fingerprints)
                        try {
                            eval1.crossValidateModel(svm, fingerprints,
                                                     10, Random(1))
                            Log.i("$goal ${numberOfDataPoints} svm", eval1
                                .toSummaryString())
                            Log.i("$goal ${numberOfDataPoints} svm", "confusion matrix")
                            eval1.confusionMatrix()
                                .forEach { Log.i("$goal svm", it.contentToString()) }
                        } catch (e: Exception) {
                            Log.i("$goal", e.toString())
                        }
                        attribselected = AttributeSelectedClassifier()
                        val eval2 = Evaluation(fingerprints)
                        try {
                            eval2.crossValidateModel(attribselected, fingerprints,
                                                     10, Random(1))
                            Log.i("$goal ${numberOfDataPoints} attr}", eval2
                                .toSummaryString())
                            Log.i("$goal ${numberOfDataPoints} attr}",
                                  "confusion matrix")
                            eval2.confusionMatrix()
                                .forEach {
                                    Log.i("$goal ${attribselected.toString()}", it
                                        .contentToString())
                                }
                        } catch (e: Exception) {
                            Log.i("$goal", e.toString())
                        }
                        classifier.buildClassifier(fingerprints)
                        /* Checks if external storage is available for read and write */
                        val state = Environment.getExternalStorageState();
                        if (Environment.MEDIA_MOUNTED.equals(state)) {
                            with(ArffSaver()) {
                                val outputFile = File(context.getExternalFilesDir(null),
                                                      "${ARFF_PREFIX}" +
                                                          "_${goal}_" +
                                                          "_${fingerprints.size}_instances" +
                                                          "_${numberOfDataPoints}_windowSize_" +
                                                          "f1_" +
                                                          "${eval?.fMeasure(
                                                              fingerprints
                                                                  .attribute(CLASSIFICATION)
                                                                  .indexOfValue(MATCH))}" +
                                                          ".arff")
                                Log.d(goal, "writing to " + outputFile.absolutePath)
                                instances = fingerprints
                                setFile(outputFile)
                                writeBatch()
                            }
                        } else {
                            Log.w(goal, "could not write fingerprints, external storage is " +
                                "unavailable")
                        }
                        Log.i("$goal classifier", classifier.toString())


                        Log.d("$goal rebuilding done",
                              (System.currentTimeMillis() - time).toString())
                        uiThread {
                            this@Jitai.evaluation.add(eval)
                            this@Jitai.evaluation1.add(eval1)
                            this@Jitai.evaluation2.add(eval2)
                            this@Jitai.classifier = classifier
                            //hack to get through all window sizes
                            if (numberOfDataPoints < 12) {
                                this@Jitai.classifier = null
                                numberOfDataPoints++
                            } else {
                                evaluation.mapIndexed({ i, it ->
                                                          Log.i("bagging $i", it!!
                                                              .toSummaryString())
                                                      })
                                evaluation.mapIndexed({ i, it ->
                                                          Log.i("bagging $i", it!!
                                                              .toClassDetailsString())
                                                      })
                                evaluation.map { Log.i("bagging f1", "" + it!!.weightedFMeasure()) }
                                evaluation1.mapIndexed({ i, it ->
                                                           Log.i("svm $i", it!!
                                                               .toSummaryString())
                                                       })
                                evaluation1.mapIndexed({ i, it ->
                                                           Log.i("svm $i", it!!
                                                               .toClassDetailsString())
                                                       })
                                evaluation1.map { Log.i("svm f1", "" + it!!.weightedFMeasure()) }
                                evaluation2.mapIndexed({ i, it ->
                                                           Log.i("attributeSelected $i", it!!
                                                               .toSummaryString())
                                                       })
                                evaluation2.mapIndexed({ i, it ->
                                                           Log.i("attributeSelected $i", it!!
                                                               .toClassDetailsString())
                                                       })
                                evaluation2.map { Log.i("attr f1", "" + it!!.weightedFMeasure()) }
                            }

                            updatingClassifier = false
                            db!!.enterJitaiEvent(id,
                                                 sensorData.time,
                                                 JITAI_CLASSIFIER_UPDATED,
                                                 sensorData.id)
                        }
                    }
                } else {
                    Log.d("$goal ", "classifier building in progress " + (System.currentTimeMillis
                    () - time)
                        .toString())
                }
            }
        }
    }

    private fun checkSVM(sensorData: SensorDataSet): Boolean {
        if (classifier != null) {
            val fingerprint = createFingerprint(
                this,
                context,
                sensorData,
                NO_MATCH,
                FingerPrintAttributes(
                    numberOfDataPoints))
            val prediction = classifier!!.classifyInstance(fingerprint[0])
            if (nominal[prediction.toInt()] == MATCH) {
                db!!.enterJitaiEvent(id, sensorData.time, JITAI_POSITIVE_PREDICTION, sensorData.id)
                Log.d("$goal prediction", "match")
                return true
            } else {
                db!!.enterJitaiEvent(id, sensorData.time, JITAI_NEGATIVE_PREDICTION, sensorData.id)
                Log.d("$goal prediction", "no_match")
                return false
            }
        }
        //send initial event
        db!!.enterJitaiEvent(id, sensorData.time, JITAI_CLASSIFIER_UNINITIALIZED, sensorData.id)
        return false
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