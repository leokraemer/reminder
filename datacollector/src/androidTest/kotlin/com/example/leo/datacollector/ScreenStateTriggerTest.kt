package com.example.leo.datacollector

import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.jitai.ScreenStateTrigger
import com.example.leo.datacollector.jitai.ScreenStateTrigger.Companion.SCREEN_OFF
import com.example.leo.datacollector.jitai.ScreenStateTrigger.Companion.SCREEN_ON
import com.example.leo.datacollector.models.SensorDataSet
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ScreenStateTriggerTest {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<EntryActivity>(EntryActivity::class.java)
    lateinit var sensorDataSet: SensorDataSet
    lateinit var db: JitaiDatabase
    val fiveMinInMillis = TimeUnit.MINUTES.toMillis(2)
    val fiveSeconds = TimeUnit.SECONDS.toMillis(5)

    @Before
    fun setup() {
        getTargetContext().deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(activityRule.activity)
        val data = mutableListOf<SensorDataSet>()
        //create and enter 5 minutes of step data
        for (i in 0..fiveMinInMillis step fiveSeconds) {
            sensorDataSet = SensorDataSet(i, "teststeps")
            sensorDataSet.screenState = getScreenState(i)
            data.add(sensorDataSet)
        }
        db.insertSensorDataBatch(data)
    }

    private fun getScreenState(i: Long): Boolean {
        if (i <= TimeUnit.MINUTES.toMillis(1))
            return true
        return false
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun screenStateONTriggerTest() {
        val screenStateTrigger = ScreenStateTrigger(activityRule.activity, SCREEN_ON,
                                                    TimeUnit.MINUTES.toMillis(1))
        sensorDataSet.screenState = false
        Assert.assertFalse(screenStateTrigger.check(sensorDataSet))
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(1), "oneMinute")
        sensorDataSet.screenState = true
        Assert.assertTrue(screenStateTrigger.check(sensorDataSet))
    }

    @Test
    fun screenStateOFFTriggerTest() {
        val screenStateTrigger = ScreenStateTrigger(activityRule.activity, SCREEN_OFF,
                                                    TimeUnit.MINUTES.toMillis(1) - 1)
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(2), "twoMinute")
        sensorDataSet.screenState = false
        Assert.assertTrue(screenStateTrigger.check(sensorDataSet))
        sensorDataSet.screenState = true
        Assert.assertFalse(screenStateTrigger.check(sensorDataSet))
    }

    @Test
    fun screenStateNoStateInIntervalTriggerTest() {
        val screenStateTrigger = ScreenStateTrigger(activityRule.activity, SCREEN_ON, 1)
        //no more data
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(3), "twoMinute")
        sensorDataSet.screenState = true
        Assert.assertTrue(screenStateTrigger.check(sensorDataSet))
        sensorDataSet.screenState = false
        Assert.assertFalse(screenStateTrigger.check(sensorDataSet))
    }

    @Test
    fun screenStateZeroIntervalTriggerTest() {
        val screenStateTrigger = ScreenStateTrigger(activityRule.activity, SCREEN_ON, 0)
        //no more data
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(3), "twoMinute")
        sensorDataSet.screenState = true
        Assert.assertTrue(screenStateTrigger.check(sensorDataSet))
        sensorDataSet.screenState = false
        Assert.assertFalse(screenStateTrigger.check(sensorDataSet))
    }

    @Test
    fun screenStateZeroIntervalOFFTriggerTest() {
        val screenStateTrigger = ScreenStateTrigger(activityRule.activity, SCREEN_OFF, 0)
        //no more data
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(3), "twoMinute")
        sensorDataSet.screenState = false
        Assert.assertTrue(screenStateTrigger.check(sensorDataSet))
        sensorDataSet.screenState = true
        Assert.assertFalse(screenStateTrigger.check(sensorDataSet))
    }
}