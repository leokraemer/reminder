package com.example.leo.datacollector

import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.jitai.*
import com.example.leo.datacollector.models.SensorDataSet
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SoundTriggerTest {

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
            sensorDataSet.ambientSound = getSound(i)
            data.add(sensorDataSet)
        }
        db.insertSensorDataBatch(data)
    }

    private fun getSound(i: Long): Double {
        if (i < TimeUnit.MINUTES.toMillis(3)) {
            return 90.0
        }
        return 400.0
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun minSoundTriggerTest() {
        val minThanTrigger = LouderThanSoundTrigger(activityRule.activity, 100.0, TimeUnit.MINUTES
                .toMillis(1))
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(2), "twoMinute")
        sensorDataSet.ambientSound = 0.0
        Assert.assertFalse(minThanTrigger.check(sensorDataSet))
        sensorDataSet.ambientSound = 100.0
        Assert.assertTrue(minThanTrigger.check(sensorDataSet))
        sensorDataSet = SensorDataSet(TimeUnit.SECONDS.toMillis(210), "twoMinute")
        sensorDataSet.ambientSound = 50.0
        Assert.assertTrue(minThanTrigger.check(sensorDataSet))
    }

    @Test
    fun maxSoundTriggerTest() {
        val minThanTrigger = LessLoudThanSoundTrigger(activityRule.activity, 100.0, TimeUnit.MINUTES
                .toMillis(1))
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(2), "twoMinute")
        sensorDataSet.ambientSound = 0.0
        Assert.assertTrue(minThanTrigger.check(sensorDataSet))
        sensorDataSet.ambientSound = 100.0
        Assert.assertFalse(minThanTrigger.check(sensorDataSet))
        sensorDataSet = SensorDataSet(TimeUnit.SECONDS.toMillis(90), "twoMinute")
        sensorDataSet.ambientSound = 50.0
        Assert.assertTrue(minThanTrigger.check(sensorDataSet))
    }
}