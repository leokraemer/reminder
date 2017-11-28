package com.example.leo.datacollector.jitai

import com.example.leo.datacollector.models.SensorDataSet
import com.example.leo.datacollector.utils.TimeUtils.getDateFromString
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalTime
import java.util.*

/**
 * @param timeRange
 * The range of time the intervention is valid in. Must be within one day.
 *
 * @param days
 * 0 = sunday
 * 1 = monday
 * ...
 * 7 = saturday
 * @see Calendar.DAY_OF_WEEK
 */
class TimeTrigger(val timeRange: ClosedRange<LocalTime>, val days: List<DayOfWeek>) : Trigger {

    init {
        if (timeRange.start > timeRange.endInclusive) {
            throw IllegalArgumentException("start > end")
        }
    }

    override fun check(sensorData: SensorDataSet): Boolean {
        if (timeRange.contains(sensorData.time.toLocalTime())
                && days.any({ day -> day == sensorData.time.dayOfWeek }))
            return true
        return false
    }
}