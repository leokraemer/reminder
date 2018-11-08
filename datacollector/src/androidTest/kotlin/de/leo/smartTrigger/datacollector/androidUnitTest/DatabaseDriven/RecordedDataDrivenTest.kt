package de.leo.smartTrigger.datacollector.androidUnitTest.DatabaseDriven

import android.app.Instrumentation
import android.app.Instrumentation.ActivityMonitor
import androidx.test.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import android.util.Log
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.manage.NaturalTriggerJitai
import de.leo.smartTrigger.datacollector.testUtil.TestDatabase
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.list.TriggerManagingActivity
import de.leo.smartTrigger.datacollector.ui.notifications.FullscreenJitai
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.jetbrains.anko.defaultSharedPreferences
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import kotlin.math.max
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
    var am: ActivityMonitor? = null

    @Before
    fun setup() {
        db = JitaiDatabase.getInstance(targetContext)
        db.close()
        targetContext.deleteDatabase(JitaiDatabase.NAME
                                    )
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
        val sensorDataSets = setUpDbAndGetSensorDataSets("testdb.sql", 1018, 1025).take(100)

        val trigger = testDb.getAllActiveNaturalTriggerJitai()
        assertTrue(trigger.size > 0)
        val hits = MutableList(trigger.size) { i -> HitResult(trigger[i], 0) }
        sensorDataSets.forEach { sensorData ->
            trigger.forEachIndexed { index, it ->
                if (it.check(sensorData)) {
                    hits[index].hits++
                }
            }
        }
        val jitaiEvents = trigger.map { db.getJitaiEvents(it.id) }
        assertTrue(jitaiEvents.size > 0)
        //more than one hit
        assertTrue(hits.sumBy { it.hits } > 0)
    }

    @Test
    fun testAllSensorDataTest() {
        val sensorDataSets = setUpDbAndGetSensorDataSets("testdb.sql", 1018, 1025)

        val trigger = testDb.getAllActiveNaturalTriggerJitai()
        assertTrue(trigger.size > 0)

        val hits = MutableList(trigger.size) { i -> HitResult(trigger[i], 0) }
        sensorDataSets.forEachIndexed { i, sensorData ->
            if (i % 100 == 0) {
                Log.d("allSensorDataTest",
                      "${(i.toFloat() / sensorDataSets.size.toFloat() * 100).roundToInt()} % done")
            }
            trigger.forEachIndexed { index, it ->
                if (it.check(sensorData)) {
                    hits[index].hits++
                }
            }
        }
        val jitaiEvents = trigger.map { db.getJitaiEvents(it.id) }
        assertTrue(jitaiEvents.size > 0)
        //more than one hit
        assertTrue(hits.sumBy { it.hits } > 0)
    }

    @Test
    fun testLinksRechts() {
        val sensorDataSets = setUpDbAndGetSensorDataSets("testData_links_rechts.sql", 1025, 1025)
        val trigger = testDb.getAllActiveNaturalTriggerJitai()
        assertTrue(trigger.size > 0)
        val hits = MutableList(trigger.size) { i -> HitResult(trigger[i], 0) }
        sensorDataSets.forEachIndexed { i, sensorData ->
            if (i % 100 == 0) {
                Log.d("links_rechts",
                      "${(i.toFloat() / sensorDataSets.size.toFloat() * 100).roundToInt()} % done")
            }
            trigger.forEachIndexed { index, it ->
                if (it.check(sensorData))
                    hits[index].hits++
            }
        }
        val jitaiEvents = trigger.map { db.getJitaiEvents(it.id) }

        assertTrue(jitaiEvents.size > 0)
        //more than one hit
        assertTrue(hits.sumBy { it.hits } > 0)
    }

    @Test
    fun testLinksRechtsIndividualJitai() {
        val sensorDataSets = setUpDbAndGetSensorDataSets("testData_links_rechts.sql", 1025, 1025)
        val trigger = testDb.getAllActiveNaturalTriggerJitai()
        db.insertSensorDataBatch(sensorDataSets)
        assertTrue(trigger.size > 0)
        val hits = mutableListOf<HitResult>()
        trigger.forEachIndexed { index, trigger ->
            val hitResult = HitResult(trigger, 0)
            sensorDataSets.forEachIndexed { i, sensorData ->
                if (trigger.check(sensorData)) {
                    hitResult.hits++
                    hitResult.timestamps.add(sensorData.time)
                }
            }
            hits.add(hitResult)
        }
        hits.forEach {
            Log.d("trigger ${it.trigger.naturalTriggerModel.ID}", "${it.hits} hits on " +
                "${it.trigger.naturalTriggerModel} at ${it.timestamps}")
        }
        val jitaiEvents = trigger.map { db.getJitaiEvents(it.id) }
        assertTrue(jitaiEvents.filter { it.isNotEmpty() }.size > 0)
        linksRechtsExpectedResults.forEachIndexed { index, expected ->
            assertEquals("${hits[index].trigger.naturalTriggerModel} at ${expected.timestamps}",
                         expected.id, hits[index].trigger.id)
            assertEquals("${hits[index].trigger.naturalTriggerModel} at ${expected.timestamps}",
                         expected.hits, hits[index].hits)
            assertEquals("${hits[index].trigger.naturalTriggerModel} at ${expected.timestamps}",
                         expected.timestamps, hits[index].timestamps)
        }
    }

    @Test
    fun testLinksRechts50() {
        val sensorDataSets = setUpDbAndGetSensorDataSets("testData_links_rechts.sql", 1025, 1025)
        //sit for 120000 ms dwell inside for 300000 ms links from 00:00 to 23:59
        val trigger = testDb.getActiveNaturalTriggerJitai(50)
        val hits = mutableListOf<HitResult>()
        trigger.let {
            var count = 0
            sensorDataSets.forEachIndexed { i, sensorData ->
                if (it!!.check(sensorData)) count++
            }
            hits.add(HitResult(it!!, count))
        }
        //no hit in sensor data
        assertEquals(0, hits[0].hits)
    }

    @Test
    fun testSit2MinDwellInside5MinWlan() {
        //1539878328577, 1539878738637 == sit BadeWannenWlan
        val sensorDataSets = setUpDbAndGetSensorDataSets("testData_links_rechts.sql", 1025, 1025,
                                                         1539878328577, 1539878738637)
        //sit for 120000 ms dwell inside for 300000 ms BadewannenWlan from 00:00 to 23:59
        val trigger = testDb.getActiveNaturalTriggerJitai(66)
        val hits = HitResult(trigger!!, 0)
        trigger.let {
            sensorDataSets.forEachIndexed { i, sensorData ->
                if (it.check(sensorData)) {
                    hits.hits++
                    hits.timestamps.add(sensorData.time)
                }
            }
        }
        //one hit
        assertEquals(1, hits.hits)
    }

    @Test
    fun testWalk0and5MinDwellOutside5MinBadeWannenWlan() {
        val sensorDataSets = setUpDbAndGetSensorDataSets("testData_links_rechts.sql", 1025, 1025)
        //sit for 120000 ms dwell inside for 300000 ms BadewannenWlan from 00:00 to 23:59
        val trigger0Min = testDb.getActiveNaturalTriggerJitai(91)!!
        val hits0Min = HitResult(trigger0Min, 0)
        trigger0Min.let {
            sensorDataSets.forEachIndexed { i, sensorData ->
                if (it.check(sensorData)) {
                    hits0Min.hits++
                    hits0Min.timestamps.add(sensorData.time)
                }
            }
        }
        //9 hits
        assertEquals(3, hits0Min.hits)

        val trigger5min = testDb.getActiveNaturalTriggerJitai(92)!!
        val hits5min = HitResult(trigger5min, 0)
        trigger5min.let {
            sensorDataSets.forEachIndexed { i, sensorData ->
                if (it.check(sensorData)) {
                    hits5min.hits++
                    hits5min.timestamps.add(sensorData.time)
                }
            }
        }
        //one hit
        assertEquals(2, hits5min.hits)
    }

    @Test
    fun testWalk2MindwellOutside5Minlinks() {
        val sensorDataSets = setUpDbAndGetSensorDataSets("testData_links_rechts.sql", 1025, 1025)
        //sit for 120000 ms dwell inside for 300000 ms BadewannenWlan from 00:00 to 23:59
        val trigger = testDb.getActiveNaturalTriggerJitai(76)
        val hits = HitResult(trigger!!, 0)
        trigger.let {
            sensorDataSets.forEachIndexed { i, sensorData ->
                if (it.check(sensorData)) {
                    hits.hits++
                    hits.timestamps.add(sensorData.time)
                }
            }
        }
        //two hits
        assertEquals(2, hits.hits)
    }

    @Test
    fun testS8VsAsusVsJ5() {
        val sensorDataSetsS8 = setUpDbAndGetSensorDataSets("s8TestDataKaufi.sql", 1025, 1025)
        val triggerS8 = testDb.getAllActiveNaturalTriggerJitai()
        val jitaiEventsS8 = triggerS8.map { db.getJitaiEvents(it.id) }
        TestDatabase.reset()
        val sensorDataSetsAsus = setUpDbAndGetSensorDataSets("asusTestDataKaufi.sql", 1025, 1025)
        val triggerAsus = testDb.getAllActiveNaturalTriggerJitai()
        val jitaiEventsAsus = triggerS8.map { db.getJitaiEvents(it.id) }
        TestDatabase.reset()
        val sensorDataSetsJ5 = setUpDbAndGetSensorDataSets("j5TestDataKaufi.sql", 1025, 1025)
        val triggerJ5 = testDb.getAllActiveNaturalTriggerJitai()
        val jitaiEventsJ5 = triggerS8.map { db.getJitaiEvents(it.id) }
        //sit for 120000 ms dwell inside for 300000 ms BadewannenWlan from 00:00 to 23:59
        val hitsS8 = mutableListOf<HitResult>()
        triggerS8.forEachIndexed { index, trigger ->
            val hitResult = HitResult(trigger, 0)
            sensorDataSetsS8.forEachIndexed { i, sensorData ->
                if (trigger.check(sensorData)) {
                    hitResult.hits++
                    hitResult.timestamps.add(sensorData.time)
                }
            }
            hitsS8.add(hitResult)
        }
        val hitsAsus = mutableListOf<HitResult>()
        triggerAsus.forEachIndexed { index, trigger ->
            val hitResult = HitResult(trigger, 0)
            sensorDataSetsAsus.forEachIndexed { i, sensorData ->
                if (trigger.check(sensorData)) {
                    hitResult.hits++
                    hitResult.timestamps.add(sensorData.time)
                }
            }
            hitsAsus.add(hitResult)
        }
        val hitsJ5 = mutableListOf<HitResult>()
        triggerJ5.forEachIndexed { index, trigger ->
            val hitResult = HitResult(trigger, 0)
            sensorDataSetsJ5.forEachIndexed { i, sensorData ->
                if (trigger.check(sensorData)) {
                    hitResult.hits++
                    hitResult.timestamps.add(sensorData.time)
                }
            }
            hitsJ5.add(hitResult)
        }
        val padding = max(max(hitsS8.fold(0) { x, it -> max(x, it.timestamps.size) }, hitsAsus
            .fold(0) { x, it -> max(x, it.timestamps.size) }), hitsJ5.fold(0) { x, it ->
            max(x, it.timestamps
                .size)
        })
        hitsS8.forEachIndexed { index, it ->
            Log.i("hits: ",
                  "${it.trigger.naturalTriggerModel.ID} ${it.trigger.naturalTriggerModel} " +
                      "s8 ${it.hits} ${it.timestamps} ${", ".repeat(padding - it.timestamps.size)}" +
                      "asus ${hitsAsus[index].hits} ${hitsAsus[index].timestamps} ${", ".repeat
                      (padding - hitsAsus[index].timestamps.size)}" +
                      "j5 ${hitsJ5[index].hits} ${hitsJ5[index].timestamps}")
            /*Assert.assertEquals(
                "For trigger ${it.trigger.naturalTriggerModel} hits s8: ${it.hits}, asus: ${hitsAsus[index].hits}",
                it.hits, hitsAsus[index].hits)
            Assert.assertEquals(
                "For trigger ${it.trigger.naturalTriggerModel} hits s8: ${it.hits}, j5: ${hitsAsus[index].hits}",
                it.hits, hitsJ5[index].hits)*/
        }
    }


    private fun setUpDbAndGetSensorDataSets(dbName: String, oldVersion: Int, newVersion: Int):
        List<SensorDataSet> {
        testDb = TestDatabase.getInstance(dbName, oldVersion, newVersion, testContext)
        val sensorDataSets = testDb.getAllSensorDataSets()
        testDb.swapContext(targetContext)
        db.insertSensorDataBatch(sensorDataSets)
        return sensorDataSets
    }

    private fun setUpDbAndGetSensorDataSets(dbName: String, oldVersion: Int, newVersion: Int,
                                            begin: Long, end: Long):
        List<SensorDataSet> {
        testDb = TestDatabase.getInstance(dbName, oldVersion, newVersion, testContext)
        val sensorDataSets = testDb.getSensorDataSets(begin, end)
        testDb.swapContext(targetContext)
        db.insertSensorDataBatch(sensorDataSets)
        return sensorDataSets
    }

    data class HitResult(val trigger: NaturalTriggerJitai, var hits: Int, val timestamps:
    MutableList<Long> =
        mutableListOf())

    val linksRechtsExpectedResults = listOf(ExpectedResult(1, 0, emptyList()),
                                            ExpectedResult(2, 0, emptyList()),
                                            ExpectedResult(3, 0, emptyList()),
                                            ExpectedResult(4, 0, emptyList()),
                                            ExpectedResult(5, 1, listOf(1539880919001)),
                                            ExpectedResult(6, 1, listOf(1539880919001)),
                                            ExpectedResult(7, 0, emptyList()),
                                            ExpectedResult(8, 0, emptyList()),
                                            ExpectedResult(9, 1, listOf(1539878348580)),
                                            ExpectedResult(10, 0, emptyList()),
                                            ExpectedResult(11, 0, emptyList()),
                                            ExpectedResult(12, 0, emptyList()),
                                            ExpectedResult(13, 1, listOf(1539880438949)),
                                            ExpectedResult(14, 0, emptyList()),
                                            ExpectedResult(15, 0, emptyList()),
                                            ExpectedResult(16, 0, emptyList()),
                                            ExpectedResult(17, 1, listOf(1539878338577)),
                                            ExpectedResult(18, 0, emptyList()),
                                            ExpectedResult(19, 1, listOf(1539879198700)),
                                            ExpectedResult(20, 1, listOf(1539879198700)),
                                            ExpectedResult(21, 0, emptyList()),
                                            ExpectedResult(22, 0, emptyList()),
                                            ExpectedResult(23, 0, emptyList()),
                                            ExpectedResult(24, 0, emptyList()),
                                            ExpectedResult(25, 0, emptyList()),
                                            ExpectedResult(26, 0, emptyList()),
                                            ExpectedResult(27, 0, emptyList()),
                                            ExpectedResult(28, 0, emptyList()),
                                            ExpectedResult(29, 0, emptyList()),
                                            ExpectedResult(30, 0, emptyList()),
                                            ExpectedResult(31, 0, emptyList()),
                                            ExpectedResult(32, 0, emptyList()),
                                            ExpectedResult(33, 0, emptyList()),
                                            ExpectedResult(34, 0, emptyList()),
                                            ExpectedResult(35, 0, emptyList()),
                                            ExpectedResult(36, 0, emptyList()),
                                            ExpectedResult(37, 1, listOf(1539880898999)),
                                            ExpectedResult(38, 1, listOf(1539880898999)),
                                            ExpectedResult(39, 0, emptyList()),
                                            ExpectedResult(40, 0, emptyList()),
                                            ExpectedResult(41, 0, emptyList()),
                                            ExpectedResult(42, 0, emptyList()),
                                            ExpectedResult(43, 1, listOf(1539879188700)),
                                            ExpectedResult(44, 1, listOf(1539879188700)),
                                            ExpectedResult(45, 0, emptyList()),
                                            ExpectedResult(46, 0, emptyList()),
                                            ExpectedResult(47, 0, emptyList()),
                                            ExpectedResult(48, 0, emptyList()),
                                            ExpectedResult(49, 0, emptyList()),
                                            ExpectedResult(50, 0, emptyList()),
                                            ExpectedResult(51, 1, listOf(1539881279189)),
                                            ExpectedResult(52, 1, listOf(1539881429204)),
                                            ExpectedResult(53,
                                                           2,
                                                           listOf(1539881219164, 1539881549059)),
                                            ExpectedResult(54, 1, listOf(1539881219164)),
                                            ExpectedResult(55, 0, emptyList()),
                                            ExpectedResult(56, 0, emptyList()),
                                            ExpectedResult(57, 1, listOf(1539878648626)),
                                            ExpectedResult(58, 1, listOf(1539878648626)),
                                            ExpectedResult(59,
                                                           2,
                                                           listOf(1539878748638, 1539879058684)),
                                            ExpectedResult(60,
                                                           2,
                                                           listOf(1539878858660, 1539879168697)),
                                            ExpectedResult(61, 1, listOf(1539880738985)),
                                            ExpectedResult(62, 1, listOf(1539880738985)),
                                            ExpectedResult(63, 0, emptyList()),
                                            ExpectedResult(64, 0, emptyList()),
                                            ExpectedResult(65, 1, listOf(1539878638625)),
                                            ExpectedResult(66, 1, listOf(1539878638625)),
                                            ExpectedResult(67,
                                                           2,
                                                           listOf(1539878748638, 1539879058684)),
                                            ExpectedResult(68,
                                                           2,
                                                           listOf(1539878858660, 1539879168697)),
                                            ExpectedResult(69, 0, emptyList()),
                                            ExpectedResult(70, 0, emptyList()),
                                            ExpectedResult(71, 0, emptyList()),
                                            ExpectedResult(72, 0, emptyList()),
                                            ExpectedResult(73, 1, listOf(1539878628623)),
                                            ExpectedResult(74, 1, listOf(1539878628623)),
                                            ExpectedResult(75,
                                                           3,
                                                           listOf(1539878748638,
                                                                  1539879058684,
                                                                  1539880108917)),
                                            ExpectedResult(76,
                                                           2,
                                                           listOf(1539878858660, 1539879168697)),
                                            ExpectedResult(77,
                                                           3,
                                                           listOf(1539880048908,
                                                                  1539880358944,
                                                                  1539880668977)),
                                            ExpectedResult(78,
                                                           3,
                                                           listOf(1539880158922,
                                                                  1539880528957,
                                                                  1539880838996)),
                                            ExpectedResult(79, 0, emptyList()),
                                            ExpectedResult(80, 0, emptyList()),
                                            ExpectedResult(81, 0, emptyList()),
                                            ExpectedResult(82, 0, emptyList()),
                                            ExpectedResult(83,
                                                           2,
                                                           listOf(1539880348943, 1539881279189)),
                                            ExpectedResult(84, 1, listOf(1539881429204)),
                                            ExpectedResult(85,
                                                           3,
                                                           listOf(1539880328943,
                                                                  1539881199162,
                                                                  1539881549059)),
                                            ExpectedResult(86,
                                                           2,
                                                           listOf(1539880328943, 1539881199162)),
                                            ExpectedResult(87, 0, emptyList()),
                                            ExpectedResult(88, 0, emptyList()),
                                            ExpectedResult(89, 0, emptyList()),
                                            ExpectedResult(90, 0, emptyList()),
                                            ExpectedResult(91,
                                                           3,
                                                           listOf(1539880348943,
                                                                  1539881019137,
                                                                  1539881329192)),
                                            ExpectedResult(92, 1, listOf(1539881429204)),
                                            ExpectedResult(93,
                                                           4,
                                                           listOf(1539880328943,
                                                                  1539880638971,
                                                                  1539880949003,
                                                                  1539881259167)),
                                            ExpectedResult(94,
                                                           4,
                                                           listOf(1539880328943,
                                                                  1539880638971,
                                                                  1539880949003,
                                                                  1539881259167)),
                                            ExpectedResult(95,
                                                           0,
                                                           emptyList()),
                                            ExpectedResult(96, 0, emptyList()))

}

class ExpectedResult(val id: Int, val hits: Int, val timestamps: List<Long>)

fun sitBadewannenWlan(db: JitaiDatabase) = db.getSensorDataSets(1539878328577, 1539878738637)
fun walkBadewannenWlan(db: JitaiDatabase) = db.getSensorDataSets(1539878748638, 1539879288826)
fun bikeAnBahnstrecke(db: JitaiDatabase) = db.getSensorDataSets(1539880028783, 1539878738637)
