package de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.EVERYWHERE
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
        inside.setOnClickListener { spendTimeInside() }
        outside.setOnClickListener { spendTimeOutside() }
        insidetv.setOnClickListener { spendTimeInside() }
        outsidetv.setOnClickListener { spendTimeOutside() }
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
        model!!.wifi = model!!.wifi?.copy(enter = true,
                                          exit = false,
                                          dwellInside = false,
                                          dwellOutside = false)
        model!!.geofence = model!!.geofence?.copy(enter = true,
                                                  exit = false,
                                                  dwellInside = false,
                                                  dwellOutside = false)

    }

    private fun updateLoiteringDelay(millis: Long) {
        model!!.geofence = model!!.geofence?.copy(loiteringDelay = millis)
        model!!.wifi = model!!.wifi?.copy(loiteringDelay = millis)
    }


    private fun exit() {
        model!!.geofence = model!!.geofence?.copy(enter = false,
                                                  exit = true,
                                                  dwellInside = false,
                                                  dwellOutside = false)
        model!!.wifi = model!!.wifi?.copy(enter = false,
                                          exit = true,
                                          dwellInside = false,
                                          dwellOutside = false)

    }

    private fun spendTimeInside() {
        model!!.geofence = model!!.geofence?.copy(enter = false,
                                                  exit = false,
                                                  dwellInside = true,
                                                  dwellOutside = false)
        model!!.wifi = model!!.wifi?.copy(enter = false,
                                          exit = false,
                                          dwellInside = true,
                                          dwellOutside = false)
    }


    private fun spendTimeOutside() {
        model!!.geofence = model!!.geofence?.copy(enter = false,
                                                  exit = false,
                                                  dwellInside = false,
                                                  dwellOutside = true)
        model!!.wifi = model!!.wifi?.copy(enter = false,
                                          exit = false,
                                          dwellInside = false,
                                          dwellOutside = true)
    }


    override fun updateView() {
        val geofence = model?.wifi ?: model?.geofence
        enterButton?.isChecked = geofence?.enter ?: false
        exitButton?.isChecked = geofence?.exit ?: false
        enterButton?.isEnabled = geofence?.name != EVERYWHERE
        exitButton?.isEnabled = geofence?.name != EVERYWHERE
        inside?.isChecked = geofence?.dwellInside ?: false
        outside?.isChecked = geofence?.dwellOutside ?: false
        if (geofence?.dwellInside == true || geofence?.dwellOutside == true) {
            if (geofence.dwellInside == true)
                inside?.isChecked = true
            else
                outside?.isChecked = true
            fiveminutes?.isEnabled = true
            fiveteenminutes?.isEnabled = true
            thirtyminutes?.isEnabled = true
            moreminutes?.isEnabled = true
            fiveminutes?.isChecked = geofence.loiteringDelay ==
                TimeUnit.MINUTES.toMillis(5)
            fiveteenminutes?.isChecked = geofence.loiteringDelay ==
                TimeUnit.MINUTES.toMillis(15)
            thirtyminutes?.isChecked = geofence.loiteringDelay ==
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
            inside?.isChecked = false
            outside?.isChecked = false
            fiveminutes?.isChecked = false
            fiveteenminutes?.isChecked = false
            thirtyminutes?.isChecked = false
            moreminutes?.isChecked = false
        }
        situation_text?.setText(model?.situation)
    }
}