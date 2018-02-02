package com.example.leo.datacollector

import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.jitai.MaxStepsTrigger
import com.example.leo.datacollector.jitai.MinStepsTrigger
import com.example.leo.datacollector.models.SensorDataSet
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class StepsTriggerTest {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<EntryActivity>(EntryActivity::class.java)
    lateinit var sensorDataSet: SensorDataSet
    lateinit var db: JitaiDatabase
    val fiveMinInMillis = TimeUnit.MINUTES.toMillis(5)

    @Before
    fun setup() {
        getTargetContext().deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(activityRule.activity)
        val data = mutableListOf<SensorDataSet>()
        //create and enter 5 minutes of step data
        for (i in 0..fiveMinInMillis step 5000) {
            sensorDataSet = SensorDataSet(i, "teststeps")
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
        val minThanTrigger = MinStepsTrigger(activityRule.activity, 100.0, TimeUnit.MINUTES
                .toMillis(1))
        Assert.assertFalse(minThanTrigger.check(sensorDataSet))
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(2), "twoMinute")
        sensorDataSet.totalStepsToday = 200
        Assert.assertTrue(minThanTrigger.check(sensorDataSet))
        sensorDataSet.totalStepsToday = 199
        Assert.assertFalse(minThanTrigger.check(sensorDataSet))
        //no more step data within one minute
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(6), "twoMinute")
        sensorDataSet.totalStepsToday = 500
        Assert.assertTrue(minThanTrigger.check(sensorDataSet))
        sensorDataSet.totalStepsToday = 99
        Assert.assertFalse(minThanTrigger.check(sensorDataSet))
    }

    @Test
    fun maxStepTriggerTest() {
        val maxThanTrigger = MaxStepsTrigger(activityRule.activity, 100.0, TimeUnit.MINUTES
                .toMillis(1))
        Assert.assertTrue(maxThanTrigger.check(sensorDataSet))
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(2), "twoMinute")
        sensorDataSet.totalStepsToday = 201
        Assert.assertFalse(maxThanTrigger.check(sensorDataSet))
        //no more step data within one minute
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(6), "after")
        sensorDataSet.totalStepsToday = 500
        Assert.assertFalse(maxThanTrigger.check(sensorDataSet))
        sensorDataSet.totalStepsToday = 100
        Assert.assertTrue(maxThanTrigger.check(sensorDataSet))
    }
}