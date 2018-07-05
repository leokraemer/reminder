package de.leo.fingerprint.datacollector.ui.naturalTrigger.creation

import de.leo.fingerprint.datacollector.datacollection.models.WifiInfo
import de.leo.fingerprint.datacollector.jitai.Location.GeofenceTrigger
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import org.threeten.bp.LocalTime

/**
 * Model class containing the natural trigger during creation.
 * Created by Leo on 06.03.2018.
 */
class NaturalTriggerModel() {
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

    var geofence: MyGeofence? = null
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }
    var wifi: WifiInfo? = null
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

    internal var activity = mutableSetOf<JITAI_ACTIVITY>()


    fun addActivity(a: JITAI_ACTIVITY) {
        if (!activity.contains(a)) {
            activity.add(a)
            modelChangelListener?.modelChangedCallback()
        }
    }

    fun removeActivity(a: JITAI_ACTIVITY) {
        if (activity.contains(a)) {
            activity.remove(a)
            modelChangelListener?.modelChangedCallback()
        }
    }

    fun checkActivity(activit: JITAI_ACTIVITY): Boolean {
        return activity.contains(activit)
    }

    var modelChangelListener: ModelChangedListener? = null

    interface ModelChangedListener {
        fun modelChangedCallback()
    }
}

enum class JITAI_ACTIVITY {
    SIT,
    WALK,
    BIKE,
    BUS,
    CAR
}