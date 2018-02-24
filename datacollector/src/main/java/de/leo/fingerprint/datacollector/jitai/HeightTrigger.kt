package de.leo.fingerprint.datacollector.jitai

import android.content.Context
import de.leo.fingerprint.datacollector.database.JitaiDatabase
import de.leo.fingerprint.datacollector.database.TABLE_REALTIME_AIR
import de.leo.fingerprint.datacollector.models.SensorDataSet


// 1kPa = 7.5006157584566 mmHg
const val PaTommHg = 750.06157584566F
const val g = 9.81F
const val densityHg = 13.595F

fun preassureDifferentialToHeightDifferential(preassureDifferential: Float): Float =
        preassureDifferential * PaTommHg / (g * densityHg)

fun heightDifferentialToPressureDifferential(heightDifferential: Double): Double =
        heightDifferential * g * densityHg / PaTommHg

/**
 * Created by Leo on 11.01.2018.
 */
class PressureHigherThanTrigger(val threshold: Double, val interval: Long) :
        Trigger {
    @Transient
    var db: JitaiDatabase? = null

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (db == null)
            db = JitaiDatabase.getInstance(context)
        return (db!!.getSensorValues(sensorData.time - interval, sensorData.time,
        TABLE_REALTIME_AIR)
                .any { v -> v.second > threshold })
    }
}


class PressureLowerThanTrigger(val threshold: Double, val interval: Long) :
        Trigger {
    @Transient
    var db: JitaiDatabase? = null

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (db == null)
            db = JitaiDatabase.getInstance(context)
        return !db!!.getSensorValues(sensorData.time - interval, sensorData.time,
                                     TABLE_REALTIME_AIR)
                .any { v -> v.second > threshold }
    }
}

/*class heightDifferentialTrigger(val context: Context, val threshold: Float, val interval: Long) :
        Trigger {
    val db: JitaiDatabase

    init {
        db = JitaiDatabase.getInstance(context)
    }

    override fun check(sensorData: SensorDataSet): Boolean {
        val airPressureProfile = db.getSensorValues(sensorData.time - interval,
                                                    sensorData.time,
                                                    TABLE_REALTIME_AIR)
        if (airPressureProfile.size > 3) {
            val smoothed = movingAverage(airPressureProfile, 3)
            val high = Double.MIN_VALUE
            val low = Double.MAX_VALUE
            if(smoothed.any())
        }
    }

    /**
     * Simple moving average algorithm. Resulting list has windowSize - 1 values. Cutoff happens
     * in the beginning.
     */
    fun movingAverage(data: MutableCollection<Pair<Long, Double>>, windowSize: Int):
            MutableList<Pair<Long, Double>> {
        val smoothed = mutableListOf<Pair<Long, Double>>()
        if (data.size < windowSize)
            return smoothed
        var movingAverage = 0.0
        var window = ArrayDeque<Double>(windowSize)
        data.forEachIndexed { i, value ->
            movingAverage += (value.second / windowSize)
            window.addLast(value.second / windowSize)
            //window initialized
            if (i >= windowSize - 1) {
                smoothed.add(Pair(value.first, movingAverage))
                movingAverage -= window.removeFirst()
            }
        }
        return smoothed
    }
}*/
