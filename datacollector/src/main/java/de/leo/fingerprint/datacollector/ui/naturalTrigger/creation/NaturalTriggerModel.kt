package de.leo.fingerprint.datacollector.ui.naturalTrigger.creation

import com.google.android.gms.location.DetectedActivity
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import de.leo.fingerprint.datacollector.jitai.MyWifiGeofence
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.everywhere_geofence
import org.threeten.bp.LocalTime

/**
 * Model class containing the natural trigger during creation.
 * Created by Leo on 06.03.2018.
 */
class NaturalTriggerModel {
    companion object JITAI_ACTIVITY {
        val SIT = DetectedActivity.STILL
        val WALK = DetectedActivity.WALKING
        val BIKE = DetectedActivity.ON_BICYCLE
        //val BUS = DetectedActivity.IN_VEHICLE
        val CAR = DetectedActivity.IN_VEHICLE
    }

    var ID = -1

    var goal = ""
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }
    var message = ""
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }
    var situation = ""
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }

    var geofence: MyGeofence? = everywhere_geofence()
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }

    var wifi: MyWifiGeofence? = null
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }
    var beginTime = LocalTime.of(8, 0)
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }
    var endTime = LocalTime.of(20, 0)
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }

    /**
     * The time that must be spent in one of the [activity]s, before the trigger is hit. In
     * milliseconds.
     */
    var timeInActivity = 0L
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }

    internal var activity = mutableSetOf<Int>()

    fun addActivity(a: Int) {
        if (!activity.contains(a)) {
            activity.add(a)
            modelChangelListener?.modelChangedCallback()
        }
    }

    fun removeActivity(a: Int) {
        if (activity.contains(a)) {
            activity.remove(a)
            modelChangelListener?.modelChangedCallback()
        }
    }

    fun checkActivity(activit: Int): Boolean {
        return activity.contains(activit)
    }

    var modelChangelListener: ModelChangedListener? = null

    interface ModelChangedListener {
        fun modelChangedCallback()
    }
}

