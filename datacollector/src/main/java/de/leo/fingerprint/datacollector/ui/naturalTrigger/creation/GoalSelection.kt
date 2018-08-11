package de.leo.fingerprint.datacollector.ui.naturalTrigger.creation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import de.leo.fingerprint.datacollector.R
import kotlinx.android.synthetic.main.activity_natural_trigger_tabs.*
import kotlinx.android.synthetic.main.fragment_goal.*

/**
 * Created by Leo on 06.03.2018.
 */
class GoalSelection : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_goal, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateView()
        goal_edittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                model?.goal = s.toString()
            }

            override fun afterTextChanged(s: Editable) {}
        })
        message_edittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                model?.message = s.toString()
            }

            override fun afterTextChanged(s: Editable) {}

        })

        goal_edittext.setOnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                message_edittext.requestFocus()
                handled = true
            }
            handled
        }

        message_edittext.setOnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                activity?.next_page?.performClick()
                handled = true
            }
            handled
        }
    }

    override fun updateView() {
        if (model?.goal != goal_edittext?.text.toString())
            goal_edittext?.setText(model?.goal ?: "")
        if (model?.message != message_edittext?.text.toString())
            message_edittext?.setText(model?.message ?: "")
    }
}