package de.leo.fingerprint.datacollector.ui.naturalTrigger.creation

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import de.leo.fingerprint.datacollector.R
import kotlinx.android.synthetic.main.activity_natural_trigger_tabs.*
import kotlinx.android.synthetic.main.fragment_situation.*
import android.content.Context.INPUT_METHOD_SERVICE
import android.view.inputmethod.InputMethodManager


/**
 * Created by Leo on 06.03.2018.
 */
class SituationSelection : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_situation, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        situation_edittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                model?.situation = s.toString()
            }

            override fun afterTextChanged(s: Editable) {}

        })

        situation_edittext.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                activity?.next_page?.performClick()
                val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as
                    InputMethodManager
                imm.hideSoftInputFromWindow(situation_edittext.windowToken, 0)
                handled = true

            }
            handled
        }
        updateView()
    }

    override fun updateView() {
        if (model?.situation != situation_edittext?.text.toString())
            situation_edittext?.setText(model?.situation ?: "")
    }
}