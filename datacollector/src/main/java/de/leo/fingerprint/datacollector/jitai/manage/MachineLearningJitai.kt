package de.leo.fingerprint.datacollector.jitai.manage

import android.content.Context
import android.os.Environment
import android.util.Log
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.jitai.JitaiEvent
import de.leo.fingerprint.datacollector.jitai.activityDetection.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
//import weka.classifiers.Classifier
//import weka.classifiers.Evaluation
//import weka.classifiers.functions.LibSVM
//import weka.classifiers.meta.AttributeSelectedClassifier
//import weka.classifiers.meta.Bagging
//import weka.core.converters.ArffSaver
import java.io.File
import java.util.*

class MachineLearningJitai(context: Context) : Jitai(context) {
    companion object {
        const val ARFF_PREFIX = "ex3_getSensordata_by_timestamp"
    }

    override fun check(sensorData: SensorDataSet) {
        /*if (classifier == null) {
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
        }*/
    }/*

    var numberOfDataPoints: Int = 1

    @Transient
    private var events: MutableList<JitaiEvent>? = null

    @Transient
    private var updatingClassifier = false

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
                            this@MachineLearningJitai,
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
                                                      "$ARFF_PREFIX" +
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
                            this@MachineLearningJitai.evaluation.add(eval)
                            this@MachineLearningJitai.evaluation1.add(eval1)
                            this@MachineLearningJitai.evaluation2.add(eval2)
                            this@MachineLearningJitai.classifier = classifier
                            //hack to get through all window sizes
                            if (numberOfDataPoints < 12) {
                                this@MachineLearningJitai.classifier = null
                                numberOfDataPoints++
                            } else {
                                evaluation.mapIndexed { i, it ->
                                    Log.i("bagging $i", it!!
                                        .toSummaryString())
                                }
                                evaluation.mapIndexed { i, it ->
                                    Log.i("bagging $i", it!!
                                        .toClassDetailsString())
                                }
                                evaluation.map { Log.i("bagging f1", "" + it!!.weightedFMeasure()) }
                                evaluation1.mapIndexed { i, it ->
                                    Log.i("svm $i", it!!
                                        .toSummaryString())
                                }
                                evaluation1.mapIndexed { i, it ->
                                    Log.i("svm $i", it!!
                                        .toClassDetailsString())
                                }
                                evaluation1.map { Log.i("svm f1", "" + it!!.weightedFMeasure()) }
                                evaluation2.mapIndexed { i, it ->
                                    Log.i("attributeSelected $i", it!!
                                        .toSummaryString())
                                }
                                evaluation2.mapIndexed { i, it ->
                                    Log.i("attributeSelected $i", it!!
                                        .toClassDetailsString())
                                }
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
    }*/
}