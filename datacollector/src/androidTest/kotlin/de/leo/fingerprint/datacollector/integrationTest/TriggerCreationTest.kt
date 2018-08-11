package de.leo.fingerprint.datacollector.integrationTest

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.testUtil.EspressoTestsMatchers.withDrawable
import de.leo.fingerprint.datacollector.ui.EntryActivity
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity
import org.hamcrest.CoreMatchers.not

@RunWith(AndroidJUnit4::class)
class TriggerCreationTest {

    @Rule
    @JvmField
    val mActivityRule: ActivityTestRule<CreateTriggerActivity> = ActivityTestRule(
        CreateTriggerActivity::class.java)

    val GOAL = "goal"
    val MESSAGE = "message"
    val SITUATION = "test"

    @Test
    fun testStart_StopService() {
        onView(withId(R.id.goal_edittext)).perform(typeText(GOAL))
        onView(withId(R.id.message_edittext)).perform(typeText(MESSAGE))
        //next page
        onView(withId(R.id.next_page)).perform(click())
        onView(withId(R.id.situation_edittext)).perform(typeText(SITUATION))
        //next page
        onView(withId(R.id.next_page)).perform(click())
        onView(withId(R.id.homeGeofenceButton)).perform(click())
        onView(withId(R.id.geofenceIcon)).check(matches(withDrawable(R.drawable.ic_home_white_48dp)))
        //next page
        onView(withId(R.id.next_page)).perform(click())
        onView(withId(R.id.exitButton)).perform(click())
        onView(withId(R.id.geofenceDirection)).check(matches(withDrawable(R.drawable.ic_exit_geofence_fat_arrow_white2)))

        //next page
        onView(withId(R.id.next_page)).perform(click())


        //next page
        onView(withId(R.id.next_page)).perform(click())


        //next page
        onView(withId(R.id.next_page)).perform(click())
    }
}