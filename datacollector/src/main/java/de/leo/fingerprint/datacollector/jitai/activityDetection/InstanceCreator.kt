package de.leo.fingerprint.datacollector.jitai.activityDetection

import android.content.Context
import android.net.wifi.WifiManager
import de.leo.fingerprint.datacollector.datacollection.database.*
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.datacollection.models.WifiInfo
import de.leo.fingerprint.datacollector.datacollection.models.deSerializeWifi
import de.leo.fingerprint.datacollector.jitai.ProximityTrigger
import de.leo.fingerprint.datacollector.jitai.algorithms.FFT
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
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

const val GEOFENCE_PROXIMITY_TO_CENTER = "ProximityToCenter"

const val GEOFENCE_X_DISTANCETOCENTER = "x_DistanceToCenter"

const val GEOFENCE_Y_DISTANCETOCENTER = "y_DistanceToCenter"

const val ABSOLUTE_HEIGHT = "absolute height from GPS"

const val TIME_SPENT_IN_GEOFENCE = "timeSpentInGeofence"

const val PORTION_OF_VALIDITY_INTERVAL_THAT_HAS_PASSED = "portionOfValidityIntervalThatHasPassed"

const val DAY = "day"

const val ACC_PEAK_SMV = "accPeakAmplitude"

const val ACC_MEAN_SMV = "accMeanAmplitude"

const val DOMINANT_FREQUENCY = "dominant_frequency"
const val POWER_OF_DOMINANT_FREQUENCY = "power_of_dominant_frequency"
const val SECOND_DOMINANT_FREQUENCY = "second_dominant_frequency"
const val POWER_OF_SECOND_DOMINANT_FREQUENCY = "power_of_second_dominant_frequency"
const val DOMINANT_FREQUENCY_625 = "dominant_frequency_625"
const val POWER_OF_DOMINANT_FREQUENCY_625 = "power_of_dominant_frequency_625"
const val TOTAL_POWER = "total_power"
const val PART_OF_TOTAL_POWER_IN_P1 = "ratio_between_p1_and_total_power"
const val RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW = "power_ratio_between_this_and_previous_window"

const val GYRO_ACC_PEAK_SMV = "GYRO_accPeakAmplitude"

const val GYRO_ACC_MEAN_SMV = "GYRO_accMeanAmplitude"

const val GYRO_DOMINANT_FREQUENCY = "gyro_dominant_frequency"
const val GYRO_POWER_OF_DOMINANT_FREQUENCY = "gyro_power_of_dominant_frequency"
const val GYRO_SECOND_DOMINANT_FREQUENCY = "gyro_second_dominant_frequency"
const val GYRO_POWER_OF_SECOND_DOMINANT_FREQUENCY = "gyro_power_of_second_dominant_frequency"
const val GYRO_DOMINANT_FREQUENCY_625 = "gyro_dominant_frequency_625"
const val GYRO_POWER_OF_DOMINANT_FREQUENCY_625 = "gyro_power_of_dominant_frequency_625"
const val GYRO_TOTAL_POWER = "gyro_total_power"
const val GYRO_PART_OF_TOTAL_POWER_IN_P1 = "gyro_ratio_between_p1_and_total_power"
const val GYRO_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW =
    "gyro_power_ratio_between_this_and_previous_window"

const val MAG_ACC_PEAK_SMV = "MAG_accPeakAmplitude"

const val MAG_ACC_MEAN_SMV = "MAG_accMeanAmplitude"

const val MAG_DOMINANT_FREQUENCY = "mag_dominant_frequency"
const val MAG_POWER_OF_DOMINANT_FREQUENCY = "mag_power_of_dominant_frequency"
const val MAG_SECOND_DOMINANT_FREQUENCY = "mag_second_dominant_frequency"
const val MAG_POWER_OF_SECOND_DOMINANT_FREQUENCY = "mag_power_of_second_dominant_frequency"
const val MAG_DOMINANT_FREQUENCY_625 = "mag_dominant_frequency_625"
const val MAG_POWER_OF_DOMINANT_FREQUENCY_625 = "mag_power_of_dominant_frequency_625"
const val MAG_TOTAL_POWER = "mag_total_power"
const val MAG_PART_OF_TOTAL_POWER_IN_P1 = "mag_ratio_between_p1_and_total_power"
const val MAG_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW = "mag_power_ratio_between_this_and_previous_window"

const val PROXIMITY_SENSOR_STATE = "proximitiSensorState"
//const val TIME_IN_PROXIMITY_STATE = "timeInProximityState"

const val SCREEN_STATE = "screenState"
//const val TIME_IN_SCREEN_STATE = "timeInScreenState"

const val STEPS_IN_INTERVAL = "stepsInInterval"

const val PEAK_LIGHT_VALUE = "peakLightValue"

