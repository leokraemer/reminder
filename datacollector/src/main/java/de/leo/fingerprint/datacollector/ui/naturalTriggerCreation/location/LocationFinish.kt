package de.leo.fingerprint.datacollector.ui.naturalTriggerCreation.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.ui.naturalTriggerCreation.NaturalTriggerFragment
import kotlinx.android.synthetic.main.fragment_location_finish.*

/**
 * Created by Leo on 06.03.2018.
 */
class LocationFinish : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater!!.inflate(
            R.layout.fragment_location_finish, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterButton.setOnClickListener { enter() }
        enterText.setOnClickListener { enter() }
        exitButton.setOnClickListener { exit() }
        exitText.setOnClickListener { exit() }
        updateView()
    }

    private fun enter() {
        if(model!!.geofence != null){
            model!!.geofence = model!!.geofence!!.copy(enter = true, exit = false)
        }
    }

    private fun exit() {
        if(model!!.geofence != null){
            model!!.geofence = model!!.geofence!!.copy(enter = false, exit = true)
        }
    }

    override fun updateView() {
        enterButton?.isChecked = model?.geofence?.enter ?: false
        exitButton?.isChecked = model?.geofence?.exit ?: false
        situation_text?.setText(model?.situation)
    }
}