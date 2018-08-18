package de.leo.fingerprint.datacollector.integrationTest

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingRegistry
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isEnabled
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.testUtil.ViewPagerIdlingResource
import de.leo.fingerprint.datacollector.ui.EntryActivity
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.HOME_CODE
import kotlinx.android.synthetic.main.activity_natural_trigger_tabs.*
import org.hamcrest.CoreMatchers.not
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceTest {

    @Rule
    @JvmField
    val mActivityRule: ActivityTestRule<EntryActivity> = ActivityTestRule(EntryActivity::class.java)

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