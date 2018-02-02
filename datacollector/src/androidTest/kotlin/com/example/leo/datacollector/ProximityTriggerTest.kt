package com.example.leo.datacollector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.database.TABLE_REALTIME_PROXIMITY
import com.example.leo.datacollector.jitai.ProximityTrigger
import com.example.leo.datacollector.models.SensorDataSet
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ProximityTriggerTest {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<EntryActivity>(EntryActivity::class.java)
    var maxDistance : Float = 0f
    lateinit var sensorDataSet: SensorDataSet
    lateinit var db: JitaiDatabase
    val fiveMinInMillis = TimeUnit.MINUTES.toMillis(5)

    @Before
    fun setup() {
        getTargetContext().deleteDatabase(JitaiDatabase.NAME)
        val sm = activityRule.activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        maxDistance = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY).maximumRange
        db = JitaiDatabase.getInstance(activityRule.activity)
        val data = mutableListOf<Pair<Long, Float>>()
        //create and enter 5 minutes of step data
        for (i in 0..fiveMinInMillis step 5000) {
            data.add(Pair<Long, Float>(i, getProximity(i)))
        }
        db.enterSingleDimensionDataBatch(0, TABLE_REALTIME_PROXIMITY, data)
    }

    private fun getProximity(i: Long): Float {
        if (i <= TimeUnit.MINUTES.toMillis(2)) {
            return 0f
        }
        return maxDistance
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun proximityTest(){
        val proximityTrigger = ProximityTrigger(activityRule.activity, true)
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(2), "proximityTest")
        Assert.assertTrue(proximityTrigger.check(sensorDataSet))
        sensorDataSet = SensorDataSet(TimeUnit.MINUTES.toMillis(3), "proximityTest")
        Assert.assertFalse(proximityTrigger.check(sensorDataSet))
    }
}