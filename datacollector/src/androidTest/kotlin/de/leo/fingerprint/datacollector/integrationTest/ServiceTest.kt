package de.leo.fingerprint.datacollector.integrationTest

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.ui.EntryActivity
import org.hamcrest.CoreMatchers.not

@RunWith(AndroidJUnit4::class)
class ChangeTextBehaviorTest {

    @Rule
    @JvmField
    val mActivityRule: ActivityTestRule<EntryActivity> = ActivityTestRule(EntryActivity::class.java)

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