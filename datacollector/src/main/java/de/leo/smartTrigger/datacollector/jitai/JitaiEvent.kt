package de.leo.smartTrigger.datacollector.jitai

/**
 * Created by Leo on 29.01.2018.
 */
data class JitaiEvent(val JITAI_ID: Int,
                      val timestamp: Long,
                      val username: String,
                      val eventType: String,
                      val sensorDatasetId: Long)