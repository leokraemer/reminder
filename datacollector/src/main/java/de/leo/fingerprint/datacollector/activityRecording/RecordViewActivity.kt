package de.leo.fingerprint.datacollector.activityRecording

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.TimePicker
import android.widget.Toast
import de.leo.fingerprint.datacollector.*
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import de.leo.fingerprint.datacollector.activityDetection.getPowerSpectralAnalysis
import de.leo.fingerprint.datacollector.activityDetection.getSignalMagnitudeVector
import de.leo.fingerprint.datacollector.database.*
import kotlinx.android.synthetic.main.recording_detail.*
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// 1kPa = 7.5006157584566 mmHg
const val PaTommHg = 750.06157584566F
const val g = 9.81F
const val densityHg = 13.595F

fun preassureDifferentialToHeightDifferential(preassureDifferential: Float): Float =
    preassureDifferential * PaTommHg / (g * densityHg)

/**
 * Created by Leo on 28.11.2017.
 */
class RecordViewActivity : AppCompatActivity() {
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
        val db = JitaiDatabase.getInstance(this)

        if (true)
            record = db.getRecording(rec_id)

        initSoundViews()
        initTimeViews()
        initMovementViews()
        initWeatherView()
        acc_chart.setData(ACCELERATION, record)
        ori_chart.setData(ORIENTATION, record)
        magnet_chart.setData(MAGNET, record)
        gyro_chart.setData(GYROSCOPE, record)
        initHeightGraph()
        if (record.proximity.size > 0)
            initProximityGraph()
        if (record.activities.size > 0)
            initActivityView()
        initLightView()
        initWifiView()
        initBTView()
        if (record.screenState.size > 0)
            initScreenView()
        initStepsView()
        if (db.getRecognizedActivitiesId() == rec_id) {
            activity_switch.isChecked = true
        }
        activity_switch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                db.insertEvent(rec_id)
                activity_switch.isChecked = true
            }
        })
        nameEditText.setText(record.name)
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                db.setRecordingName(s.toString(), rec_id)
                record.name = s.toString()
            }
        })
    }

    private fun initStepsView() {
        val entries = mutableListOf<Entry>()
        record.steps.forEachIndexed { i, value ->
            entries.add(
                Entry(getTimeForRecord(i),
                      value.toFloat()))
        }
        val data = createHeightLineDataSet(entries, "Schritte")
        data.color = getResources().getColor(R.color.black)
        val lineData = LineData(data)
        lineData.setDrawValues(false)
        stepschart.data = lineData
        stepschart.invalidate()
        stepschart.setTouchEnabled(false)
        adjustXAxisForTime(stepschart)
        stepschart.axisLeft.setDrawLabels(true)
        stepschart.axisLeft.setDrawGridLines(false)
        stepschart.axisLeft.setDrawGridLines(false)
        stepschart.axisLeft.axisMinimum = 0F
        stepschart.legend.setEnabled(true)
        stepschart.legend.setEntries(listOf(LegendEntry("Schritte",
                                                        Legend.LegendForm.LINE,
                                                        Float.NaN,
                                                        Float.NaN,
                                                        null,
                                                        0)))
        stepschart.description.isEnabled = false
    }

    private fun initProximityGraph() {
        val entries = mutableListOf<Entry>()
        val firstTimeStamp = record.proximity.first().first
        record.proximity.forEach { value ->
            val time = value.first - firstTimeStamp
            entries.add(
                Entry(time.toFloat(),
                      value.second.toFloat()))
        }
        val data = createHeightLineDataSet(entries, "Näherungssensor")
        data.color = getResources().getColor(R.color.black)
        data.mode = LineDataSet.Mode.STEPPED
        val lineData = LineData(data)
        lineData.setDrawValues(false)

        proximityChart.data = lineData
        proximityChart.invalidate()
        proximityChart.setTouchEnabled(false)
        adjustXAxisForTime(proximityChart)
        proximityChart.axisLeft.setDrawLabels(true)
        proximityChart.axisLeft.valueFormatter = object : IAxisValueFormatter {
            override fun getFormattedValue(value: Float, axis: AxisBase?): String {
                if (value <= 0) return "nah"
                return "fern"
            }
        }
        proximityChart.axisLeft.labelCount = 2
        proximityChart.axisLeft.setDrawGridLines(false)
        proximityChart.axisLeft.axisMinimum = -1f
        proximityChart.axisLeft.axisMaximum = (getSystemService(Context
                                                                    .SENSOR_SERVICE) as SensorManager).getDefaultSensor(
            Sensor.TYPE_PROXIMITY).maximumRange + 1

        proximityChart.axisRight.setDrawLabels(false)
        proximityChart.legend.setEnabled(true)
        proximityChart.legend.setEntries(listOf(LegendEntry("Näherungssensor",
                                                            Legend.LegendForm.LINE,
                                                            Float.NaN,
                                                            Float.NaN,
                                                            null,
                                                            0)))
        proximityChart.description.isEnabled = false
    }

    private fun initBTView() {
        bluetoothText.text = record.bluetooth.toString()
    }

    private fun initWifiView() {
        wifiText.text = record.wifis.toString()
    }

    private fun initScreenView() {
        val entries = Array<MutableList<BarEntry>>(2, { _ -> mutableListOf() })
        record.screenState.forEachIndexed { i, value ->
            entries[record.screenState[i]].add(BarEntry(getTimeForRecord(i), 1F))
        }
        val barDataSet = Array(2, { i -> BarDataSet(entries[i], getScreenStateValue(i)) })
        barDataSet[0].color = resources.getColor(R.color.black)
        barDataSet[1].color = resources.getColor(R.color.lightorange)
        val barData = BarData()
        for (i in 0 until barDataSet.size) {
            barData.addDataSet(barDataSet[i])
        }
        barData.barWidth = 1.0f * TimeUnit.SECONDS.toMillis(5)
        barData.setDrawValues(false)
        screenStateChart.setTouchEnabled(false)
        screenStateChart.data = barData
        adjustXAxisForTime(screenStateChart)
        screenStateChart.axisLeft.axisMaximum = 1F
        screenStateChart.axisLeft.axisMinimum = 0F
        screenStateChart.axisLeft.setLabelCount(0, true)
        screenStateChart.axisLeft.setDrawGridLines(false)
        screenStateChart.axisLeft.setDrawLabels(false)
        screenStateChart.axisRight.setDrawLabels(false)
        screenStateChart.legend.setEnabled(true)
        screenStateChart.legend.setEntries(listOf(LegendEntry("Bildschirmstatus", Legend.LegendForm
            .LINE,
                                                              Float.NaN,
                                                              Float.NaN, null, 0)))
        screenStateChart.description.isEnabled = false
    }

    fun getScreenStateValue(i: Int): String? = if (i > 0) "an" else "aus"

    private fun initLightView() {
        val entries = mutableListOf<Entry>()

        val firstTimeStamp = record.ambientLight.first().first
        record.ambientLight.forEach { value ->
            val time = value.first - firstTimeStamp
            entries.add(Entry(time.toFloat(),
                              value.second.toFloat()))
        }
        val data = createHeightLineDataSet(entries, "Umgebungslicht")
        data.color = getResources().getColor(R.color.black)
        val lineData = LineData(data)
        lineData.setDrawValues(false)
        lightchart.data = lineData
        lightchart.invalidate()
        lightchart.setTouchEnabled(false)
        adjustXAxisForTime(lightchart)
        lightchart.axisLeft.setDrawLabels(true)
        lightchart.axisLeft.setDrawGridLines(false)
        lightchart.axisRight.setDrawLabels(false)
        lightchart.legend.setEnabled(true)
        lightchart.legend.setEntries(listOf(LegendEntry("Umgebungslicht", Legend.LegendForm.LINE,
                                                        Float.NaN,
                                                        Float.NaN, null, 0)))
        lightchart.description.isEnabled = false
    }

    private fun initHeightGraph() {
        val entries = mutableListOf<Entry>()
        if (record.pressure.isNotEmpty()) {
            val initial = record.pressure.first().second.toFloat()
            val firstTimeStamp = record.pressure.first().first
            record.pressure.forEach { value ->
                val time = value.first - firstTimeStamp
                entries.add(Entry(time.toFloat(),
                                  preassureDifferentialToHeightDifferential(
                                      initial - value.second.toFloat())))
            }
            val data = createHeightLineDataSet(entries, "ungefährer Höhenunterschied in meter")
            data.color = getResources().getColor(R.color.black)
            val lineData = LineData(data)
            lineData.setDrawValues(false)
            hightchart.data = lineData
            hightchart.invalidate()
            hightchart.setTouchEnabled(false)
            adjustXAxisForTime(hightchart)
            hightchart.axisLeft.setDrawLabels(true)
            hightchart.axisLeft.setDrawGridLines(false)
            hightchart.axisRight.setDrawLabels(false)
            hightchart.legend.setEnabled(true)
            hightchart.legend.setEntries(listOf(LegendEntry("dB", Legend.LegendForm.LINE, Float.NaN,
                                                            Float.NaN, null, 0)))
            hightchart.description.isEnabled = false

            Log.d("foo", "bar")
        }
    }

    private fun adjustXAxisForTime(chart: Chart<out ChartData<out IDataSet<out Entry>>>) {
        chart.xAxis.setDrawLabels(true)
        chart.xAxis.valueFormatter = minuteValueFormatter
        chart.xAxis.granularity = TimeUnit.SECONDS.toNanos(30).toFloat()
        chart.xAxis.labelCount = 4
    }

    private val minuteValueFormatter = object : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase?): String =
            LocalTime.ofNanoOfDay(value.toLong())
                .format(DateTimeFormatter.ofPattern("mm:ss"))
    }

    private fun initActivityView() {
        val entries = Array<MutableList<BarEntry>>(10, { _ -> mutableListOf() })
        record.activities.forEachIndexed { i, value ->
            val activity = mapActivities(value)
            val confidence = getConfidence(value)
            entries[activity.roundToInt()].add(BarEntry(getTimeForRecord(i), confidence))
        }
        val barDataSet = Array(10, { i -> BarDataSet(entries[i], getActivityLabel(i)) })
        barDataSet[0].color = resources.getColor(R.color.red)
        barDataSet[1].color = resources.getColor(R.color.lightblue)
        barDataSet[2].color = resources.getColor(R.color.lightgreen)
        barDataSet[3].color = resources.getColor(R.color.lightorange)
        barDataSet[4].color = resources.getColor(R.color.green)
        barDataSet[5].color = resources.getColor(R.color.orange)
        barDataSet[6].color = resources.getColor(R.color.violet)
        barDataSet[7].color = resources.getColor(R.color.purple)
        barDataSet[8].color = resources.getColor(R.color.pink)
        barDataSet[9].color = resources.getColor(R.color.middleblue)
        val barData = BarData()
        for (i in 0 until barDataSet.size) {
            barData.addDataSet(barDataSet[i])
        }
        barData.barWidth = 1.0f * TimeUnit.SECONDS.toMillis(5)
        barData.setDrawValues(false)
        activity_chart.setTouchEnabled(false)
        activity_chart.data = barData
        adjustXAxisForTime(activity_chart)
        activity_chart.axisLeft.axisMaximum = 100F
        activity_chart.axisLeft.axisMinimum = 0F
        activity_chart.axisLeft.setLabelCount(6, true)
        activity_chart.axisLeft.setDrawGridLines(false)
        activity_chart.axisLeft.setDrawLabels(true)
        activity_chart.axisLeft.valueFormatter = object : IAxisValueFormatter {
            override fun getFormattedValue(value: Float,
                                           axis: AxisBase?): String = value.roundToInt().toString() + "%"
        }
        activity_chart.axisRight.setDrawLabels(false)
        activity_chart.legend.setEnabled(true)
        activity_chart.legend.setEntries(listOf(LegendEntry("Aktivität", Legend.LegendForm.LINE,
                                                            Float.NaN,
                                                            Float.NaN, null, 0)))
        activity_chart.description.isEnabled = false
    }

    private fun getConfidence(value: String): Float {
        val rest = value.substringAfter("confidence=")
        return rest.substring(0, rest.length - 1).toFloat()
    }

    private fun getActivityLabel(i: Int): String =
        when (i) {
            0    -> "In Fahrzeug"
            1    -> "Fahrrad"
            2    -> "Zu Fuss"
            3    -> "Still"
            4    -> "Unbekannt"
            5    -> "Kippen"
            6    -> "Gehen"
            7    -> "Rennen"
            8    -> "Straßenfahrzeug"
            9    -> "Schienenfahrzeug"
            else -> "Unbekannt"
        }


    private fun mapActivities(value: String): Float {
        if (value.contains("IN_VEHICLE")) return 0f
        if (value.contains("ON_BICYCLE")) return 1f
        if (value.contains("ON_FOOT")) return 2f
        if (value.contains("STILL")) return 3f
        if (value.contains("UNKNOWN")) return 4f
        if (value.contains("TILTING")) return 5f
        if (value.contains("WALKING")) return 6f
        if (value.contains("RUNNING")) return 7f
        if (value.contains("IN_ROAD_VEHICLE")) return 8f
        if (value.contains("IN_RAIL_VEHICLE")) return 9f
        else return 4f //unknown
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
        val startDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(record.beginTime), ZoneId
            .systemDefault())
        val endDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(record.endTime), ZoneId
            .systemDefault())
        timeFrom.setText(startDate.format(DateTimeFormatter.ofPattern("HH:mm")))
        timeTo.setText(endDate.format(DateTimeFormatter.ofPattern("HH:mm")))
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
                    getTimeForRecord(i),
                    toDecibel(value))
                       )
        }
        val soundData = LineDataSet(entries, "Mittlere Lautstärke in dB")
        soundData.setDrawCircles(false)
        soundData.color = resources.getColor(R.color.background_material_dark)
        soundData.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(soundData)
        lineData.setDrawValues(false)
        soundchart.data = lineData
        soundchart.invalidate()
        soundchart.setTouchEnabled(false)
        adjustXAxisForTime(soundchart)
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

    private fun getTimeForRecord(i: Int) = (record.timestamps.get(i) - record.beginTime).toFloat()

    private fun createHeightLineDataSet(entries_x: MutableList<Entry>, label: String):
        LineDataSet {
        val accData_x = LineDataSet(entries_x, label)
        accData_x.setDrawCircles(false)
        accData_x.setColor(R.color.black)
        accData_x.mode = LineDataSet.Mode.CUBIC_BEZIER
        return accData_x
    }

    // +1 to prevent log(0)
    private fun toDecibel(value: Double): Float = 20 * Math.log10((value) / 32767.0).toFloat()

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
