package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.ScreenStateTrigger
import de.leo.smartTrigger.datacollector.jitai.ScreenStateTrigger.Companion.SCREEN_OFF
import de.leo.smartTrigger.datacollector.jitai.ScreenStateTrigger.Companion.SCREEN_ON
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ScreenStateTriggerTest {

    lateinit var context : Context
    lateinit var sensorDataSet: SensorDataSet
    lateinit var db: JitaiDatabase
    val fiveMinInMillis = TimeUnit.MINUTES.toMillis(2)
    val fiveSeconds = TimeUnit.SECONDS.toMillis(5)

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        db = JitaiDatabase.getInstance(context)
        db.close()
        context.deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(context)
        val data = mutableListOf<SensorDataSet>()
        //create and enter 5 minutes of step data
        for (i in 0..fiveMinInMillis step fiveSeconds) {
            sensorDataSet = SensorDataSet(i,
                                                                                                 "teststeps")
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
        val screenStateTrigger = ScreenStateTrigger(SCREEN_ON, TimeUnit.MINUTES.toMillis(1))
        sensorDataSet.screenState = false
        Assert.assertFalse(screenStateTrigger.check(context, sensorDataSet))
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(1),
            "oneMinute")
        sensorDataSet.screenState = true
        Assert.assertTrue(screenStateTrigger.check(context, sensorDataSet))
    }

    @Test
    fun screenStateOFFTriggerTest() {
        val screenStateTrigger = ScreenStateTrigger(SCREEN_OFF,
                                                    TimeUnit.MINUTES.toMillis(1) - 1)
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(2),
            "twoMinute")
        sensorDataSet.screenState = false
        Assert.assertTrue(screenStateTrigger.check(context, sensorDataSet))
        sensorDataSet.screenState = true
        Assert.assertFalse(screenStateTrigger.check(context, sensorDataSet))
    }

    @Test
    fun screenStateNoStateInIntervalTriggerTest() {
        val screenStateTrigger = ScreenStateTrigger(SCREEN_ON, 1)
        //no more data
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(3),
            "twoMinute")
        sensorDataSet.screenState = true
        Assert.assertTrue(screenStateTrigger.check(context, sensorDataSet))
        sensorDataSet.screenState = false
        Assert.assertFalse(screenStateTrigger.check(context, sensorDataSet))
    }

    @Test
    fun screenStateZeroIntervalTriggerTest() {
        val screenStateTrigger = ScreenStateTrigger( SCREEN_ON, 0)
        //no more data
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(3),
            "twoMinute")
        sensorDataSet.screenState = true
        Assert.assertTrue(screenStateTrigger.check(context, sensorDataSet))
        sensorDataSet.screenState = false
        Assert.assertFalse(screenStateTrigger.check(context, sensorDataSet))
    }

    @Test
    fun screenStateZeroIntervalOFFTriggerTest() {
        val screenStateTrigger = ScreenStateTrigger( SCREEN_OFF, 0)
        //no more data
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(3),
            "twoMinute")
        sensorDataSet.screenState = false
        Assert.assertTrue(screenStateTrigger.check(context, sensorDataSet))
        sensorDataSet.screenState = true
        Assert.assertFalse(screenStateTrigger.check(context, sensorDataSet))
    }
}