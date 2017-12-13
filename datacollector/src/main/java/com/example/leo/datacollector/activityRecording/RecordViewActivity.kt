package com.example.leo.datacollector.activityRecording

import android.app.Activity
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TimePicker
import android.widget.Toast
import com.example.leo.datacollector.R
import com.example.leo.datacollector.database.SqliteDatabase
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.recording_detail.*
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Created by Leo on 28.11.2017.
 */
class RecordViewActivity : Activity() {
    var rec_id: Int = -1
    lateinit var map: GoogleMap

    private lateinit var record: ActivityRecord

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recording_detail)
        mapView.onCreate(savedInstanceState)
        if (intent != null) {
            rec_id = intent.getIntExtra(RECORDING_ID, -1)
        }
        if (rec_id < 0) {
            Toast.makeText(this, "bad recordingId", Toast.LENGTH_LONG).show()
            Log.e("RECORD DETAIL VIEW", "Bad recording id")
            onBackPressed()
        }
        val db = SqliteDatabase.getInstance(this)
        val cursor = db.getRecording(rec_id)
        record = ActivityRecord(cursor)

        moveText.setText(record.activities.toString())
        initSoundViews()
        initTimeViews()
        initMovementViews()
        initWeatherView()
        initRawDataView(acc_chart, record.accelerometerData)
        initRawDataView(ori_chart, record.orientationData)
        initRawDataView(magnet_chart, record.magnetData)
        initRawDataView(gyro_chart, record.gyroscopData)
    }

    private fun initWeatherView() {

        weatherImageView.setImageResource(record.weather!!.weatherIcon)
        weatherText.setText(record.weather!!.currentCondition.descr + "\n" +
                                    kelvinToCelsius(record.weather!!.temperature.temp) + " °C")
    }

    private fun kelvinToCelsius(value: Float) = Math.round(value - 273.15f)

    private fun initMovementViews() {
        mapView.visibility = View.INVISIBLE
        mapView.getMapAsync { googleMap ->
            map = googleMap
            if (record.geolocations.size > 2) {
                val options = PolylineOptions()

                options.color(Color.parseColor("#CC0000FF"))
                options.width(5f)
                options.visible(true)

                val builder = LatLngBounds.Builder()

                for (locRecorded in record.geolocations) {
                    builder.include(locRecorded)
                    options.add(locRecorded)
                }

                map.addPolyline(options)

                /**initialize the padding for map boundary*/
                val padding = 50
                val bounds = builder.build()
                /**create the camera with bounds and padding to set into map*/
                val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                /**call the map call back to know map is loaded or not*/
                map.setOnMapLoadedCallback {
                    /**set zoom camera into map */
                    map.moveCamera(cu)
                }
                map.uiSettings.setAllGesturesEnabled(false)
                mapView.visibility = View.VISIBLE
            }
        }
    }

    private fun initTimeViews() {
        timeFrom.setText(record.beginTime.format(DateTimeFormatter.ofPattern("HH:mm")))
        timeTo.setText(record.endTime.format(DateTimeFormatter.ofPattern("HH:mm")))
        timeFrom.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val currentTime = Calendar.getInstance()
                var hour = currentTime.get(Calendar.HOUR_OF_DAY);
                var minute = currentTime.get(Calendar.MINUTE);
                val timePicker = TimePickerDialog(
                        this@RecordViewActivity,
                        object : TimePickerDialog.OnTimeSetListener {
                            override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                                timeFrom.setText("${hourOfDay}:${minute}")
                            }
                        },
                        hour, minute, true)
                timePicker.setTitle("Select Time");
                timePicker.show();
            }
        })
        timeTo.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val currentTime = Calendar.getInstance()
                var hour = currentTime.get(Calendar.HOUR_OF_DAY);
                var minute = currentTime.get(Calendar.MINUTE);
                val timePicker = TimePickerDialog(
                        this@RecordViewActivity,
                        object : TimePickerDialog.OnTimeSetListener {
                            override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                                timeTo.setText("${hourOfDay}:${minute}")
                            }
                        },
                        hour, minute, true)
                timePicker.setTitle("Select Time");
                timePicker.show();
            }
        })
    }

    private fun initSoundViews() {
        val entries = mutableListOf<Entry>()
        record.ambientSound.forEachIndexed { i, value ->
            entries.add(
                    Entry(
                            record.timestamps.get(i).toNanoOfDay().toFloat(),
                            toDecibel(value))
                       )
        }
        val soundData = LineDataSet(entries, "Mittlere Lautstärke in dB")
        soundData.setDrawCircles(false)
        soundData.setColor(R.color.background_material_dark)
        soundData.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(soundData)
        lineData.setDrawValues(false)
        soundchart.data = lineData
        soundchart.invalidate()
        soundchart.setTouchEnabled(false)
        soundchart.xAxis.setDrawLabels(true)
        soundchart.xAxis.valueFormatter = object : IAxisValueFormatter {
            override fun getFormattedValue(value: Float, axis: AxisBase?): String =
                    LocalTime.ofNanoOfDay(value.toLong()).format(
                            DateTimeFormatter.ofPattern("HH:mm:ss"))
        }
        soundchart.xAxis.granularity = TimeUnit.SECONDS.toNanos(30).toFloat()
        soundchart.xAxis.labelCount = 4
        soundchart.axisLeft.setDrawLabels(true)
        soundchart.axisLeft.axisMaximum = 0F
        soundchart.axisLeft.axisMinimum = -100F
        soundchart.axisLeft.granularity = 20F
        soundchart.axisLeft.setDrawGridLines(false)
        soundchart.axisRight.setDrawLabels(false)
        soundchart.legend.setEnabled(true)
        soundchart.legend.setEntries(listOf(LegendEntry("dB", Legend.LegendForm.LINE, Float.NaN,
                                                        Float.NaN, null, 0)))
        soundchart.description.isEnabled = false
    }

    private fun initRawDataView(chart: LineChart, data: MutableList<FloatArray>) {
        val entries_x = mutableListOf<Entry>()
        val entries_y = mutableListOf<Entry>()
        val entries_z = mutableListOf<Entry>()
        data.forEachIndexed { i, value ->
            createAccEntry(entries_x, i, value, 0)
            createAccEntry(entries_y, i, value, 1)
            createAccEntry(entries_z, i, value, 2)
        }
        val accData_x = createAccLineDataSet(entries_x, "x")
        val accData_y = createAccLineDataSet(entries_y, "y")
        val accData_z = createAccLineDataSet(entries_z, "z")
        accData_x.color = getResources().getColor(R.color.red)
        accData_y.color = getResources().getColor(R.color.black)
        accData_z.color = getResources().getColor(R.color.blue)
        val lineData = LineData(accData_x)
        lineData.addDataSet(accData_y)
        lineData.addDataSet(accData_z)
        lineData.setDrawValues(false)
        chart.data = lineData
        chart.invalidate()
        chart.setTouchEnabled(false)
        chart.xAxis.setDrawLabels(true)
        chart.xAxis.valueFormatter = object : IAxisValueFormatter {
            override fun getFormattedValue(value: Float, axis: AxisBase?): String =
                    LocalTime.ofNanoOfDay(value.toLong()).format(
                            DateTimeFormatter.ofPattern("HH:mm:ss"))
        }
        chart.xAxis.granularity = TimeUnit.SECONDS.toNanos(30).toFloat()
        chart.xAxis.labelCount = 4
        chart.axisLeft.setDrawLabels(true)
        /*acc_chart.axisLeft.axisMaximum = 0F
        acc_chart.axisLeft.axisMinimum = -100F
        acc_chart.axisLeft.granularity = 20F*/
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawLabels(false)
        chart.legend.setEnabled(true)
        chart.legend.setEntries(listOf(LegendEntry("dB", Legend.LegendForm.LINE, Float.NaN,
                                                   Float.NaN, null, 0)))
        chart.description.isEnabled = false
    }

    private inline fun createAccEntry(entries_x: MutableList<Entry>,
                                      i: Int,
                                      value: FloatArray, axis: Int) {
        entries_x.add(
                Entry(
                        record.timestamps.get(i).toNanoOfDay().toFloat(),
                        value[axis])
                     )
    }

    private inline fun createAccLineDataSet(entries_x: MutableList<Entry>, label: String):
            LineDataSet {
        val accData_x = LineDataSet(entries_x, label)
        accData_x.setDrawCircles(false)
        accData_x.setColor(R.color.background_material_dark)
        accData_x.mode = LineDataSet.Mode.CUBIC_BEZIER
        return accData_x
    }

    // +1 to prevent log(0)
    private fun toDecibel(value: Double) = 20 * Math.log10((value) / 32767F).toFloat()

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}