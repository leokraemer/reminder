package de.leo.fingerprint.datacollector.activityDetection

import android.content.Context
import android.util.Log
import de.leo.fingerprint.datacollector.algorithms.FFT
import de.leo.fingerprint.datacollector.database.*
import de.leo.fingerprint.datacollector.jitai.ProximityTrigger
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
import de.leo.fingerprint.datacollector.models.SensorDataSet
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.Instances
import weka.filters.Filter
import weka.filters.supervised.instance.Resample
import java.util.concurrent.ConcurrentHashMap


/**
 * Created by Leo on 27.01.2018.
 */

val GEOFENCE_PROXIMITY_TO_CENTER = "ProximityToCenter"

val GEOFENCE_X_DISTANCETOCENTER = "x_DistanceToCenter"

val GEOFENCE_Y_DISTANCETOCENTER = "y_DistanceToCenter"

val ABSOLUTE_HEIGHT = "absolute height from GPS"

val TIME_SPENT_IN_GEOFENCE = "timeSpentInGeofence"

val PORTION_OF_VALIDITY_INTERVAL_THAT_HAS_PASSED = "portionOfValidityIntervalThatHasPassed"

val DAY = "day"

val ACC_PEAK_SMV = "accPeakAmplitude"

val ACC_MEAN_SMV = "accMeanAmplitude"

val DOMINANT_FREQUENCY = "dominant_frequency"
val POWER_OF_DOMINANT_FREQUENCY = "power_of_dominant_frequency"
val SECOND_DOMINANT_FREQUENCY = "second_dominant_frequency"
val POWER_OF_SECOND_DOMINANT_FREQUENCY = "power_of_second_dominant_frequency"
val DOMINANT_FREQUENCY_625 = "dominant_frequency_625"
val POWER_OF_DOMINANT_FREQUENCY_625 = "power_of_dominant_frequency_625"
val TOTAL_POWER = "total_power"
val PART_OF_TOTAL_POWER_IN_P1 = "ratio_between_p1_and_total_power"
val RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW = "power_ratio_between_this_and_previous_window"

val GYRO_ACC_PEAK_SMV = "GYRO_accPeakAmplitude"

val GYRO_ACC_MEAN_SMV = "GYRO_accMeanAmplitude"

val GYRO_DOMINANT_FREQUENCY = "gyro_dominant_frequency"
val GYRO_POWER_OF_DOMINANT_FREQUENCY = "gyro_power_of_dominant_frequency"
val GYRO_SECOND_DOMINANT_FREQUENCY = "gyro_second_dominant_frequency"
val GYRO_POWER_OF_SECOND_DOMINANT_FREQUENCY = "gyro_power_of_second_dominant_frequency"
val GYRO_DOMINANT_FREQUENCY_625 = "gyro_dominant_frequency_625"
val GYRO_POWER_OF_DOMINANT_FREQUENCY_625 = "gyro_power_of_dominant_frequency_625"
val GYRO_TOTAL_POWER = "gyro_total_power"
val GYRO_PART_OF_TOTAL_POWER_IN_P1 = "gyro_ratio_between_p1_and_total_power"
val GYRO_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW =
    "gyro_power_ratio_between_this_and_previous_window"

val MAG_ACC_PEAK_SMV = "MAG_accPeakAmplitude"

val MAG_ACC_MEAN_SMV = "MAG_accMeanAmplitude"

val MAG_DOMINANT_FREQUENCY = "mag_dominant_frequency"
val MAG_POWER_OF_DOMINANT_FREQUENCY = "mag_power_of_dominant_frequency"
val MAG_SECOND_DOMINANT_FREQUENCY = "mag_second_dominant_frequency"
val MAG_POWER_OF_SECOND_DOMINANT_FREQUENCY = "mag_power_of_second_dominant_frequency"
val MAG_DOMINANT_FREQUENCY_625 = "mag_dominant_frequency_625"
val MAG_POWER_OF_DOMINANT_FREQUENCY_625 = "mag_power_of_dominant_frequency_625"
val MAG_TOTAL_POWER = "mag_total_power"
val MAG_PART_OF_TOTAL_POWER_IN_P1 = "mag_ratio_between_p1_and_total_power"
val MAG_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW = "mag_power_ratio_between_this_and_previous_window"

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

