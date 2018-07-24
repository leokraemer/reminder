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

    var geofence1 = MyGeofence(0, "0", 45.0, 0.0, 0f, true,
                               false, false, 0, 0)
    var geofence2 = MyGeofence(1, "1", 45.0, 0.1, 1000f, true,
                               false, false, 0, 0)
    var geofence3 = MyGeofence(2, "2", 45.0, 0.2, 0f, true,
                               false, false, 0, 0)


    @Before
    fun setup() {
        context = activityRule.activity
    }

    @Test
    fun testMyGeofenceEnter() {
        //location distance must be direction independent
        Assert.assertEquals(geofence1.getLocation().distanceTo(geofence2.getLocation()),
                            geofence2.getLocation().distanceTo(geofence1.getLocation()))
        val geofence4 = MyGeofence(geofence3.id,
                                   geofence3.name,
                                   geofence3.latitude,
                                   geofence3.longitude,
                                   geofence3.getLocation().distanceTo(geofence2.getLocation()),
                                   true,
                                   false,
                                   false,
                                   0,
                                   0)
        Assert.assertTrue(geofence4.checkCondition(geofence3.getLocation()))
        Assert.assertFalse(geofence3.checkCondition(geofence1.getLocation()))
    }
}