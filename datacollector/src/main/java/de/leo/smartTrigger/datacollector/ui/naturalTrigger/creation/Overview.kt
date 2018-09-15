package de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.smartTrigger.datacollector.R
import kotlinx.android.synthetic.main.fragment_trigger_overview.*

/**
 * Created by Leo on 06.03.2018.
 */
class Overview : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_trigger_overview, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateView()
    }

    override fun updateView() {
        goal?.text = model?.goal
        message?.text = model?.message
        situation?.text = model?.situation
    }
}