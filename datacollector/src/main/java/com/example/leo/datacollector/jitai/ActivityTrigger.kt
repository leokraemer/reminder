package com.example.leo.datacollector.jitai;

import com.example.leo.datacollector.models.SensorDataSet
import com.google.android.gms.location.DetectedActivity

/**
 * Created by Leo on 16.11.2017.
 */

class ActivityTrigger(val activity: DetectedActivity) : Trigger {
    override fun check(sensorData: SensorDataSet): Boolean {
        return activity.type == sensorData.activity.type
    }
}
