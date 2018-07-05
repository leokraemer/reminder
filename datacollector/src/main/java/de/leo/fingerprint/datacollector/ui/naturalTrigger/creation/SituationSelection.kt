package de.leo.fingerprint.datacollector.ui.naturalTrigger.creation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.fingerprint.datacollector.R
import kotlinx.android.synthetic.main.fragment_situation.*

/**
 * Created by Leo on 06.03.2018.
 */
class SituationSelection : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = inflater!!.inflate(
            R.layout.fragment_situation, container, false) as ViewGroup

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        situation_edittext!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                model?.situation = s.toString()
            }

            override fun afterTextChanged(s: Editable) {}

        })
        updateView()
    }

    override fun updateView() {
        if (model?.situation != situation_edittext?.text.toString())
            situation_edittext?.setText(model?.situation ?: "")
    }
}