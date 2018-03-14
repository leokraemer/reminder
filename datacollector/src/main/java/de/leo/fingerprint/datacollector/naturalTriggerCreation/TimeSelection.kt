package de.leo.fingerprint.datacollector.naturalTriggerCreation

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.fingerprint.datacollector.R
import kotlinx.android.synthetic.main.fragment_time_selection.*
import org.threeten.bp.LocalTime

/**
 * Created by Leo on 06.03.2018.
 */
class TimeSelection : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater!!.inflate(
            R.layout.fragment_time_selection, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        morningButton.setOnClickListener { morning() }
        morningText.setOnClickListener { morning() }
        middayButton.setOnClickListener { midday() }
        middayText.setOnClickListener { midday() }
        eveningButton.setOnClickListener { evening() }
        eveningText.setOnClickListener { evening() }
        nightButton.setOnClickListener { night() }
        nightText.setOnClickListener { night() }
        situation_text.setText(model?.situation)
        //23:59
        time_range_slider.setMax(86399)
        time_range_slider.setMin(0)
        time_range_slider.setOnThumbValueChangeListener { multiSlider, thumb, thumbIndex, value ->
            when (thumbIndex) {
                0    -> model!!.beginTime = LocalTime.ofSecondOfDay(value.toLong())
                1    -> model!!.endTime = LocalTime.ofSecondOfDay(value.toLong())
                else -> throw RuntimeException("you have a third thumb o.0")
            }
        }
        updateView()
    }

    private fun night() {
        model!!.beginTime = LocalTime.of(18, 0, 0)
        model!!.endTime = LocalTime.of(23, 59)
    }

    private fun evening() {
        model!!.beginTime = LocalTime.of(12, 0)
        model!!.endTime = LocalTime.of(18, 0)
    }

    private fun midday() {
        model!!.beginTime = LocalTime.of(10, 0)
        model!!.endTime = LocalTime.of(14, 0)
    }

    private fun morning() {
        model!!.beginTime = LocalTime.of(6, 0)
        model!!.endTime = LocalTime.of(12, 0)
    }

    override fun updateView() {
        if (model != null) {
            time_range_slider?.getThumb(0)?.setValue(model!!.beginTime.toSecondOfDay())
            time_range_slider?.getThumb(1)?.setValue(model!!.endTime.toSecondOfDay())
        }
        situation_text?.setText(model?.situation)
    }
}