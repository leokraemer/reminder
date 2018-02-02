package com.example.leo.datacollector.activityDetection

import android.content.Context
import android.util.Log
import com.example.leo.datacollector.algorithms.FFT
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.database.TABLE_REALTIME_AIR
import com.example.leo.datacollector.database.TABLE_REALTIME_LIGHT
import com.example.leo.datacollector.database.TABLE_REAL_TIME_ACC
import com.example.leo.datacollector.jitai.ProximityTrigger
import com.example.leo.datacollector.jitai.manage.Jitai
import com.example.leo.datacollector.models.SensorDataSet
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.Instances
import weka.filters.Filter
import weka.filters.supervised.instance.Resample


/**
 * Created by Leo on 27.01.2018.
 */

val GEOFENCE_PROXIMITY_TO_CENTER = "ProximityToCenter"

val GEOFENCE_X_DISTANCETOCENTER = "x_DistanceToCenter"

val GEOFENCE_Y_DISTANCETOCENTER = "y_DistanceToCenter"

val TIME_SPENT_IN_GEOFENCE = "timeSpentInGeofence"

val PORTION_OF_VALIDITY_INTERVAL_THAT_HAS_PASSED = "portionOfValidityIntervalThatHasPassed"

val DAY = "day"

val ACC_PEAK_SMV = "accPeakAmplitude"

val ACC_MEAN_SMV = "accMeanAmplitude"

val DOMINANT_FREQUENCY = "DOMINANT_FREQUENCY"
val POWER_OF_DOMINANT_FREQUENCY = "POWER_OF_DOMINANT_FREQUENCY"
val SECOND_DOMINANT_FREQUENCY = "SECOND_DOMINANT_FREQUENCY"
val POWER_OF_SECOND_DOMINANT_FREQUENCY = "POWER_OF_SECOND_DOMINANT_FREQUENCY"
val DOMINANT_FREQUENCY_625 = "DOMINANT_FREQUENCY_625"
val POWER_OF_DOMINANT_FREQUENCY_625 = "POWER_OF_DOMINANT_FREQUENCY_625"
val TOTAL_POWER = "TOTAL_POWER"
val PART_OF_TOTAL_POWER_IN_P1 = "RATIO_BETWEEN_P1_AND_TOTAL_POWER"
//not applicable, because we do not necessarily have continuous windows
//val RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW = "POWER_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW"


val PROXIMITY_SENSOR_STATE = "proximitiSensorState"
//val TIME_IN_PROXIMITY_STATE = "timeInProximityState"

val SCREEN_STATE = "screenState"
//val TIME_IN_SCREEN_STATE = "timeInScreenState"

val STEPS_IN_INTERVAL = "stepsInInterval"

val PEAK_LIGHT_VALUE = "peakLightValue"

val MIN_LIGHT_VALUE = "minLightValue"

val MEAN_LIGHT_VALUE = "meanLightValue"

val PEAK_VOLUME = "peakVolume"

val HEIGHT_DIFFERENCE_IN_INTERVAL = "height"

val MATCH = "match"
val NO_MATCH = "no_match"

val CLASSIFICATION = "classification"

val nominal: List<String> = arrayListOf(MATCH, NO_MATCH)

class FingerPrintAttributes : ArrayList<Attribute>() {



