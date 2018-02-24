package de.leo.fingerprint.datacollector

import android.location.Location


/**
 * Created by Leo on 17.01.2018.
 */

fun getDummyLocation(): Location {
    val loc = Location("test")
    loc.longitude = 45.0
    loc.longitude = 0.0
    return loc
}