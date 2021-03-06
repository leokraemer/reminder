package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.AskingTrigger
import de.leo.smartTrigger.datacollector.jitai.manage.Jitai
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


/**
 * Created by Leo on 25.02.2018.
 */
@RunWith(AndroidJUnit4::class)
class AskingTriggerTest {

    lateinit var context: Context
    lateinit var sensorDataSet: SensorDataSet
    lateinit var db: JitaiDatabase
    val jitaiID = 0
    val USERNAME = "test"

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        db = JitaiDatabase.getInstance(context)
        db.close()
        context.deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(context)
    }

    @Test
    fun testBasicFunction() {
        val trigger = AskingTrigger(jitaiID, TimeUnit.MINUTES.toMillis(30))
        sensorDataSet = SensorDataSet(3,
                                      "dummy",
                                      0)
        Assert.assertFalse(trigger.check(context, sensorDataSet))
        db.enterUserJitaiEvent(jitaiID, 1, USERNAME, Jitai.NOTIFICATION_TRIGGER_YES, -1, -1,
                               -1, "")
        Assert.assertTrue(trigger.check(context, sensorDataSet))
        db.enterUserJitaiEvent(jitaiID, 2, USERNAME, Jitai.NOTIFICATION_TRIGGER_NO, -1, -1,
                               -1, "")
        Assert.assertFalse(trigger.check(context, sensorDataSet))
    }

    @Test
    fun testBasicTimeout() {
        val trigger = AskingTrigger(jitaiID, TimeUnit.MINUTES.toMillis(30))
        sensorDataSet = SensorDataSet(3,
                                      "dummy",
                                      0)
        Assert.assertFalse(trigger.check(context, sensorDataSet))
        db.enterUserJitaiEvent(jitaiID, 1, USERNAME, Jitai.NOTIFICATION_TRIGGER_YES, -1, -1,
                               -1, "")
        Assert.assertTrue(trigger.check(context, sensorDataSet))
        //<= 30 minutes later
        sensorDataSet = sensorDataSet.copy(time = TimeUnit.MINUTES.toMillis(30))
        Assert.assertTrue(trigger.check(context, sensorDataSet))
        //>30 minutes later
        sensorDataSet = sensorDataSet.copy(time = TimeUnit.MINUTES.toMillis(30) + 1)
        Assert.assertFalse(trigger.check(context, sensorDataSet))
    }

    @Test
    fun testNoAndTimeout() {
        val trigger = AskingTrigger(jitaiID, TimeUnit.MINUTES.toMillis(30))
        sensorDataSet = SensorDataSet(3,
                                      "dummy",
                                      0)
        Assert.assertFalse(trigger.check(context, sensorDataSet))
        db.enterUserJitaiEvent(jitaiID, 1, USERNAME, Jitai.NOTIFICATION_TRIGGER_YES, -1, -1,
                               -1, "")
        Assert.assertTrue(trigger.check(context, sensorDataSet))
        db.enterUserJitaiEvent(jitaiID, 2, USERNAME, Jitai.NOTIFICATION_TRIGGER_NO, -1, -1,
                               -1, "")
        Assert.assertFalse(trigger.check(context, sensorDataSet))
        sensorDataSet = sensorDataSet.copy(time = TimeUnit.MINUTES.toMillis(30) + 1)
        Assert.assertFalse(trigger.check(context, sensorDataSet))
    }


    @After
    fun tearDown() {
        db.close()
    }
}