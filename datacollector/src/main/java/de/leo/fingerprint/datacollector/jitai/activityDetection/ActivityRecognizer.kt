package de.leo.fingerprint.datacollector.jitai.activityDetection

import android.content.Context
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
import de.leo.fingerprint.datacollector.jitai.manage.NaturalTriggerJitai


/**
 * Created by Leo on 21.12.2017.
 */
class ActivityRecognizer(val context: Context) {

    val jitaiList = mutableListOf<Jitai>()
    val db: JitaiDatabase by lazy { JitaiDatabase.getInstance(context) }

    init {
        jitaiList.addAll(db.getActiveMachineLearningJitai())
        jitaiList.addAll(db.getAllActiveNaturalTriggerJitai())
    }

    fun updateNaturalTrigger(id: Int) {
        val iterator = jitaiList.iterator()
        while (iterator.hasNext()) {
            val jitai = iterator.next()
            if (jitai.id == id && jitai is NaturalTriggerJitai) {
                iterator.remove()
                break
            }
        }
        db.getActiveNaturalTriggerJitai(id)?.let { jitaiList.add(it) }
    }

    fun recognizeActivity(sensorValues: SensorDataSet) {
        jitaiList.forEach {
            it.check(sensorValues)
        }
    }
}
