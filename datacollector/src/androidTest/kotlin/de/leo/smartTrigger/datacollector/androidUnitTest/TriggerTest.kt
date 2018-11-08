package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.TimeTrigger
import de.leo.smartTrigger.datacollector.utils.TimeUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.DayOfWeek
import org.threeten.bp.ZoneId


@RunWith(AndroidJUnit4::class)
class TriggerTest {

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
    }

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
        val sensorData = SensorDataSet(
            dummyTime.toInstant().toEpochMilli(), "dummy")

        //0 == thursday

        Assert.assertTrue(tt.check(context, sensorData))
        Assert.assertTrue(!tt1.check(context, sensorData))
        Assert.assertTrue(tt2.check(context, sensorData))
        Assert.assertTrue(tt3.check(context, sensorData))
    }
}