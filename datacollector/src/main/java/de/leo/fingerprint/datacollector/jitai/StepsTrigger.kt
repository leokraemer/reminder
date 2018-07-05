package de.leo.fingerprint.datacollector.jitai

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet

/**
 * Created by Leo on 11.01.2018.
 */
class MinStepsTrigger(var minSteps: Double, val interval: Long) : Trigger {

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        val db = JitaiDatabase.getInstance(context)
        val steps = db.getStepData(sensorData.time - interval)
        if (steps.size < 1)
            return sensorData.totalStepsToday!! >= minSteps
        val stepsInInterval: Double = sensorData.totalStepsToday!! - steps.first().second
        return stepsInInterval >= minSteps
    }

    override fun reset() {
        //noop
    }
}

class MaxStepsTrigger(var maxSteps: Double, val interval: Long) : Trigger {
    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        val db = JitaiDatabase.getInstance(context)
        val steps = db.getStepData(sensorData.time - interval)
        if (steps.size < 1)
            return sensorData.totalStepsToday!! <= maxSteps
        val stepsInInterval: Double = sensorData.totalStepsToday!! - steps.first().second
        return stepsInInterval <= maxSteps
    }

    override fun reset() {
        //noop
    }
}