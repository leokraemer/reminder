package de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation

import com.google.android.gms.location.DetectedActivity
import de.leo.smartTrigger.datacollector.jitai.MyGeofence
import de.leo.smartTrigger.datacollector.jitai.MyWifiGeofence
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.everywhere_geofence
import org.threeten.bp.LocalTime

/**
 * Model class containing the natural trigger during creation.
 * Created by Leo on 06.03.2018.
 */
class NaturalTriggerModel(var ID: Int = -1) {
    companion object JITAI_ACTIVITY {
        val SIT = DetectedActivity.STILL
        val WALK = DetectedActivity.ON_FOOT
        val BIKE = DetectedActivity.ON_BICYCLE
        //val BUS = DetectedActivity.IN_VEHICLE
        val CAR = DetectedActivity.IN_VEHICLE
    }

    var active = true
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }

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
    var beginTime : LocalTime? = null
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }
    var endTime : LocalTime? = null
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
        private set

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

