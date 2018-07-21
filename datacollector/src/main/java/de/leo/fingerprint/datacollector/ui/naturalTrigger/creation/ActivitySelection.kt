package de.leo.fingerprint.datacollector.ui.naturalTrigger.creation

import android.os.Bundle
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
        return inflater.inflate(
            R.layout.fragment_activity_selection, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        walkButton.setOnClickListener { walk() }
        walkText.setOnClickListener { walk() }
        bikeButton.setOnClickListener { bike() }
        bikeText.setOnClickListener { bike() }
        //busButton.setOnClickListener { bus() }
        //busText.setOnClickListener { bus() }
        carButton.setOnClickListener { car() }
        carText.setOnClickListener { car() }
        sitButton.setOnClickListener { sit() }
        sitText.setOnClickListener { sit() }
        updateView()
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
        situation_text?.setText(model?.situation)
        walkButton?.isChecked = model?.checkActivity(NaturalTriggerModel.WALK) == true
        bikeButton?.isChecked = model?.checkActivity(NaturalTriggerModel.BIKE) == true
        //busButton?.isChecked = model?.checkActivity(NaturalTriggerModel.BUS) == true
        carButton?.isChecked = model?.checkActivity(NaturalTriggerModel.CAR) == true
        sitButton?.isChecked = model?.checkActivity(NaturalTriggerModel.SIT) == true
    }
}