package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getContext
import android.support.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.database.TABLE_REALTIME_LIGHT
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.BrighterThanTrigger
import de.leo.smartTrigger.datacollector.jitai.DimmerThanTrigger
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LightTriggerTest {

    lateinit var context : Context
    lateinit var sensorDataSet: SensorDataSet
    lateinit var db: JitaiDatabase
    val fiveMinInMillis = TimeUnit.MINUTES.toMillis(5)

    @Before
    fun setup() {
        getContext().deleteDatabase(JitaiDatabase.NAME)
        context = InstrumentationRegistry.getTargetContext()
        db = JitaiDatabase.getInstance(context)
        val data = mutableListOf<Pair<Long, Float>>()
        //create and enter 5 minutes of step data
        for (i in 0..fiveMinInMillis step 5000) {
            data.add(Pair<Long, Float>(i, getBrightness(i)))
        }
        db.enterSingleDimensionDataBatch(0,
                                         TABLE_REALTIME_LIGHT, data)
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
        val lightTrigger = BrighterThanTrigger(600.0, TimeUnit.SECONDS
                .toMillis(20))
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(2),
            "lightTest")
        Assert.assertFalse(lightTrigger.check(context, sensorDataSet))
        sensorDataSet = SensorDataSet(
            TimeUnit.SECONDS.toMillis(130),
            "lightTest")
        Assert.assertTrue(lightTrigger.check(context, sensorDataSet))
    }


    @Test
    fun DimmerLightTriggerTest() {
        val lightTrigger = DimmerThanTrigger(600.0, TimeUnit.SECONDS
                .toMillis(20))
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(2),
            "lightTest")
        Assert.assertTrue(lightTrigger.check(context, sensorDataSet))
        sensorDataSet = SensorDataSet(
            TimeUnit.SECONDS.toMillis(130),
            "lightTest")
        Assert.assertFalse(lightTrigger.check(context, sensorDataSet))
    }
}