package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.DetectedActivity.IN_VEHICLE
import com.google.android.gms.location.DetectedActivity.ON_FOOT
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.ActivityTrigger
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class ActivityTriggerTest {

    val inVehicle = DetectedActivity(IN_VEHICLE, 100)
    val onFoot = DetectedActivity(ON_FOOT, 100)


    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
    }

    @Test
    fun testActivityTriggerUninterrupted() {
        val trigger = ActivityTrigger(listOf(DetectedActivity(IN_VEHICLE, 100)), 5000)
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        sensorData.activity = listOf(inVehicle)
        Assert.assertFalse(trigger.check(context, sensorData))
        val sensorData2 = SensorDataSet(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(6),
                                        "test")
        sensorData2.activity = listOf(inVehicle)
        Assert.assertTrue(trigger.check(context, sensorData2))
    }


    @Test
    fun testActivityTriggerRepeatEvent() {
        val trigger = ActivityTrigger(listOf(DetectedActivity(IN_VEHICLE, 100)), 5000)
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        sensorData.activity = listOf(inVehicle)
        Assert.assertFalse(trigger.check(context, sensorData))
        val sensorData2 = SensorDataSet(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(6),
                                        "test")
        sensorData2.activity = listOf(inVehicle)
        Assert.assertTrue(trigger.check(context, sensorData2))
        Assert.assertTrue(trigger.check(context, sensorData2))
        trigger.reset(sensorData2)
        Assert.assertFalse(trigger.check(context, sensorData2))
        Assert.assertFalse(trigger.check(context, sensorData2))
    }

    @Test
    fun testActivityTriggerInterrupted() {
        val trigger = ActivityTrigger(listOf(DetectedActivity(IN_VEHICLE, 100)), 5000)
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        sensorData.activity = listOf(inVehicle)
        Assert.assertFalse(trigger.check(context, sensorData))
        //interruption
        sensorData.activity = listOf(onFoot)
        Assert.assertFalse(trigger.check(context, sensorData))
        val sensorData2 = SensorDataSet(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(6),
                                        "test")
        sensorData2.activity = listOf(inVehicle)
        Assert.assertFalse(trigger.check(context, sensorData2))
    }

    @Test
    fun testActivityTriggerReset() {
        val trigger = ActivityTrigger(listOf(DetectedActivity(IN_VEHICLE, 100)), 5000)
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        sensorData.activity = listOf(inVehicle)
        Assert.assertFalse(trigger.check(context, sensorData))
        val sensorData2 = SensorDataSet(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(6),
                                        "test")
        sensorData2.activity = listOf(inVehicle)
        trigger.reset(sensorData2)
        Assert.assertFalse(trigger.check(context, sensorData2))
    }
}