package com.example.leo.datacollector

import android.support.test.runner.AndroidJUnit4
import com.example.leo.datacollector.jitai.TimeTrigger
import com.example.leo.datacollector.models.SensorDataSet
import com.example.leo.datacollector.utils.TimeUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.DayOfWeek
import org.threeten.bp.ZoneId


@RunWith(AndroidJUnit4::class)
class TriggerTest {

    val start = TimeUtils.getDateFromString("2017-11-16:12-00-00").toLocalTime()
    val end = TimeUtils.getDateFromString("2017-11-16:18-00-00").toLocalTime()
    val tt = TimeTrigger(start.rangeTo(end), listOf(DayOfWeek.THURSDAY))
    val tt1 = TimeTrigger(start.rangeTo(end), listOf(DayOfWeek.MONDAY))
    val tt2 = TimeTrigger(start.rangeTo(end), listOf(DayOfWeek.THURSDAY, DayOfWeek.SATURDAY))
    val tt3 = TimeTrigger(start.rangeTo(end),
                          listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.WEDNESDAY))

    @Test
    fun testTimeTrigger() {
        val dummyTime = TimeUtils.getDateFromString("2017-11-16:12-30-00")
        val sensorData = SensorDataSet(dummyTime.atZone(ZoneId.systemDefault()).toEpochSecond(),
                                       "dummy")

        //0 == thursday

        Assert.assertTrue(tt.check(sensorData))
        Assert.assertTrue(!tt1.check(sensorData))
        Assert.assertTrue(tt2.check(sensorData))
        Assert.assertTrue(tt3.check(sensorData))
    }
}