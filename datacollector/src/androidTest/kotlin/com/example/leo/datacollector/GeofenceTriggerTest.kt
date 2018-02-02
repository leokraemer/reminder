package com.example.leo.datacollector

import android.location.Location
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.example.leo.datacollector.jitai.Location.GeofenceTrigger
import com.example.leo.datacollector.jitai.MyGeofence
import com.example.leo.datacollector.models.SensorDataSet
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GeofenceTriggerTest {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<EntryActivity>(EntryActivity::class.java)


    var location1 = Location("testLocation")
    var location2 = Location("testLocation2")
    var location3 = Location("testLocation2")

    var geofence1 = MyGeofence(location1, 1000f)
    var geofence2 = MyGeofence(location2, 1000f)
    var geofence3 = MyGeofence(location3, 1000f)

    @Before
    fun setup() {
        location1.latitude = 45.0
        location1.longitude = 0.0
        location2.latitude = 45.0
        location2.longitude = 1.0
        location3.latitude = 45.0
        location3.longitude = 2.0
    }

    @Test
    fun simpleGeofenceTriggerTest() {
        val sensorData = SensorDataSet(System.currentTimeMillis(), "dummy")
        sensorData.gps = location1
        val trigger = GeofenceTrigger(listOf(geofence1))
        Assert.assertTrue(trigger.check(sensorData))
        Assert.assertTrue(trigger.check(sensorData))
    }

    @Test
    fun twoGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(geofence1, geofence2))
        val sensorData = SensorDataSet(System.currentTimeMillis(), "dummy")
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(sensorData))
        sensorData.gps = location2
        Assert.assertTrue(trigger.check(sensorData))
        Assert.assertTrue(trigger.check(sensorData))
    }

    @Test
    fun breakChainGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(geofence1, geofence2, geofence3))
        val sensorData = SensorDataSet(System.currentTimeMillis(), "dummy")
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(sensorData))
        sensorData.gps = location2
        Assert.assertFalse(trigger.check(sensorData))
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(sensorData))
        //break chain by going to end instead of 2 -> reset
        sensorData.gps = location3
        //must hit location 1 first
        Assert.assertFalse(trigger.check(sensorData))
        sensorData.gps = location2
        Assert.assertFalse(trigger.check(sensorData))
        //go through whole chain for confirmation
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(sensorData))
        sensorData.gps = location2
        Assert.assertFalse(trigger.check(sensorData))
        sensorData.gps = location3
        Assert.assertTrue(trigger.check(sensorData))
    }


    @Test
    fun timeoutGeofenceTriggerTest() {
        val trigger = GeofenceTrigger(listOf(geofence1, geofence2))
        val sensorData = SensorDataSet(System.currentTimeMillis(), "dummy")
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(sensorData))
        sensorData.gps = location2
        Assert.assertTrue(trigger.check(sensorData))
        val sensorData2 = SensorDataSet(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                                        "dummy")
        sensorData2.gps = location2
        //must fail and return to state 0
        Assert.assertFalse(trigger.check(sensorData2))
        //check line to see if reset works properly
        sensorData.gps = location1
        Assert.assertFalse(trigger.check(sensorData))
        sensorData.gps = location2
        Assert.assertTrue(trigger.check(sensorData))
    }

}