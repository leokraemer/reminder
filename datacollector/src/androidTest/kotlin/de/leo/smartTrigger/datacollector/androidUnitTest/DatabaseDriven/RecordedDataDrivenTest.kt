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
import de.leo.smartTrigger.datacollector.jitai.manage.NaturalTriggerJitai
import de.leo.smartTrigger.datacollector.ui.notifications.FullscreenJitai
import de.leo.smartTrigger.datacollector.ui.notifications.NotificationService
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.jetbrains.anko.defaultSharedPreferences
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
        db = JitaiDatabase.getInstance(targetContext)
        db.close()
        db = JitaiDatabase.getInstance(targetContext)
        am = Instrumentation.ActivityMonitor(FullscreenJitai::class.java.name, null, true)
        InstrumentationRegistry.getInstrumentation().addMonitor(am)
        targetContext.defaultSharedPreferences.edit().putString("userName", "test").apply()
    }

    @After
    fun tearDown() {
        db.close()
        if (::testDb.isInitialized)
            testDb.close()
    }

    @Test
    fun testGetEventFromDB() {
        testDb = TestDatabase.getInstance("testdb.sql", 1018, 1025, testContext)
        testDb.swapContext(targetContext)
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
        testDb = TestDatabase.getInstance("testdb.sql", 1018, 1025, testContext)
        testDb.swapContext(targetContext)
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

    @Test
    fun testLinksRechts() {
        testDb = TestDatabase.getInstance("testData_links_rechts.sql", 1025, 1025, testContext)
        testDb.swapContext(targetContext)
        val sensorDataSets = testDb.getAllSensorDataSets()
        val trigger = testDb.getAllActiveNaturalTriggerJitai()
        assertTrue(trigger.size > 0)
        sensorDataSets.forEachIndexed { i, sensorData ->
            if (i % 100 == 0) {
                Log.d("links_rechts",
                      "${(i.toFloat() / sensorDataSets.size.toFloat() * 100).roundToInt()} % done")
            }
            trigger.forEach { it.check(sensorData) }
        }
        val jitaiEvents = trigger.map { db.getJitaiEvents(it.id) }
        assertTrue(jitaiEvents.size > 0)
        assertTrue(InstrumentationRegistry.getInstrumentation().checkMonitorHit(am, 1));
    }

    @Test
    fun testLinksRechtsIndividualJitai() {
        testDb = TestDatabase.getInstance("testData_links_rechts.sql", 1025, 1025, testContext)
        testDb.swapContext(targetContext)
        val sensorDataSets = testDb.getAllSensorDataSets()
        val trigger = testDb.getAllActiveNaturalTriggerJitai()
        assertTrue(trigger.size > 0)
        val hits = mutableListOf<Pair<NaturalTriggerJitai, Int>>()
        trigger.forEach {
            var count = 0
            sensorDataSets.forEachIndexed { i, sensorData ->
                if (it.check(sensorData)) count++
            }
            hits.add(Pair(it, count))
            InstrumentationRegistry.getInstrumentation().removeMonitor(am)
            resetActivityMonitor()
            InstrumentationRegistry.getInstrumentation().addMonitor(am)
        }
        hits.forEach { Log.d("trigger", "${it.second} hits on ${it.first.naturalTriggerModel}") }
        val jitaiEvents = trigger.map { db.getJitaiEvents(it.id) }
        assertTrue(jitaiEvents.size > 0)
    }

    @Test
    fun testLinksRechts50() {
        testDb = TestDatabase.getInstance("testData_links_rechts.sql", 1025, 1025, testContext)
        testDb.swapContext(targetContext)
        val sensorDataSets = testDb.getAllSensorDataSets()
        db.insertSensorDataBatch(sensorDataSets)
        //sit for 120000 ms dwell inside for 300000 ms links from 00:00 to 23:59
        val trigger = testDb.getActiveNaturalTriggerJitai(50)
        val hits = mutableListOf<Pair<NaturalTriggerJitai, Int>>()
        trigger.let {
            var count = 0
            sensorDataSets.forEachIndexed { i, sensorData ->
                if (it!!.check(sensorData)) count++
            }
            hits.add(Pair(it!!, count))
            InstrumentationRegistry.getInstrumentation().removeMonitor(am)
            resetActivityMonitor()
            InstrumentationRegistry.getInstrumentation().addMonitor(am)
        }
        //no hit in sensor data
        assertEquals(0, am.hits)
    }

    fun resetActivityMonitor() {
        am = Instrumentation.ActivityMonitor(FullscreenJitai::class.java.name, null, true)
    }
}