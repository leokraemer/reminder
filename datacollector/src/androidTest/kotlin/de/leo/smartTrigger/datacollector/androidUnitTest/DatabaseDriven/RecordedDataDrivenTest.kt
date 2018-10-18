package de.leo.smartTrigger.datacollector.androidUnitTest.DatabaseDriven

import android.app.Instrumentation
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.testUtil.TestDatabase
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.list.TriggerManagingActivity
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.app.Instrumentation.ActivityMonitor
import android.util.Log
import de.leo.smartTrigger.datacollector.ui.notifications.FullscreenJitai
import de.leo.smartTrigger.datacollector.ui.notifications.NotificationService
import junit.framework.Assert.assertTrue
import kotlin.math.roundToInt


@RunWith(AndroidJUnit4::class)
class RecordedDataDrivenTest {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<TriggerManagingActivity>(TriggerManagingActivity::class.java)

    val targetContext by lazy { InstrumentationRegistry.getTargetContext() }
    val testContext by lazy { InstrumentationRegistry.getContext() }
    lateinit var testDb: TestDatabase
    lateinit var db: JitaiDatabase
    lateinit var am: ActivityMonitor

    @Before
    fun setup() {
        testDb = TestDatabase(testContext)
        testDb.swapContext(targetContext)
        db = JitaiDatabase.getInstance(targetContext)
        db.close()
        db = JitaiDatabase.getInstance(targetContext)
        am = Instrumentation.ActivityMonitor(FullscreenJitai::class.java.name, null, true)
        InstrumentationRegistry.getInstrumentation().addMonitor(am)
    }

    @After
    fun tearDown() {
        db.close()
        testDb.close()
    }

    @Test
    fun testGetEventFromDB() {
        val sensorDataSets = testDb.getSensorDataSets(100)
        val trigger = testDb.getAllActiveNaturalTriggerJitai()
        assertTrue(trigger.size > 0)
        sensorDataSets.forEach { sensorData -> trigger.forEach { it.check(sensorData) } }
        val jitaiEvents = trigger.map { db.getJitaiEvents(it.id) }
        assertTrue(jitaiEvents.size > 0)
        assertTrue(InstrumentationRegistry.getInstrumentation().checkMonitorHit(am, 1));
    }

    @Test
    fun testAllSensorDataTest() {
        val sensorDataSets = testDb.getAllSensorDataSets()
        val trigger = testDb.getAllActiveNaturalTriggerJitai()
        assertTrue(trigger.size > 0)
        sensorDataSets.forEachIndexed { i, sensorData ->
            if (i % 100 == 0) {
                Log.d("allSensorDataTest",
                      "${(i.toFloat() / sensorDataSets.size.toFloat() * 100).roundToInt()} % done")
            }
            trigger.forEach { it.check(sensorData) }
        }
        val jitaiEvents = trigger.map { db.getJitaiEvents(it.id) }
        assertTrue(jitaiEvents.size > 0)
        assertTrue(InstrumentationRegistry.getInstrumentation().checkMonitorHit(am, 1));
    }
}