    //desired attributes are in comments
    init {
        //location related
        addAttribute(GEOFENCE_PROXIMITY_TO_CENTER)
        addAttribute(GEOFENCE_X_DISTANCETOCENTER)
        addAttribute(GEOFENCE_Y_DISTANCETOCENTER)
        addAttribute(TIME_SPENT_IN_GEOFENCE)
        //addAttribute("timeSpentOutsideGeofence")

        //time related
        addAttribute(PORTION_OF_VALIDITY_INTERVAL_THAT_HAS_PASSED)
        addAttribute(DAY)
        //addAttribute("howManyRemindersThereWereAlready")

        //accelerationSensor
        addAttribute(ACC_PEAK_SMV)
        addAttribute(ACC_MEAN_SMV)
        //addAttribute("dymanicTimeWarpingDistanceToSample")
        addAttribute(DOMINANT_FREQUENCY)
        addAttribute(POWER_OF_DOMINANT_FREQUENCY)
        addAttribute(SECOND_DOMINANT_FREQUENCY)
        addAttribute(POWER_OF_SECOND_DOMINANT_FREQUENCY)
        addAttribute(DOMINANT_FREQUENCY_625)
        addAttribute(POWER_OF_DOMINANT_FREQUENCY_625)
        addAttribute(TOTAL_POWER)
        addAttribute(PART_OF_TOTAL_POWER_IN_P1)
        //addAttribute(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)

        //proximitySensor
        addAttribute(PROXIMITY_SENSOR_STATE) //near or far
        //addAttribute(TIME_IN_PROXIMITY_STATE)

        //screenState
        addAttribute(SCREEN_STATE)
        //addAttribute(TIME_IN_SCREEN_STATE)

        //step count
        addAttribute(STEPS_IN_INTERVAL)

        //light sensor
        addAttribute(PEAK_LIGHT_VALUE)
        addAttribute(MIN_LIGHT_VALUE)
        addAttribute(MEAN_LIGHT_VALUE)

        //sound
        addAttribute(PEAK_VOLUME)
        //addAttribute("minVolume")
        //addAttribute("medianVolume")

        //height
        addAttribute(HEIGHT_DIFFERENCE_IN_INTERVAL)
        add(Attribute(CLASSIFICATION, nominal))
    }

    private fun addAttribute(title: String) = add(Attribute(title))
}

private val FINGERPRINT_ATTRS = FingerPrintAttributes()

fun createFingerprint(jitai: Jitai, context: Context, sensorData: SensorDataSet, classification:
String): Fingerprint {
    val fingerprint = Fingerprint("fingerprint", FINGERPRINT_ATTRS)
    createInstance(context,
                   fingerprint,
                   FINGERPRINT_ATTRS,
                   jitai,
                   sensorData,
                   classification)
    return fingerprint
}

fun createFingerprint(jitai: Jitai, context: Context, sensorData: List<Pair<SensorDataSet,
        String>>):
        Fingerprint {
    val fingerprint = Fingerprint("fingerprint", FINGERPRINT_ATTRS)
    sensorData.forEach {
        createInstance(context,
                       fingerprint,
                       FINGERPRINT_ATTRS,
                       jitai,
                       it.first,
                       it.second)
    }
    val resampledfingerprint = Fingerprint("fingerprint", FINGERPRINT_ATTRS)
    resampledfingerprint.addAll(dumbResample(fingerprint))
    return resampledfingerprint
}


@Throws(Exception::class)
private fun dumbResample(reduced: Instances): Instances {
    val resampleFilter = Resample()
    resampleFilter.setRandomSeed(1)
    resampleFilter.biasToUniformClass = 1.0
    resampleFilter.setInputFormat(reduced)
    return Filter.useFilter(reduced, resampleFilter)
}

