package de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.smartTrigger.datacollector.R
import kotlinx.android.synthetic.main.fragment_activity_selection.*
import kotlinx.android.synthetic.main.minutepicker.*
import java.util.concurrent.TimeUnit

/**
 * Created by Leo on 06.03.2018.
 */
class ActivitySelection : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(
            R.layout.fragment_activity_selection_constraintlayout, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        walkButton.setOnClickListener { walk() }
        walkText.setOnClickListener { walk() }
        bikeButton.setOnClickListener { bike() }
        bikeText.setOnClickListener { bike() }
        carButton.setOnClickListener { car() }
        carText.setOnClickListener { car() }
        sitButton.setOnClickListener { sit() }
        sitText.setOnClickListener { sit() }
        moreminutes.setOnClickListener { showMinutePicker() }
        fiveminutes.setOnClickListener {
            model?.timeInActivity = if (fiveminutes.isChecked) TimeUnit.MINUTES.toMillis(5) else 0
        }
        fiveteenminutes.setOnClickListener {
            model?.timeInActivity = if (fiveteenminutes.isChecked) TimeUnit.MINUTES.toMillis(15) else 0
        }
        thirtyminutes.setOnClickListener {
            model?.timeInActivity = if (thirtyminutes.isChecked) TimeUnit.MINUTES.toMillis(30) else 0
        }
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
            model?.timeInActivity ?: 5L).toInt()
        d.setMinute.setOnClickListener {
            model!!.timeInActivity = TimeUnit.MINUTES.toMillis(d.minutePicker.value.toLong())
            d.dismiss()
        }
        d.cancelMinute.setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun walk() {
        if (walkButton.isChecked) {
            model?.addActivity(NaturalTriggerModel.WALK)
            model?.removeActivity(NaturalTriggerModel.SIT)
        } else
            model?.removeActivity(NaturalTriggerModel.WALK)
    }

    private fun bike() {
        if (bikeButton.isChecked) {
            model?.addActivity(NaturalTriggerModel.BIKE)
            model?.removeActivity(NaturalTriggerModel.SIT)
        } else
            model?.removeActivity(NaturalTriggerModel.BIKE)
    }

    private fun car() {
        if (carButton.isChecked) {
            model?.addActivity(NaturalTriggerModel.CAR)
            model?.removeActivity(NaturalTriggerModel.SIT)
        } else
            model?.removeActivity(NaturalTriggerModel.CAR)
    }

    /*private fun bus() {
        if (busButton.isChecked) {
            model?.addActivity(NaturalTriggerModel.BUS)
            model?.removeActivity(NaturalTriggerModel.SIT)
        } else
            model?.removeActivity(NaturalTriggerModel.BUS)
    }*/

    private fun sit() {
        if (sitButton.isChecked) {
            model?.addActivity(NaturalTriggerModel.SIT)
            model?.removeActivity(NaturalTriggerModel.WALK)
            model?.removeActivity(NaturalTriggerModel.BIKE)
            //model?.removeActivity(NaturalTriggerModel.BUS)
            model?.removeActivity(NaturalTriggerModel.CAR)
        } else
            model?.removeActivity(NaturalTriggerModel.SIT)
    }

    override fun updateView() {
        model?.let {
            val sit = it.checkActivity(NaturalTriggerModel.SIT)
            val walk = it.checkActivity(NaturalTriggerModel.WALK)
            val bike = it.checkActivity(NaturalTriggerModel.BIKE)
            val car = it.checkActivity(NaturalTriggerModel.CAR)
            situation_text?.setText(it.situation)
            walkButton?.isChecked = walk
            bikeButton?.isChecked = bike
            carButton?.isChecked = car
            sitButton?.isChecked = sit
            val activitySelected = sit || walk || bike || car
            fiveminutes?.isEnabled = activitySelected
            fiveteenminutes?.isEnabled = activitySelected
            thirtyminutes?.isEnabled = activitySelected
            moreminutes?.isEnabled = activitySelected
            fiveminutes?.isChecked = activitySelected
                && it.timeInActivity == TimeUnit.MINUTES.toMillis(5)
            fiveteenminutes?.isChecked = activitySelected
                && it.timeInActivity == TimeUnit.MINUTES.toMillis(15)
            thirtyminutes?.isChecked = activitySelected
                && it.timeInActivity == TimeUnit.MINUTES.toMillis(30)
            moreminutes?.isChecked = activitySelected
                && !(fiveminutes?.isChecked == true
                || fiveteenminutes?.isChecked == true
                || thirtyminutes?.isChecked == true)
        }
    }
}
