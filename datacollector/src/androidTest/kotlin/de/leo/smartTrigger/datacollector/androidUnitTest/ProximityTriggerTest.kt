package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.database.TABLE_REALTIME_PROXIMITY
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.ProximityTrigger
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ProximityTriggerTest {

    lateinit var context: Context
    var maxDistance: Float = 0f
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
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        maxDistance = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY).maximumRange
        val data = mutableListOf<Pair<Long, Float>>()
        //create and enter 5 minutes of step data
        for (i in 0..fiveMinInMillis step 5000) {
            data.add(Pair<Long, Float>(i, getProximity(i)))
        }
        db.enterSingleDimensionDataBatch(TABLE_REALTIME_PROXIMITY, data)
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
    fun proximityTest() {
        val proximityTrigger = ProximityTrigger(true)
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(2),
            "proximityTest")
        Assert.assertTrue(proximityTrigger.check(context, sensorDataSet))
        sensorDataSet = SensorDataSet(
            TimeUnit.MINUTES.toMillis(3),
            "proximityTest")
        Assert.assertFalse(proximityTrigger.check(context, sensorDataSet))
    }
}