val ACTIVITIES = arrayListOf("IN_VEHICLE",
                             "ON_BICYCLE",
                             "ON_FOOT",
                             "STILL",
                             "UNKNOWN",
                             "TILTING",
                             "WALKING",
                             "RUNNING",
                             "IN_ROAD_VEHICLE",
                             "IN_RAIL_VEHICLE")

val DETECTED_ACTIVITY = "detected_activity"

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
        addAttribute(ABSOLUTE_HEIGHT)
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
        addAttribute(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)

        //gyro
        addAttribute(GYRO_ACC_PEAK_SMV)
        addAttribute(GYRO_ACC_MEAN_SMV)
        //addAttribute(GYRO_"dymanicTimeWarpingDistanceToSample")
        addAttribute(GYRO_DOMINANT_FREQUENCY)
        addAttribute(GYRO_POWER_OF_DOMINANT_FREQUENCY)
        addAttribute(GYRO_SECOND_DOMINANT_FREQUENCY)
        addAttribute(GYRO_POWER_OF_SECOND_DOMINANT_FREQUENCY)
        addAttribute(GYRO_DOMINANT_FREQUENCY_625)
        addAttribute(GYRO_POWER_OF_DOMINANT_FREQUENCY_625)
        addAttribute(GYRO_TOTAL_POWER)
        addAttribute(GYRO_PART_OF_TOTAL_POWER_IN_P1)
        addAttribute(GYRO_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)

        //mag
        addAttribute(MAG_ACC_PEAK_SMV)
        addAttribute(MAG_ACC_MEAN_SMV)
        //addAttribute(MAG_"dymanicTimeWarpingDistanceToSample")
        addAttribute(MAG_DOMINANT_FREQUENCY)
        addAttribute(MAG_POWER_OF_DOMINANT_FREQUENCY)
        addAttribute(MAG_SECOND_DOMINANT_FREQUENCY)
        addAttribute(MAG_POWER_OF_SECOND_DOMINANT_FREQUENCY)
        addAttribute(MAG_DOMINANT_FREQUENCY_625)
        addAttribute(MAG_POWER_OF_DOMINANT_FREQUENCY_625)
        addAttribute(MAG_TOTAL_POWER)
        addAttribute(MAG_PART_OF_TOTAL_POWER_IN_P1)
        addAttribute(MAG_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)

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
        //detected activity
        add(Attribute(DETECTED_ACTIVITY, ACTIVITIES))
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
    val inflatedSensorData = sensorData.fold(
        mutableListOf<Pair<SensorDataSet, String>>(),
        { list, it ->
            for (i in 0 until 5) {
                val copy = it.copy(first = it.first.copy(time = it.first.time - 1000 * i))
                list.add(copy)
            }
            list
        })
    sensorData.forEach {
        createInstance(context,
                       fingerprint,
                       FINGERPRINT_ATTRS,
                       jitai,
                       it.first,
                       it.second)
    }
    //val resampledfingerprint = Fingerprint("fingerprint", FINGERPRINT_ATTRS)
    //resampledfingerprint.addAll(dumbResample(fingerprint))
    return fingerprint
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

private val instanceStore = ConcurrentHashMap<Long, DoubleArray>()