val interval = 5200 // equals 260 * 20 and therefore 256 activity samples at 50Hz (plus 4 for
// fluctuations)
private fun createInstance(context: Context,
                           fingerprint: Fingerprint,
                           FINGERPRINT_ATTRS: FingerPrintAttributes,
                           jitai: Jitai,
                           sensorData: SensorDataSet,
                           classification: String) {
    val values = DoubleArray(FINGERPRINT_ATTRS.size)
    val db = JitaiDatabase.getInstance(context)
    val proximityTrigger = ProximityTrigger(true)
    with(fingerprint) {
        val time = System.currentTimeMillis()
        with(FINGERPRINT_ATTRS) {
            val geofence = jitai.geofenceTrigger!!.getCurrentLocation()
            val currentLocation = sensorData.gps!!
            //normalized by geofence.radius to [0,1]
            values[getAttributeIndex(GEOFENCE_PROXIMITY_TO_CENTER)] =
                    geofence.getLocation().distanceTo(currentLocation).toDouble() / geofence.radius
            values[getAttributeIndex(GEOFENCE_X_DISTANCETOCENTER)] =
                    normalizeDegreeDistance(currentLocation.longitude - geofence.longitude)
            values[getAttributeIndex(GEOFENCE_Y_DISTANCETOCENTER)] =
                    normalizeDegreeDistance(currentLocation.latitude - geofence.latitude)
            //TODO
            values[getAttributeIndex(TIME_SPENT_IN_GEOFENCE)] = 0.0

            values[getAttributeIndex(PORTION_OF_VALIDITY_INTERVAL_THAT_HAS_PASSED)]
            val localDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(sensorData.time),
                                                    ZoneId.systemDefault())
            // days range from 1 to 7 -> (day.value -1) / 6 in [0,1]
            values[getAttributeIndex(DAY)] = (localDate.dayOfWeek.value - 1).toDouble() / 6.0
            Log.d("after day", (System.currentTimeMillis() - time).toString())
            //acceleration
            val accelerationData = db.getALL3DSensorValuesNoTimestamp(sensorData.time - interval,
                                                                      sensorData
                                                                              .time,
                                                                      TABLE_REAL_TIME_ACC)
            if (accelerationData.size >= 256) {
                Log.d("after acc db", (System.currentTimeMillis() - time).toString())
                //Signal Magnitude Vector (SMV) see https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3795931/
                val smv = getSignalMagnitudeVector(accelerationData)
                values[getAttributeIndex(ACC_PEAK_SMV)] = smv.max
                values[getAttributeIndex(ACC_MEAN_SMV)] = smv.average
                //use only the last 256 values
                val fft = getPowerSpectralAnalysis(smv.values.takeLast(256).toDoubleArray())
                val frequencyAnalysData = getFrequencyAnalysisData(fft)
                values[getAttributeIndex(DOMINANT_FREQUENCY)] = frequencyAnalysData.f1
                values[getAttributeIndex(POWER_OF_DOMINANT_FREQUENCY)] = frequencyAnalysData.p1
                values[getAttributeIndex(SECOND_DOMINANT_FREQUENCY)] = frequencyAnalysData.f2
                values[getAttributeIndex(POWER_OF_SECOND_DOMINANT_FREQUENCY)] = frequencyAnalysData.p2
                values[getAttributeIndex(DOMINANT_FREQUENCY_625)] = frequencyAnalysData.f625
                values[getAttributeIndex(POWER_OF_DOMINANT_FREQUENCY_625)] = frequencyAnalysData.p625
                values[getAttributeIndex(TOTAL_POWER)] = frequencyAnalysData.totalPower
                values[getAttributeIndex(PART_OF_TOTAL_POWER_IN_P1)] =
                        frequencyAnalysData.p1 / frequencyAnalysData.totalPower
            }
            Log.d("after acc", (System.currentTimeMillis() - time).toString())
            //proximitySensor
            values[getAttributeIndex(PROXIMITY_SENSOR_STATE)] =
                    proximityTrigger.check(context, sensorData).toDouble()
            //values[getAttributeIndex(TIME_IN_PROXIMITY_STATE)] = 0.0

            //screenState
            values[getAttributeIndex(SCREEN_STATE)] = sensorData.screenState.toDouble()
            //values[getAttributeIndex(TIME_IN_SCREEN_STATE)] = 0.0

            //step count
            val stepData = db.getStepData(sensorData.time - interval, sensorData.time)
            if (stepData.size > 0)
                values[getAttributeIndex(STEPS_IN_INTERVAL)] =
                        Math.min(0.0, stepData.last().second - stepData.first().second)
            Log.d("after step", (System.currentTimeMillis() - time).toString())
            //light sensor
            val lightData = db.getSensorValues(sensorData.time - interval,
                                               sensorData.time,
                                               TABLE_REALTIME_LIGHT)
            val (lightPeak, lightMin, lightMean) = getLightValues(lightData)
            values[getAttributeIndex(PEAK_LIGHT_VALUE)] = lightPeak
            values[getAttributeIndex(MIN_LIGHT_VALUE)] = lightMin
            values[getAttributeIndex(MEAN_LIGHT_VALUE)] = lightMean

            //sound
            values[getAttributeIndex(PEAK_VOLUME)] = sensorData.ambientSound!! / 32767.0
            //addAttribute("minVolume")
            //addAttribute("medianVolume")

            //height
            val heightData = db.getSensorValues(sensorData.time - interval, sensorData.time,
                                                TABLE_REALTIME_AIR)
            values[getAttributeIndex(HEIGHT_DIFFERENCE_IN_INTERVAL)] = calcHeigthDifference(
                    heightData)
            values[getAttributeIndex(CLASSIFICATION)] = nominal.indexOf(classification).toDouble()
            Log.d("after all", (System.currentTimeMillis() - time).toString())
        }

    }
    fingerprint.add(DenseInstance(1.0, values))
}

