package com.example.leo.datacollector.jitai

import android.app.Activity
import android.app.TimePickerDialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TimePicker
import com.example.leo.datacollector.R
import com.google.gson.Gson
import kotlinx.android.synthetic.main.jitai_add.*
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by Leo on 16.11.2017.
 */
class AddJitaiActivity : Activity() {
    companion object {
        val INTENT_KEY
                : String = "JITAI_NAME"
    }


    var jitai_name: String = ""
    var jitai_text: String = ""

    var jitai_time: Date = Date()

    private lateinit var dayCheckBoxes: Array<CheckBox>

    private lateinit var sharedPreferencesName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferencesName = intent.extras.getString(INTENT_KEY)
        setContentView(R.layout.jitai_add)
        name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                //noop
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //noop
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                jitai_name = s.toString()
            }

        }
        )
        text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                //noop
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //noop
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                jitai_text = s.toString()
            }

        }
        )
        fence_spinner.adapter = ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                arrayOf("Geofence1"))
        fence_spinner.setSelection(0)
        weather_spinner.adapter = ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                arrayOf("egal", "gut", "schlecht"))
        weather_spinner.setSelection(0)
        activity_spinner.adapter = ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                arrayOf("egal", "Im Fahrzeug", "Fahrradfahren", "Zu Fuss", "Gehen", "Rennen", "Tilten"))
        activity_spinner.setSelection(0)
        time.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val currentTime = Calendar.getInstance()
                var hour = currentTime.get(Calendar.HOUR_OF_DAY);
                var minute = currentTime.get(Calendar.MINUTE);
                val timePicker = TimePickerDialog(
                        this@AddJitaiActivity,
                        object : TimePickerDialog.OnTimeSetListener {
                            override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                                time.setText("${hourOfDay}:${minute}")
                                jitai_time = SimpleDateFormat("HH:MM").parse("${hourOfDay}:${minute}")
                            }
                        },
                        hour, minute, true)
                timePicker.setTitle("Select Time");
                timePicker.show();
            }
        })
        ok.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                createJitaiAndExit()
            }
        })
        time_switch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                if (isChecked) {
                    dayCheckBoxes.map { it.isEnabled = false }
                    time.isEnabled = false
                } else {
                    dayCheckBoxes.map { it.isEnabled = true }
                    time.isEnabled = true
                }
            }
        })
        dayCheckBoxes = arrayOf(mo, di, mi, don, fr, sa, so)
    }

    private fun createJitaiAndExit() {
        val jitai = Jitai(jitai_name, jitai_text, listOf<Trigger>())
        jitai.activity = activity_spinner.selectedItem as String
        jitai.weather = weather_spinner.selectedItem as String
        jitai.geofence = fence_spinner.selectedItem as String
        val prefsEditor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        val gson = Gson()
        val json = gson.toJson(jitai)
        prefsEditor.putString(sharedPreferencesName, json)
        prefsEditor.commit()
    }
}