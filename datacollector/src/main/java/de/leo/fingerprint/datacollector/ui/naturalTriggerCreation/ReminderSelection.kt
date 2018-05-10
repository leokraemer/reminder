package de.leo.fingerprint.datacollector.ui.naturalTriggerCreation

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.fingerprint.datacollector.R
import kotlinx.android.synthetic.main.fragment_reminder_selection.*
import kotlinx.android.synthetic.main.fragment_situation.*

/**
 * Created by Leo on 06.03.2018.
 */
class ReminderSelection : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater!!.inflate(
            R.layout.fragment_reminder_selection, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateView()
    }
    override fun updateView() {
        situation_text?.setText(model?.situation ?: "")
    }
}