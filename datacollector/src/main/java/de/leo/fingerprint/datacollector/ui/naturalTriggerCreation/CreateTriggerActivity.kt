package de.leo.fingerprint.datacollector.ui.naturalTriggerCreation

import android.graphics.drawable.Drawable
import android.net.wifi.ScanResult
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup.LayoutParams
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import de.leo.fingerprint.datacollector.ui.naturalTriggerCreation.location.LocationFinish
import de.leo.fingerprint.datacollector.ui.naturalTriggerCreation.location.LocationSelection
import de.leo.fingerprint.datacollector.ui.naturalTriggerCreation.location.GeofenceDialogListener
import de.leo.fingerprint.datacollector.ui.naturalTriggerCreation.location.WifiDialogListener
import de.leo.fingerprint.datacollector.ui.uiElements.LockableViewPager
import kotlinx.android.synthetic.main.activity_natural_trigger_tabs.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.threeten.bp.format.DateTimeFormatter


/**
 * Created by Leo on 01.03.2018.
 */
class CreateTriggerActivity : GeofenceDialogListener,
                              WifiDialogListener,
                              NaturalTrigger.ModelChangedListener,
                              AppCompatActivity() {

    override fun modelChangedCallback() {
        with(model) {
            if (geofence != null) {
                if (geofence!!.imageResId > -1)
                    geofenceIcon.setImageDrawable(
                        resources.obtainTypedArray(R.array.geofence_icons)
                            .getDrawable(geofence!!.imageResId))
                else
                    geofenceIcon.setImageDrawable(null)
                geofenceName.setText(geofence!!.name)
                if (geofence!!.enter)
                    geofenceDirection.setImageDrawable(
                        resources.getDrawable(R.drawable.ic_enter_geofence_fat_arrow_white2))
                else
                    geofenceDirection.setImageDrawable(
                        resources.getDrawable(R.drawable.ic_exit_geofence_fat_arrow_white))
            } else {
                geofenceIcon.setImageResource(R.drawable.ic_public_white_48dp)
                geofenceDirection.setImageDrawable(null)
                geofenceName.setText("Überall")
            }
            if (checkActivity(JITAI_ACTIVITY.SIT)) {
                activity1.setImageDrawable(resources.getDrawable(R.drawable.ic_airline_seat_recline_normal_white_48dp))
                activity2.setImageDrawable(null)
                activity3.setImageDrawable(null)
            } else {
                val walk = checkActivity(JITAI_ACTIVITY.WALK)
                val bike = checkActivity(JITAI_ACTIVITY.BIKE)
                val bus = checkActivity(JITAI_ACTIVITY.BUS)
                val car = checkActivity(JITAI_ACTIVITY.CAR)
                activity1.setImageDrawable(null)
                activity2.setImageDrawable(null)
                activity3.setImageDrawable(null)
                if (walk && bike && car && bus) {
                } else if (walk) {
                    activity1.setImageDrawable(walkIcon)
                    if (bike) {
                        activity2.setImageDrawable(bikeIcon)
                        if (bus)
                            activity3.setImageDrawable(busIcon)
                        else if (car)
                            activity3.setImageDrawable(carIcon)
                    } else if (bus) {
                        activity2.setImageDrawable(busIcon)
                        if (car)
                            activity3.setImageDrawable(carIcon)
                    } else if (car)
                        activity2.setImageDrawable(carIcon)
                } else if (bike) {
                    activity1.setImageDrawable(bikeIcon)
                    if (bus)
                        activity2.setImageDrawable(busIcon)
                    else if (car)
                        activity2.setImageDrawable(carIcon)
                } else if (bus) {
                    activity1.setImageDrawable(busIcon)
                    if (car)
                        activity2.setImageDrawable(carIcon)
                } else if (car)
                    activity1.setImageDrawable(carIcon)
            }
            val beginTime = model.beginTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            val endTime = model.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            timeView.setText(beginTime + "-\n" + endTime)

            //enable/disable view paging
            if (mPager!!.currentItem == 0
                && (model.goal.isNullOrEmpty() || model.message.isNullOrEmpty())) {
                mPager?.setPagingEnabled(false)
            } else if (mPager!!.currentItem == 1 && model.situation.isNullOrEmpty()) {
                mPager!!.setPagingEnabled(false)
            } else {
                mPager?.setPagingEnabled(true)
            }
        }
        //update child views
        activitySelection.updateView()
        locationSelection.updateView()
        locationFinish.updateView()
        goalSelection.updateView()
        situationSelection.updateView()
        timeSelection.updateView()
    }

    lateinit var sitIcon: Drawable
    lateinit var walkIcon: Drawable
    lateinit var bikeIcon: Drawable
    lateinit var busIcon: Drawable
    lateinit var carIcon: Drawable

    private val model: NaturalTrigger = NaturalTrigger()
    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private var NUM_PAGES = 6

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private var mPager: LockableViewPager? = null

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private var mPagerAdapter: PagerAdapter? = null

    //Fragments
    private val goalSelection = GoalSelection()
    private val situationSelection = SituationSelection()
    private val locationSelection = LocationSelection()
    private val locationFinish = LocationFinish()
    private val activitySelection = ActivitySelection()
    private val timeSelection = TimeSelection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_natural_trigger_tabs)

        sitIcon = resources.getDrawable(R.drawable
                                            .ic_airline_seat_recline_normal_white_48dp)
        walkIcon = resources.getDrawable(R.drawable.ic_directions_walk_white_48dp)
        bikeIcon = resources.getDrawable(R.drawable.ic_directions_bike_white_48dp)
        busIcon = resources.getDrawable(R.drawable.ic_directions_bus_white_48dp)
        carIcon = resources.getDrawable(R.drawable.ic_directions_car_white_48dp)


        // Instantiate a ViewPager and a PagerAdapter.
        mPager = viewPager
        mPagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        mPager!!.adapter = mPagerAdapter
        mPager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            var scrollState = ViewPager.SCROLL_STATE_IDLE
            override fun onPageScrollStateChanged(state: Int) {
                scrollState = state
            }

            override fun onPageScrolled(position: Int,
                                        positionOffset: Float,
                                        positionOffsetPixels: Int) {
                if (position == 2) {
                    expand(reminder_card, positionOffset)
                }

            }

            override fun onPageSelected(position: Int) {
                //for onResume
                if (position >= 3 && scrollState == ViewPager.SCROLL_STATE_IDLE) {
                    expand(reminder_card, 1f)
                }
                if (position > NUM_PAGES)
                    finish()
            }
        })
        previous_page.onClick { onBackPressed() }
        next_page.onClick {
            if (model.checkState(mPager!!.currentItem)) {
                mPager!!.currentItem++
            }
        }
        model.modelChangelListener = this
        goalSelection.model = model
        situationSelection.model = model
        locationFinish.model = model
        locationSelection.model = model
        activitySelection.model = model
        timeSelection.model = model
        //kick off view initialisation
        modelChangedCallback()
    }

    fun expand(v: View, positionOffset: Float) {
        v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val targetHeight = v.measuredHeight
        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        v.layoutParams.height = if (positionOffset >= 1f)
            LayoutParams.WRAP_CONTENT
        else
            (targetHeight * positionOffset).toInt()
        v.requestLayout()
    }

    override fun onBackPressed() {
        if (mPager!!.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            mPager!!.currentItem = mPager!!.currentItem - 1
        }
    }


    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0    -> goalSelection
                1    -> situationSelection
                2    -> locationSelection
                3    -> locationFinish
                4    -> activitySelection
                5    -> timeSelection
                else -> timeSelection // error
            }
        }

        override fun getCount(): Int {
            return NUM_PAGES
        }
    }

    override fun onNoGeofenceSelected() {
        model?.geofence = null
    }

    override fun onGeofenceSelected(geofence: MyGeofence) {
        model?.geofence = geofence
    }

    override fun onWifiSelected(wifi: ScanResult) {
        model?.wifi = wifi
    }

    override fun onNoWifiSelected() {
        model?.wifi = null
    }
}
