package de.leo.fingerprint.datacollector.integrationTest

import android.content.Context
import android.location.Location
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.datacollection.models.WifiInfo
import de.leo.fingerprint.datacollector.integrationTest.util.TestNaturalTriggerJitai
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import de.leo.fingerprint.datacollector.jitai.MyWifiGeofence
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.LocalTime
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(AndroidJUnit4::class)
class NaturalTriggerJitaiTest {

    lateinit var context: Context
    lateinit var db: JitaiDatabase

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        context.deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(context)
        Buynormand_Location.latitude = 45.0
        Buynormand_Location.longitude = 0.0
        Catimini_Location.latitude = 45.0
        Catimini_Location.longitude = 2.0
    }

    @Test
    fun testActivitySitTrigger() {
        val data = mutableListOf<SensorDataSet>()
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            val sensorDataSet = SensorDataSet(time = i,
                                              userName = USER,
                                              activity = listOf(SIT_CONFIDENT))
            sensorDataSet.activity
            data.add(sensorDataSet)
        }
        db.insertSensorDataBatch(data)
        val sitNaturalTriggerModel = testNaturalTriggerModel()
        sitNaturalTriggerModel.addActivity(NaturalTriggerModel.SIT)
        sitNaturalTriggerModel.timeInActivity = FIVE_MINUTES
        val activityJitai = TestNaturalTriggerJitai(-1, context, sitNaturalTriggerModel)
        test(activityJitai)
    }

    @Test
    fun testTwoActivityTrigger() {
        val data = mutableListOf<SensorDataSet>()
        for (i in 0..FIVTY_MINUTES step (2 * FIVE_SECONDS)) {
            val sit = SensorDataSet(time = i, userName = USER, activity = listOf(BIKE_CONFIDENT))
            val walk = SensorDataSet(time = i + FIVE_SECONDS,
                                     userName = USER,
                                     activity = listOf(WALK_CONFIDENT))
            data.add(sit)
            data.add(walk)
        }
        db.insertSensorDataBatch(data)
        val twoActivityNaturalTriggerModel = testNaturalTriggerModel()
        twoActivityNaturalTriggerModel.addActivity(NaturalTriggerModel.BIKE)
        twoActivityNaturalTriggerModel.addActivity(NaturalTriggerModel.WALK)
        twoActivityNaturalTriggerModel.timeInActivity = FIVE_MINUTES
        val activityJitai = TestNaturalTriggerJitai(-1, context, twoActivityNaturalTriggerModel)
        test(activityJitai)
    }

    @Test
    fun testGeofenceTrigger() {
        val positive = SensorDataSet(time = 0, userName = USER)
        val negative = SensorDataSet(time = 0, userName = USER)
        positive.gps = Catimini_Location
        negative.gps = Buynormand_Location
        setupDB(positive, negative)
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
        val geofenceJitai = TestNaturalTriggerJitai(-1, context, geofenceNaturalTriggerModel)
        test(geofenceJitai)
    }


    @Test
    fun testWifiTrigger() {
        val positive = SensorDataSet(time = 0, userName = USER)
        val negative = SensorDataSet(time = 0, userName = USER)
        negative.wifiInformation = listOf(WifiInfo(BSSID2, 0, "", "", 0))
        positive.wifiInformation = listOf(WifiInfo(BSSID, 0, "", "", 0),
                                          WifiInfo(BSSID2, 0, "", "", 0))
        setupDB(positive, negative)
        val wifiGeofenceNaturalTriggerModel = testNaturalTriggerModel()
        val wifiGeofence = MyWifiGeofence(name = "test",
                                          enter = true,
                                          exit = false,
                                          dwellInside = false,
                                          dwellOutside = false,
                                          loiteringDelay = 0L,
                                          bssid = BSSID
                                         )
        wifiGeofenceNaturalTriggerModel.wifi = wifiGeofence
        val wifiGeofenceJitai = TestNaturalTriggerJitai(-1,
                                                        context,
                                                        wifiGeofenceNaturalTriggerModel)
        test(wifiGeofenceJitai)
    }

    @Test
    fun testActivityGeofenceTrigger() {
        val positive = SensorDataSet(time = 0, userName = USER, activity = listOf(SIT_CONFIDENT))
        val negative = SensorDataSet(time = 0, userName = USER, activity = listOf(SIT_CONFIDENT))
        positive.gps = Catimini_Location
        negative.gps = Buynormand_Location
        setupDB(positive, negative)
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
        val geofenceJitai = TestNaturalTriggerJitai(-1,
                                                    context,
                                                    activityGeofenceNaturalTriggerModel)
        test(geofenceJitai)
    }

    @Test
    fun testTimeActivityGeofenceTrigger() {
        val positive = SensorDataSet(time = 0, userName = USER, activity = listOf(SIT_CONFIDENT))
        val negative = SensorDataSet(time = 0, userName = USER, activity = listOf(SIT_CONFIDENT))
        positive.gps = Catimini_Location
        negative.gps = Buynormand_Location
        setupDB(positive, negative)
        val activityGeofenceTimeNaturalTriggerModel = testNaturalTriggerModel()
        activityGeofenceTimeNaturalTriggerModel.beginTime = LocalTime.of(0, 10)
        activityGeofenceTimeNaturalTriggerModel.endTime = LocalTime.of(0, 40)
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
        activityGeofenceTimeNaturalTriggerModel.addActivity(NaturalTriggerModel.SIT)
        activityGeofenceTimeNaturalTriggerModel.timeInActivity = FIVE_MINUTES
        activityGeofenceTimeNaturalTriggerModel.geofence = geofence
        val jitai = TestNaturalTriggerJitai(-1,
                                            context,
                                            activityGeofenceTimeNaturalTriggerModel)
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            val sensorDataSet = db.getSensorDataSets(i, i + FIVE_SECONDS).first()
            //match only every 5 minutes between 10 and 40 minutes
            if (i < MINUTES.toMillis(10) || i > MINUTES.toMillis(40) || i % FIVE_MINUTES != 0L)
                Assert.assertFalse("Expected false at $i seconds",
                                   jitai.check(sensorDataSet))
            else
                Assert.assertTrue("Expected true at $i seconds",
                                  jitai.check(sensorDataSet))
            Log.d(TAG, "$i ${jitai.check(sensorDataSet)}")
        }
    }

    fun test(jitai: TestNaturalTriggerJitai) {
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            val sensorDataSet = db.getSensorDataSets(i, i + FIVE_SECONDS).first()
            //match only every 5 minutes
            if (i == 0L || i % FIVE_MINUTES != 0L)
                Assert.assertFalse("Expected false at $i seconds", jitai.check(sensorDataSet))
            else
                Assert.assertTrue("Expected true at $i seconds", jitai.check(sensorDataSet))
            Log.d(TAG, "$i ${jitai.check(sensorDataSet)}")
        }
    }


    private fun setupDB(positiveMatch: SensorDataSet, negativeMatch: SensorDataSet) {
        val data = mutableListOf<SensorDataSet>()
        for (i in 0..FIVTY_MINUTES step FIVE_SECONDS) {
            if (i == 0L || i % FIVE_MINUTES != 0L)
                data.add(negativeMatch.copy(time = i))
            else
                data.add(positiveMatch.copy(time = i))
        }
        db.insertSensorDataBatch(data)
    }

    var Buynormand_Location = Location("test")
    var Catimini_Location = Location("test")

    private val BSSID = "DE:AD:BE:EF"
    private val BSSID2 = "DE:CA:FB:AD"
    private val TAG = this.javaClass.simpleName
    private val FIVE_MINUTES = MINUTES.toMillis(5)
    private val FIVTY_MINUTES = MINUTES.toMillis(50)

    private val FIVE_SECONDS = SECONDS.toMillis(5)

    private val USER = "dummy"

    private val CONFIDENCE_CONFIDENT = 100
    private val CONFIDENCE_UNCONFIDENT = 5


    private val SIT_CONFIDENT = DetectedActivity(NaturalTriggerModel.SIT, CONFIDENCE_CONFIDENT)
    private val BIKE_CONFIDENT = DetectedActivity(NaturalTriggerModel.BIKE, CONFIDENCE_CONFIDENT)
    private val WALK_CONFIDENT = DetectedActivity(NaturalTriggerModel.WALK, CONFIDENCE_CONFIDENT)
    private val SIT_UNCONFIDENT = DetectedActivity(NaturalTriggerModel.SIT, CONFIDENCE_UNCONFIDENT)

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