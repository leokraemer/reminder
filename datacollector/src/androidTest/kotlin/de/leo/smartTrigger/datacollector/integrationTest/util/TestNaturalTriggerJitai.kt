package de.leo.smartTrigger.datacollector.integrationTest.util

import android.content.Context
import de.leo.smartTrigger.datacollector.jitai.manage.NaturalTriggerJitai
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel

class TestNaturalTriggerJitai(id: Int, context: Context, model: NaturalTriggerModel) :
    NaturalTriggerJitai(id, context, model) {
    override fun postNotification(id: Int,
                                  timestamp: Long,
                                  goal: String,
                                  message: String,
                                  sensorDataId: Long) {
        //noop
    }
}