package de.leo.fingerprint.datacollector.jitai

import android.content.Context
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
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
class MyGeofenceTest() {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<EntryActivity>(EntryActivity::class.java)

    lateinit var context: Context

    /**
     * The Layout of the geofences is: B - A - C on a line along the 45° line, with about 110 km
     * between each.
     */

    var Auberge_du_coq = MyGeofence(0, "A", 45.0, 0.1, 1000f, true,
                                    false, false, false, 0, 0)
    var Buynormand = MyGeofence(1, "B", 45.0, 0.0, 0f, true,
                                false, false, false, 0, 0)
    var Catimini = MyGeofence(2, "C", 45.0, 0.2, 0f, true,
                              false, false, false, 0, 0)


    @Before
    fun setup() {
        context = activityRule.activity
    }

    @Test
    fun testSymmetryOfGeofences() {
        //location distance must be direction independent
        Assert.assertEquals(Buynormand.location.distanceTo(Auberge_du_coq.location),
                            Auberge_du_coq.location.distanceTo(Buynormand.location))
    }

    @Test
    fun testMyGeofenceEnter() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(Auberge_du_coq.location),
                            true,
                            false,
                            false,
                            false,
                            0,
                            0)
        Assert.assertTrue(CA.update(Catimini.location, 0L))
        Assert.assertTrue(CA.checkCondition())
        Assert.assertTrue(CA.update(Buynormand.location, 0L))
        Assert.assertFalse(CA.checkCondition())
    }

    @Test
    fun testMyGeofenceExit() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(Auberge_du_coq.location),
                            false,
                            true,
                            false,
                            false,
                            0,
                            0)
        Assert.assertTrue(CA.update(Catimini.location, 0L))
        Assert.assertFalse(CA.checkCondition())
        Assert.assertTrue(CA.update(Buynormand.location, 0L))
        Assert.assertTrue(CA.checkCondition())
    }

    @Test
    fun testMyGeofenceDwellInside() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(Auberge_du_coq.location),
                            false,
                            false,
                            true,
                            false,
                            1,
                            0)
        Assert.assertTrue(CA.update(Catimini.location, 0L))
        Assert.assertFalse(CA.checkCondition())
        Assert.assertTrue(CA.update(Catimini.location, 2L))
        Assert.assertTrue(CA.checkCondition())
    }

    @Test
    fun testMyGeofenceDwellOutside() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(Auberge_du_coq.location),
                            false,
                            false,
                            false,
                            true,
                            1,
                            0)
        Assert.assertFalse(CA.update(Buynormand.location, 0L))
        Assert.assertFalse(CA.entered())
        Assert.assertTrue(CA.exited())
        Assert.assertFalse(CA.loiteringInside())
        Assert.assertFalse(CA.loiteringOutside())
        Assert.assertFalse(CA.checkCondition())
        Assert.assertTrue(CA.update(Buynormand.location, 2L))
        Assert.assertTrue(CA.checkCondition())
    }

    @Test
    fun testCheckStateWithParameters() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(Auberge_du_coq.location),
                            true,
                            false,
                            false,
                            false,
                            1,
                            0)
        Assert.assertTrue(CA.update(Catimini.location, 0L))
        Assert.assertEquals(CA.checkCondition(), CA.checkCondition(Catimini.location, 0L))
        Assert.assertNotSame(CA.checkCondition(), CA.checkCondition(Buynormand.location, 0L))
        //second check to make sure the state did not change by calling checkCondition(... , ...)
        Assert.assertEquals(CA.checkCondition(), CA.checkCondition(Catimini.location, 0L))
    }
}