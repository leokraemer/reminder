package de.leo.fingerprint.datacollector.jitai

import android.content.Context
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import de.leo.fingerprint.datacollector.checkGeofenceState
import de.leo.fingerprint.datacollector.ui.EntryActivity
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Leo on 25.02.2018.
 */
@RunWith(AndroidJUnit4::class)
class MyWifiGeofenceTest() {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<EntryActivity>(EntryActivity::class.java)

    lateinit var context: Context

    val A = "A"
    val B = "B"
    val RSSI = 45

    var A_Router = MyWifiGeofence(bssid = A, rssi = RSSI)

    @Before
    fun setup() {
        context = activityRule.activity
    }

    @Test
    fun testMyGeofenceEnter() {
        A_Router = A_Router.copy(enter = true)
        Assert.assertTrue(A_Router.update(0L, A, RSSI))
        Assert.assertTrue(A_Router.checkCondition())
        Assert.assertTrue(A_Router.update(0L, B, RSSI))
        Assert.assertFalse(A_Router.checkCondition())
    }

    @Test
    fun testMyGeofenceExit() {
        A_Router = A_Router.copy(enter = false, exit = true)
        Assert.assertTrue(A_Router.update(0L, A, RSSI))
        Assert.assertFalse(A_Router.checkCondition())
        Assert.assertTrue(A_Router.update(0L, B, RSSI))
        Assert.assertTrue(A_Router.checkCondition())
    }

    @Test
    fun testMyGeofenceDwellInside() {
        A_Router = A_Router.copy(enter = false, dwellInside = true, loiteringDelay = 2)
        Assert.assertTrue(A_Router.update(0L, A, RSSI))
        Assert.assertFalse(A_Router.checkCondition())
        Assert.assertTrue(A_Router.update(2L, A, RSSI))
        Assert.assertTrue(A_Router.checkCondition())
    }

    @Test
    fun testMyGeofenceDwellOutside() {
        A_Router = A_Router.copy(enter = false, dwellOutside = true, loiteringDelay = 2)
        //initial state
        A_Router.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(A_Router.updateAndCheck(0L, B, RSSI))
        A_Router.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(A_Router.updateAndCheck(2L, B, RSSI))
        A_Router.checkGeofenceState(false, true, false, true)
    }

    @Test
    fun testCheckStateWithParameters() {
        Assert.assertTrue(A_Router.update(0L, A, RSSI))
        Assert.assertEquals(A_Router.checkCondition(),
                            A_Router.checkCondition(0L, A, RSSI))
        Assert.assertNotSame(A_Router.checkCondition(),
                             A_Router.checkCondition(0L, B, RSSI))
        //second check to make sure the state did not change by calling checkCondition(... , ...)
        Assert.assertEquals(A_Router.checkCondition(),
                            A_Router.checkCondition(0L, A, RSSI))
    }

    @Test
    fun testRepeatedFiringOfLoiteringDelayInside() {
        A_Router = A_Router.copy(enter = false, dwellInside = true, loiteringDelay = 2)
        Assert.assertFalse(A_Router.updateAndCheck(0L, A, RSSI))
        A_Router.checkGeofenceState(true, false, false, false)
        Assert.assertFalse(A_Router.updateAndCheck(1L, A, RSSI))
        A_Router.checkGeofenceState(true, false, false, false)
        Assert.assertTrue(A_Router.updateAndCheck(2L, A, RSSI))
        A_Router.checkGeofenceState(true, false, true, false)
        Assert.assertFalse(A_Router.updateAndCheck(2L, A, RSSI))
        A_Router.checkGeofenceState(true, false, false, false)
        Assert.assertFalse(A_Router.updateAndCheck(3L, A, RSSI))
        A_Router.checkGeofenceState(true, false, false, false)
        Assert.assertTrue(A_Router.updateAndCheck(4L, A, RSSI))
        A_Router.checkGeofenceState(true, false, true, false)
    }

    @Test
    fun testRepeatedFiringOfLoiteringDelayOutside() {
        A_Router = A_Router.copy(enter = false, dwellOutside = true, loiteringDelay = 2)
        Assert.assertFalse(A_Router.updateAndCheck(0L, B, RSSI))
        A_Router.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(A_Router.updateAndCheck(1L, B, RSSI))
        A_Router.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(A_Router.updateAndCheck(2L, B, RSSI))
        A_Router.checkGeofenceState(false, true, false, true)
        Assert.assertFalse(A_Router.updateAndCheck(2L, B, RSSI))
        A_Router.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(A_Router.updateAndCheck(3L, B, RSSI))
        A_Router.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(A_Router.updateAndCheck(4L, B, RSSI))
        A_Router.checkGeofenceState(false, true, false, true)
    }
}