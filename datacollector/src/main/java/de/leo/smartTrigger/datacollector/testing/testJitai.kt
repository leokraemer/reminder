package de.leo.smartTrigger.datacollector.testing

import android.text.format.Time
import com.google.android.gms.location.DetectedActivity
import de.leo.smartTrigger.datacollector.jitai.MyAbstractGeofence
import de.leo.smartTrigger.datacollector.jitai.MyGeofence
import de.leo.smartTrigger.datacollector.jitai.MyWifiGeofence
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import org.threeten.bp.LocalTime
import java.util.*
import java.util.concurrent.TimeUnit

const val rechts_lat = 47.67816103666347
const val rechts_lon = 9.15347445756197
const val rechts_rad = 663.0F

const val links_lat = 47.67061785643904
const val links_lon = 9.139331839978695
const val links_rad = 718.0F


fun createTestNaturalTriggers(): List<NaturalTriggerModel> {
    val geofences = mutableListOf<MyAbstractGeofence>()
    for (i in 1..4) {
        geofences.add(MyGeofence(i,
                                 "links",
                                 links_lat,
                                 links_lon,
                                 links_rad,
                                 i == 1,
                                 i == 2,
                                 i == 3,
                                 i == 4,
                                 if (i > 2) TimeUnit.MINUTES.toMillis(5) else 0,
                                 0))
        geofences.add(MyGeofence(i + 4,
                                 "rechts",
                                 rechts_lat,
                                 rechts_lon,
                                 rechts_rad,
                                 i == 1,
                                 i == 2,
                                 i == 3,
                                 i == 4,
                                 if (i > 2) TimeUnit.MINUTES.toMillis(5) else 0,
                                 0))
        geofences.add(MyWifiGeofence(i + 8,
                                     "badewannenwlan",
                                     "e8:de:27:73:86:bb",
                                     -126,
                                     i == 1,
                                     i == 2,
                                     i == 3,
                                     i == 4,
                                     if (i > 2) TimeUnit.MINUTES.toMillis(5) else 0))
    }
    val activities = mutableListOf(NaturalTriggerModel.SIT, NaturalTriggerModel.WALK,
                                   NaturalTriggerModel.BIKE, NaturalTriggerModel.CAR)
    val activityTimes = listOf(0, TimeUnit.MINUTES.toMillis(2))
    val naturalTriggers = mutableListOf<NaturalTriggerModel>()
    val random = Random()
    geofences.forEach { fence ->
        activities.forEach { activity ->
            activityTimes.forEach { timeInActivity ->
                val model = NaturalTriggerModel()
                if (fence is MyGeofence) model.geofence = fence
                else model.wifi = fence as MyWifiGeofence
                model.addActivity(activity)
                model.timeInActivity = timeInActivity
                model.beginTime = LocalTime.of(0, 0)
                model.endTime = LocalTime.of(23, 59)
                model.goal = "${mapActivity(activity)} ${geofenceDirection(fence)} ${fence.name} " +
                    "from ${model.beginTime.toString()} to ${model.endTime.toString()}"
                model.situation = model.goal
                model.message = qoutes[random.nextInt(qoutes.size)]
                naturalTriggers.add(model)
            }
        }
    }
    return naturalTriggers
}

fun geofenceDirection(geofence: MyAbstractGeofence): String =
    if (geofence.enter) "enter"
    else if (geofence.exit) "exit"
    else if (geofence.dwellInside) "dwell inside"
    else if (geofence.dwellOutside) "dwell outside"
    else "unknown"

fun mapActivity(activity: Int) =
    when (activity) {
        (NaturalTriggerModel.SIT)  -> "sit"
        (NaturalTriggerModel.BIKE) -> "bike"
        (NaturalTriggerModel.WALK) -> "walk"
        (NaturalTriggerModel.CAR)  -> "car"
        else                       -> "unknown activity"
    }

val qoutes = listOf("Denken + Handeln = Gewinnen.",
                    "Failure is an option here. If things are not failing, you are not innovating enough.",
                    "Einen Fehler begehen und nicht wieder gutzumachen, das erst heißt wahrhaft " +
                        "fehlen.",
                    "I have not failed. I’ve just found 10,000 ways that won’t work.",
                    "You only live once, but if you do it right, once is enough.",
                    "Be the change that you wish to see in the world.",
                    "Strive not to be a success, but rather to be of value.",
                    "Two roads diverged in a wood, and I—I took the one less traveled by, And " +
                        "that has made all the difference.",
                    "I attribute my success to this: I never gave or took any excuse.",
                    "Every strike brings me closer to the next home run.",
                    "Definiteness of purpose is the starting point of all achievement.",
                    "The mind is everything. What you think you become.")

