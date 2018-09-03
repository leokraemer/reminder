package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.DetectedActivity.IN_VEHICLE
import com.google.android.gms.location.DetectedActivity.ON_FOOT
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.DatabaseBackedActivityTrigger
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit.SECONDS


@RunWith(AndroidJUnit4::class)
class DatabaseBackedActivityTriggerTest {

    val inVehicle = DetectedActivity(IN_VEHICLE, 100)
    val onFoot = DetectedActivity(ON_FOOT, 100)


    lateinit var context: Context
    lateinit var db: JitaiDatabase

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        context.deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val TWO_SECONDS = SECONDS.toMillis(2)

    private val FIVE_SECONDS = SECONDS.toMillis(5)

    private val SIX_SECONDS = SECONDS.toMillis(6)

    @Test
    fun testActivityTriggerUninterrupted() {
        val trigger = DatabaseBackedActivityTrigger(listOf(DetectedActivity(IN_VEHICLE, 100)),
                                                    FIVE_SECONDS)
        val sensorData = SensorDataSet(0, "test")
        val sensorData2 = SensorDataSet(0 + SIX_SECONDS, "test")
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
        val trigger = DatabaseBackedActivityTrigger(listOf(DetectedActivity(IN_VEHICLE, 100)),
                                                    FIVE_SECONDS, 1.0)
        val sensorData = SensorDataSet(0, "test")
        val sensorData2 = SensorDataSet(0 + SIX_SECONDS, "test")
        sensorData.activity = listOf(inVehicle)
        sensorData2.activity = listOf(inVehicle)
        db.insertSensorDataBatch(listOf(sensorData, sensorData2))
        //at time of sensorData it should assert false
        Assert.assertFalse(trigger.check(context, sensorData))
        //at time of sensorData2 it should assert true
        Assert.assertTrue(trigger.check(context, sensorData2))
        Assert.assertTrue(trigger.check(context, sensorData2))
        trigger.reset()
        Assert.assertFalse(trigger.check(context, sensorData2))
        Assert.assertFalse(trigger.check(context, sensorData2))
    }

    @Test
    fun testDatabaseBackedActivityTriggerInterrupted() {
        val trigger = DatabaseBackedActivityTrigger(listOf(DetectedActivity(IN_VEHICLE, 100)),
                                                    FIVE_SECONDS)
        val sensorData = SensorDataSet(0, "test")
        val interuptionSensorData = SensorDataSet(0 + TWO_SECONDS, "test")
        val sensorData2 = SensorDataSet(0 + SIX_SECONDS, "test")
        sensorData.activity = listOf(inVehicle)
        interuptionSensorData.activity = listOf(onFoot)
        sensorData2.activity = listOf(inVehicle)
        db.insertSensorDataBatch(listOf(sensorData, interuptionSensorData, sensorData2))
        Assert.assertFalse(trigger.check(context, sensorData2))
    }

    @Test
    fun testDatabaseBackedActivityTriggerReset() {
        val trigger = DatabaseBackedActivityTrigger(listOf(DetectedActivity(IN_VEHICLE, 100)),
                                                    FIVE_SECONDS)
        val sensorData = SensorDataSet(0, "test")
        val sensorData2 = SensorDataSet(0 + SIX_SECONDS, "test")
        sensorData.activity = listOf(inVehicle)
        sensorData2.activity = listOf(inVehicle)
        db.insertSensorDataBatch(listOf(sensorData, sensorData2))
        Assert.assertTrue(trigger.check(context, sensorData2))
        trigger.reset()
        Assert.assertFalse(trigger.check(context, sensorData2))
    }
}