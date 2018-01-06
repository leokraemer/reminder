package com.example.leo.datacollector.activityDetection

import android.content.Context
import android.util.Log
import com.example.leo.datacollector.activityRecording.ActivityRecord
import com.example.leo.datacollector.database.SqliteDatabase
import com.example.leo.datacollector.models.SensorDataSet
import com.timeseries.TimeSeries
import com.timeseries.TimeSeriesPoint
import com.util.DistanceFunctionFactory
import java.util.*

private val PREASSURE_THRESHOLD: Double = 10.0
private val SOUND_THRESHOLD: Double = 10.0
private val ACCELERATION_THRESHOLD = 100.0
private val TURN_THRESHOLD = 100.0

/**
 * Created by Leo on 21.12.2017.
 */
class ActivityRecognizer(val context: Context) {
    var record: ActivityRecord? = null

    var turnTimeSeries: TimeSeries? = null
    var accTimeSeries: TimeSeries? = null
    var soundTimeSeries: TimeSeries? = null
    var preassureTimeSeries: TimeSeries? = null

    init {
        val db = SqliteDatabase.getInstance(context)
        val id = db.getRecognizedActivitiesId()
        if (id > 0) {
            record = db.getRecording(db.getRecognizedActivitiesId())

            accTimeSeries = TimeSeries(3)
            val accData = record!!.accelerometerData
            for (i in 0 until accData.size) {
                val values = DoubleArray(3)
                values[0] = accData[i][0].toDouble()
                values[1] = accData[i][1].toDouble()
                values[2] = accData[i][2].toDouble()
                val tsPoint = TimeSeriesPoint(values)
                accTimeSeries!!.addLast(record!!.timestamps[i].toDouble(), tsPoint)
            }
            turnTimeSeries = TimeSeries(3)
            val turnData = record!!.orientationData
            for (i in 0 until turnData.size) {
                val values = DoubleArray(3)
                values[0] = turnData[i][0].toDouble()
                values[1] = turnData[i][1].toDouble()
                values[2] = turnData[i][2].toDouble()
                val tsPoint = TimeSeriesPoint(values)
                turnTimeSeries!!.addLast(record!!.timestamps[i].toDouble(), tsPoint)
            }
            val sound = DoubleArray(record!!.ambientSound.size, { i ->
                (record!!.ambientSound[i])
            })
            soundTimeSeries = TimeSeries(sound)
            val preassures = DoubleArray(record!!.pressure.size, { i ->
                (record!!.pressure[i])
            })
            preassureTimeSeries = TimeSeries(preassures)
        }
    }

    val maxLength = 20;
    var one = doubleArrayOf(987.8076171875, 987.822998046875, 987.779541015625,
                            987.803466796875, 987.80029296875, 987.996337890625,
                            988.1962890625, 988.24072265625, 988.449951171875,
                            988.56640625, 988.5361328125, 988.435302734375,
                            988.21875, 988.2197265625, 987.971435546875,
                            987.81884765625, 987.816650390625, 987.82568359375,
                            987.823486328125, 987.779296875)
    var two = doubleArrayOf(987.96435546875, 988.1640625,
                            988.3408203125, 988.51220703125, 988.52001953125,
                            988.471923828125, 988.2783203125, 988.1806640625,
                            988.041259765625, 987.857666015625, 987.8076171875,
                            987.80859375)

    var three = doubleArrayOf(987.81103515625, 987.807373046875, 987.920654296875,
                              988.06884765625, 988.209228515625, 988.385009765625,
                              988.54443359375, 988.512451171875, 988.48193359375,
                              988.173095703125, 988.092529296875, 987.8642578125,
                              987.776611328125)

    var four = doubleArrayOf(987.799072265625, 987.82080078125, 987.862548828125,
                             988.164306640625, 988.290283203125, 988.50537109375,
                             988.53076171875, 988.453857421875, 988.1904296875,
                             988.042724609375, 987.8515625, 987.780029296875,
                             987.80517578125, 987.777587890625)

