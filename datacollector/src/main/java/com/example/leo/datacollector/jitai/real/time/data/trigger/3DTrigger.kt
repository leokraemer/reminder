package com.example.leo.datacollector.jitai.real.time.data.trigger

import com.timeseries.TimeSeries
import com.timeseries.TimeSeriesPoint
import com.util.DistanceFunctionFactory

/**
 * Created by Leo on 16.01.2018.
 */
class RealTimeDataTrigger(reference: MutableCollection<Pair<Long, FloatArray>>,
                         private var threshold: Double) {
    companion object {
        val DISTANCE_FUNCTION = DistanceFunctionFactory.getDistFnByName("EuclideanDistance")
        val SEARCH_RADIUS = 3
    }

    private val referenceTimeSeries: TimeSeries = getTimeSeries(reference)

    //initialized to comparison with all zero data
    val minDistance: Double

    init {
        val allZeroTimeSeriesData = MutableList(referenceTimeSeries.size(), { i ->
            Pair(i.toLong(), FloatArray(3))
        })
        minDistance = getDistance3D(referenceTimeSeries, getTimeSeries(allZeroTimeSeriesData))
        threshold = Math.min(threshold, minDistance)
    }

    fun check(sensorData: MutableCollection<Pair<Long, FloatArray>>): Boolean {
        return threshold < getDistance3D(referenceTimeSeries, getTimeSeries(sensorData))
    }

    fun getSize(){
        referenceTimeSeries.size()
    }

    private fun getTimeSeries(data: Collection<Pair<Long, FloatArray>>): TimeSeries {
        var lastTime1 = 0L
        val ts2 = TimeSeries(3)
        data.forEach { values ->
            val array = DoubleArray(3)
            array[0] = discretize2(values.second[0])
            array[1] = discretize2(values.second[1])
            array[2] = discretize2(values.second[2])
            val tp = TimeSeriesPoint(array)
            if (lastTime1 < values.first)
                ts2.addLast(values.first.toDouble(), tp)
            lastTime1 = values.first
        }
        return ts2
    }

    // modelled after http://sclab.yonsei.ac.kr/courses/10TPR/10TPR.files/uWave_Accelerometer_based%20personalized%20gesture%20recognition%20and%20its%20applications.pdf
    private fun discretize(value: Float): Double {
        if (value == 0f)
            return 0.0
        if (value > 2f)
            return 16.0
        if (value < -2f)
            return -16.0
        if (value > 1f) {
            return Math.ceil((value.toDouble() - 1) * 5) + 10
        }
        if (value > 0f) {
            return Math.ceil(value.toDouble() * 10)
        }
        if (value < -1f) {
            return Math.floor((value.toDouble() + 1) * 5) - 10
        }
        if (value < 0f) {
            return Math.floor(value.toDouble() * 10)
        }
        return 0.0
    }

    private fun discretize2(value: Float): Double {
        return value.toDouble()
    }

    private fun getDistance3D(valuesToTest: TimeSeries, trace: TimeSeries): Double {
        val warpInfo = com.dtw.FastDTW.getWarpInfoBetween(valuesToTest, trace, SEARCH_RADIUS,
                                                          DISTANCE_FUNCTION);
        return warpInfo.distance
    }
}