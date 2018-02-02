package com.example.leo.datacollector.jitai

import android.content.Context
import com.example.leo.datacollector.models.SensorDataSet
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
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
        val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(sensorData.time),
                                           ZoneId.systemDefault())
        if (timeRange?.contains(time.toLocalTime()) ?: false
                && days.any({ day ->
                                day == time.dayOfWeek
                            }))
            return true
        return false
    }

    override fun toString(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return startTime.format(formatter) + " - " +
                endInclusiveTime.format(formatter) + ", " +
                days.map { it.getDisplayName(TextStyle.SHORT, Locale.GERMANY) }.toString()
    }
}