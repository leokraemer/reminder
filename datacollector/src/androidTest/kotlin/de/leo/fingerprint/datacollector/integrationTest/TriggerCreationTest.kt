package de.leo.fingerprint.datacollector.integrationTest

import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingRegistry
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.testUtil.EspressoTestsMatchers.withDrawable
import de.leo.fingerprint.datacollector.testUtil.MultiSliderActions.setThumbValue
import de.leo.fingerprint.datacollector.testUtil.ViewPagerIdlingResource
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.HOME_CODE
import kotlinx.android.synthetic.main.activity_natural_trigger_tabs.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TriggerCreationTest {

    @Rule
    @JvmField
    val mActivityRule: ActivityTestRule<CreateTriggerActivity> = ActivityTestRule(
        CreateTriggerActivity::class.java)

    val GOAL = "goal"
    val MESSAGE = "message"
    val SITUATION = "test"


    private val GEOFENCE_NAME = "testGeofence"

    @Before
    fun setup() {
        IdlingRegistry.getInstance()
            .register(ViewPagerIdlingResource(mActivityRule.activity.viewPager, "viewpager"))
        JitaiDatabase.getInstance(getTargetContext()).enterGeofence(0,
                                                                    GEOFENCE_NAME,
                                                                    LatLng(0.0, 0.0),
                                                                    0f,
                                                                    true,
                                                                    false,
                                                                    false,
                                                                    false,
                                                                    1L,
                                                                    HOME_CODE)
    }


    @Test
    fun createSimpleTrigger() {
        onView(withId(R.id.goal_edittext)).perform(typeText(GOAL), pressImeActionButton())
        onView(withId(R.id.message_edittext)).perform(typeText(MESSAGE), pressImeActionButton())
        //next page
        onView(withId(R.id.situation_edittext)).perform(typeText(SITUATION), pressImeActionButton())
        //next page
        onView(withId(R.id.homeGeofenceButton)).perform(click())
        onView(withId(R.id.geofenceName)).check(matches(withText(GEOFENCE_NAME)))
        onView(withId(R.id.geofenceIcon)).check(matches(withDrawable(R.drawable.ic_home_white_48dp)))
        //next page
        onView(withId(R.id.next_page)).perform(click())
        onView(withId(R.id.geofenceIcon)).check(matches(withDrawable(R.drawable.ic_home_white_48dp)))
        onView(withId(R.id.exitButton)).perform(click())
        onView(withId(R.id.geofenceDirection)).check(matches(withDrawable(R.drawable.ic_exit_geofence_fat_arrow_white)))

        //next page
        onView(withId(R.id.next_page)).perform(click())
        onView(withId(R.id.sitButton)).perform(click())
        onView(withId(R.id.activity1)).check(matches(withDrawable(R.drawable.ic_airline_seat_recline_normal_white_48dp)))

        //next page
        onView(withId(R.id.next_page)).perform(click())
        onView(withId(R.id.time_range_slider)).perform(
            setThumbValue(0, TimeUnit.HOURS.toMinutes(10).toInt()))
        onView(withId(R.id.time_range_slider)).perform(
            setThumbValue(1, TimeUnit.HOURS.toMinutes(11).toInt()))
        onView(withId(R.id.timeView)).check(matches(withText("10:00-\n11:00")))

    }
}