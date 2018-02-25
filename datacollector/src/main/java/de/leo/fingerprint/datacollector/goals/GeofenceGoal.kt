package de.leo.fingerprint.datacollector.goals

import android.content.Context


/**
 * Created by Leo on 25.02.2018.
 */
class GeofenceGoal : Goal {
    override fun check(context: Context): Boolean {
        return false
    }
}