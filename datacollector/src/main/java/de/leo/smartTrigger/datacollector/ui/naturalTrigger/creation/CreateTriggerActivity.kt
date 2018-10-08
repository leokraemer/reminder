package de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation

import android.animation.ObjectAnimator
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup.GONE
import android.view.ViewGroup.LayoutParams
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService
import de.leo.smartTrigger.datacollector.datacollection.database.JITAI_ID
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.WifiInfo
import de.leo.smartTrigger.datacollector.jitai.MyAbstractGeofence
import de.leo.smartTrigger.datacollector.jitai.MyGeofence
import de.leo.smartTrigger.datacollector.jitai.MyWifiGeofence
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.EVERYWHERE
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.everywhere_geofence
import de.leo.smartTrigger.datacollector.utils.UPDATE_JITAI
import kotlinx.android.synthetic.main.activity_natural_trigger_tabs.*
import kotlinx.android.synthetic.main.naturaltriggerview.*
import kotlinx.android.synthetic.main.naturaltriggerview.view.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.threeten.bp.LocalTime
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
        const val NATURALTRIGGER_ID = "natural_trigger_id"
    }

    val db by lazy { JitaiDatabase.getInstance(this) }

    private lateinit var model: NaturalTriggerModel
    /**
     * The number of pages to show.
     */
    private var NUM_PAGES = 7


    //Fragments
    private val goalSelection = GoalSelection()
    private val situationSelection = SituationSelection()
    private val locationSelection = LocationSelection()
    private val locationFinish = LocationFinish()
    private val activitySelection = ActivitySelection()
    private val timeSelection = TimeSelection()
    private val overview = Overview()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_natural_trigger_tabs)
        val naturalTriggerId = intent.getIntExtra(NATURALTRIGGER_ID, -1)
        if (naturalTriggerId != -1) {
            model = db.getNaturalTrigger(naturalTriggerId)
        } else {
            model = NaturalTriggerModel()
        }
        viewPager.adapter = ScreenSlidePagerAdapter(supportFragmentManager)
        viewPager.addOnPageChangeListener(MyOnPageChangeListener())
        previous_page.setOnClickListener { onBackPressed() }
        next_page.setOnClickListener { if (viewPager.isPagingEnabled == true) nextButtonClick() }
        if (intent.action == EDIT) {
            expand(reminder_card, 1F)
        }
        model.modelChangelListener = this
        goalSelection.model = model
        situationSelection.model = model
        locationFinish.model = model
        locationSelection.model = model
        activitySelection.model = model
        timeSelection.model = model
        overview.model = model

        //kick off view initialisation
        modelChangedCallback()
    }

    var geofenceUndefinedBefore = NUM_PAGES

    override fun modelChangedCallback() {
        //notifyDatasetChanged only when the geofence changed fron defined to undefined or otherwise
        if (geofenceUndefinedBefore != viewPager.adapter?.count) {
            viewPager.adapter?.notifyDataSetChanged()
            geofenceUndefinedBefore = viewPager.adapter?.count ?: NUM_PAGES
        }
        updateNaturalTriggerReminderCardView(model, reminder_card)
        //enable/disable view paging
        if (viewPager!!.currentItem == 0
            && (model.goal.isEmpty() || model.message.isEmpty())) {
            viewPager?.isPagingEnabled = false
        } else if (viewPager!!.currentItem == 1 && model.situation.isEmpty()) {
            viewPager!!.isPagingEnabled = false
        } else {
            viewPager?.isPagingEnabled = true
        }
        //update child views
        activitySelection.updateView()
        locationSelection.updateView()
        locationFinish.updateView()
        goalSelection.updateView()
        situationSelection.updateView()
        timeSelection.updateView()
        overview.updateView()
    }

    private fun nextButtonClick() {
        //last page
        if (viewPager!!.currentItem == viewPager!!.adapter!!.count - 1) {
            model.ID = db.enterNaturalTrigger(model)
            toast("Erinnerung erfolgreich erstellt")
            startService(intentFor<DataCollectorService>()
                             .setAction(UPDATE_JITAI)
                             .putExtra(JITAI_ID, model.ID)
                        )
            super.onBackPressed()
        }
        viewPager!!.currentItem++
    }

    fun expand(v: View, positionOffset: Float) {
        v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val actualHeight = v.height
        val targetHeight = v.measuredHeight
        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        v.layoutParams.height = if (positionOffset >= 1f)
            LayoutParams.WRAP_CONTENT
        else
        //do not shrink the view once it was expanded
            Math.max(actualHeight, (targetHeight * positionOffset).toInt())
        v.requestLayout()
    }

    override fun onBackPressed() {
        if (viewPager!!.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activities and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            viewPager!!.currentItem = viewPager!!.currentItem - 1
        }
    }


    private inner class MyOnPageChangeListener : ViewPager.OnPageChangeListener {
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
            viewPager.adapter?.let {
                if (position == it.count - 1)
                    next_page.text = "Fertig"
                if (position < it.count - 1)
                    next_page.text = "Weiter"
                //second to last page is the time page
                if (position == it.count - 2) {
                    if (model.beginTime == null) {
                        model.beginTime = LocalTime.of(8, 0)
                        model.endTime = LocalTime.of(20, 0)
                    }
                }
                if (position > NUM_PAGES)
                    finish()
                ObjectAnimator.ofInt(scrollView, "scrollY", scrollView.height)
                    .setDuration(200)
                    .start();
                //scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
    }

    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {
        private fun showGeofenceDwellTimeSelection(): Boolean {
            return model.geofence?.name != EVERYWHERE || model.wifi != null
        }

        override fun getItem(position: Int): Fragment =
            if (showGeofenceDwellTimeSelection())
                when (position) {
                    0    -> goalSelection
                    1    -> situationSelection
                    2    -> locationSelection
                    3    -> locationFinish
                    4    -> activitySelection
                    5    -> timeSelection
                    6    -> overview
                    else -> overview // error
                } else
                when (position) {
                    0    -> goalSelection
                    1    -> situationSelection
                    2    -> locationSelection
                    3    -> activitySelection
                    4    -> timeSelection
                    5    -> overview
                    else -> overview // error
                }

        override fun getCount(): Int =
            if (showGeofenceDwellTimeSelection())
                NUM_PAGES
            else
                NUM_PAGES - 1

        override fun getItemPosition(item: Any): Int = PagerAdapter.POSITION_NONE/*
            if (showGeofenceDwellTimeSelection()) {
                when (item) {
                    goalSelection      -> 0
                    situationSelection -> 1
                    locationSelection  -> 2
                    locationFinish     -> 3
                    activitySelection  -> 4
                    timeSelection      -> 5
                    else               -> PagerAdapter.POSITION_NONE // error
                }
            } else {
                when (item) {
                    goalSelection      -> 0
                    situationSelection -> 1
                    locationSelection  -> 2
                    locationFinish     -> PagerAdapter.POSITION_NONE
                    activitySelection  -> 3
                    timeSelection      -> 4
                    else               -> PagerAdapter.POSITION_NONE // error
                }
            }*/
    }

    override fun onCreateGeofence() {
        locationSelection.clickMap()
    }

    override fun onNoGeofenceSelected() {
        locationSelection.updateView()
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
        model.wifi = null
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
        model.geofence = everywhere_geofence()
    }

    override fun onNoWifiSelected() {
        locationSelection.updateView()
    }

    override fun onStart() {
        super.onStart()
        viewPager.adapter?.notifyDataSetChanged()
    }
}

