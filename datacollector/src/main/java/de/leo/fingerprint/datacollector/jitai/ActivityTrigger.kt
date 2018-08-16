package de.leo.fingerprint.datacollector.jitai;

import android.content.Context
import com.google.android.gms.location.DetectedActivity
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 16.11.2017.
 */

open class ActivityTrigger(open val activity: DetectedActivity, open val duration: Long) : Trigger {

    //in percent
    open val confidenceThreshold = 20
    open var lastTime: Long = Long.MAX_VALUE

    override fun reset() {
        lastTime = Long.MAX_VALUE
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (sensorData.activity.find { it.type == activity.type }?.let { it.confidence > confidenceThreshold } == true) {
            //first seen the activity
            if (lastTime == Long.MAX_VALUE)
                lastTime = sensorData.time
            //the activity has occurred long enough
            if (sensorData.time - lastTime > duration)
                return true
            return false
        } else {
            //reset
            lastTime = Long.MAX_VALUE
            return false
        }
    }

}
