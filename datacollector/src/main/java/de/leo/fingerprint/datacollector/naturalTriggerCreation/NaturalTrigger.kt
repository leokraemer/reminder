package de.leo.fingerprint.datacollector.naturalTriggerCreation

import android.net.wifi.ScanResult
import com.google.android.gms.location.Geofence
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import org.threeten.bp.LocalTime

/**
 * Created by Leo on 06.03.2018.
 */
class NaturalTrigger() {
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
    var wifi: ScanResult? = null
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }
    var beginTime = LocalTime.MIDNIGHT
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }
    var endTime = LocalTime.MIN
        set(value) {
            if (field != value) {
                field = value
                modelChangelListener?.modelChangedCallback()
            }
        }

    private var activity = mutableSetOf<JITAI_ACTIVITY>()


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


    fun checkState(currentItem: Int): Boolean {
        return true
    }

    var modelChangelListener: ModelChangedListener? = null

    interface ModelChangedListener {
        fun modelChangedCallback()
    }
}

enum class JITAI_ACTIVITY{
    SIT,
    WALK,
    BIKE,
    BUS,
    CAR
}