private const val sitIcon = R.drawable.ic_airline_seat_recline_normal_white_48dp
private const val walkIcon = R.drawable.ic_directions_walk_white_48dp
private const val bikeIcon = R.drawable.ic_directions_bike_white_48dp
private const val carIcon = R.drawable.ic_directions_car_white_48dp

fun updateNaturalTriggerReminderCardView(naturalTriggerModel: NaturalTriggerModel, view: View) {
    view.reminder_card?.apply {
        with(naturalTriggerModel) {
            //geofence
            if (geofence != null && geofence?.name != EVERYWHERE) {
                updateGeofenceView(this@apply, geofence!!)
            } else if (wifi != null) {
                updateGeofenceView(this@apply, wifi!!)
            } else {
                geofenceIcon.setImageResource(R.drawable.ic_public_white_48dp)
                geofenceDirection.setImageDrawable(null)
                geofenceName.setText("Überall")
                spendTimeGeofence.visibility = View.GONE
            }
            //activities
            val sit = checkActivity(NaturalTriggerModel.SIT)
            val walk = checkActivity(NaturalTriggerModel.WALK)
            val bike = checkActivity(NaturalTriggerModel.BIKE)
            val car = checkActivity(NaturalTriggerModel.CAR)
            if (sit) {
                activity1.setImageResource(sitIcon)
                activity2.setImageDrawable(null)
                activity3.setImageDrawable(null)
            } else {
                activity1.setImageDrawable(null)
                activity2.setImageDrawable(null)
                activity3.setImageDrawable(null)
                if (walk) {
                    activity1.setImageResource(walkIcon)
                    if (bike) {
                        activity2.setImageResource(bikeIcon)
                        if (car)
                            activity3.setImageResource(carIcon)
                    }
                } else if (bike) {
                    activity1.setImageResource(bikeIcon)
                    if (car)
                        activity2.setImageResource(carIcon)
                } else if (car)
                    activity1.setImageResource(carIcon)
            }
            spendTimeActivity.visibility = View.GONE
            if (walk || car || bike || sit) {
                if (timeInActivity > 0) {
                    spendTimeActivity.visibility = View.VISIBLE
                    spendTimeActivity.text = "${TimeUnit.MILLISECONDS.toMinutes(
                        timeInActivity)}"
                }
            }

            //TimeSelection
            if (beginTime != null && endTime != null) {
                val beginTime = beginTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
                val endTime = endTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
                timeView.visibility = View.VISIBLE
                timeView.setText(beginTime + "-\n" + endTime)
            } else timeView.visibility = View.INVISIBLE
        }
    }
}

