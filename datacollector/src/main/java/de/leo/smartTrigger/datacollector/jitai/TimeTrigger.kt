package de.leo.smartTrigger.datacollector.jitai

import android.content.Context
import android.util.Log
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.ChronoUnit
import java.util.*

/**
 * @param timeRange
 * The range of time the intervention is valid in. Must be within one day.
 *
 * @param days
 * 1 = sunday
 * 2 = monday
 * ...
 * 7 = saturday
 * @see Calendar.DAY_OF_WEEK
 */
class TimeTrigger() : Trigger {

    override fun reset(sensorData: SensorDataSet) {
    }

    companion object {
        val ALL_DAYS = listOf(DayOfWeek.MONDAY,
                              DayOfWeek.TUESDAY,
                              DayOfWeek.WEDNESDAY,
                              DayOfWeek.THURSDAY,
                              DayOfWeek.FRIDAY,
                              DayOfWeek.SATURDAY,
                              DayOfWeek.SUNDAY)
    }

    private lateinit var startTime: LocalTime
        private set
    private lateinit var endInclusiveTime: LocalTime
        private set

    @Transient
    var timeRange: ClosedRange<LocalTime>? = null
        private set

    lateinit var days: List<DayOfWeek>
        private set

    constructor(timeRange: ClosedRange<LocalTime>, days: List<DayOfWeek>) : this() {
        startTime = timeRange.start
        endInclusiveTime = timeRange.endInclusive
        this.timeRange = timeRange
        this.days = days
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        if (timeRange == null)
            timeRange = startTime.rangeTo(endInclusiveTime)
        val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(sensorData.time),
                                           ZoneId.systemDefault())
        if (timeRange?.contains(time.toLocalTime()) == true
            && days.any { day -> day == time.dayOfWeek })
            return true
        return false
    }

    override fun toString(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return startTime.format(formatter) + " - " +
            endInclusiveTime.format(formatter) + ", " +
            days.map { it.getDisplayName(TextStyle.SHORT, Locale.GERMANY) }.toString()
    }

    fun getPassedTimePercent(time: LocalTime): Double {
        if (timeRange == null)
            timeRange = startTime.rangeTo(endInclusiveTime)
        if (timeRange!!.contains(time)) {
            val totalTime = startTime.until(endInclusiveTime, ChronoUnit.MILLIS)
            val passedTime = startTime.until(time, ChronoUnit.MILLIS)
            return passedTime.toDouble() / totalTime.toDouble()
        }
        return -1.0
    }

    //wants to be checked again immediately
    override fun nextUpdate(): Long {
        var delay = 0L
        if (timeRange == null)
            timeRange = startTime.rangeTo(endInclusiveTime)
        val now = ZonedDateTime.now()
        if (timeRange?.contains(now.toLocalTime()) == true) {
            if (days.any { day -> day == now.dayOfWeek })
                delay = 0
            else delay = Duration.between(now.toLocalTime(), LocalTime.MAX).toMillis()
        } else if (now.toLocalTime().isBefore(startTime)) {
            delay = Duration.between(now.toLocalTime(), startTime).toMillis()
        } else if (now.toLocalTime().isAfter(endInclusiveTime))
            delay = Duration.between(now.toLocalTime(), LocalTime.MAX).toMillis()
        Log.d("time delay", "$delay")
        return delay
    }
}