package de.leo.smartTrigger.datacollector.androidUnitTest.jitai

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.androidUnitTest.checkGeofenceState
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.jitai.MyGeofence
import de.leo.smartTrigger.datacollector.ui.EntryActivity
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
class MyGeofenceTest() {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<EntryActivity>(EntryActivity::class.java)

    lateinit var db: JitaiDatabase
    lateinit var context: Context
    /**
     * The Layout of the geofences is: B - A - C on a line along the 45° line, with about 110 km
     * between each.
     */

    var Auberge_du_coq = MyGeofence(0,
                                    "A",
                                    45.0,
                                    0.1,
                                    1000f,
                                    true,
                                    false,
                                    false,
                                    false,
                                    0,
                                    0)
    var Buynormand = MyGeofence(1, "B", 45.0, 0.0, 0f, true,
                                false, false, false, 0, 0)

    var Catimini = MyGeofence(2, "C", 45.0, 0.2, 0f, true,
                              false, false, false, 0, 0)

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        context.deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDatabase() {
        val id = db.enterGeofence(Auberge_du_coq)
        //test that we actually entered it
        Assert.assertNotSame(-1, id)
        val retrieved = db.getMyGeofence(id)
        Assert.assertNotNull(retrieved)
        Assert.assertEquals(Auberge_du_coq.copy(id = id), retrieved!!)
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
                            Catimini.location.distanceTo(
                                Auberge_du_coq.location),
                            true,
                            false,
                            false,
                            false,
                            0,
                            0)
        Assert.assertTrue(CA.update(0L, Catimini.location))
        Assert.assertTrue(CA.checkCondition())
        Assert.assertTrue(CA.update(0L, Buynormand.location))
        Assert.assertFalse(CA.checkCondition())
    }

    @Test
    fun testMyGeofenceExit() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(
                                Auberge_du_coq.location),
                            false,
                            true,
                            false,
                            false,
                            0,
                            0)
        Assert.assertTrue(CA.update(0L, Catimini.location))
        Assert.assertFalse(CA.checkCondition())
        Assert.assertTrue(CA.update(0L, Buynormand.location))
        Assert.assertTrue(CA.checkCondition())
    }

    @Test
    fun testMyGeofenceDwellInside() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(
                                Auberge_du_coq.location),
                            false,
                            false,
                            true,
                            false,
                            1,
                            0)
        Assert.assertTrue(CA.update(0L, Catimini.location))
        Assert.assertFalse(CA.checkCondition())
        Assert.assertTrue(CA.update(2L, Catimini.location))
        Assert.assertTrue(CA.checkCondition())
    }

    @Test
    fun testMyGeofenceDwellOutside() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(
                                Auberge_du_coq.location),
                            false,
                            false,
                            false,
                            true,
                            1,
                            0)
        //initial state
        CA.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(CA.updateAndCheck(0L, Buynormand.location))
        CA.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(CA.updateAndCheck(2L, Buynormand.location))
        CA.checkGeofenceState(false, true, false, true)
    }

    @Test
    fun testCheckStateWithParameters() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(
                                Auberge_du_coq.location),
                            true,
                            false,
                            false,
                            false,
                            1,
                            0)
        Assert.assertTrue(CA.update(0L, Catimini.location))
        Assert.assertEquals(CA.checkCondition(), CA.checkCondition(0L, Catimini.location))
        Assert.assertNotSame(CA.checkCondition(), CA.checkCondition(0L, Buynormand.location))
        //second check to make sure the state did not change by calling checkCondition(... , ...)
        Assert.assertEquals(CA.checkCondition(), CA.checkCondition(0L, Catimini.location))
    }

    @Test
    fun testRepeatedFiringOfLoiteringDelayInside() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(
                                Auberge_du_coq.location),
                            false,
                            false,
                            true,
                            false,
                            2,
                            0)
        Assert.assertFalse(CA.updateAndCheck(0L, Catimini.location))
        CA.checkGeofenceState(true, false, false, false)
        Assert.assertFalse(CA.updateAndCheck(1L, Catimini.location))
        CA.checkGeofenceState(true, false, false, false)
        Assert.assertTrue(CA.updateAndCheck(2L, Catimini.location))
        CA.checkGeofenceState(true, false, true, false)
        Assert.assertFalse(CA.updateAndCheck(2L, Catimini.location))
        CA.checkGeofenceState(true, false, false, false)
        Assert.assertFalse(CA.updateAndCheck(3L, Catimini.location))
        CA.checkGeofenceState(true, false, false, false)
        Assert.assertTrue(CA.updateAndCheck(4L, Catimini.location))
        CA.checkGeofenceState(true, false, true, false)
    }

    @Test
    fun testRepeatedFiringOfLoiteringDelayOutside() {
        val CA = MyGeofence(Catimini.id,
                            Catimini.name,
                            Catimini.latitude,
                            Catimini.longitude,
                            Catimini.location.distanceTo(
                                Auberge_du_coq.location),
                            false,
                            false,
                            false,
                            true,
                            2,
                            0)
        Assert.assertFalse(CA.updateAndCheck(0L, Buynormand.location))
        CA.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(CA.updateAndCheck(1L, Buynormand.location))
        CA.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(CA.updateAndCheck(2L, Buynormand.location))
        CA.checkGeofenceState(false, true, false, true)
        Assert.assertFalse(CA.updateAndCheck(2L, Buynormand.location))
        CA.checkGeofenceState(false, true, false, false)
        Assert.assertFalse(CA.updateAndCheck(3L, Buynormand.location))
        CA.checkGeofenceState(false, true, false, false)
        Assert.assertTrue(CA.updateAndCheck(4L, Buynormand.location))
        CA.checkGeofenceState(false, true, false, true)
    }
}