package de.leo.fingerprint.datacollector.ui.naturalTriggerCreation

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.fingerprint.datacollector.R
import kotlinx.android.synthetic.main.fragment_activity_selection.*

/**
 * Created by Leo on 06.03.2018.
 */
class ActivitySelection : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater!!.inflate(
            R.layout.fragment_activity_selection, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        walkButton.setOnClickListener { walk() }
        walkText.setOnClickListener { walk() }
        bikeButton.setOnClickListener { bike() }
        bikeText.setOnClickListener { bike() }
        busButton.setOnClickListener { bus() }
        busText.setOnClickListener { bus() }
        carButton.setOnClickListener { car() }
        carText.setOnClickListener { car() }
        sitButton.setOnClickListener { sit() }
        sitText.setOnClickListener { sit() }
        updateView()
    }

    private fun walk() {
        if (walkButton.isChecked) {
            model?.addActivity(JITAI_ACTIVITY.WALK)
            model?.removeActivity(JITAI_ACTIVITY.SIT)
        } else
            model?.removeActivity(JITAI_ACTIVITY.WALK)
    }

    private fun bike() {
        if (bikeButton.isChecked) {
            model?.addActivity(JITAI_ACTIVITY.BIKE)
            model?.removeActivity(JITAI_ACTIVITY.SIT)
        } else
            model?.removeActivity(JITAI_ACTIVITY.BIKE)
    }

    private fun car() {
        if (carButton.isChecked) {
            model?.addActivity(JITAI_ACTIVITY.CAR)
            model?.removeActivity(JITAI_ACTIVITY.SIT)
        } else
            model?.removeActivity(JITAI_ACTIVITY.CAR)
    }

    private fun bus() {
        if (busButton.isChecked) {
            model?.addActivity(JITAI_ACTIVITY.BUS)
            model?.removeActivity(JITAI_ACTIVITY.SIT)
        } else
            model?.removeActivity(JITAI_ACTIVITY.BUS)
    }

    private fun sit() {
        if (sitButton.isChecked) {
            model?.addActivity(JITAI_ACTIVITY.SIT)
            model?.removeActivity(JITAI_ACTIVITY.WALK)
            model?.removeActivity(JITAI_ACTIVITY.BIKE)
            model?.removeActivity(JITAI_ACTIVITY.BUS)
            model?.removeActivity(JITAI_ACTIVITY.CAR)
        } else
            model?.removeActivity(JITAI_ACTIVITY.SIT)
    }

    override fun updateView() {
        situation_text?.setText(model?.situation)
        walkButton?.isChecked = model?.checkActivity(JITAI_ACTIVITY.WALK) == true
        bikeButton?.isChecked = model?.checkActivity(JITAI_ACTIVITY.BIKE) == true
        busButton?.isChecked = model?.checkActivity(JITAI_ACTIVITY.BUS) == true
        carButton?.isChecked = model?.checkActivity(JITAI_ACTIVITY.CAR) == true
        sitButton?.isChecked = model?.checkActivity(JITAI_ACTIVITY.SIT) == true
    }
}