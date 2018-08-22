package de.leo.fingerprint.datacollector.androidUnitTest

import android.content.Context
import android.location.Location
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.jitai.Location.GeofenceTrigger
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GeofenceTriggerTest {

    lateinit var context: Context

    var Auberge_du_coq_Location = Location("testLocation")
    var Buynormand_Location = Location("testLocation2")
    var Catimini_Location = Location("testLocation2")

    lateinit var Auberge_du_coq: MyGeofence
    lateinit var Buynormand: MyGeofence
    lateinit var Catimini: MyGeofence

    @Before
    fun setup() {
        Auberge_du_coq_Location.latitude = 45.0
        Auberge_du_coq_Location.longitude = 1.0
        Buynormand_Location.latitude = 45.0
        Buynormand_Location.longitude = 0.0
        Catimini_Location.latitude = 45.0
        Catimini_Location.longitude = 2.0

        /**
         * The Layout of the geofences is: B - A - C on a line along the 45° line, with about 110 km
         * between each.
         */

        Auberge_du_coq = MyGeofence(0,
                                    "A",
                                    Auberge_du_coq_Location.latitude,
                                    Auberge_du_coq_Location.longitude,
                                    0f,
                                    true,
                                    false,
                                    false,
                                    false,
                                    1,
                                    0)
        Buynormand = MyGeofence(1,
                                "B",
                                Buynormand_Location.latitude,
                                Buynormand_Location.longitude,
                                0f,
                                true,
                                false,
                                false,
                                false,
                                1,
                                0)
        Catimini = MyGeofence(2,
                              "C",
                              Catimini_Location.latitude,
                              Catimini_Location.longitude,
                              0f,
                              true,
                              false,
                              false,
                              false,
                              1,
                              0)


        context = InstrumentationRegistry.getTargetContext()
    }

    @Test
    fun simpleGeofenceTriggerTest() {
        val sensorData = SensorDataSet(0L,
                                       "dummy")
        sensorData.gps = Auberge_du_coq_Location
        val trigger = GeofenceTrigger(listOf(Auberge_du_coq))
        Assert.assertTrue(trigger.check(context, sensorData))
        //trigger only once when enter is chosen
        Assert.assertFalse(trigger.check(context, sensorData))
    }

    @Test
    fun twoGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(Auberge_du_coq, Buynormand))
        val sensorData = SensorDataSet(0L,
                                       "dummy")
        sensorData.gps = Auberge_du_coq_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = Buynormand_Location
        Assert.assertTrue(trigger.check(context, sensorData))
        //trigger only once when enter is chosen
        Assert.assertFalse(trigger.check(context, sensorData))
    }

    @Test
    fun breakChainGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(Auberge_du_coq, Buynormand, Catimini))
        val sensorData = SensorDataSet(0L,
                                       "dummy")
        sensorData.gps = Auberge_du_coq_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = Buynormand_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = Auberge_du_coq_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        //break chain by going to end instead of 2 -> reset
        sensorData.gps = Catimini_Location
        //must hit location 1 first
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = Buynormand_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        //go through whole chain for confirmation
        sensorData.gps = Auberge_du_coq_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = Buynormand_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = Catimini_Location
        Assert.assertTrue(trigger.check(context, sensorData))
    }


    @Test
    fun timeoutGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(Auberge_du_coq, Buynormand))
        val sensorData = SensorDataSet(0L, "dummy")
        sensorData.gps = Auberge_du_coq_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = Buynormand_Location
        Assert.assertTrue(trigger.check(context, sensorData))
        val sensorData2 = SensorDataSet(TimeUnit.HOURS.toMillis(1), "dummy")
        sensorData2.gps = Buynormand_Location
        //must fail and return to state 0
        Assert.assertFalse(trigger.check(context, sensorData2))
        //check line to see if reset works properly
        sensorData.gps = Auberge_du_coq_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = Buynormand_Location
        Assert.assertTrue(trigger.check(context, sensorData))
    }

    @Test
    fun exitGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(Auberge_du_coq.copy(enter = false, exit = true)))
        val sensorData = SensorDataSet(0L, "dummy")
        //test outside
        sensorData.gps = Buynormand_Location
        val fence = trigger.getCurrentLocation()
        fence.checkGeofenceState(false, true, false, false)

        val triggered = trigger.check(context, sensorData)
        fence.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(triggered)
        //entering
        sensorData.gps = Auberge_du_coq_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        //exiting again
        sensorData.gps = Buynormand_Location
        Assert.assertTrue(trigger.check(context, sensorData))
    }

    @Test
    fun dwellGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(Auberge_du_coq.copy(enter = false,
                                                                 dwellInside = true)))
        val sensorData = SensorDataSet(0L, "dummy")
        //entering
        sensorData.gps = Auberge_du_coq_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        //loitering
        val laterSensorData = sensorData.copy(time = 2L)
        Assert.assertTrue(trigger.check(context, laterSensorData))
        //exiting again
        laterSensorData.gps = Buynormand_Location
        Assert.assertFalse(trigger.check(context, laterSensorData))
    }

    @Test
    fun dwellOutsideGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(Auberge_du_coq.copy(enter = false,
                                                                 dwellOutside = true)))
        val sensorData = SensorDataSet(0L, "dummy")
        //entering
        sensorData.gps = Auberge_du_coq_Location
        Assert.assertFalse(trigger.check(context, sensorData))
        //exiting
        val exitingSensorData = sensorData.copy(time = 2L, gps = Buynormand_Location)
        Assert.assertFalse(trigger.check(context, exitingSensorData))
        //loitering
        val loiteringSensorData = sensorData.copy(time = 4L, gps = Buynormand_Location)
        Assert.assertTrue(trigger.check(context, loiteringSensorData))
        //not loitering
        val notLoiteringSensorData = sensorData.copy(time = 6L, gps = Auberge_du_coq_Location)
        Assert.assertFalse(trigger.check(context, notLoiteringSensorData))
    }

    @Test
    fun dwellGeofenceTriggerTest2() {
        val trigger = GeofenceTrigger(listOf(Auberge_du_coq.copy(enter = false, dwellInside =
        true, loiteringDelay = 0L)))
        val sensorData = SensorDataSet(0L, "dummy")
        //entering
        sensorData.gps = Auberge_du_coq_Location
        Assert.assertTrue(trigger.check(context, sensorData))
        trigger.reset()
        //loitering
        val laterSensorData = sensorData.copy(time = 2L)
        Assert.assertTrue(trigger.check(context, laterSensorData))
        //exiting again
        laterSensorData.gps = Buynormand_Location
        Assert.assertFalse(trigger.check(context, laterSensorData))
    }
}