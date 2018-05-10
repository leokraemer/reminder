package de.leo.fingerprint.datacollector.jitai.activityDetection

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import weka.core.Attribute


/**
 * Created by Leo on 21.12.2017.
 */
class ActivityRecognizer(val context: Context) {

    val jitaiList = mutableListOf<Jitai>()
    val db: JitaiDatabase

    init {
        db = JitaiDatabase.getInstance(context)
        jitaiList.addAll(db.getActiveJitai())
    }


    fun recognizeActivity(sensorValues: SensorDataSet) {
        jitaiList.forEach {
            it.check(sensorValues)
        }
    }
}