// 256 samples @ 50 Hz -> 128 buckets with index 127 representing 25 Hz
//round(128 / (25 / 15)) = 76.8 -> index 76
const val FIVETEEN_HZ_CUTOFF = 76
//round(128 / (25/ 0.3)) = 2 -> index 1
const val ZERO_POINT_THREE_HZ_CUTOFF = 1
//round(128 / (25/ 0.6)) = 3.07 -> index 2
const val ZERO_POINT_SIX_HZ_CUTOFF = 2
//round(128 / (25/ 2.5)) = 12.8 -> index 12
const val TWO_POINT_FIVE_HZ_CUTOFF = 12

fun getFrequencyAnalysisData(fft: DoubleArray): FrequencyAnalysisData {
    var f1 = Double.MIN_VALUE
    var p1 = Double.MIN_VALUE
    var f2 = Double.MIN_VALUE
    var p2 = Double.MIN_VALUE
    var f625 = Double.MIN_VALUE
    var p625 = Double.MIN_VALUE
    var totalPower = Double.MIN_VALUE
    for (i in ZERO_POINT_THREE_HZ_CUTOFF until FIVETEEN_HZ_CUTOFF) {
        totalPower += fft[i]
        if (p1 < fft[i]) {
            p2 = p1
            f2 = f1
            f1 = i.toDouble()
            p1 = fft[i]
            if (i > ZERO_POINT_SIX_HZ_CUTOFF && i < TWO_POINT_FIVE_HZ_CUTOFF) {
                f625 = i.toDouble()
                p625 = fft[i]
            }
        }
    }
    totalPower /= (FIVETEEN_HZ_CUTOFF - ZERO_POINT_THREE_HZ_CUTOFF)
    return FrequencyAnalysisData(f1, p1, f2, p2, f625, p625, totalPower)
}

class FrequencyAnalysisData(val f1: Double, val p1: Double, val f2: Double, val p2: Double, val
f625: Double, val p625: Double, val totalPower: Double)

fun calcHeigthDifference(heightData: MutableList<Pair<Long, Double>>): Double {
    var min = Double.MAX_VALUE
    var max = 0.0
    heightData.forEach {
        min = Math.min(it.second, min)
        max = Math.max(it.second, max)
    }
    return min - max
}

fun getLightValues(lightData: MutableList<Pair<Long, Double>>): DoubleArray {
    //peak, min, mean
    val values = DoubleArray(3)
    values[1] = Double.MAX_VALUE
    lightData.forEach {
        values[0] = Math.max(values[0], Math.abs(it.second))
        values[1] = Math.min(values[1], Math.abs(it.second))
        values[2] += Math.abs(it.second) / lightData.size.toDouble()
    }
    return values
}

fun Boolean.toDouble() = if (this) 0.0 else 1.0

private fun getSignalMagnitudeVector(data: MutableList<DoubleArray>): SMV {
    var average = 0.0
    var max = 0.0
    val values = DoubleArray(data.size)
    data.forEachIndexed { i, it ->
        val x = it[0]
        val y = it[1]
        val z = it[2]
        val smv = Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0)
        values[i] = Math.sqrt(smv)
        max = Math.max(max, smv)
        average += smv / data.size
    }
    return SMV(Math.sqrt(average), Math.sqrt(max), values)
}

private class SMV(val average: Double, val max: Double, val values: DoubleArray)

/**
 * Do the fft and transform the resulting real and imaginary parts to magnitudes.
 * resulting array has half the size of the input.
 */
fun getPowerSpectralAnalysis(data: DoubleArray): DoubleArray {
    //get maximum size (must be power of 2)
    val n = Integer.highestOneBit(data.size)
    val p = DoubleArray(n / 2)
    val x = data
    val y = DoubleArray(n)
    val fft = FFT(n)
    fft.fft(x, y)
    for (i in 0 until p.size)
        p[i] += Math.pow(x[i], 2.0) + Math.pow(y[i], 2.0)
    return p
}


//1 km is about a difference of 0.0025
fun normalizeDegreeDistance(d: Double): Double = d * 1000.0 / 2.5


class Fingerprint
(jitai: String, fingerPrintAttributes: FingerPrintAttributes)
    : Instances("$jitai + fingerprint", fingerPrintAttributes, 0) {
    fun getAttributeIndex(name: String) = attribute(name).index()

    init {
        setClassIndex(getAttributeIndex(CLASSIFICATION))
    }
}