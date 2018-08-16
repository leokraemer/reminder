package de.leo.fingerprint.datacollector.androidUnitTest

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import de.leo.fingerprint.datacollector.jitai.ActivityTrigger
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.DetectedActivity.IN_VEHICLE
import com.google.android.gms.location.DetectedActivity.ON_FOOT
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.jitai.DatabaseBackedActivityTrigger
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class DatabaseBackedActivityTriggerTest {

    val inVehicle = DetectedActivity(IN_VEHICLE, 100)
    val onFoot = DetectedActivity(ON_FOOT, 100)


    lateinit var context: Context
    lateinit var db: JitaiDatabase

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        db = JitaiDatabase.getInstance(context)
    }

    @Test
    fun testActivityTriggerUninterrupted() {
        val trigger = DatabaseBackedActivityTrigger(DetectedActivity(IN_VEHICLE, 100), 5000)
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        val sensorData2 = SensorDataSet(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(6),
                                        "test")
        sensorData.activity = listOf(inVehicle)
        sensorData2.activity = listOf(inVehicle)
        db.insertSensorDataBatch(listOf(sensorData, sensorData2))
        //at time of sensorData it should assert false
        Assert.assertFalse(trigger.check(context, sensorData))
        //at time of sensorData2 it should assert true
        Assert.assertTrue(trigger.check(context, sensorData2))
    }


    @Test
    fun testDatabaseBackedActivityTriggerRepeatEvent() {
        val trigger = DatabaseBackedActivityTrigger(DetectedActivity(IN_VEHICLE, 100), 5000)
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        sensorData.activity = listOf(inVehicle)
        Assert.assertFalse(trigger.check(context, sensorData))
        val sensorData2 = SensorDataSet(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(6),
                                        "test")
        sensorData2.activity = listOf(inVehicle)
        Assert.assertTrue(trigger.check(context, sensorData2))
        Assert.assertTrue(trigger.check(context, sensorData2))
        trigger.reset()
        Assert.assertFalse(trigger.check(context, sensorData2))
        Assert.assertFalse(trigger.check(context, sensorData2))
    }

    @Test
    fun testDatabaseBackedActivityTriggerInterrupted() {
        val trigger = DatabaseBackedActivityTrigger(DetectedActivity(IN_VEHICLE, 100), 5000)
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
    fun testDatabaseBackedActivityTriggerReset() {
        val trigger = DatabaseBackedActivityTrigger(DetectedActivity(IN_VEHICLE, 100), 5000)
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        sensorData.activity = listOf(inVehicle)
        Assert.assertFalse(trigger.check(context, sensorData))
        val sensorData2 = SensorDataSet(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(6),
                                        "test")
        sensorData2.activity = listOf(inVehicle)
        trigger.reset()
        Assert.assertFalse(trigger.check(context, sensorData2))
    }
}