package de.leo.smartTrigger.datacollector.integrationTest

import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.list.TriggerManagingActivity
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceTest {

    @Rule
    @JvmField
    val mActivityRule: ActivityTestRule<TriggerManagingActivity> = ActivityTestRule(TriggerManagingActivity::class.java)

    lateinit var db : JitaiDatabase

    @Before
    fun setup() {
        db = JitaiDatabase.getInstance(InstrumentationRegistry.getTargetContext())
        InstrumentationRegistry.getTargetContext().deleteDatabase(JitaiDatabase.NAME)
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun testStart_StopService() {
        onView(withId(R.id.button_start_service)).check(matches(isEnabled()))
        onView(withId(R.id.button_stop_service)).check(matches(not(isEnabled())))
        //start
        onView(withId(R.id.button_start_service)).perform(click())
        onView(withId(R.id.button_start_service)).check(matches(not(isEnabled())))
        onView(withId(R.id.button_stop_service)).check(matches(isEnabled()))
        //stop
        onView(withId(R.id.button_stop_service)).perform(click())
        onView(withId(R.id.button_start_service)).check(matches(isEnabled()))
        onView(withId(R.id.button_stop_service)).check(matches(not(isEnabled())))
    }
}