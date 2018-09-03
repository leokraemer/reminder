package de.leo.smartTrigger.datacollector.jitai

import android.content.Context
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 11.01.2018.
 */
class ScreenStateTrigger(screenState: String, val interval: Long) :
        Trigger {
    private val isScreenOn: Boolean

    init {
        when (screenState) {
            SCREEN_OFF -> isScreenOn = false
            SCREEN_ON -> isScreenOn = true
            else -> isScreenOn = false
        }
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (sensorData.screenState != isScreenOn)
            return false

        if (interval > 0) {
            val db = JitaiDatabase.getInstance(context)
            val states = db.getScreenState(sensorData.time - interval, sensorData.time)
            if (states.size == 0)
                return sensorData.screenState == isScreenOn
            if (isScreenOn) {
                //use any to find only first
                return !states.any { pair -> pair.second == false }
            }
            return !states.any { pair -> pair.second == true }
        } else
            return sensorData.screenState == isScreenOn
    }

    override fun reset() {
        //noop
    }

    companion object {
        const val SCREEN_ON = "on"
        const val SCREEN_OFF = "off"
    }
}