const val MIN_LIGHT_VALUE = "minLightValue"

const val MEAN_LIGHT_VALUE = "meanLightValue"

const val PEAK_VOLUME = "peakVolume"

const val HEIGHT_DIFFERENCE_IN_INTERVAL = "height_difference_in_interval"

/**
IN_VEHICLE = 0;
ON_BICYCLE = 1;
ON_FOOT = 2;
STILL = 3;
UNKNOWN = 4;
TILTING = 5;
WALKING = 7;
RUNNING = 8;
IN_ROAD_VEHICLE = 16;
IN_RAIL_VEHICLE = 17;

 */
val ACTIVITIES = arrayListOf(
    "IN_VEHICLE",
    "ON_BICYCLE",
    "ON_FOOT",
    "STILL",
    "UNKNOWN",
    "TILTING",
    "WALKING",
    "RUNNING",
    "9",
    "10",
    "11",
    "12",
    "13",
    "14",
    "15",
    "IN_ROAD_VEHICLE",
    "IN_RAIL_VEHICLE")

const val DETECTED_ACTIVITY = "detected_activity"
const val DETECTED_ACTIVITY_CONFIDENCE = "detected_activity_confidence"
const val DETECTED_ACTIVITY_2 = "detected_activity_2"
const val DETECTED_ACTIVITY_2_CONFIDENCE = "detected_activity_confidence_2"
const val DETECTED_ACTIVITY_3 = "detected_activity_3"
const val DETECTED_ACTIVITY_3_CONFIDENCE = "detected_activity_confidence_3"
const val WIFI_SIGNAL_STRENGTH_1 = "wifi_signal_strength_1"
const val WIFI_SIGNAL_STRENGTH_2 = "wifi_signal_strength_2"
const val WIFI_SIGNAL_STRENGTH_3 = "wifi_signal_strength_3"
const val WIFI_SIGNAL_STRENGTH_4 = "wifi_signal_strength_4"
const val WIFI_SIGNAL_STRENGTH_5 = "wifi_signal_strength_5"
const val WIFI_SIGNAL_STRENGTH_6 = "wifi_signal_strength_6"
const val WIFI_SIGNAL_STRENGTH_7 = "wifi_signal_strength_7"
const val WIFI_SIGNAL_STRENGTH_8 = "wifi_signal_strength_8"
const val WIFI_SIGNAL_STRENGTH_9 = "wifi_signal_strength_9"
const val WIFI_SIGNAL_STRENGTH_10 = "wifi_signal_strength_10"
const val CONNECTED_WIFI = "connected_wifi" // 1..10

const val MATCH = "match"
const val NO_MATCH = "no_match"

const val CLASSIFICATION = "classification"

val nominal: List<String> = arrayListOf(MATCH,
                                        NO_MATCH)



class FingerPrintAttributes(var numberOfDataPoints : Int = 1) : ArrayList<Attribute>() {

