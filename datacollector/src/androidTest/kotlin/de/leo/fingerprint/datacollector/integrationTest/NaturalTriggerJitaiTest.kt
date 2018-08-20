package de.leo.fingerprint.datacollector.integrationTest

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.integrationTest.util.TestNaturalTriggerJitai
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.LocalTime
import java.util.concurrent.TimeUnit.*

@RunWith(AndroidJUnit4::class)
class NaturalTriggerJitaiTest {

    lateinit var context: Context
    lateinit var db: JitaiDatabase

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        context.deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(context)
    }

    @Test
    fun testActivitySitTrigger() {
        setupDBforSit()
        val sitNaturalTriggerModel = testNaturalTriggerModel()
        sitNaturalTriggerModel.addActivity(NaturalTriggerModel.SIT)
        sitNaturalTriggerModel.timeInActivity = FIVE_MINUTES
        val activityJitai = TestNaturalTriggerJitai(context, sitNaturalTriggerModel)
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            val sensorDataSet = db.getSensorDataSets(i, i + FIVE_SECONDS).first()
            if (i == 0L || i % FIVE_MINUTES != 0L)
                Assert.assertFalse("Expected false at $i seconds",
                                   activityJitai.check(sensorDataSet))
            else
                Assert.assertTrue("Expected true at $i seconds",
                                  activityJitai.check(sensorDataSet))
            Log.d(TAG, "$i ${activityJitai.check(sensorDataSet)}")
        }
    }

    private val TAG = this.javaClass.simpleName
    private val FIVE_MINUTES = MINUTES.toMillis(5)
    private val FIVTY_MINUTES = MINUTES.toMillis(50)

    private val FIVE_SECONDS = SECONDS.toMillis(5)

    private val USER = "dummy"

    private val CONFIDENCE_CONFIDENT = 100
    private val CONFIDENCE_UNCONFIDENT = 5


    private val SIT_CONFIDENT = DetectedActivity(NaturalTriggerModel.SIT, CONFIDENCE_CONFIDENT)
    private val SIT_UNCONFIDENT = DetectedActivity(NaturalTriggerModel.SIT, CONFIDENCE_UNCONFIDENT)

    private fun setupDBforSit() {
        Log.d(TAG, "Start creating Sensordata")
        val data = mutableListOf<SensorDataSet>()
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            val sensorDataSet = SensorDataSet(time = i,
                                              userName = USER,
                                              activity = listOf(SIT_CONFIDENT))
            sensorDataSet.activity
            data.add(sensorDataSet)
        }
        Log.d(TAG, "Start inserting Sensordata")
        db.insertSensorDataBatch(data)
        Log.d(TAG, "Done inserting Sensordata")
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun testNaturalTriggerModel() : NaturalTriggerModel {
        val naturalTriggerModel = NaturalTriggerModel()
        naturalTriggerModel.beginTime = LocalTime.MIN
        naturalTriggerModel.endTime = LocalTime.MAX
        return naturalTriggerModel
    }

}