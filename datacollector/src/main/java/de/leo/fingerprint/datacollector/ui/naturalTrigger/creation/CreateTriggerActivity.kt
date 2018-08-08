package de.leo.fingerprint.datacollector.ui.naturalTrigger.creation

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
import de.leo.fingerprint.datacollector.datacollection.DataCollectorService
import de.leo.fingerprint.datacollector.datacollection.database.JITAI_ID
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.models.WifiInfo
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import de.leo.fingerprint.datacollector.jitai.MyWifiGeofence
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.EVERYWHERE
import de.leo.fingerprint.datacollector.ui.uiElements.LockableViewPager
import de.leo.fingerprint.datacollector.utils.UPDATE_JITAI
import kotlinx.android.synthetic.main.activity_natural_trigger_tabs.*
import kotlinx.android.synthetic.main.naturaltriggerview.*
import kotlinx.android.synthetic.main.naturaltriggerview.view.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast
import org.threeten.bp.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


/**
 * Created by Leo on 01.03.2018.
 */
class CreateTriggerActivity : GeofenceDialogListener,
                              WifiDialogListener,
                              NaturalTriggerModel.ModelChangedListener,
                              AppCompatActivity() {

    companion object {
        const val EDIT = "edit"
        const val EDIT_COPY = "edit_copy"
        const val NATURALTRIGGER_ID = "natural_trigger_id"
    }

    val db by lazy { JitaiDatabase.getInstance(this) }

    private lateinit var model: NaturalTriggerModel
    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private var NUM_PAGES = 6

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private var lockableViewPager: LockableViewPager? = null

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private var pagerAdapter: PagerAdapter? = null

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
        setTheme(R.style.AppBaseTheme_Fullscreen)
        val intent = getIntent()
        val naturalTriggerId = intent.getIntExtra(NATURALTRIGGER_ID, -1)
        if (naturalTriggerId != -1) {
            model = db.getNaturalTrigger(naturalTriggerId)
            if (intent.action == EDIT_COPY) {
                model.ID = -1
            }
        } else {
            model = NaturalTriggerModel()
        }
        // Instantiate a ViewPager and a PagerAdapter.
        lockableViewPager = viewPager
        pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        lockableViewPager!!.adapter = pagerAdapter
        lockableViewPager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            var scrollState = ViewPager.SCROLL_STATE_IDLE
            override fun onPageScrollStateChanged(state: Int) {
                scrollState = state
            }

            override fun onPageScrolled(position: Int,
                                        positionOffset: Float,
                                        positionOffsetPixels: Int) {
                if (position == 2) {
                    expand(reminder_card, positionOffset)
                } else if (position >= 3) {
                    expand(reminder_card, 1f)
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
        next_page.onClick { nextButtonClick() }
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

    override fun modelChangedCallback() {
        updateNaturalTriggerReminderCardView(model, reminder_card)
        pagerAdapter?.notifyDataSetChanged()
        //enable/disable view paging
        if (lockableViewPager!!.currentItem == 0
            && (model.goal.isNullOrEmpty() || model.message.isNullOrEmpty())) {
            lockableViewPager?.setPagingEnabled(false)
        } else if (lockableViewPager!!.currentItem == 1 && model.situation.isNullOrEmpty()) {
            lockableViewPager!!.setPagingEnabled(false)
        } else {
            lockableViewPager?.setPagingEnabled(true)
        }
        //update child views
        activitySelection.updateView()
        locationSelection.updateView()
        locationFinish.updateView()
        goalSelection.updateView()
        situationSelection.updateView()
        timeSelection.updateView()
    }

    private fun nextButtonClick() {
        //last page
        if (lockableViewPager!!.currentItem == lockableViewPager!!.adapter!!.count - 1) {
            val id = db.enterNaturalTrigger(model)
            toast("Erinnerung erfolgreich erstellt")
            startService(intentFor<DataCollectorService>()
                             .setAction(UPDATE_JITAI)
                             .putExtra(JITAI_ID, id)
                        )
            super.onBackPressed()
        }
        lockableViewPager!!.currentItem++
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
        if (lockableViewPager!!.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            lockableViewPager!!.currentItem = lockableViewPager!!.currentItem - 1
        }
    }


    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {
        private val showGeofenceDwellTimeSelection = (model.geofence?.name != EVERYWHERE
            && (model.geofence?.dwellOutside == true || model.geofence?.dwellInside == true))

        override fun getItem(position: Int): Fragment =
            if (showGeofenceDwellTimeSelection)
                when (position) {
                    0    -> goalSelection
                    1    -> situationSelection
                    2    -> locationSelection
                    3    -> locationFinish
                    4    -> activitySelection
                    5    -> timeSelection
                    else -> timeSelection // error
                } else
                when (position) {
                    0    -> goalSelection
                    1    -> situationSelection
                    2    -> locationSelection
                    3    -> activitySelection
                    4    -> timeSelection
                    else -> timeSelection // error
                }

        override fun getCount(): Int =
            if (showGeofenceDwellTimeSelection)
                NUM_PAGES
            else
                NUM_PAGES - 1
    }

    override fun onNoGeofenceSelected() {
        model.geofence = null
    }

    override fun onGeofenceSelected(geofence: MyGeofence) {
        //to get around the invalid state
        val allfalse = model.wifi?.enter == true
            || model.wifi?.exit == true
            || model.wifi?.dwellOutside == true
            || model.wifi?.dwellInside == true
        model.geofence = geofence.copy(enter = model.wifi?.enter == true || !allfalse,
                                       exit = model.wifi?.exit == true,
                                       dwellOutside = model.wifi?.dwellOutside == true,
                                       dwellInside = model.wifi?.dwellInside == true,
                                       loiteringDelay = model.wifi?.loiteringDelay ?: 0)
    }

    override fun onWifiSelected(wifi: WifiInfo) {
        //to get around the invalid state
        val allfalse = model.geofence?.enter == true
            || model.geofence?.exit == true
            || model.geofence?.dwellOutside == true
            || model.geofence?.dwellInside == true
        model.wifi = MyWifiGeofence(name = wifi.SSID,
                                    bssid = wifi.BSSID,
            //rssi = wifi.rssi //because we do not want that. The
            // threshholds are unknown
                                    enter = model.geofence?.enter == true || !allfalse,
                                    exit = model.geofence?.exit == true,
                                    dwellOutside = model.geofence?.dwellOutside == true,
                                    dwellInside = model.geofence?.dwellInside == true,
                                    loiteringDelay = model.geofence?.loiteringDelay ?: 0)
    }

    override fun onNoWifiSelected() {
        model.wifi = null
    }
}

private const val sitIcon = R.drawable.ic_airline_seat_recline_normal_white_48dp
private const val walkIcon = R.drawable.ic_directions_walk_white_48dp
private const val bikeIcon = R.drawable.ic_directions_bike_white_48dp
private const val busIcon = R.drawable.ic_directions_bus_white_48dp
private const val carIcon = R.drawable.ic_directions_car_white_48dp

fun updateNaturalTriggerReminderCardView(naturalTriggerModel: NaturalTriggerModel, view: View) {
    view.reminder_card?.apply {
        with(naturalTriggerModel) {
            //geofence
            if (geofence != null && geofence?.name != EVERYWHERE) {
                if (geofence!!.imageResId > -1)
                    geofenceIcon.setImageDrawable(
                        resources.obtainTypedArray(R.array.geofence_icons)
                            .getDrawable(geofence!!.imageResId))
                else
                    geofenceIcon.setImageDrawable(null)
                geofenceName.setText(geofence!!.name)
                if (geofence!!.enter)
                    geofenceDirection.setImageDrawable(
                        resources.getDrawable(R.drawable.ic_enter_geofence_fat_arrow_white2, null))
                else if (geofence!!.exit)
                    geofenceDirection.setImageDrawable(
                        resources.getDrawable(R.drawable.ic_exit_geofence_fat_arrow_white, null))
                else if (geofence!!.dwellInside) {
                    geofenceDirection.setImageDrawable(
                        resources.getDrawable(R.drawable.ic_inside_white, null))
                } else if (geofence!!.dwellOutside) {
                    geofenceDirection.setImageDrawable(
                        resources.getDrawable(R.drawable.ic_outside7_white, null))
                }
                if (geofence!!.dwellInside || geofence!!.dwellOutside) {
                    spendTimeGeofence.visibility = View.VISIBLE
                    spendTimeGeofence.text = "${TimeUnit.MILLISECONDS
                        .toMinutes(geofence!!.loiteringDelay.toLong())}"
                } else {
                    spendTimeGeofence.visibility = View.GONE
                }
            } else {
                geofenceIcon.setImageResource(R.drawable.ic_public_white_48dp)
                geofenceDirection.setImageDrawable(null)
                geofenceName.setText("Überall")
                spendTimeGeofence.visibility = View.GONE
            }
            //activity
            val sit = checkActivity(NaturalTriggerModel.SIT)
            val walk = checkActivity(NaturalTriggerModel.WALK)
            val bike = checkActivity(NaturalTriggerModel.BIKE)
            val bus = false //checkActivity(NaturalTriggerModel.BUS)
            val car = checkActivity(NaturalTriggerModel.CAR)
            if (sit) {
                activity1.setImageResource(sitIcon)
                activity2.setImageDrawable(null)
                activity3.setImageDrawable(null)
            } else {
                activity1.setImageDrawable(null)
                activity2.setImageDrawable(null)
                activity3.setImageDrawable(null)
                if (walk && bike && car && bus) {
                } else if (walk) {
                    activity1.setImageResource(walkIcon)
                    if (bike) {
                        activity2.setImageResource(bikeIcon)
                        if (bus)
                            activity3.setImageResource(busIcon)
                        else if (car)
                            activity3.setImageResource(carIcon)
                    } else if (bus) {
                        activity2.setImageResource(busIcon)
                        if (car)
                            activity3.setImageResource(carIcon)
                    } else if (car)
                        activity2.setImageResource(carIcon)
                } else if (bike) {
                    activity1.setImageResource(bikeIcon)
                    if (bus)
                        activity2.setImageResource(busIcon)
                    else if (car)
                        activity2.setImageResource(carIcon)
                } else if (bus) {
                    activity1.setImageResource(busIcon)
                    if (car)
                        activity2.setImageResource(carIcon)
                } else if (car) {
                    activity1.setImageResource(carIcon)
                }
            }
            spendTimeActivity.visibility = View.GONE
            if (walk || car || bike || sit) {
                if (timeInActivity > 0) {
                    spendTimeActivity.visibility = View.VISIBLE
                    spendTimeActivity.text = "${TimeUnit.MILLISECONDS.toMinutes(timeInActivity)}"
                }
            }

            //TimeSelection
            val beginTime = beginTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            val endTime = endTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            timeView.setText(beginTime + "-\n" + endTime)


        }
    }
}
