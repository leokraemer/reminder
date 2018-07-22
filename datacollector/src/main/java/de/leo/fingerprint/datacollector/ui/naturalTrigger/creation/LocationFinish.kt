package de.leo.fingerprint.datacollector.ui.naturalTrigger.creation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.EVERYWHERE
import kotlinx.android.synthetic.main.fragment_location_finish.*
import kotlinx.android.synthetic.main.minutepicker.*
import java.util.concurrent.TimeUnit


/**
 * Created by Leo on 06.03.2018.
 */
class LocationFinish : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(
            R.layout.fragment_location_finish, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterButton.setOnClickListener { enter() }
        enterText.setOnClickListener { enter() }
        exitButton.setOnClickListener { exit() }
        exitText.setOnClickListener { exit() }
        spendTime.setOnClickListener { spendTime() }
        fiveminutes.setOnClickListener { updateLoiteringDelay(TimeUnit.MINUTES.toMillis(5)) }
        fiveteenminutes.setOnClickListener { updateLoiteringDelay(TimeUnit.MINUTES.toMillis(15)) }
        thirtyminutes.setOnClickListener { updateLoiteringDelay(TimeUnit.MINUTES.toMillis(30)) }
        moreminutes.setOnClickListener { showMinutePicker() }
        updateView()
    }

    fun showMinutePicker() {

        val d = Dialog(context)
        d.setContentView(R.layout.minutepicker)
        d.setTitle("Wählen sie eine Dauer:")
        d.setCancelable(true)
        d.setCanceledOnTouchOutside(true)
        d.setOnCancelListener { d.dismiss() }
        d.minutePicker.minValue = 0
        d.minutePicker.maxValue = 300
        d.minutePicker.wrapSelectorWheel = false
        d.minutePicker.value = TimeUnit.MILLISECONDS.toMinutes(
            model?.geofence?.loiteringDelay?.toLong() ?: 5L).toInt()
        d.setMinute.setOnClickListener {
            updateLoiteringDelay(TimeUnit.MINUTES.toMillis(d.minutePicker.value.toLong()))
            d.dismiss()
        }
        d.cancelMinute.setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun enter() {
        if (model!!.geofence != null) {
            model!!.geofence = model!!.geofence!!.copy(enter = true, exit = false, dwell = false)
        }
    }

    private fun updateLoiteringDelay(millis: Long) {
        if (model!!.geofence != null) {
            model!!.geofence = model!!.geofence!!.copy(enter = false, exit = false, dwell = true,
                                                       loiteringDelay = millis.toInt())
        }
    }

    private fun exit() {
        if (model!!.geofence != null) {
            model!!.geofence = model!!.geofence!!.copy(enter = false, exit = true, dwell = false)
        }
    }

    private fun spendTime() {
        if (model!!.geofence != null) {
            model!!.geofence = model!!.geofence!!.copy(enter = false, exit = false, dwell = true,
                                                       loiteringDelay = model!!.geofence!!
                                                           .loiteringDelay)
        }
    }

    override fun updateView() {
        enterButton?.isChecked = model?.geofence?.enter ?: false
        exitButton?.isChecked = model?.geofence?.exit ?: false
        enterButton?.isEnabled = model?.geofence?.name != EVERYWHERE
        exitButton?.isEnabled = model?.geofence?.name != EVERYWHERE
        spendTime?.isChecked = model?.geofence?.dwell ?: false
        if (model?.geofence?.dwell == true) {
            spendTime?.isChecked = true
            fiveminutes?.isEnabled = true
            fiveteenminutes?.isEnabled = true
            thirtyminutes?.isEnabled = true
            moreminutes?.isEnabled = true
            fiveminutes?.isChecked = model!!.geofence!!.loiteringDelay.toLong() ==
                TimeUnit.MINUTES.toMillis(5)
            fiveteenminutes?.isChecked = model!!.geofence!!.loiteringDelay.toLong() ==
                TimeUnit.MINUTES.toMillis(15)
            thirtyminutes?.isChecked = model!!.geofence!!.loiteringDelay.toLong() ==
                TimeUnit.MINUTES.toMillis(30)
            moreminutes?.isChecked =
                !fiveminutes.isChecked &&
                !fiveteenminutes.isChecked &&
                !thirtyminutes.isChecked
        } else {
            fiveminutes?.isEnabled = false
            fiveteenminutes?.isEnabled = false
            thirtyminutes?.isEnabled = false
            moreminutes?.isEnabled = false
            spendTime?.isChecked = false
            fiveminutes?.isChecked = false
            fiveteenminutes?.isChecked = false
            thirtyminutes?.isChecked = false
            moreminutes?.isChecked = false
        }
        situation_text?.setText(model?.situation)
    }
}