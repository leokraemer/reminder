package de.leo.fingerprint.datacollector

import android.content.Context
import android.location.Location
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import de.leo.fingerprint.datacollector.jitai.Location.GeofenceTrigger
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GeofenceTriggerTest {

    lateinit var context: Context

    var location1 = Location("testLocation")
    var location2 = Location("testLocation2")
    var location3 = Location("testLocation2")

    lateinit var geofence1: MyGeofence
    lateinit var geofence2: MyGeofence
    lateinit var geofence3: MyGeofence

    @Before
    fun setup() {
        location1.latitude = 45.0
        location1.longitude = 0.0
        location2.latitude = 45.0
        location2.longitude = 1.0
        location3.latitude = 45.0
        location3.longitude = 2.0

        geofence1 = MyGeofence(0, "0", location1.latitude, location1.longitude, 1000f, true,
                               false, false, 0, 0)
        geofence2 = MyGeofence(1, "1", location2.latitude, location2.longitude, 1000f, true,
                               false, false, 0, 0)
        geofence3 = MyGeofence(2, "2", location3.latitude, location3.longitude, 1000f, true,
                               false, false, 0, 0)
        context = InstrumentationRegistry.getTargetContext()
    }

    @Test
    fun simpleGeofenceTriggerTest() {
        val sensorData = SensorDataSet(System.currentTimeMillis(),
                                       "dummy")
        sensorData.gps = location1
        val trigger = GeofenceTrigger(listOf(geofence1))
        Assert.assertTrue(trigger.check(context, sensorData))
        Assert.assertTrue(trigger.check(context, sensorData))
    }

    @Test
    fun twoGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(geofence1, geofence2))
        val sensorData = SensorDataSet(System.currentTimeMillis(),
                                       "dummy")
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = location2
        Assert.assertTrue(trigger.check(context, sensorData))
        Assert.assertTrue(trigger.check(context, sensorData))
    }

    @Test
    fun breakChainGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(geofence1, geofence2, geofence3))
        val sensorData = SensorDataSet(System.currentTimeMillis(),
                                       "dummy")
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = location2
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(context, sensorData))
        //break chain by going to end instead of 2 -> reset
        sensorData.gps = location3
        //must hit location 1 first
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = location2
        Assert.assertFalse(trigger.check(context, sensorData))
        //go through whole chain for confirmation
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = location2
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = location3
        Assert.assertTrue(trigger.check(context, sensorData))
    }


    @Test
    fun timeoutGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(geofence1, geofence2))
        val sensorData = SensorDataSet(System.currentTimeMillis(),
                                       "dummy")
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = location2
        Assert.assertTrue(trigger.check(context, sensorData))
        val sensorData2 = SensorDataSet(
            System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
            "dummy")
        sensorData2.gps = location2
        //must fail and return to state 0
        Assert.assertFalse(trigger.check(context, sensorData2))
        //check line to see if reset works properly
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.gps = location2
        Assert.assertTrue(trigger.check(context, sensorData))
    }

}