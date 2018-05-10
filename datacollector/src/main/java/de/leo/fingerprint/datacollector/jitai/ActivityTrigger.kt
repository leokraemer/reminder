package de.leo.fingerprint.datacollector.jitai;

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import com.google.android.gms.location.DetectedActivity

/**
 * Created by Leo on 16.11.2017.
 */

class ActivityTrigger(val activity: DetectedActivity) : Trigger {
    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        return activity.type == sensorData.activity.maxBy { it.confidence }!!.type
    }
}
