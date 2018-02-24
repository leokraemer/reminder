package de.leo.fingerprint.datacollector

import android.support.test.runner.AndroidJUnit4
import de.leo.fingerprint.datacollector.jitai.ActivityTrigger
import de.leo.fingerprint.datacollector.models.SensorDataSet
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.DetectedActivity.IN_VEHICLE
import com.google.android.gms.location.DetectedActivity.ON_FOOT
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ActivityTriggerTest {

    val inVehicle = DetectedActivity(IN_VEHICLE, 100)
    val onFoot = DetectedActivity(ON_FOOT, 100)

    @Test
    fun testTimeTrigger() {
        val trigger = ActivityTrigger(DetectedActivity(IN_VEHICLE, 100))
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        sensorData.activity = inVehicle
        Assert.assertTrue(trigger.check(sensorData))
        sensorData.activity = onFoot
        Assert.assertFalse(trigger.check(sensorData))
    }
}