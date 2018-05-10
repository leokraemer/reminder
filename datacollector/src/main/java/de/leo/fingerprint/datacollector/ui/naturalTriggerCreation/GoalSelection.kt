package de.leo.fingerprint.datacollector.ui.naturalTriggerCreation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.fingerprint.datacollector.R
import kotlinx.android.synthetic.main.fragment_goal.*
import kotlinx.android.synthetic.main.fragment_situation.*

/**
 * Created by Leo on 06.03.2018.
 */
class GoalSelection : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = inflater!!.inflate(
            R.layout.fragment_goal, container, false) as ViewGroup

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        goal_edittext!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                model?.goal = s.toString()
            }

            override fun afterTextChanged(s: Editable) {}

        })
        reminder_edittext!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                model?.message = s.toString()
            }

            override fun afterTextChanged(s: Editable) {}

        })
        updateView()
    }

    override fun updateView() {
        if (model?.goal != goal_edittext?.text.toString())
            goal_edittext?.setText(model?.goal ?: "")
        if (model?.message != reminder_edittext?.text.toString())
            reminder_edittext?.setText(model?.message ?: "")
    }
}