    //desired attributes are in comments
    init {
        instanceStore.clear()
        for (i in 0 until numberOfDataPoints) {
            //location related
            addAttribute(GEOFENCE_PROXIMITY_TO_CENTER + i)
            addAttribute(GEOFENCE_X_DISTANCETOCENTER + i)
            addAttribute(GEOFENCE_Y_DISTANCETOCENTER + i)
            addAttribute(ABSOLUTE_HEIGHT + i)
            addAttribute(TIME_SPENT_IN_GEOFENCE + i)
            //addAttribute("timeSpentOutsideGeofence")

            //time related
            addAttribute(PORTION_OF_VALIDITY_INTERVAL_THAT_HAS_PASSED + i)
            addAttribute(DAY + i)
            //addAttribute("howManyRemindersThereWereAlready")

            //accelerationSensor
            addAttribute(ACC_PEAK_SMV + i)
            addAttribute(ACC_MEAN_SMV + i)
            //addAttribute("dymanicTimeWarpingDistanceToSample")
            addAttribute(DOMINANT_FREQUENCY + i)
            addAttribute(POWER_OF_DOMINANT_FREQUENCY + i)
            addAttribute(SECOND_DOMINANT_FREQUENCY + i)
            addAttribute(POWER_OF_SECOND_DOMINANT_FREQUENCY + i)
            addAttribute(DOMINANT_FREQUENCY_625 + i)
            addAttribute(POWER_OF_DOMINANT_FREQUENCY_625 + i)
            addAttribute(TOTAL_POWER + i)
            addAttribute(PART_OF_TOTAL_POWER_IN_P1 + i)
            addAttribute(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW + i)

            //gyro
            addAttribute(GYRO_ACC_PEAK_SMV + i)
            addAttribute(GYRO_ACC_MEAN_SMV + i)
            //addAttribute(GYRO_"dymanicTimeWarpingDistanceToSample")
            addAttribute(GYRO_DOMINANT_FREQUENCY + i)
            addAttribute(GYRO_POWER_OF_DOMINANT_FREQUENCY + i)
            addAttribute(GYRO_SECOND_DOMINANT_FREQUENCY + i)
            addAttribute(GYRO_POWER_OF_SECOND_DOMINANT_FREQUENCY + i)
            addAttribute(GYRO_DOMINANT_FREQUENCY_625 + i)
            addAttribute(GYRO_POWER_OF_DOMINANT_FREQUENCY_625 + i)
            addAttribute(GYRO_TOTAL_POWER + i)
            addAttribute(GYRO_PART_OF_TOTAL_POWER_IN_P1 + i)
            addAttribute(GYRO_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW + i)

            //mag
            addAttribute(MAG_ACC_PEAK_SMV + i)
            addAttribute(MAG_ACC_MEAN_SMV + i)
            //addAttribute(MAG_"dymanicTimeWarpingDistanceToSample")
            addAttribute(MAG_DOMINANT_FREQUENCY + i)
            addAttribute(MAG_POWER_OF_DOMINANT_FREQUENCY + i)
            addAttribute(MAG_SECOND_DOMINANT_FREQUENCY + i)
            addAttribute(MAG_POWER_OF_SECOND_DOMINANT_FREQUENCY + i)
            addAttribute(MAG_DOMINANT_FREQUENCY_625 + i)
            addAttribute(MAG_POWER_OF_DOMINANT_FREQUENCY_625 + i)
            addAttribute(MAG_TOTAL_POWER + i)
            addAttribute(MAG_PART_OF_TOTAL_POWER_IN_P1 + i)
            addAttribute(MAG_RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW + i)

            //proximitySensor
            addAttribute(PROXIMITY_SENSOR_STATE + i) //near or far
            //addAttribute(TIME_IN_PROXIMITY_STATE)

            //screenState
            addAttribute(SCREEN_STATE + i)
            //addAttribute(TIME_IN_SCREEN_STATE)

            //step count
            addAttribute(STEPS_IN_INTERVAL + i)

            //light sensor
            addAttribute(PEAK_LIGHT_VALUE + i)
            addAttribute(MIN_LIGHT_VALUE + i)
            addAttribute(MEAN_LIGHT_VALUE + i)

            //sound
            addAttribute(PEAK_VOLUME + i)
            //addAttribute("minVolume")
            //addAttribute("medianVolume")

            //height
            addAttribute(HEIGHT_DIFFERENCE_IN_INTERVAL + i)
            //detected activity
            add(Attribute(DETECTED_ACTIVITY + i,
                          ACTIVITIES))
            addAttribute(DETECTED_ACTIVITY_CONFIDENCE + i)
            add(Attribute(DETECTED_ACTIVITY_2 + i,
                          ACTIVITIES))
            addAttribute(DETECTED_ACTIVITY_2_CONFIDENCE + i)
            add(Attribute(DETECTED_ACTIVITY_3 + i,
                          ACTIVITIES))
            addAttribute(DETECTED_ACTIVITY_3_CONFIDENCE + i)
            addAttribute(WIFI_SIGNAL_STRENGTH_1 + i)
            addAttribute(WIFI_SIGNAL_STRENGTH_2 + i)
            addAttribute(WIFI_SIGNAL_STRENGTH_3 + i)
            addAttribute(WIFI_SIGNAL_STRENGTH_4 + i)
            addAttribute(WIFI_SIGNAL_STRENGTH_5 + i)
            addAttribute(WIFI_SIGNAL_STRENGTH_6 + i)
            addAttribute(WIFI_SIGNAL_STRENGTH_7 + i)
            addAttribute(WIFI_SIGNAL_STRENGTH_8 + i)
            addAttribute(WIFI_SIGNAL_STRENGTH_9 + i)
            addAttribute(WIFI_SIGNAL_STRENGTH_10 + i)
            add(Attribute(CONNECTED_WIFI + i, MutableList(10, { it -> (1 + it).toString() })))
        }
        add(Attribute(CLASSIFICATION,
                      nominal))
    }

    private fun addAttribute(title: String) = add(Attribute(title))
}


fun createFingerprint(jitai: Jitai,
                      context: Context,
                      sensorData: SensorDataSet,
                      classification:
                      String,
                      fingerPrintAttrs: FingerPrintAttributes): Fingerprint {
    val time = System.currentTimeMillis()
    val fingerprint = Fingerprint("fingerprint",
                                                                                           fingerPrintAttrs)
    var relevantWifi: List<String> = listOf()
    val wifis = deSerializeWifi(sensorData.wifiInformation!!)
    if (wifis.size < 10)
        relevantWifi = wifis.map { it.BSSID }
    else {
        wifis.sortedByDescending { it.rssi }
        relevantWifi = wifis.take(10).map { it.BSSID }
    }
    //Log.d("wifi ambiguation", (System.currentTimeMillis() - time).toString())
    createInstance(context,
                                                                            fingerprint,
                                                                            fingerPrintAttrs,
                                                                            jitai,
                                                                            sensorData,
                                                                            classification,
                                                                            relevantWifi)
    return fingerprint
}

