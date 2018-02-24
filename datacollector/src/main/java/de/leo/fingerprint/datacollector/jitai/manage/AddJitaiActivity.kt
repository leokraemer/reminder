package de.leo.fingerprint.datacollector.jitai.manage

import android.app.Activity


import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import de.leo.fingerprint.datacollector.GeofencesWithPlayServices.GeofenceMapActivity
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.database.JitaiDatabase
import de.leo.fingerprint.datacollector.jitai.Location.GeofenceTrigger
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import de.leo.fingerprint.datacollector.jitai.TimeTrigger
import de.leo.fingerprint.datacollector.jitai.WeatherTrigger
import de.leo.fingerprint.datacollector.models.Weather
import kotlinx.android.synthetic.main.jitai_add_flow_activity.*
import org.jetbrains.anko.toast
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalTime
import java.util.*

/**
 * Created by Leo on 16.11.2017.
 */
class AddJitaiActivity : Activity() {

    private var jitai = Jitai(this)
    private var from_h: Int = 0
    private var from_m: Int = 0
    private var to_h: Int = 23
    private var to_m: Int = 59
    private lateinit var dayCheckBoxes: Array<CheckBox>
    private lateinit var db: JitaiDatabase
    private lateinit var geofences: List<MyGeofence>

    val REQUEST_GEOFENCE_ID = 4711
    private lateinit var imm: InputMethodManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.jitai_add_flow_activity)
        db = JitaiDatabase.getInstance(this)
        message_container.visibility = View.GONE
        geofence_container.visibility = View.GONE
        time_container.visibility = View.GONE
        day_container.visibility = View.GONE
        weather_container.visibility = View.GONE

        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        goal_edittext.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                jitai.goal = s.toString()
            }
        })
        goal_edittext.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (!v?.text.isNullOrBlank()) {
                        jitai.goal = v?.text.toString()
                        message_container.visibility = View.VISIBLE
                        message_edittext.requestFocus()
                    }else {
                        toast("Ziel darf nicht leer sein")
                    }
                    return true
                }
                return false
            }
        })

        message_edittext.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (!v?.text.isNullOrBlank()) {
                        jitai.message = v?.text.toString()
                        geofence_container.visibility = View.VISIBLE
                        imm.hideSoftInputFromWindow(v?.windowToken, 0)
                    } else {
                        toast("Nachricht darf nicht leer sein")
                    }
                    return true
                }
                return false
            }
        })
        message_edittext.addTextChangedListener(
                object :
                        TextWatcher {
                    override fun afterTextChanged(s: Editable?) {}
                    override fun beforeTextChanged(s: CharSequence?,
                                                   start: Int,
                                                   count: Int,
                                                   after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?,
                                               start: Int,
                                               before: Int,
                                               count: Int) {
                        jitai.message = s.toString()
                    }
                })
        time_from.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val currentTime = Calendar.getInstance()
                var hour = currentTime.get(Calendar.HOUR_OF_DAY);
                var minute = currentTime.get(Calendar.MINUTE);
                val timePicker =
                        TimePickerDialog(
                                this@AddJitaiActivity,
                                object : TimePickerDialog.OnTimeSetListener {
                                    override fun onTimeSet(view: TimePicker?,
                                                           hourOfDay: Int,
                                                           minute: Int) {
                                        time_from.setText(String.format("%02d:%02d",
                                                                        hourOfDay,
                                                                        minute))
                                        from_h = hourOfDay
                                        from_m = minute
                                    }
                                },
                                hour,
                                minute,
                                true)
                timePicker.setTitle("Beginn des Erinnerungszitraums");
                timePicker.show();
            }
        })
        time_to.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val currentTime = Calendar.getInstance()
                var hour = currentTime.get(Calendar.HOUR_OF_DAY);
                var minute = currentTime.get(Calendar.MINUTE);
                val timePicker = TimePickerDialog(
                        this@AddJitaiActivity,
                        object : TimePickerDialog.OnTimeSetListener {
                            override fun onTimeSet(view: TimePicker?,
                                                   hourOfDay: Int,
                                                   minute: Int) {
                                time_to.setText(String.format("%02d:%02d",
                                                              hourOfDay,
                                                              minute))
                                to_h = hourOfDay
                                to_m = minute
                            }
                        },
                        hour, minute, true)
                timePicker.setTitle("Ende des Erinnerungszeitraums");
                timePicker.show();
            }
        })
        dayCheckBoxes = arrayOf(mo2, di2, mi2, don2, fr2, sa2, so2)

        geofences = initGeofenceSpinner()

        weather_spinner.adapter =
                ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                                     arrayOf("Jedes Wetter", "Nur schönes Wetter",
                                             "Nur schlechtes Wetter"))
        ok.setOnClickListener { _ ->
            if (message_container.visibility == View.GONE) {
                if (jitai.goal.isNotBlank()) {
                    message_container.visibility = View.VISIBLE
                    message_container.requestFocus()
                    goal_edittext.clearFocus()
                } else {
                    toast("Ziel darf nicht leer sein")
                }
            } else if (geofence_container.visibility == View.GONE) {
                if (jitai.message.isNotBlank()) {
                    geofence_container.visibility = View.VISIBLE
                    geofence_container.requestFocus()
                    imm.hideSoftInputFromWindow(geofence_container?.windowToken, 0)
                } else {
                    toast("Nachricht darf nicht leer sein")
                }
            } else if (time_container.visibility == View.GONE) {
                time_container.visibility = View.VISIBLE
            } else if (day_container.visibility == View.GONE) {
                day_container.visibility = View.VISIBLE
            } else if (weather_container.visibility == View.GONE) {
                weather_container.visibility = View.VISIBLE
            } else {
                if (sanitiseTime() && sanitiseJitai() && sanitiseGeofence()) {
                    var index = geofences.indexOfFirst({ value ->
                                                           value.name == geofence_spinner
                                                                   .getSelectedItem().toString()
                                                       })
                    jitai.geofenceTrigger = GeofenceTrigger(listOf(geofences[index]))
                    val from_time = LocalTime.of(from_h, from_m)
                    val to_time = LocalTime.of(to_h, to_m)
                    val days: MutableList<DayOfWeek> = mutableListOf()
                    dayCheckBoxes.forEachIndexed { i, box ->
                        if (box.isChecked) {
                            days.add(DayOfWeek.of(i + 1))
                        }
                    }
                    jitai.timeTrigger = TimeTrigger(from_time.rangeTo(to_time), days)

                    val weatherIdentifier = weather_spinner.selectedItemPosition + 1
                    val weather = Weather()
                    when (weatherIdentifier) {
                        ANY_WEATHER -> weather.currentCondition.weatherId = -1
                        GOOD_WEATHER -> weather.currentCondition.weatherId = 800
                        BAD_WEATHER -> weather.currentCondition.weatherId = 799
                    }
                    jitai.weatherTrigger = WeatherTrigger(weather)
                    jitai = db.enterJitai(jitai)
                    finish()
                }
            }
        }
    }

    private fun sanitiseGeofence(): Boolean {
        if (geofence_spinner.getSelectedItem() == newGeofenceText) {
            toast("Bitte wählen sie einen Ort")
        }
        return true
    }

    val newGeofenceText = Html.fromHtml("<i>Neuen Ort hinzufügen...</i>")

    private fun initGeofenceSpinner(): List<MyGeofence> {
        val geofences: List<MyGeofence> = db.getAllMyGeofences()
        val geofence_names = MutableList<CharSequence>(geofences.size,
                                                       { i -> geofences[i].name })

        geofence_names.add(newGeofenceText)
        geofence_spinner.adapter = ArrayAdapter<CharSequence>(this,
                                                              android.R.layout.simple_spinner_item,
                                                              geofence_names)
        geofence_spinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                    override fun onItemSelected(parent: AdapterView<*>?,
                                                view: View?,
                                                position: Int,
                                                id: Long) {
                        if (position == geofences.size) {
                            val intent = Intent(this@AddJitaiActivity,
                                                GeofenceMapActivity::class.java)
                            startActivityForResult(intent, REQUEST_GEOFENCE_ID)
                        } else {
                            jitai.geofenceTrigger = GeofenceTrigger(listOf(geofences[position]))
                        }
                    }
                }
        return geofences
    }

    private fun sanitiseJitai(): Boolean {
        if (jitai.goal.isBlank()) {
            toast("Ziel darf nicht leer sein")
            return false
        }
        if (jitai.message.isBlank()) {
            toast("Nachricht darf nicht leer sein")
            return false
        }
        if (jitai.geofenceTrigger == null) {
            toast("Ort muss gesetzt sein")
            return false
        }
        return true
    }

    private fun sanitiseTime(): Boolean {
        if (from_h > to_h || (from_h == to_h && from_m >= to_m)) {
            toast("Startzeitpunkt muss vor Ende liegen")
            return false
        }
        return true
    }


    override fun onResume() {
        super.onResume()
        geofences = initGeofenceSpinner()
    }
}

const val ANY_WEATHER = 1
const val GOOD_WEATHER = 2
const val BAD_WEATHER = 3

