package com.example.leo.datacollector

import android.content.Context
import android.util.AttributeSet
import com.example.leo.datacollector.activityRecording.ActivityRecord
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Created by Leo on 03.01.2018.
 */
class raw3DChart : LineChart {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context,
                                                                              attrs,
                                                                              defStyle)

    private lateinit var record: ActivityRecord
    private val minuteValueFormatter = object : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase?): String =
                LocalTime.ofNanoOfDay(value.toLong())
                        .format(DateTimeFormatter.ofPattern("mm:ss"))
    }

    fun setData(type: String, record: ActivityRecord) {
        var rawdata: MutableList<FloatArray>
        when (type) {
            ACCELERATION -> rawdata = record.accelerometerData
            MAGNET -> rawdata = record.magnetData
            ORIENTATION -> rawdata = record.orientationData
            GYROSCOPE -> rawdata = record.gyroscopData
            else -> rawdata = record.accelerometerData
        }
        this.record = record
        val entries_x = mutableListOf<Entry>()
        val entries_y = mutableListOf<Entry>()
        val entries_z = mutableListOf<Entry>()
        rawdata.forEachIndexed { i, value ->
            createRawEntry(entries_x, i, value, 0)
            createRawEntry(entries_y, i, value, 1)
            createRawEntry(entries_z, i, value, 2)
        }
        val data_x = createAccLineDataSet(entries_x, "x")
        val data_y = createAccLineDataSet(entries_y, "y")
        val data_z = createAccLineDataSet(entries_z, "z")
        data_x.color = getResources().getColor(R.color.red)
        data_y.color = getResources().getColor(R.color.black)
        data_z.color = getResources().getColor(R.color.blue)
        val lineData = LineData(data_x, data_y, data_z)
        lineData.setDrawValues(false)
        data = lineData
        invalidate()
        setTouchEnabled(true)
        xAxis.setDrawLabels(true)
        xAxis.valueFormatter = minuteValueFormatter
        xAxis.granularity = TimeUnit.SECONDS.toNanos(30).toFloat()
        xAxis.labelCount = 4
        axisLeft.setDrawLabels(true)
        axisLeft.setDrawGridLines(false)
        axisRight.setDrawLabels(false)
        legend.setEnabled(true)
        description.isEnabled = false
        when (type) {
            ACCELERATION -> adjustAccGraph()
            ORIENTATION -> adjustOriGraph()
            MAGNET -> adjustMagGraph()
            GYROSCOPE -> adjustGyroGraph()
            else -> adjustAccGraph()
        }
    }


    private fun createRawEntry(entries_x: MutableList<Entry>,
                               i: Int,
                               value: FloatArray,
                               axis: Int) {
        entries_x.add(Entry(getTimeForRecord(i), value[axis]))
    }

    private fun getTimeForRecord(i: Int) = (record.timestamps.get(i) - record
            .beginTime).toFloat()

    private fun createAccLineDataSet(entries: MutableList<Entry>, label: String):
            LineDataSet {
        val accData = LineDataSet(entries, label)
        accData.setDrawCircles(false)
        accData.setColor(R.color.background_material_dark)
        accData.mode = LineDataSet.Mode.LINEAR//CUBIC_BEZIER
        return accData
    }


    private fun adjustAccGraph() {
        data.getDataSetByLabel("z", true).label = "z - in m/s²"
        axisLeft.axisMaximum = 1f
        axisLeft.axisMinimum = -1f
    }

    private fun adjustMagGraph() {
        data.getDataSetByLabel("z", true).label = "z - in µT"
        axisLeft.axisMaximum = 50f
        axisLeft.axisMinimum = -50f
    }

    private fun adjustGyroGraph() {
        data.getDataSetByLabel("z", true).label = "z - in deg/s"
        axisLeft.axisMaximum = 1f
        axisLeft.axisMinimum = -1f
    }

    private fun adjustOriGraph() {
        val pi: Float = Math.PI.toFloat()
        (data.getDataSetByLabel("x", true) as LineDataSet)
                .values.forEach { v -> v.y = v.y * 180f / pi }
        (data.getDataSetByLabel("y", true) as LineDataSet)
                .values.forEach { v -> v.y = v.y * 180f / pi }
        (data.getDataSetByLabel("z", true) as LineDataSet)
                .values.forEach { v -> v.y = v.y * 180f / pi }

        data.getDataSetByLabel("z", true).label = "z - grad"
        axisLeft.axisMaximum = 190f
        axisLeft.axisMinimum = -190f
    }
}

const val ACCELERATION = "acc"
const val ORIENTATION = "ori"
const val MAGNET = "mag"
const val GYROSCOPE = "gyro"