/**
 * sensorData = list<sensordataset, classification>
 */
fun createFingerprint(jitai: Jitai,
                      context: Context,
                      sensorData: List<Pair<SensorDataSet, String>>,
                      fingerPrintAttrs: FingerPrintAttributes):
    Fingerprint {
    val fingerprint = Fingerprint("fingerprint",
                                                                                           fingerPrintAttrs)
    /*val inflatedSensorData = sensorData.fold(
        mutableListOf<Pair<SensorDataSet, String>>(),
        { list, it ->
            for (i in 0 until 5) {
                val copy = it.copy(first = it.first.copy(time = it.first.time - 1000 * i))
                list.add(copy)
            }
            list
        })*/
    var relevantWifi: List<String> = listOf()
    sensorData.forEach {
        val wifis = deSerializeWifi(it.first.wifiInformation!!)
        if (wifis.size < 10)
            relevantWifi = wifis.map { it.BSSID }
        else {
            val grouped = wifis.groupBy { it.BSSID }
            val ranked = grouped.map {
                Pair(it.key, it.value.fold(0.0, { sum, f ->
                    sum + f.rssi.toDouble()
                }) / it.value.size)
            }
            ranked.sortedByDescending { it.second }
            relevantWifi = ranked.take(10).map { it.first }
        }
    }
    sensorData.forEach {
        createInstance(context,
                                                                                fingerprint,
                                                                                fingerPrintAttrs,
                                                                                jitai,
                                                                                it.first,
                                                                                it.second,
                                                                                relevantWifi)
    }
    //val resampledfingerprint = Fingerprint("fingerprint", fingerPrintAttrs)
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

const val windowSize = 5200 // equals 260 * 20 and therefore 256 activity samples at 50Hz (plus 4
// for
// fluctuations)

private val instanceStore = ConcurrentHashMap<Pair<Long, Long>, DoubleArray>()
private val sensorDataSetStore = ConcurrentHashMap<Long, SensorDataSet>()

/**
 * context - for db
 * fingerprint - for weka
 * jitai - what is this about
 * windowDefiningSensorData - last dataset of any window
 * classification - match or no_match
 * relevantBSSIDs - the ten strongest (by average) wifi in this dataset
 */
private fun createInstance(context: Context,
                           fingerprint: Fingerprint,
                           fingerPrintAttrs: FingerPrintAttributes,
                           jitai: Jitai,
                           windowDefiningSensorData: SensorDataSet,
                           classification: String,
                           relevantBSSIDS: List<String>) {
    val methodStartTime = System.currentTimeMillis()
    //where we will put the data, initialize with missing value
    var values = DoubleArray(fingerPrintAttrs.size, { _ -> Double.NaN })
    val db = JitaiDatabase.getInstance(context)
    //val sensorDataSetsInFrame: List<SensorDataSet?> = getSensorDatasetsById(windowDefiningSensorData,db)
    val sensorDataSetsInFrame: List<SensorDataSet?> =
        getSensorDatasetsByTime(
            windowDefiningSensorData,
            db,
            fingerPrintAttrs)
    //Log.d("${jitai.goal} get data", (System.currentTimeMillis() - methodStartTime).toString())
    val temp = instanceStore.get(Pair(windowDefiningSensorData.id, windowDefiningSensorData.time))
    if (temp == null) {
        for (i in 0 until fingerPrintAttrs.numberOfDataPoints) {
            val dataPoint = sensorDataSetsInFrame[i]
            if (dataPoint != null) {
                val db = JitaiDatabase.getInstance(context)
                val proximityTrigger = ProximityTrigger(true)
                with(fingerprint) {
                    with(fingerPrintAttrs) {
                        val geofence = jitai.geofenceTrigger!!.getCurrentLocation()
                        val currentLocation = dataPoint.gps!!
                        //normalized by geofence.radius to [0,1]
                        values[getAttributeIndex(GEOFENCE_PROXIMITY_TO_CENTER + i)] =
                            geofence.location.distanceTo(currentLocation).toDouble() /
                            geofence.radius
                        values[getAttributeIndex(GEOFENCE_X_DISTANCETOCENTER + i)] =
                            normalizeDegreeDistance(
                                currentLocation.longitude - geofence.longitude)
                        values[getAttributeIndex(GEOFENCE_Y_DISTANCETOCENTER + i)] =
                            normalizeDegreeDistance(
                                currentLocation.latitude - geofence.latitude)
                        if (currentLocation.hasAltitude())
                            values[getAttributeIndex(ABSOLUTE_HEIGHT + i)] =
                                normalizePower(
                                    currentLocation.altitude,
                                    1000.0)

                        //TODO
                        values[getAttributeIndex(TIME_SPENT_IN_GEOFENCE + i)] = 0.0

                        val localDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(dataPoint.time),
                                                                ZoneId.systemDefault())
                        /*values[getAttributeIndex(PORTION_OF_VALIDITY_INTERVAL_THAT_HAS_PASSED +
                                                      i)] =
                            jitai.timeTrigger!!.getPassedTimePercent(localDate.toLocalTime())
                        // days range from 1 to 7 -> (day.value -1) / 6 in [0,1]
                        values[getAttributeIndex(DAY + i)] = (localDate.dayOfWeek.value - 1)
                        .toDouble() / 6.0*/
                        //acceleration
                        val accelerationData = db.getALL3DSensorValuesNoTimestamp(dataPoint.time - windowSize,
                                                                                  dataPoint.time,
                                                                                  TABLE_REAL_TIME_ACC)
                        if (accelerationData.size >= 256) {
                            val prevAccelerationData =
                                db.getALL3DSensorValuesNoTimestamp(dataPoint.time - windowSize - windowSize,
                                                                   dataPoint.time - windowSize,
                                                                   TABLE_REAL_TIME_ACC)
                            //Signal Magnitude Vector (SMV) see https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3795931/
                            val smv = getSignalMagnitudeVector(
                                accelerationData.takeLast(256))
                            values[getAttributeIndex(ACC_PEAK_SMV + i)] =
                                normalizePower(
                                    smv.max,
                                    40.0)
                            values[getAttributeIndex(ACC_MEAN_SMV + i)] =
                                normalizePower(
                                    smv.average,
                                    12.0)
                            //use only the last 256 values
                            val fft = getPowerSpectralAnalysis(
                                smv.values)
                            val frequencyAnalysData = getFrequencyAnalysisData(
                                fft)
                            values[getAttributeIndex(DOMINANT_FREQUENCY + i)] = frequencyAnalysData.f1
                            values[getAttributeIndex(POWER_OF_DOMINANT_FREQUENCY + i)] =
                                normalizePower(
                                    frequencyAnalysData.p1,
                                    500000.0)
                            values[getAttributeIndex(SECOND_DOMINANT_FREQUENCY + i)] = frequencyAnalysData.f2
                            values[getAttributeIndex(POWER_OF_SECOND_DOMINANT_FREQUENCY + i)] =
                                normalizePower(
                                    frequencyAnalysData.p2,
                                    50000.0)
                            values[getAttributeIndex(DOMINANT_FREQUENCY_625 + i)] = frequencyAnalysData.f625
                            values[getAttributeIndex(POWER_OF_DOMINANT_FREQUENCY_625 + i)] =
                                normalizePower(
                                    frequencyAnalysData.p625,
                                    500000.0)
                            values[getAttributeIndex(TOTAL_POWER + i)] =
                                normalizePower(
                                    frequencyAnalysData.totalPower,
                                    5000.0)
                            values[getAttributeIndex(PART_OF_TOTAL_POWER_IN_P1 + i)] =
                                normalizePower(
                                    frequencyAnalysData.p1 / frequencyAnalysData.totalPower,
                                    50.0)
                            if (prevAccelerationData.size >= 256) {
                                val prevFFT = getFrequencyAnalysisData(
                                    getPowerSpectralAnalysis(
                                        getSignalMagnitudeVector(
                                            prevAccelerationData.takeLast(256)).values))
                                values[getAttributeIndex(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW + i)] =
                                    normalizePower(
                                        frequencyAnalysData.totalPower / prevFFT.totalPower,
                                        15.0)
                            }
                        }
                        //gyro
                        if (true) {
                            val gyroData = db.getALL3DSensorValuesNoTimestamp(dataPoint.time - windowSize,
                                                                              dataPoint.time,
                                                                              TABLE_REAL_TIME_GYRO)
                            val prevGyroData =
                                db.getALL3DSensorValuesNoTimestamp(dataPoint.time - windowSize - windowSize,
                                                                   dataPoint.time - windowSize,
                                                                   TABLE_REAL_TIME_GYRO)
                            if (gyroData.size >= 256) {
                                //Signal Magnitude Vector (SMV) see https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3795931/
                                val smv = getSignalMagnitudeVector(
                                    gyroData.takeLast(256))
                                values[getAttributeIndex(GYRO_ACC_PEAK_SMV + i)] =
                                    normalizePower(
                                        smv.max,
                                        8.0)
                                values[getAttributeIndex(GYRO_ACC_MEAN_SMV + i)] =
                                    normalizePower(
                                        smv.average,
                                        3.0)
                                //use only the last 256 values
                                val fft = getPowerSpectralAnalysis(
                                    smv.values)
                                val frequencyAnalysData = getFrequencyAnalysisData(
                                    fft)
                                values[getAttributeIndex(GYRO_DOMINANT_FREQUENCY + i)] = frequencyAnalysData.f1
                                values[getAttributeIndex(GYRO_POWER_OF_DOMINANT_FREQUENCY + i)] =
                                    normalizePower(
                                        frequencyAnalysData.p1,
                                        80000.0)
                                values[getAttributeIndex(GYRO_SECOND_DOMINANT_FREQUENCY + i)] = frequencyAnalysData.f2
                                values[getAttributeIndex(GYRO_POWER_OF_SECOND_DOMINANT_FREQUENCY + i)] =
                                    normalizePower(
                                        frequencyAnalysData.p2,
                                        10000.0)
                                values[getAttributeIndex(GYRO_DOMINANT_FREQUENCY_625 + i)] = frequencyAnalysData.f625
                                values[getAttributeIndex(GYRO_POWER_OF_DOMINANT_FREQUENCY_625 + i)] =
                                    normalizePower(
                                        frequencyAnalysData.p625,
                                        80000.0)
                                values[getAttributeIndex(GYRO_TOTAL_POWER + i)] =
                                    normalizePower(
                                        frequencyAnalysData.totalPower,
                                        2500.0)
                                values[getAttributeIndex(GYRO_PART_OF_TOTAL_POWER_IN_P1 + i)] =
                                    normalizePower(
                                        frequencyAnalysData.p1 / frequencyAnalysData
                                            .totalPower,
                                        60.0)
                                if (prevGyroData.size >= 256) {
                                    val prevFFT = getFrequencyAnalysisData(
                                        getPowerSpectralAnalysis(
                                            getSignalMagnitudeVector(
                                                prevGyroData.takeLast(256)).values))
                                    values[getAttributeIndex(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW + i)] =
                                        normalizePower(
                                            frequencyAnalysData.totalPower / prevFFT
                                                .totalPower,
                                            15.0)
                                }
                            }
                        }
                        //mag
                        if (true) {
                            val magData = db.getALL3DSensorValuesNoTimestamp(dataPoint.time - windowSize,
                                                                             dataPoint.time,
                                                                             TABLE_REAL_TIME_MAG)
                            val prevMagData =
                                db.getALL3DSensorValuesNoTimestamp(dataPoint.time - windowSize - windowSize,
                                                                   dataPoint.time - windowSize,
                                                                   TABLE_REAL_TIME_MAG)
                            if (magData.size >= 256) {
                                //Signal Magnitude Vector (SMV) see https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3795931/
                                val smv = getSignalMagnitudeVector(
                                    magData.takeLast(256))
                                values[getAttributeIndex(MAG_ACC_PEAK_SMV + i)] =
                                    normalizePower(
                                        smv.max,
                                        3000.0)
                                values[getAttributeIndex(MAG_ACC_MEAN_SMV + i)] =
                                    normalizePower(
                                        smv.average,
                                        3000.0)
                                //use only the last 256 values
                                val fft = getPowerSpectralAnalysis(
                                    smv.values)
                                val frequencyAnalysData = getFrequencyAnalysisData(
                                    fft)
                                values[getAttributeIndex(MAG_DOMINANT_FREQUENCY + i)] = frequencyAnalysData.f1
                                values[getAttributeIndex(MAG_POWER_OF_DOMINANT_FREQUENCY + i)] =
                                    normalizePower(
                                        frequencyAnalysData.p1,
                                        10000000.0)
                                values[getAttributeIndex(MAG_SECOND_DOMINANT_FREQUENCY + i)] = frequencyAnalysData.f2
                                values[getAttributeIndex(MAG_POWER_OF_SECOND_DOMINANT_FREQUENCY + i)] =
                                    normalizePower(
                                        frequencyAnalysData.p2,
                                        5000.0)
                                values[getAttributeIndex(MAG_DOMINANT_FREQUENCY_625 + i)] = frequencyAnalysData.f625
                                values[getAttributeIndex(MAG_POWER_OF_DOMINANT_FREQUENCY_625 + i)] =
                                    normalizePower(
                                        frequencyAnalysData.p625,
                                        10000000.0)
                                values[getAttributeIndex(MAG_TOTAL_POWER + i)] =
                                    normalizePower(
                                        frequencyAnalysData.totalPower,
                                        10000000.0)
                                values[getAttributeIndex(MAG_PART_OF_TOTAL_POWER_IN_P1 + i)] =
                                    normalizePower(
                                        frequencyAnalysData.p1 / frequencyAnalysData
                                            .totalPower,
                                        70.0)
                                if (prevMagData.size >= 256) {
                                    val prevFFT = getFrequencyAnalysisData(
                                        getPowerSpectralAnalysis(
                                            getSignalMagnitudeVector(
                                                prevMagData.takeLast(256)).values))
                                    values[getAttributeIndex(RATIO_BETWEEN_THIS_AND_PREVIOUS_WINDOW + i)] =
                                        normalizePower(
                                            frequencyAnalysData.totalPower / prevFFT
                                                .totalPower,
                                            15.0)
                                }
                            }
                        }
                        //proximitySensor
                        values[getAttributeIndex(PROXIMITY_SENSOR_STATE + i)] =
                            proximityTrigger.check(context, dataPoint).toDouble()
                        //values[getAttributeIndex(TIME_IN_PROXIMITY_STATE+i)] = 0.0

                        //screenState
                        values[getAttributeIndex(SCREEN_STATE + i)] = dataPoint.screenState.toDouble()
                        //values[getAttributeIndex(TIME_IN_SCREEN_STATE+i)] = 0.0

                        //step count
                        val previousSensorDataset = db.getSensorDataset(dataPoint.id - 1)
                        if (previousSensorDataset?.totalStepsToday != null
                            && dataPoint?.totalStepsToday != null) {
                            values[getAttributeIndex(STEPS_IN_INTERVAL + i)] =
                                Math.min(0.0, dataPoint.totalStepsToday!!.toDouble()
                                    - previousSensorDataset.totalStepsToday!!.toDouble())
                        }
                        //light sensor
                        val lightData = db.getSensorValues(dataPoint.time - windowSize,
                                                           dataPoint.time,
                                                           TABLE_REALTIME_LIGHT)
                        val (lightPeak, lightMin, lightMean) = getLightValues(
                            lightData)
                        values[getAttributeIndex(PEAK_LIGHT_VALUE + i)] = normalizePower(
                            lightPeak,
                            5000.0)
                        values[getAttributeIndex(MIN_LIGHT_VALUE + i)] = normalizePower(
                            lightMin,
                            500.0)
                        values[getAttributeIndex(MEAN_LIGHT_VALUE + i)] = normalizePower(
                            lightMean,
                            5000.0)

                        //sound
                        values[getAttributeIndex(PEAK_VOLUME + i)] = dataPoint.ambientSound!! / 32767.0
                        //addAttribute("minVolume")
                        //addAttribute("medianVolume")

                        //height
                        val heightData = db.getSensorValues(dataPoint.time - windowSize,
                                                            dataPoint.time,
                                                            TABLE_REALTIME_AIR)
                        values[getAttributeIndex(HEIGHT_DIFFERENCE_IN_INTERVAL + i)] =
                            calcHeigthDifference(
                                heightData)

                        val detectedActivitys = dataPoint.activity.sortedBy { it.confidence }
                        values[getAttributeIndex(DETECTED_ACTIVITY + i)] = detectedActivitys[0]!!.type
                            .toDouble()
                        values[getAttributeIndex(DETECTED_ACTIVITY_CONFIDENCE + i)] = detectedActivitys[0]!!.confidence
                            .toDouble()
                        if (detectedActivitys.size > 1) {
                            values[getAttributeIndex(DETECTED_ACTIVITY_2 + i)] = detectedActivitys[1]!!.type
                                .toDouble()
                            values[getAttributeIndex(DETECTED_ACTIVITY_2_CONFIDENCE + i)] = detectedActivitys[1]!!
                                .confidence.toDouble()
                        }
                        if (detectedActivitys.size > 2) {
                            values[getAttributeIndex(DETECTED_ACTIVITY_3 + i)] = detectedActivitys[2]!!.type
                                .toDouble()
                            values[getAttributeIndex(DETECTED_ACTIVITY_3_CONFIDENCE + i)] = detectedActivitys[2]!!
                                .confidence.toDouble()
                        }
                        val wifis = deSerializeWifi(
                            dataPoint.wifiInformation!!)
                        var wifi = wifis.firstOrNull { it.BSSID == relevantBSSIDS.getOrNull(0) }
                        var wifi2 = wifis.firstOrNull { it.BSSID == relevantBSSIDS.getOrNull(1) }
                        var wifi3 = wifis.firstOrNull { it.BSSID == relevantBSSIDS.getOrNull(2) }
                        var wifi4 = wifis.firstOrNull { it.BSSID == relevantBSSIDS.getOrNull(3) }
                        var wifi5 = wifis.firstOrNull { it.BSSID == relevantBSSIDS.getOrNull(4) }
                        var wifi6 = wifis.firstOrNull { it.BSSID == relevantBSSIDS.getOrNull(5) }
                        var wifi7 = wifis.firstOrNull { it.BSSID == relevantBSSIDS.getOrNull(6) }
                        var wifi8 = wifis.firstOrNull { it.BSSID == relevantBSSIDS.getOrNull(7) }
                        var wifi9 = wifis.firstOrNull { it.BSSID == relevantBSSIDS.getOrNull(8) }
                        var wifi10 = wifis.firstOrNull { it.BSSID == relevantBSSIDS.getOrNull(9) }
                        var connected = wifis.firstOrNull { it.networkId > -1 }
                        values[getAttributeIndex(WIFI_SIGNAL_STRENGTH_1 + i)] = getWifiLevel(
                            wifi)
                        values[getAttributeIndex(WIFI_SIGNAL_STRENGTH_2 + i)] = getWifiLevel(
                            wifi2)
                        values[getAttributeIndex(WIFI_SIGNAL_STRENGTH_3 + i)] = getWifiLevel(
                            wifi3)
                        values[getAttributeIndex(WIFI_SIGNAL_STRENGTH_4 + i)] = getWifiLevel(
                            wifi4)
                        values[getAttributeIndex(WIFI_SIGNAL_STRENGTH_5 + i)] = getWifiLevel(
                            wifi5)
                        values[getAttributeIndex(WIFI_SIGNAL_STRENGTH_6 + i)] = getWifiLevel(
                            wifi6)
                        values[getAttributeIndex(WIFI_SIGNAL_STRENGTH_7 + i)] = getWifiLevel(
                            wifi7)
                        values[getAttributeIndex(WIFI_SIGNAL_STRENGTH_8 + i)] = getWifiLevel(
                            wifi8)
                        values[getAttributeIndex(WIFI_SIGNAL_STRENGTH_9 + i)] = getWifiLevel(
                            wifi9)
                        values[getAttributeIndex(WIFI_SIGNAL_STRENGTH_10 + i)] = getWifiLevel(
                            wifi10)
                        if (connected != null)
                            values[getAttributeIndex(CONNECTED_WIFI + i)] =
                                wifis.indexOf(connected).toDouble()
                        //Log.d("${jitai.goal} created","${dataPoint.id} " + (System.currentTimeMillis() - methodStartTime).toString())
                    }
                }
            }
        }
        instanceStore.put(Pair(windowDefiningSensorData.id, windowDefiningSensorData.time),
                          values)
    } else {
        values = temp
       // Log.d("${jitai.goal} cash hit","${windowDefiningSensorData.id} " + (System.currentTimeMillis() - methodStartTime).toString())
    }

    values[fingerprint.getAttributeIndex(CLASSIFICATION)] = nominal.indexOf(classification)
        .toDouble()
    val instance = DenseInstance(1.0, values)
    fingerprint.add(instance)
}

