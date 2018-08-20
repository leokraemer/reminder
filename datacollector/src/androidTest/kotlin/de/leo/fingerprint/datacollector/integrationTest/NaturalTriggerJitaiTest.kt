package de.leo.fingerprint.datacollector.integrationTest

import android.content.Context
import android.location.Location
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.integrationTest.util.TestNaturalTriggerJitai
import de.leo.fingerprint.datacollector.jitai.MyGeofence
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
            //match only every 5 minutes
            if (i == 0L || i % FIVE_MINUTES != 0L)
                Assert.assertFalse("Expected false at $i seconds",
                                   activityJitai.check(sensorDataSet))
            else
                Assert.assertTrue("Expected true at $i seconds",
                                  activityJitai.check(sensorDataSet))
            Log.d(TAG, "$i ${activityJitai.check(sensorDataSet)}")
        }
    }

    @Test
    fun testGeofenceTrigger() {
        setupDBforGeofence()
        val geofenceNaturalTriggerModel = testNaturalTriggerModel()
        val geofence = MyGeofence(name = "test",
                                  enter = true,
                                  exit = false,
                                  dwellInside = false,
                                  dwellOutside = false,
                                  loiteringDelay = 0L,
                                  imageResId = 0,
                                  latitude = Catimini_Location.latitude,
                                  longitude = Catimini_Location.longitude,
                                  radius = 100F)
        geofenceNaturalTriggerModel.geofence = geofence
        val geofenceJitai = TestNaturalTriggerJitai(context, geofenceNaturalTriggerModel)
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            val sensorDataSet = db.getSensorDataSets(i, i + FIVE_SECONDS).first()
            //match only every 5 minutes
            if (i == 0L || i % FIVE_MINUTES != 0L)
                Assert.assertFalse("Expected false at $i seconds",
                                   geofenceJitai.check(sensorDataSet))
            else
                Assert.assertTrue("Expected true at $i seconds",
                                  geofenceJitai.check(sensorDataSet))
            Log.d(TAG, "$i ${geofenceJitai.check(sensorDataSet)}")
        }
    }

    @Test
    fun testActivityGeofenceTrigger() {
        setupDBforGeofenceAndActivity()
        val activityGeofenceNaturalTriggerModel = testNaturalTriggerModel()
        val geofence = MyGeofence(name = "test",
                                  enter = true,
                                  exit = false,
                                  dwellInside = false,
                                  dwellOutside = false,
                                  loiteringDelay = 0L,
                                  imageResId = 0,
                                  latitude = Catimini_Location.latitude,
                                  longitude = Catimini_Location.longitude,
                                  radius = 100F)
        activityGeofenceNaturalTriggerModel.addActivity(NaturalTriggerModel.SIT)
        activityGeofenceNaturalTriggerModel.timeInActivity = FIVE_MINUTES
        activityGeofenceNaturalTriggerModel.geofence = geofence
        val geofenceJitai = TestNaturalTriggerJitai(context, activityGeofenceNaturalTriggerModel)
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            val sensorDataSet = db.getSensorDataSets(i, i + FIVE_SECONDS).first()
            //match only every 5 minutes
            if (i == 0L || i % FIVE_MINUTES != 0L)
                Assert.assertFalse("Expected false at $i seconds",
                                   geofenceJitai.check(sensorDataSet))
            else
                Assert.assertTrue("Expected true at $i seconds",
                                  geofenceJitai.check(sensorDataSet))
            Log.d(TAG, "$i ${geofenceJitai.check(sensorDataSet)}")
        }
    }

    var Buynormand_Location = Location("test")
    var Catimini_Location = Location("test")

    private fun setupDBforGeofence() {
        Buynormand_Location.latitude = 45.0
        Buynormand_Location.longitude = 0.0
        Catimini_Location.latitude = 45.0
        Catimini_Location.longitude = 2.0

        val data = mutableListOf<SensorDataSet>()
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            val sensorDataSet = SensorDataSet(time = i,
                                              userName = USER)
            if (i == 0L || i % FIVE_MINUTES != 0L)
                sensorDataSet.gps = Buynormand_Location
            else
                sensorDataSet.gps = Catimini_Location
            data.add(sensorDataSet)
        }
        db.insertSensorDataBatch(data)
    }

    private fun setupDBforGeofenceAndActivity() {
        Buynormand_Location.latitude = 45.0
        Buynormand_Location.longitude = 0.0
        Catimini_Location.latitude = 45.0
        Catimini_Location.longitude = 2.0

        val data = mutableListOf<SensorDataSet>()
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            val sensorDataSet = SensorDataSet(time = i,
                                              userName = USER,
                                              activity = listOf(SIT_CONFIDENT))
            if (i == 0L || i % FIVE_MINUTES != 0L)
                sensorDataSet.gps = Buynormand_Location
            else
                sensorDataSet.gps = Catimini_Location
            data.add(sensorDataSet)
        }
        db.insertSensorDataBatch(data)
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
        val data = mutableListOf<SensorDataSet>()
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            val sensorDataSet = SensorDataSet(time = i,
                                              userName = USER,
                                              activity = listOf(SIT_CONFIDENT))
            sensorDataSet.activity
            data.add(sensorDataSet)
        }
        db.insertSensorDataBatch(data)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun testNaturalTriggerModel(): NaturalTriggerModel {
        val naturalTriggerModel = NaturalTriggerModel()
        naturalTriggerModel.beginTime = LocalTime.MIN
        naturalTriggerModel.endTime = LocalTime.MAX
        return naturalTriggerModel
    }

}