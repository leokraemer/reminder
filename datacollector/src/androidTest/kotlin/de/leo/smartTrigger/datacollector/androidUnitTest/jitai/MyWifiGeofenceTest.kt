package de.leo.smartTrigger.datacollector.androidUnitTest.jitai

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.androidUnitTest.checkGeofenceState
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.WifiInfo
import de.leo.smartTrigger.datacollector.jitai.MyWifiGeofence
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.list.TriggerManagingActivity
import junit.framework.Assert
import org.junit.After
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
    var activityRule = ActivityTestRule<TriggerManagingActivity>(TriggerManagingActivity::class.java)

    lateinit var db: JitaiDatabase
    lateinit var context: Context

    val A = "A"
    val B = "B"
    val C = "C"
    val RSSI = 45
    val wifiInfo = WifiInfo(BSSID = A, rssi = RSSI, SSID = A, IP = "0:0:0:0", networkId = -1)

    var TEST_GEOFENCE_A = MyWifiGeofence(bssid = A, rssi = RSSI)
    var SCAN_RESULT_A = listOf(wifiInfo.copy(BSSID = A))
    var SCAN_RESULT_B = listOf(wifiInfo.copy(BSSID = B))
    var SCAN_RESULT_AB = listOf(wifiInfo.copy(BSSID = B), wifiInfo.copy(BSSID = A))
    var SCAN_RESULT_BC = listOf(wifiInfo.copy(BSSID = B), wifiInfo.copy(BSSID = C))
    var SCAN_RESULT_ABC = listOf(wifiInfo.copy(BSSID = A),
                                 wifiInfo.copy(BSSID = B),
                                 wifiInfo.copy(BSSID = C))

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        db = JitaiDatabase.getInstance(context)
        db.close()
        context.deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDatabase() {
        val id = db.enterMyWifiGeofence(TEST_GEOFENCE_A)
        Assert.assertNotSame(-1, id)
        val retrieved = db.getMyWifiGeofence(id)
        Assert.assertNotNull(retrieved)
        Assert.assertEquals(TEST_GEOFENCE_A.copy(id = id), retrieved!!)
    }

    @Test
    fun testMyGeofenceEnter() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = true)
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_A))
        Assert.assertTrue(TEST_GEOFENCE_A.checkCondition())
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_B))
        Assert.assertFalse(TEST_GEOFENCE_A.checkCondition())
    }

    @Test
    fun testMyGeofenceEnterMoreThanOneWifi() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = true)
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_ABC))
        Assert.assertTrue(TEST_GEOFENCE_A.checkCondition())
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_BC))
        Assert.assertFalse(TEST_GEOFENCE_A.checkCondition())
    }

    @Test
    fun testMyGeofenceEnterTwice() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = true)
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_A))
        Assert.assertTrue(TEST_GEOFENCE_A.checkCondition())
        //inside again must not change state...
        Assert.assertFalse(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_A))
        //... and but is still inside
        Assert.assertTrue(TEST_GEOFENCE_A.checkCondition())
        //exiting must change state
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_B))
        //but not trigger enter
        Assert.assertFalse(TEST_GEOFENCE_A.checkCondition())
        ///entering again must work
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_A))
        Assert.assertTrue(TEST_GEOFENCE_A.checkCondition())
    }

    @Test
    fun testMyGeofenceExit() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = false, exit = true)
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_A))
        Assert.assertFalse(TEST_GEOFENCE_A.checkCondition())
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_B))
        Assert.assertTrue(TEST_GEOFENCE_A.checkCondition())
    }

    @Test
    fun testMyGeofenceExitTwoWifi() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = false, exit = true)
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_ABC))
        Assert.assertFalse(TEST_GEOFENCE_A.checkCondition())
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_BC))
        Assert.assertTrue(TEST_GEOFENCE_A.checkCondition())
    }

    @Test
    fun testMyGeofenceDwellInside() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = false,
                                               dwellInside = true,
                                               loiteringDelay = 2)
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_A))
        Assert.assertFalse(TEST_GEOFENCE_A.checkCondition())
        Assert.assertTrue(TEST_GEOFENCE_A.update(2L, SCAN_RESULT_A))
        Assert.assertTrue(TEST_GEOFENCE_A.checkCondition())
    }

    @Test
    fun testMyGeofenceDwellOutside() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = false,
                                               dwellOutside = true,
                                               loiteringDelay = 2)
        //initial state
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(0L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
    }

    @Test
    fun testCheckStateWithParameters() {
        Assert.assertTrue(TEST_GEOFENCE_A.update(0L, SCAN_RESULT_A))
        Assert.assertEquals(TEST_GEOFENCE_A.checkCondition(),
                            TEST_GEOFENCE_A.checkCondition(0L, SCAN_RESULT_A))
        Assert.assertNotSame(TEST_GEOFENCE_A.checkCondition(),
                             TEST_GEOFENCE_A.checkCondition(0L, SCAN_RESULT_B))
        //second check to make sure the state did not change by calling checkCondition(... , ...)
        Assert.assertEquals(TEST_GEOFENCE_A.checkCondition(),
                            TEST_GEOFENCE_A.checkCondition(0L, SCAN_RESULT_A))
    }

    @Test
    fun testRepeatedFiringOfLoiteringDelayInside() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = false,
                                               dwellInside = true,
                                               loiteringDelay = 2)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(0L, SCAN_RESULT_A))
        TEST_GEOFENCE_A.checkGeofenceState(true, false, false, false)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(1L, SCAN_RESULT_A))
        TEST_GEOFENCE_A.checkGeofenceState(true, false, false, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_A))
        TEST_GEOFENCE_A.checkGeofenceState(true, false, true, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_A))
        TEST_GEOFENCE_A.checkGeofenceState(true, false, false, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(3L, SCAN_RESULT_A))
        TEST_GEOFENCE_A.checkGeofenceState(true, false, false, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(4L, SCAN_RESULT_A))
        TEST_GEOFENCE_A.checkGeofenceState(true, false, true, false)
    }

    @Test
    fun testRepeatedFiringOfLoiteringDelayOutside() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = false,
                                               dwellOutside = true,
                                               loiteringDelay = 2)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(0L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(1L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
        //reset
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy()
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(3L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(4L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
    }

    @Test
    fun testRepeatedFiringOfLoiteringDelayOutsideNoReset() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = false,
                                               dwellOutside = true,
                                               loiteringDelay = 2)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(0L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(1L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(3L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(4L, SCAN_RESULT_B))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
    }

    @Test
    fun testRepeatedFiringOfLoiteringDelayOutsideTwoWifi() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = false,
                                               dwellOutside = true,
                                               loiteringDelay = 2)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(0L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(1L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy()
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(3L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(4L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
    }

    @Test
    fun testRepeatedFiringOfLoiteringDelayOutsideTwoWifiNoReset() {
        TEST_GEOFENCE_A = TEST_GEOFENCE_A.copy(enter = false,
                                               dwellOutside = true,
                                               loiteringDelay = 2)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(0L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(TEST_GEOFENCE_A.updateAndCheck(1L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(2L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(3L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
        Assert.assertTrue(TEST_GEOFENCE_A.updateAndCheck(4L, SCAN_RESULT_BC))
        TEST_GEOFENCE_A.checkGeofenceState(false, true, false, true)
    }
}