private fun createInstance(context: Context,
                           fingerprint: Fingerprint,
                           FINGERPRINT_ATTRS: FingerPrintAttributes,
                           jitai: Jitai,
                           sensorData: SensorDataSet,
                           classification: String) {
    var values = instanceStore.get(sensorData.id)
    if (values == null) {
        values = DoubleArray(FINGERPRINT_ATTRS.size)
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
                values[getAttributeIndex(ABSOLUTE_HEIGHT)] =
                    if (currentLocation.hasAltitude())
                        normalizePower(currentLocation.altitude, 1000.0)
                    else Double.NaN
                //TODO
                values[getAttributeIndex(TIME_SPENT_IN_GEOFENCE)] = 0.0

                val localDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(sensorData.time),
                                                        ZoneId.systemDefault())
                values[getAttributeIndex(PORTION_OF_VALIDITY_INTERVAL_THAT_HAS_PASSED)] =
                    jitai.timeTrigger!!.getPassedTimePercent(localDate.toLocalTime())
                // days range from 1 to 7 -> (day.value -1) / 6 in [0,1]
                values[getAttributeIndex(DAY)] = (localDate.dayOfWeek.value - 1).toDouble() / 6.0
                //acceleration
                val accelerationData = db.getALL3DSensorValuesNoTimestamp(sensorData.time - interval,
                                                                          sensorData.time,
                                                                          TABLE_REAL_TIME_ACC)
                if (accelerationData.size >= 256) {
                    val prevAccelerationData =
                        db.getALL3DSensorValuesNoTimestamp(sensorData.time - interval - 1000,
                                                           sensorData.time - 1000,
                                                           TABLE_REAL_TIME_ACC)
                    //Signal Magnitude Vector (SMV) see https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3795931/
                    val smv = getSignalMagnitudeVector(accelerationData.takeLast(256))
                    values[getAttributeIndex(ACC_PEAK_SMV)] =
                        normalizePower(smv.max, 40.0)
                    values[getAttributeIndex(ACC_MEAN_SMV)] =
                        normalizePower(smv.average, 12.0)
                    //use only the last 256 values
                    val fft = getPowerSpectralAnalysis(smv.values)
                    val frequencyAnalysData = getFrequencyAnalysisData(fft)
                    values[getAttributeIndex(DOMINANT_FREQUENCY)] = frequencyAnalysData.f1
                    values[getAttributeIndex(POWER_OF_DOMINANT_FREQUENCY)] =
                        normalizePower(frequencyAnalysData.p1, 500000.0)
                    values[getAttributeIndex(SECOND_DOMINANT_FREQUENCY)] = frequencyAnalysData.f2
                    values[getAttributeIndex(POWER_OF_SECOND_DOMINANT_FREQUENCY)] =
                        normalizePower(frequencyAnalysData.p2, 50000.0)
                    values[getAttributeIndex(DOMINANT_FREQUENCY_625)] = frequencyAnalysData.f625
                    values[getAttributeIndex(POWER_OF_DOMINANT_FREQUENCY_625)] =
                        normalizePower(frequencyAnalysData.p625, 500000.0)
                    values[getAttributeIndex(TOTAL_POWER)] =
                        normalizePower(frequencyAnalysData.totalPower, 5000.0)
                    values[getAttributeIndex(PART_OF_TOTAL_POWER_IN_P1)] =
                        normalizePower(frequencyAnalysData.p1 / frequencyAnalysData.totalPower,
                                       50.0)
                    if (prevAccelerationData.size >= 256) {
                        val prevFFT = getFrequencyAnalysisData(
                            getPowerSpectralAnalysis(
                                getSignalMagnitudeVector(prevAccelerationData.takeLast(256)).values))
                        values[getAttributeIndex(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)] =
                            normalizePower(frequencyAnalysData.totalPower / prevFFT.totalPower,
                                           15.0)
                    } else {
                        values[getAttributeIndex(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)] = Double.NaN
                    }
                } else {
                    values[getAttributeIndex(ACC_PEAK_SMV)] = Double.NaN
                    values[getAttributeIndex(ACC_MEAN_SMV)] = Double.NaN
                    values[getAttributeIndex(DOMINANT_FREQUENCY)] = Double.NaN
                    values[getAttributeIndex(POWER_OF_DOMINANT_FREQUENCY)] = Double.NaN
                    values[getAttributeIndex(SECOND_DOMINANT_FREQUENCY)] = Double.NaN
                    values[getAttributeIndex(POWER_OF_SECOND_DOMINANT_FREQUENCY)] = Double.NaN
                    values[getAttributeIndex(DOMINANT_FREQUENCY_625)] = Double.NaN
                    values[getAttributeIndex(POWER_OF_DOMINANT_FREQUENCY_625)] = Double.NaN
                    values[getAttributeIndex(TOTAL_POWER)] = Double.NaN
                    values[getAttributeIndex(PART_OF_TOTAL_POWER_IN_P1)] = Double.NaN
                    values[getAttributeIndex(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)] = Double.NaN
                }
                //gyro
                if (true) {
                    val gyroData = db.getALL3DSensorValuesNoTimestamp(sensorData.time - interval,
                                                                      sensorData.time,
                                                                      TABLE_REAL_TIME_GYRO)
                    val prevGyroData =
                        db.getALL3DSensorValuesNoTimestamp(sensorData.time - interval - 1000,
                                                           sensorData.time - 1000,
                                                           TABLE_REAL_TIME_GYRO)
                    if (gyroData.size >= 256) {
                        //Signal Magnitude Vector (SMV) see https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3795931/
                        val smv = getSignalMagnitudeVector(gyroData.takeLast(256))
                        values[getAttributeIndex(GYRO_ACC_PEAK_SMV)] =
                            normalizePower(smv.max, 8.0)
                        values[getAttributeIndex(GYRO_ACC_MEAN_SMV)] =
                            normalizePower(smv.average, 3.0)
                        //use only the last 256 values
                        val fft = getPowerSpectralAnalysis(smv.values)
                        val frequencyAnalysData = getFrequencyAnalysisData(fft)
                        values[getAttributeIndex(GYRO_DOMINANT_FREQUENCY)] = frequencyAnalysData.f1
                        values[getAttributeIndex(GYRO_POWER_OF_DOMINANT_FREQUENCY)] =
                            normalizePower(frequencyAnalysData.p1, 80000.0)
                        values[getAttributeIndex(GYRO_SECOND_DOMINANT_FREQUENCY)] = frequencyAnalysData.f2
                        values[getAttributeIndex(GYRO_POWER_OF_SECOND_DOMINANT_FREQUENCY)] =
                            normalizePower(frequencyAnalysData.p2, 10000.0)
                        values[getAttributeIndex(GYRO_DOMINANT_FREQUENCY_625)] = frequencyAnalysData.f625
                        values[getAttributeIndex(GYRO_POWER_OF_DOMINANT_FREQUENCY_625)] =
                            normalizePower(frequencyAnalysData.p625, 80000.0)
                        values[getAttributeIndex(GYRO_TOTAL_POWER)] =
                            normalizePower(frequencyAnalysData.totalPower, 2500.0)
                        values[getAttributeIndex(GYRO_PART_OF_TOTAL_POWER_IN_P1)] =
                            normalizePower(frequencyAnalysData.p1 / frequencyAnalysData
                                .totalPower, 60.0)
                        if (prevGyroData.size >= 256) {
                            val prevFFT = getFrequencyAnalysisData(
                                getPowerSpectralAnalysis(
                                    getSignalMagnitudeVector(prevGyroData.takeLast(256)).values))
                            values[getAttributeIndex(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)] =
                                normalizePower(frequencyAnalysData.totalPower / prevFFT
                                    .totalPower, 15.0)
                        } else {
                            values[getAttributeIndex(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)] = Double.NaN
                        }
                    } else {
                        //missing values
                        values[getAttributeIndex(GYRO_ACC_PEAK_SMV)] = Double.NaN
                        values[getAttributeIndex(GYRO_ACC_MEAN_SMV)] = Double.NaN
                        values[getAttributeIndex(GYRO_DOMINANT_FREQUENCY)] = Double.NaN
                        values[getAttributeIndex(GYRO_POWER_OF_DOMINANT_FREQUENCY)] = Double.NaN
                        values[getAttributeIndex(GYRO_SECOND_DOMINANT_FREQUENCY)] = Double.NaN
                        values[getAttributeIndex(GYRO_POWER_OF_SECOND_DOMINANT_FREQUENCY)] = Double.NaN
                        values[getAttributeIndex(GYRO_DOMINANT_FREQUENCY_625)] = Double.NaN
                        values[getAttributeIndex(GYRO_POWER_OF_DOMINANT_FREQUENCY_625)] = Double.NaN
                        values[getAttributeIndex(GYRO_TOTAL_POWER)] = Double.NaN
                        values[getAttributeIndex(GYRO_PART_OF_TOTAL_POWER_IN_P1)] = Double.NaN
                        values[getAttributeIndex(GYRO_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)] = Double.NaN
                    }
                }
                //mag
                if (true) {
                    val magData = db.getALL3DSensorValuesNoTimestamp(sensorData.time - interval,
                                                                     sensorData.time,
                                                                     TABLE_REAL_TIME_MAG)
                    val prevMagData =
                        db.getALL3DSensorValuesNoTimestamp(sensorData.time - interval - 1000,
                                                           sensorData.time - 1000,
                                                           TABLE_REAL_TIME_MAG)
                    if (magData.size >= 256) {
                        //Signal Magnitude Vector (SMV) see https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3795931/
                        val smv = getSignalMagnitudeVector(magData.takeLast(256))
                        values[getAttributeIndex(MAG_ACC_PEAK_SMV)] =
                            normalizePower(smv.max, 3000.0)
                        values[getAttributeIndex(MAG_ACC_MEAN_SMV)] =
                            normalizePower(smv.average, 3000.0)
                        //use only the last 256 values
                        val fft = getPowerSpectralAnalysis(smv.values)
                        val frequencyAnalysData = getFrequencyAnalysisData(fft)
                        values[getAttributeIndex(MAG_DOMINANT_FREQUENCY)] = frequencyAnalysData.f1
                        values[getAttributeIndex(MAG_POWER_OF_DOMINANT_FREQUENCY)] =
                            normalizePower(frequencyAnalysData.p1, 10000000.0)
                        values[getAttributeIndex(MAG_SECOND_DOMINANT_FREQUENCY)] = frequencyAnalysData.f2
                        values[getAttributeIndex(MAG_POWER_OF_SECOND_DOMINANT_FREQUENCY)] =
                            normalizePower(frequencyAnalysData.p2, 5000.0)
                        values[getAttributeIndex(MAG_DOMINANT_FREQUENCY_625)] = frequencyAnalysData.f625
                        values[getAttributeIndex(MAG_POWER_OF_DOMINANT_FREQUENCY_625)] =
                            normalizePower(frequencyAnalysData.p625, 10000000.0)
                        values[getAttributeIndex(MAG_TOTAL_POWER)] =
                            normalizePower(frequencyAnalysData.totalPower, 10000000.0)
                        values[getAttributeIndex(MAG_PART_OF_TOTAL_POWER_IN_P1)] =
                            normalizePower(frequencyAnalysData.p1 / frequencyAnalysData
                                .totalPower, 70.0)
                        if (prevMagData.size >= 256) {
                            val prevFFT = getFrequencyAnalysisData(
                                getPowerSpectralAnalysis(
                                    getSignalMagnitudeVector(prevMagData.takeLast(256)).values))
                            values[getAttributeIndex(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)] =
                                normalizePower(frequencyAnalysData.totalPower / prevFFT
                                    .totalPower, 15.0)
                        } else {
                            values[getAttributeIndex(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)] = Double.NaN
                        }
                    } else {
                        //missing values
                        values[getAttributeIndex(MAG_ACC_PEAK_SMV)] = Double.NaN
                        values[getAttributeIndex(MAG_ACC_MEAN_SMV)] = Double.NaN
                        values[getAttributeIndex(MAG_DOMINANT_FREQUENCY)] = Double.NaN
                        values[getAttributeIndex(MAG_POWER_OF_DOMINANT_FREQUENCY)] = Double.NaN
                        values[getAttributeIndex(MAG_SECOND_DOMINANT_FREQUENCY)] = Double.NaN
                        values[getAttributeIndex(MAG_POWER_OF_SECOND_DOMINANT_FREQUENCY)] = Double.NaN
                        values[getAttributeIndex(MAG_DOMINANT_FREQUENCY_625)] = Double.NaN
                        values[getAttributeIndex(MAG_POWER_OF_DOMINANT_FREQUENCY_625)] = Double.NaN
                        values[getAttributeIndex(MAG_TOTAL_POWER)] = Double.NaN
                        values[getAttributeIndex(MAG_PART_OF_TOTAL_POWER_IN_P1)] = Double.NaN
                        values[getAttributeIndex(MAG_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW)] = Double.NaN
                    }
                }
                //proximitySensor
                values[getAttributeIndex(PROXIMITY_SENSOR_STATE)] =
                    proximityTrigger.check(context, sensorData).toDouble()
                //values[getAttributeIndex(TIME_IN_PROXIMITY_STATE)] = 0.0

                //screenState
                values[getAttributeIndex(SCREEN_STATE)] = sensorData.screenState.toDouble()
                //values[getAttributeIndex(TIME_IN_SCREEN_STATE)] = 0.0

                //step count
                val previousSensorDataset = db.getSensorDataset(sensorData.id - 1)
                if (previousSensorDataset?.totalStepsToday != null
                    && sensorData?.totalStepsToday != null) {
                    values[getAttributeIndex(STEPS_IN_INTERVAL)] =
                        Math.min(0.0, sensorData.totalStepsToday!!.toDouble()
                            - previousSensorDataset.totalStepsToday!!.toDouble())
                } else {

                    values[getAttributeIndex(STEPS_IN_INTERVAL)] = Double.NaN
                }
                //light sensor
                val lightData = db.getSensorValues(sensorData.time - interval,
                                                   sensorData.time,
                                                   TABLE_REALTIME_LIGHT)
                val (lightPeak, lightMin, lightMean) = getLightValues(lightData)
                values[getAttributeIndex(PEAK_LIGHT_VALUE)] = normalizePower(lightPeak, 5000.0)
                values[getAttributeIndex(MIN_LIGHT_VALUE)] = normalizePower(lightMin, 500.0)
                values[getAttributeIndex(MEAN_LIGHT_VALUE)] = normalizePower(lightMean, 5000.0)

                //sound
                values[getAttributeIndex(PEAK_VOLUME)] = sensorData.ambientSound!! / 32767.0
                //addAttribute("minVolume")
                //addAttribute("medianVolume")

                //height
                val heightData = db.getSensorValues(sensorData.time - interval, sensorData.time,
                                                    TABLE_REALTIME_AIR)
                values[getAttributeIndex(HEIGHT_DIFFERENCE_IN_INTERVAL)] =
                    calcHeigthDifference(heightData)

                values[getAttributeIndex(DETECTED_ACTIVITY)] = sensorData.activity.maxBy { it.confidence }!!.type
                    .toDouble()

                values[getAttributeIndex(CLASSIFICATION)] = nominal.indexOf(classification)
                    .toDouble()
                Log.d("${jitai.goal} created",
                      "${sensorData.id} " + (System.currentTimeMillis() - time).toString())
            }

        }
        instanceStore.put(sensorData.id, values)
    } else {
        values[fingerprint.getAttributeIndex(CLASSIFICATION)] = nominal.indexOf(classification)
            .toDouble()
    }
    val instance = DenseInstance(1.0, values)
    fingerprint.add(instance)
}