private fun updateGeofenceView(cardView: View, geofence: MyAbstractGeofence) {
    if (geofence.imageResId > -1)
        cardView.geofenceIcon.setImageDrawable(
            cardView.resources.obtainTypedArray(R.array.geofence_icons)
                .getDrawable(geofence.imageResId))
    else
        cardView.geofenceIcon.setImageDrawable(null)
    cardView.geofenceName.setText(geofence.name)
    if (geofence.enter)
        cardView.geofenceDirection.setImageDrawable(
            cardView.resources.getDrawable(R.drawable.ic_enter_geofence_fat_arrow_white2,
                                           null))
    else if (geofence.exit)
        cardView.geofenceDirection.setImageDrawable(
            cardView.resources.getDrawable(R.drawable.ic_exit_geofence_fat_arrow_white,
                                           null))
    else if (geofence.dwellInside) {
        cardView.geofenceDirection.setImageDrawable(
            cardView.resources.getDrawable(R.drawable.ic_inside3_white, null))
    } else if (geofence.dwellOutside) {
        cardView.geofenceDirection.setImageDrawable(
            cardView.resources.getDrawable(R.drawable.ic_outside10_white, null))
    }
    if (geofence.dwellInside || geofence.dwellOutside) {
        cardView.spendTimeGeofence.visibility = View.VISIBLE
        cardView.spendTimeGeofence.text = "${TimeUnit.MILLISECONDS
            .toMinutes(geofence.loiteringDelay)}"
    } else {
        cardView.spendTimeGeofence.visibility = View.GONE
    }
}