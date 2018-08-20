package de.leo.fingerprint.datacollector.integrationTest.util

import android.content.Context
import de.leo.fingerprint.datacollector.jitai.manage.NaturalTriggerJitai
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel

class TestNaturalTriggerJitai(context: Context, model: NaturalTriggerModel) :
    NaturalTriggerJitai(context, model) {
    override fun postNotification(id: Int,
                                  timestamp: Long,
                                  goal: String,
                                  message: String,
                                  sensorDataId: Long) {
        //noop
    }
}