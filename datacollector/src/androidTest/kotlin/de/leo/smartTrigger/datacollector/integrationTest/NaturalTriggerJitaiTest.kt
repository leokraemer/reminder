package de.leo.smartTrigger.datacollector.integrationTest

import android.content.Context
import android.location.Location
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.datacollection.models.WifiInfo
import de.leo.smartTrigger.datacollector.integrationTest.util.TestNaturalTriggerJitai
import de.leo.smartTrigger.datacollector.jitai.MyGeofence
import de.leo.smartTrigger.datacollector.jitai.MyWifiGeofence
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import de.leo.smartTrigger.datacollector.utils.TimeUtils
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.Instant
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit.MINUTES

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
        for (i in ZERO_TIME..FIFTY_MINUTES_TIME step N_SECONDS) {
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
        for (i in ZERO_TIME..FIFTY_MINUTES_TIME step N_SECONDS) {
            val sit = SensorDataSet(time = i, userName = USER, activity = listOf(BIKE_CONFIDENT))
            val walk = SensorDataSet(time = i + N_SECONDS,
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
        val positive = SensorDataSet(time = ZERO_TIME, userName = USER)
        val negative = SensorDataSet(time = ZERO_TIME, userName = USER)
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
        val positive = SensorDataSet(time = ZERO_TIME, userName = USER)
        val negative = SensorDataSet(time = ZERO_TIME, userName = USER)
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
        val positive = SensorDataSet(time = ZERO_TIME, userName = USER,
                                     activity = listOf(SIT_CONFIDENT))
        val negative = SensorDataSet(time = ZERO_TIME, userName = USER,
                                     activity = listOf(SIT_CONFIDENT))
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
        val positive = SensorDataSet(time = ZERO_TIME,
                                     userName = USER,
                                     activity = listOf(SIT_CONFIDENT))
        val negative = SensorDataSet(time = ZERO_TIME,
                                     userName = USER,
                                     activity = listOf(SIT_CONFIDENT))
        positive.gps = Catimini_Location
        negative.gps = Buynormand_Location
        setupDB(positive, negative)
        val activityGeofenceTimeNaturalTriggerModel = testNaturalTriggerModel()
        activityGeofenceTimeNaturalTriggerModel.beginTime = LocalTime.of(0, 0)
        activityGeofenceTimeNaturalTriggerModel.endTime = LocalTime.of(0, 50)
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
        for (i in ZERO_TIME..FIFTY_MINUTES_TIME step N_SECONDS) {
            val sensorDataSet = db.getSensorDataSets(i, i + N_SECONDS).first()
            //match every 5 minutes
            val match = jitai.check(sensorDataSet)
            val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(i), ZoneId.systemDefault())
            if (sensorDataSet.time == ZERO_TIME || sensorDataSet.time % FIVE_MINUTES != 0L)
                Assert.assertFalse("Expected false at ${time.minute}:${time.second}", match)
            else
                Assert.assertTrue("Expected true at ${time.minute}:${time.second}", match)
            Log.d(TAG, "$i $match")
        }
    }

    fun test(jitai: TestNaturalTriggerJitai) {
        for (i in ZERO_TIME..FIFTY_MINUTES_TIME step N_SECONDS) {
            val sensorDataSet = db.getSensorDataSets(i, i + 1).first()
            //match only every 5 minutes
            val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(i), ZoneId.systemDefault())
            val match = jitai.check(sensorDataSet)
            if (sensorDataSet.time == ZERO_TIME || sensorDataSet.time % FIVE_MINUTES != 0L)
                Assert.assertFalse("Expected false at ${time.minute}:${time.second}", match)
            else
                Assert.assertTrue("Expected true at ${time.minute}:${time.second}", match)
            Log.d(TAG, "$i $match")
        }
    }


    private fun setupDB(positiveMatch: SensorDataSet, negativeMatch: SensorDataSet) {
        val data = mutableListOf<SensorDataSet>()
        for (i in ZERO_TIME..FIFTY_MINUTES_TIME step N_SECONDS) {
            if (i == ZERO_TIME || i % FIVE_MINUTES != 0L)
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
    val ZERO_TIME = TimeUtils.getDateFromString("2017-11-01:00-00-00").toInstant().toEpochMilli()
    val FIFTY_MINUTES_TIME = TimeUtils.getDateFromString("2017-11-01:00-50-00").toInstant()
        .toEpochMilli()
    private val FIVE_MINUTES = MINUTES.toMillis(5)
    private val FIVTY_MINUTES = MINUTES.toMillis(50)

    private val N_SECONDS = DataCollectorService.UPDATE_DELAY

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