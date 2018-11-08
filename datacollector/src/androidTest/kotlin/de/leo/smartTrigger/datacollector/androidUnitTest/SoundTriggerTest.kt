package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.InstrumentationRegistry.getContext
import androidx.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.LessLoudThanSoundTrigger
import de.leo.smartTrigger.datacollector.jitai.LouderThanSoundTrigger
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SoundTriggerTest {

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
        val minThanTrigger = LouderThanSoundTrigger(100.0, TimeUnit.MINUTES
                .toMillis(1))
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(2),
            "twoMinute")
        sensorDataSet.ambientSound = 0.0
        Assert.assertFalse(minThanTrigger.check(context, sensorDataSet))
        sensorDataSet.ambientSound = 100.0
        Assert.assertTrue(minThanTrigger.check(context, sensorDataSet))
        sensorDataSet = SensorDataSet(
            TimeUnit.SECONDS.toMillis(210),
            "twoMinute")
        sensorDataSet.ambientSound = 50.0
        Assert.assertTrue(minThanTrigger.check(context, sensorDataSet))
    }

    @Test
    fun maxSoundTriggerTest() {
        val minThanTrigger = LessLoudThanSoundTrigger(100.0, TimeUnit.MINUTES
                .toMillis(1))
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(2),
            "twoMinute")
        sensorDataSet.ambientSound = 0.0
        Assert.assertTrue(minThanTrigger.check(context, sensorDataSet))
        sensorDataSet.ambientSound = 100.0
        Assert.assertFalse(minThanTrigger.check(context, sensorDataSet))
        sensorDataSet = SensorDataSet(
            TimeUnit.SECONDS.toMillis(90),
            "twoMinute")
        sensorDataSet.ambientSound = 50.0
        Assert.assertTrue(minThanTrigger.check(context, sensorDataSet))
    }
}