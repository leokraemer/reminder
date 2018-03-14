package de.leo.fingerprint.datacollector.naturalTriggerCreation

import android.support.v4.app.Fragment

/**
 * Created by Leo on 07.03.2018.
 */
open abstract class NaturalTriggerFragment : Fragment() {
    var model: NaturalTrigger? = null
        set(value){
            if(field != value){
                field = value
                updateView()
            }
        }

    abstract fun updateView()
}