package de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation

import androidx.fragment.app.Fragment

/**
 * Created by Leo on 07.03.2018.
 */
open abstract class NaturalTriggerFragment : Fragment() {
    var model: NaturalTriggerModel? = null
        set(value){
            if(field != value){
                field = value
                updateView()
            }
        }

    abstract fun updateView()
}