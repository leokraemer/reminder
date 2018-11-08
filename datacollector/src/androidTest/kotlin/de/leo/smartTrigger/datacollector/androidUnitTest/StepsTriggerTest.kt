package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.MaxStepsTrigger
import de.leo.smartTrigger.datacollector.jitai.MinStepsTrigger
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class StepsTriggerTest {

    lateinit var context : Context
    lateinit var sensorDataSet: SensorDataSet
    lateinit var db: JitaiDatabase
    val fiveMinInMillis = TimeUnit.MINUTES.toMillis(5)

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        db = JitaiDatabase.getInstance(context)
        db.close()
        context.deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(context)
        val data = mutableListOf<SensorDataSet>()
        //create and enter 5 minutes of step data
        for (i in 0..fiveMinInMillis step 5000) {
            sensorDataSet = SensorDataSet(i,
                                                                                                 "teststeps")
            sensorDataSet.totalStepsToday = getStepCount(i)
            data.add(sensorDataSet)
        }
        db.insertSensorDataBatch(data)
    }

    private fun getStepCount(i: Long): Long {
        if (i < TimeUnit.MINUTES.toMillis(1)) {
            return 0L
        }
        if (i < TimeUnit.MINUTES.toMillis(2)) {
            return 100L
        }
        if (i < TimeUnit.MINUTES.toMillis(3)) {
            return 200L
        }
        if (i < TimeUnit.MINUTES.toMillis(4)) {
            return 300L
        }
        return 400L
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun minStepTriggerTest() {
        val minThanTrigger = MinStepsTrigger(100.0, TimeUnit.MINUTES.toMillis(1))
        Assert.assertFalse(minThanTrigger.check(context, sensorDataSet))
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(2),
            "twoMinute")
        sensorDataSet.totalStepsToday = 200
        Assert.assertTrue(minThanTrigger.check(context, sensorDataSet))
        sensorDataSet.totalStepsToday = 199
        Assert.assertFalse(minThanTrigger.check(context, sensorDataSet))
        //no more step data within one minute
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(6),
            "twoMinute")
        sensorDataSet.totalStepsToday = 500
        Assert.assertTrue(minThanTrigger.check(context, sensorDataSet))
        sensorDataSet.totalStepsToday = 99
        Assert.assertFalse(minThanTrigger.check(context, sensorDataSet))
    }

    @Test
    fun maxStepTriggerTest() {
        val maxThanTrigger = MaxStepsTrigger(100.0, TimeUnit.MINUTES.toMillis(1))
        Assert.assertTrue(maxThanTrigger.check(context, sensorDataSet))
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(2),
            "twoMinute")
        sensorDataSet.totalStepsToday = 201
        Assert.assertFalse(maxThanTrigger.check(context, sensorDataSet))
        //no more step data within one minute
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(6),
            "after")
        sensorDataSet.totalStepsToday = 500
        Assert.assertFalse(maxThanTrigger.check(context, sensorDataSet))
        sensorDataSet.totalStepsToday = 100
        Assert.assertTrue(maxThanTrigger.check(context, sensorDataSet))
    }
}