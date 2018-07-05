package de.leo.fingerprint.datacollector.ui.compare

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import com.timeseries.TimeSeries
import com.timeseries.TimeSeriesPoint
import com.util.DistanceFunctionFactory
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.database.TABLE_REAL_TIME_ACC
import de.leo.fingerprint.datacollector.ui.activityRecording.ActivityRecord
import de.leo.fingerprint.datacollector.ui.uiElements.ORIENTATION
import kotlinx.android.synthetic.main.compare_record_activity.*

/**
 * Created by Leo on 08.01.2018.
 */
class CompareActivity : AppCompatActivity() {

    var rec_id1 = 0
    var rec_id2 = 1
    lateinit var record1: ActivityRecord
    lateinit var record2: ActivityRecord
    val distFn = DistanceFunctionFactory.getDistFnByName("EuclideanDistance")
    lateinit var recordingIds: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compare_record_activity)
        val db = JitaiDatabase.getInstance(this)
        recordingIds = db.getRecordingIds()
        setRecord()
        setCompare()
        nextRecording.setOnClickListener {
            rec_id1++
            setRecord()
        }
        nextCompare.setOnClickListener {
            rec_id2++
            setCompare()
        }
        reference_chart.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP)
                updateDistance()
            false
        }
        recording_chart.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP)
                updateDistance()
            false
        }
        searchSimilar.setOnClickListener { searchSimilar() }
    }

    private fun searchSimilar() {
        reference_chart.setTouchEnabled(false)
        val db = JitaiDatabase.getInstance(this)
        val allData = //db.getRecording(recordingIds[rec_id2 % recordingIds.size])
                // .orientationData.toList()

                db.getALL3DSensorValues(recordingIds[rec_id1 % recordingIds.size],
                                        TABLE_REAL_TIME_ACC)
        var dataFrame = record1.orientationData.filter { value ->
            value.first - record1.orientationData.first().first >= reference_chart.lowestVisibleX
                    && value.first - record1.orientationData.first().first <= reference_chart
                    .highestVisibleX
        }.toList()
        var counter = dataFrame.size
        var dataFrameTimeSeries = getTimeSeries(dataFrame)
        val step = dataFrame.size / 10
        var minDistance = Double.MAX_VALUE
        val testFrame = mutableListOf<Pair<Long, FloatArray>>()
        for (i in 0 until counter) {
            testFrame.add(allData.get(i))
        }
        val testSeries = getTimeSeries(testFrame)
        var lastTime1 = testFrame.last().first
        while (dataFrame.size < allData.size - counter - step) {
            val distance = getDistance3D(testSeries, dataFrameTimeSeries)
            if (minDistance > distance) {
                recording_chart.setData(ORIENTATION,
                                        allData.drop(counter - dataFrame.size).take(dataFrame.size))
                distTextView.setText(minDistance.toString())
            }
            minDistance = Math.min(minDistance, distance)
            Log.d("counter", counter.toString())
            for (i in 0 until step)
                testSeries.removeFirst()
            while (testSeries.size() < dataFrame.size && counter < allData.size) {
                // if (allData[counter].second[0] > 0.1 || allData[counter].second[1] > 0.1 ||
                //        allData[counter].second[2] > 0.1) {
                if (lastTime1 < allData[counter].first) {
                    val array = DoubleArray(3)
                    array[0] = discretize2(allData[counter].second[0])
                    array[1] = discretize2(allData[counter].second[1])
                    array[2] = discretize2(allData[counter].second[2])
                    val tp = TimeSeriesPoint(array)
                    testSeries.addLast(allData[counter].first.toDouble(), tp)
                    lastTime1 = allData[counter].first
                }
                // }
                counter++
            }
        }
        reference_chart.setTouchEnabled(true)
    }


    private fun updateDistance() {
        val ts1 = getTimeSeries(record1.orientationData,
                                reference_chart.lowestVisibleX,
                                reference_chart.highestVisibleX)
        val ts2 = getTimeSeries(record2.orientationData,
                                recording_chart.lowestVisibleX,
                                recording_chart.highestVisibleX)
        distTextView.text = getDistance3D(ts1, ts2).toString()

    }

    private fun getTimeSeries(data: Collection<Pair<Long, FloatArray>>, minX: Float,
                              maxX: Float): TimeSeries {
        var lastTime1 = 0L
        val ts2 = TimeSeries(3)
        data.forEach { values ->
            if (values.first - data.first().first >= minX
                    && values.first - data.first().first <= maxX) {
                val array = DoubleArray(3)
                array[0] = discretize2(values.second[0])
                array[1] = discretize2(values.second[1])
                array[2] = discretize2(values.second[2])
                val tp = TimeSeriesPoint(array)
                if (lastTime1 < values.first)
                    ts2.addLast(values.first.toDouble(), tp)
                lastTime1 = values.first
            }
        }
        return ts2
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
    fun discretize(value: Float): Double {
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

    fun discretize2(value: Float): Double {
        return value.toDouble()
    }

    /*private fun movingAberage(data :  MutableCollection<Pair<Long, FloatArray>>):
            MutableCollection<Pair<Long, FloatArray>>{
    }*/

    fun setRecord() {
        val db = JitaiDatabase.getInstance(this)
        record1 = db.getRecording(recordingIds[rec_id1 % recordingIds.size])
        reference_chart.setData(ORIENTATION, record1)
        rec_name.text = record1.name
    }

    fun setCompare() {
        val db = JitaiDatabase.getInstance(this)
        record2 = db.getRecording(recordingIds[rec_id2 % recordingIds.size])
        recording_chart.setData(ORIENTATION, record2)
        compare_name.text = record2.name
    }


    fun getDistance3D(valuesToTest: TimeSeries, trace: TimeSeries): Double {
        val warpInfo = com.dtw.FastDTW.getWarpInfoBetween(valuesToTest, trace, 3, distFn);
        return warpInfo.distance
    }


}