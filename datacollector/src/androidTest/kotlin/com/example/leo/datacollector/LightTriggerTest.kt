package com.example.leo.datacollector

import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.database.TABLE_REALTIME_LIGHT
import com.example.leo.datacollector.jitai.BrighterThanTrigger
import com.example.leo.datacollector.jitai.DimmerThanTrigger
import com.example.leo.datacollector.models.SensorDataSet
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LightTriggerTest {

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
        val data = mutableListOf<Pair<Long, Float>>()
        //create and enter 5 minutes of step data
        for (i in 0..fiveMinInMillis step 5000) {
            data.add(Pair<Long, Float>(i, getBrightness(i)))
        }
        db.enterSingleDimensionDataBatch(0, TABLE_REALTIME_LIGHT, data)
    }

    private fun getBrightness(i: Long): Float {
        if (i <= TimeUnit.MINUTES.toMillis(2)) {
            return 500f
        }
        return 1000f
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun LightTriggerTest() {
        val lightTrigger = BrighterThanTrigger(activityRule.activity, 600.0, TimeUnit.SECONDS
                .toMillis(20))
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(2), "lightTest")
        Assert.assertFalse(lightTrigger.check(sensorDataSet))
        sensorDataSet = SensorDataSet(TimeUnit.SECONDS.toMillis(130), "lightTest")
        Assert.assertTrue(lightTrigger.check(sensorDataSet))
    }


    @Test
    fun DimmerLightTriggerTest() {
        val lightTrigger = DimmerThanTrigger(activityRule.activity, 600.0, TimeUnit.SECONDS
                .toMillis(20))
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(2), "lightTest")
        Assert.assertTrue(lightTrigger.check(sensorDataSet))
        sensorDataSet = SensorDataSet(TimeUnit.SECONDS.toMillis(130), "lightTest")
        Assert.assertFalse(lightTrigger.check(sensorDataSet))
    }
}