package de.leo.fingerprint.datacollector.ui.uiElements

import android.content.Context
import android.util.AttributeSet
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.ui.activityRecording.ActivityRecord
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

    private val minuteValueFormatter = object : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase?): String =
                LocalTime.ofNanoOfDay(value.toLong() * 1000)
                        .format(DateTimeFormatter.ofPattern("mm:ss"))
    }

    fun setData(type: String, record: ActivityRecord) {
        var rawdata: MutableCollection<Pair<Long, FloatArray>>
        when (type) {
            ACCELERATION -> rawdata = record.accelerometerData
            MAGNET       -> rawdata = record.magnetData
            ORIENTATION  -> rawdata = record.orientationData
            GYROSCOPE    -> rawdata = record.gyroscopData
            else                                                        -> rawdata = record.accelerometerData
        }
        val entries_x = mutableListOf<Entry>()
        val entries_y = mutableListOf<Entry>()
        val entries_z = mutableListOf<Entry>()
        val entries_scalar = mutableListOf<Entry>()
        if (rawdata.size > 0) {
            val firstTimeStamp = rawdata.first().first
            rawdata.forEach { value ->
                val time = (value.first - firstTimeStamp).toFloat()
                entries_x.add(Entry(time, value.second[0]))
                entries_y.add(Entry(time, value.second[1]))
                entries_z.add(Entry(time, value.second[2]))
                if (type == ORIENTATION)
                    entries_scalar.add(Entry(time, value.second[3]))
            }
            val data_x = createAccLineDataSet(entries_x, "x")
            val data_y = createAccLineDataSet(entries_y, "y")
            val data_z = createAccLineDataSet(entries_z, "z")
            data_x.color = getResources().getColor(R.color.red)
            data_y.color = getResources().getColor(R.color.black)
            data_z.color = getResources().getColor(R.color.blue)
            val lineData: LineData
            lineData = LineData(data_x, data_y, data_z)
            lineData.setDrawValues(false)
            data = lineData
            invalidate()
            setTouchEnabled(true)
            xAxis.setDrawLabels(true)
            xAxis.valueFormatter = minuteValueFormatter
            xAxis.granularity = TimeUnit.SECONDS.toMillis(30).toFloat()
            xAxis.labelCount = 4
            axisLeft.setDrawLabels(true)
            axisLeft.setDrawGridLines(false)
            axisRight.setDrawLabels(false)
            legend.setEnabled(true)
            description.isEnabled = false
            when (type) {
                ACCELERATION -> adjustAccGraph()
                ORIENTATION  -> adjustOriGraph()
                MAGNET       -> adjustMagGraph()
                GYROSCOPE    -> adjustGyroGraph()
                else                                                        -> adjustAccGraph()
            }
        }
    }

    fun setData(type: String, dataIn : Collection<Pair<Long, FloatArray>>) {
        var rawdata = dataIn
        val entries_x = mutableListOf<Entry>()
        val entries_y = mutableListOf<Entry>()
        val entries_z = mutableListOf<Entry>()
        if (rawdata.size > 0) {
            val firstTimeStamp = rawdata.first().first
            rawdata.forEach { value ->
                val time = (value.first - firstTimeStamp).toFloat()
                entries_x.add(Entry(time, value.second[0]))
                entries_y.add(Entry(time, value.second[1]))
                entries_z.add(Entry(time, value.second[2]))
            }
            val data_x = createAccLineDataSet(entries_x, "x")
            val data_y = createAccLineDataSet(entries_y, "y")
            val data_z = createAccLineDataSet(entries_z, "z")
            data_x.color = getResources().getColor(R.color.red)
            data_y.color = getResources().getColor(R.color.black)
            data_z.color = getResources().getColor(R.color.blue)
            val lineData: LineData
            lineData = LineData(data_x, data_y, data_z)
            lineData.setDrawValues(false)
            data = lineData
            invalidate()
            setTouchEnabled(true)
            xAxis.setDrawLabels(true)
            xAxis.valueFormatter = minuteValueFormatter
            xAxis.granularity = TimeUnit.SECONDS.toMillis(30).toFloat()
            xAxis.labelCount = 4
            axisLeft.setDrawLabels(true)
            axisLeft.setDrawGridLines(false)
            axisRight.setDrawLabels(false)
            legend.setEnabled(true)
            description.isEnabled = false
            when (type) {
                ACCELERATION -> adjustAccGraph()
                ORIENTATION  -> adjustOriGraph()
                MAGNET       -> adjustMagGraph()
                GYROSCOPE    -> adjustGyroGraph()
                else                                                        -> adjustAccGraph()
            }
        }
    }

    private fun createAccLineDataSet(entries: MutableList<Entry>, label: String):
            LineDataSet {
        val accData = LineDataSet(entries, label)
        accData.setDrawCircles(false)
        accData.setColor(R.color.background_material_dark)
        accData.mode = LineDataSet.Mode.LINEAR
        return accData
    }


    private fun adjustAccGraph() {
        data.getDataSetByLabel("z", true).label = "z - in m/s²"
        axisLeft.axisMaximum = 30f
        axisLeft.axisMinimum = -30f
    }

    private fun adjustMagGraph() {
        data.getDataSetByLabel("z", true).label = "z - in µT"
        axisLeft.axisMaximum = 60f
        axisLeft.axisMinimum = -60f
    }

    private fun adjustGyroGraph() {
        data.getDataSetByLabel("z", true).label = "z - in rad/s"
        axisLeft.axisMaximum = 10f
        axisLeft.axisMinimum = -10f
    }

    private fun adjustOriGraph() {
        val pi: Float = Math.PI.toFloat()
        /*(data.getDataSetByLabel("x", true) as LineDataSet)
                .values.forEach { v -> v.y = v.y * 180f / pi }
        (data.getDataSetByLabel("y", true) as LineDataSet)
                .values.forEach { v -> v.y = v.y * 180f / pi }
        (data.getDataSetByLabel("z", true) as LineDataSet)
                .values.forEach { v -> v.y = v.y * 180f / pi }

        data.getDataSetByLabel("z", true).label = "z - grad"
        axisLeft.axisMaximum = 190f
        axisLeft.axisMinimum = -190f*/
        axisLeft.axisMaximum = 1f
        axisLeft.axisMinimum = -1f
    }
}

const val ACCELERATION = "acc"
const val ORIENTATION = "ori"
const val MAGNET = "mag"
const val GYROSCOPE = "gyro"