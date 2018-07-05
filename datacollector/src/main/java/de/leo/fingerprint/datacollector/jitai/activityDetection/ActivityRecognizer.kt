package de.leo.fingerprint.datacollector.jitai.activityDetection

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.jitai.manage.Jitai


/**
 * Created by Leo on 21.12.2017.
 */
class ActivityRecognizer(val context: Context) {

    val jitaiList = mutableListOf<Jitai>()
    val db: JitaiDatabase

    init {
        db = JitaiDatabase.getInstance(context)
        jitaiList.addAll(db.getActiveMachineLearningJitai())
        jitaiList.addAll(db.getActiveNaturalTriggerJitai())
    }


    fun recognizeActivity(sensorValues: SensorDataSet) {
        jitaiList.forEach {
            it.check(sensorValues)
        }
    }
}
