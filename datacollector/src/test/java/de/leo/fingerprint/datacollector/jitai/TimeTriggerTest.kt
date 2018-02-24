package de.leo.fingerprint.datacollector.jitai

import de.leo.fingerprint.datacollector.models.SensorDataSet
import de.leo.fingerprint.datacollector.utils.TimeUtils.getDateFromString
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.ZoneId

/**
 * Created by Leo on 16.11.2017.
 */
class TimeTriggerTest {

    @Before
    fun setup() {
    }

    //time format: yyyy-MM-dd:HH-mm-ss
    @Test
    fun check() {
        val dummyTime = getDateFromString("2017-11-16:12-30-00")
        val sensorData = SensorDataSet(dummyTime.atZone(ZoneId.systemDefault()).toEpochSecond(), "dummy")
        val start = getDateFromString("2017-11-16:12-00-00").toLocalTime()
        val end = getDateFromString("2017-11-16:18-00-00").toLocalTime()
        //0 == thursday
        val tt = TimeTrigger(start.rangeTo(end), listOf(DayOfWeek.THURSDAY))
        val tt1 = TimeTrigger(start.rangeTo(end), listOf(DayOfWeek.MONDAY))
        val tt2 = TimeTrigger(start.rangeTo(end), listOf(DayOfWeek.THURSDAY, DayOfWeek.SATURDAY))
        val tt3 = TimeTrigger(start.rangeTo(end),
                              listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.WEDNESDAY))
        Assert.assertTrue(tt.check(sensorData))
        Assert.assertTrue(!tt1.check(sensorData))
        Assert.assertTrue(tt2.check(sensorData))
        Assert.assertTrue(tt3.check(sensorData))
    }

}