    val oneTime = TimeSeries(one.map { value -> value - one[0] }.toDoubleArray())

    val distFn = DistanceFunctionFactory.getDistFnByName("EuclideanDistance")

    fun preassureMatch(values: ArrayDeque<SensorDataSet>): Boolean {
        val allValues = DoubleArray(values.size, { i ->
            (values.toArray()[i] as
                    SensorDataSet).airPressure!!.toDouble()
        })
        val distance = getDistance(allValues, preassureTimeSeries!!)
        Log.d("pressure Distance", distance.toString())
        return distance > PREASSURE_THRESHOLD
    }

    fun soundMatch(values: ArrayDeque<SensorDataSet>): Boolean {
        val timeSeries = soundTimeSeries!!
        val allValues = DoubleArray(values.size, { i ->
            (values.toArray()[i] as
                    SensorDataSet).ambientSound!!.toDouble()
        })
        val distance = getDistance(allValues, timeSeries)
        Log.d("sound Distance", distance.toString())
        return distance > SOUND_THRESHOLD
    }

    fun accelerationMatch(values: ArrayDeque<SensorDataSet>): Boolean {
        val timeSeries = accTimeSeries!!
        val valueTimeSeries = TimeSeries(3)
        values.forEachIndexed({ i, value ->
                                  val values = DoubleArray(3)
                                  values[0] = value.acc_x.toDouble()
                                  values[1] = value.acc_y.toDouble()
                                  values[2] = value.acc_z.toDouble()
                                  val tsPoint = TimeSeriesPoint(values)
                                  valueTimeSeries.addLast(i.toDouble(), tsPoint)
                              })
        val distance = getDistance3D(valueTimeSeries, timeSeries)
        Log.d("accDistance", "" + distance)
        return distance > ACCELERATION_THRESHOLD
    }

    fun turnMatch(values: ArrayDeque<SensorDataSet>): Boolean {
        val timeSeries = turnTimeSeries!!
        val valueTimeSeries = TimeSeries(3)
        values.forEachIndexed({ i, value ->
                                  val values = DoubleArray(3)
                                  values[0] = value.azimuth.toDouble()
                                  values[1] = value.pitch.toDouble()
                                  values[2] = value.roll.toDouble()
                                  val tsPoint = TimeSeriesPoint(values)
                                  valueTimeSeries.addLast(i.toDouble(), tsPoint)
                              })
        val distance = getDistance3D(valueTimeSeries, timeSeries)
        Log.d("turnDistance", "" + distance)
        return distance > TURN_THRESHOLD
    }

    fun getDistance(allValues: DoubleArray, trace: TimeSeries): Double {
        var distance = Double.MAX_VALUE
        //min 5 max 20 values long
        for (i in 5 until Math.min(maxLength, allValues.size)) {
            val someVals: DoubleArray = allValues.sliceArray(allValues.size - i - 1 until allValues
                    .size)
            if (someVals.size > 4) {
                val ts = TimeSeries(someVals)
                val warpInfo = com.dtw.FastDTW.getWarpInfoBetween(ts, trace, 3, distFn);
                distance = Math.min(distance, warpInfo.distance)
            }
        }
        return distance
    }

    fun getDistance3D(valuesToTest: TimeSeries, trace: TimeSeries): Double {
        val warpInfo = com.dtw.FastDTW.getWarpInfoBetween(valuesToTest, trace, 3, distFn);
        return warpInfo.distance
    }

    fun recognizeActivity(sensorValues: ArrayDeque<SensorDataSet>) {
        if (record != null) {
            val turn = turnMatch(sensorValues)
            val acc = accelerationMatch(sensorValues)
            val preassure = preassureMatch(sensorValues)
            val sound = soundMatch(sensorValues)
        }
    }
}