private fun getSensorDatasetsById(windowDefiningSensorData: SensorDataSet,
                                  db: JitaiDatabase,
                                  fingerPrintAttrs: FingerPrintAttributes): List<SensorDataSet?> {
    return (0 until fingerPrintAttrs.numberOfDataPoints)
        .map {
            sensorDataSetStore.get(windowDefiningSensorData.id - it) ?: {
                val s = db.getSensorDataset(windowDefiningSensorData.id - it)
                s?.let { sensorDataSetStore.put(s.id, s) }
                s
            }()
        }
}


private fun getSensorDatasetsByTime(windowDefiningSensorData: SensorDataSet,
                                    db: JitaiDatabase,
                                    fingerPrintAttrs: FingerPrintAttributes): List<SensorDataSet?> {
    return (0 until fingerPrintAttrs.numberOfDataPoints)
        .map {
            val timestamp = windowDefiningSensorData.time - (it * 5000)
            val s = db.getSensorDatasetForTimestamp(timestamp)
            var returnVal: SensorDataSet? = null
            s?.let { returnVal = s.copy(time = timestamp) }
            returnVal
        }
}

private fun getWifiLevel(wifi: WifiInfo?) =
    (wifi?.let { WifiManager.calculateSignalLevel(it.rssi, 100).toDouble() }
        ?: Double.NaN)

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
    return FrequencyAnalysisData(f1,
                                                                                          p1,
                                                                                          f2,
                                                                                          p2,
                                                                                          f625,
                                                                                          p625,
                                                                                          totalPower)
}

class FrequencyAnalysisData(val f1: Double,
                            val p1: Double,
                            val f2: Double,
                            val p2: Double,
                            val
                            f625: Double,
                            val p625: Double,
                            val totalPower: Double)

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
    return SMV(Math.sqrt(average),
                                                                        Math.sqrt(max),
                                                                        values)
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