fun normalizePower(p1: Double, max: Double): Double = Math.min(p1, max) / max


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
        }
        if (i > ZERO_POINT_SIX_HZ_CUTOFF && i < TWO_POINT_FIVE_HZ_CUTOFF && f625 < fft[i]) {
            f625 = i.toDouble()
            p625 = fft[i]
        }
    }
    totalPower /= (FIVETEEN_HZ_CUTOFF - ZERO_POINT_THREE_HZ_CUTOFF)
    //normalisations
    f1 = (f1 - 1.0) / FIVETEEN_HZ_CUTOFF.toDouble()
    f2 = (f2 - 1.0) / FIVETEEN_HZ_CUTOFF.toDouble()
    f625 = (f625 - 2.0) / TWO_POINT_FIVE_HZ_CUTOFF.toDouble()
    return FrequencyAnalysisData(f1, p1, f2, p2, f625, p625, totalPower)
}

class FrequencyAnalysisData(val f1: Double, val p1: Double, val f2: Double, val p2: Double, val
f625: Double, val p625: Double, val totalPower: Double)

val lag = 5
fun calcHeigthDifference(heightData: MutableList<Pair<Long, Double>>): Double {
    var movingAverage = heightData.take(lag)
        .fold(0.0, { acc, it -> acc + (it.second / lag.toDouble()) })
    var min = Double.POSITIVE_INFINITY
    var max = 0.0
    heightData.forEachIndexed { i, it ->
        //discard nan
        if (!it.second.isNaN() && i >= lag) {
            movingAverage -= heightData[i - lag].second / lag.toDouble()
            movingAverage += it.second / lag.toDouble()
            min = Math.min(movingAverage, min)
            max = Math.max(movingAverage, max)
        }
    }

    val difference = min - max
    if (difference.isFinite())
        return difference
    return Double.NaN
}

fun getLightValues(lightData: MutableList<Pair<Long, Double>>): DoubleArray {
    //peak, min, mean
    val values = DoubleArray(3)
    values[1] = Double.MAX_VALUE
    lightData.forEach {
        //discard nan
        if (!it.second.isNaN()) {
            values[0] = Math.max(values[0], Math.abs(it.second))
            values[1] = Math.min(values[1], Math.abs(it.second))
            values[2] += Math.abs(it.second) / lightData.size.toDouble()
        }
    }
    return values
}

fun Boolean.toDouble() = if (this) 0.0 else 1.0

fun getSignalMagnitudeVector(data: List<DoubleArray>): SMV {
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

class SMV(val average: Double, val max: Double, val values: DoubleArray)

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