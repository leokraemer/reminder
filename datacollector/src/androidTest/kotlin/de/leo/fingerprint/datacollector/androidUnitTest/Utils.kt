package de.leo.fingerprint.datacollector.androidUnitTest

import android.location.Location
import de.leo.fingerprint.datacollector.jitai.MyAbstractGeofence
import junit.framework.Assert


/**
 * Created by Leo on 17.01.2018.
 */

fun getDummyLocation(): Location {
    val loc = Location("test")
    loc.longitude = 45.0
    loc.longitude = 0.0
    return loc
}

/**
 * Check if the Geofence has the specified state.
 */
internal fun MyAbstractGeofence.checkGeofenceState(entered: Boolean,
                                          exited: Boolean,
                                          loiteringInside: Boolean,
                                          loiteringOutside: Boolean) {
    if (entered)
        Assert.assertTrue(entered)
    else
        Assert.assertFalse(entered)
    if (exited)
        Assert.assertTrue(exited)
    else
        Assert.assertFalse(exited)
    if (loiteringInside)
        Assert.assertTrue(loiteringInside)
    else
        Assert.assertFalse(loiteringInside)
    if (loiteringOutside)
        Assert.assertTrue(loiteringOutside)
    else
        Assert.assertFalse(loiteringOutside)
}