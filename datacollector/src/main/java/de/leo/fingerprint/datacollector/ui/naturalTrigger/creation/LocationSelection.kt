package de.leo.fingerprint.datacollector.ui.naturalTrigger.creation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.*
import de.leo.fingerprint.datacollector.datacollection.models.WifiInfo
import de.leo.fingerprint.datacollector.jitai.MyGeofence
import de.leo.fingerprint.datacollector.ui.GeofencesWithPlayServices.GeofenceMapActivity
import kotlinx.android.synthetic.main.fragment_location_selection.*
import org.jetbrains.anko.support.v4.intentFor
import org.jetbrains.anko.support.v4.toast
import java.util.concurrent.TimeUnit


/**
 * Created by Leo on 06.03.2018.
 */
class LocationSelection : NaturalTriggerFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = inflater.inflate(
            R.layout.fragment_location_selection, container, false)
        db = JitaiDatabase.getInstance(context!!)
        homeGeofence = db.getMyGeofenceByCode(HOME_CODE)
        workGeofence = db.getMyGeofenceByCode(WORK_CODE)
        HOME_NAME = context!!.getString(R.string.home_geofence_name)
        WORK_NAME = context!!.getString(R.string.work_geofence_name)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeGeofenceButton.setOnClickListener { clickHome() }
        homeText.setOnClickListener { clickHome() }
        workGeofenceButton.setOnClickListener { clickWork() }
        workText.setOnClickListener { clickWork() }
        enterButton.setOnClickListener { clickMap() }
        enterText.setOnClickListener { clickMap() }
        worldGeofenceButton.setOnClickListener { clickWorld() }
        worldText.setOnClickListener { clickWorld() }
        wifiGeofenceButton.setOnClickListener { clickWifi() }
        wifiText.setOnClickListener { clickWifi() }
        shopGeofenceButton.setOnClickListener { clickShop() }
        shopText.setOnClickListener { clickShop() }
        listGeofenceButton.setOnClickListener { clickList() }
        listText.setOnClickListener { clickList() }
        busstopGeofenceButton.setOnClickListener { clickBus() }
        busStopText.setOnClickListener { clickBus() }
        updateView()
    }

    lateinit var HOME_NAME: String
    lateinit var WORK_NAME: String

    var homeGeofence: MyGeofence? = null
    var workGeofence: MyGeofence? = null
    lateinit var db: JitaiDatabase

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val geofenceId = data?.getIntExtra(ID, -1)
            if (requestCode == HOME_CODE && geofenceId != null) {
                uncheckAll()
                homeGeofenceButton.isChecked = true
                homeGeofence = db.getMyGeofence(geofenceId)
                model!!.geofence = homeGeofence
            }
            if (requestCode == WORK_CODE && geofenceId != null) {
                uncheckAll()
                workGeofenceButton.isChecked = true
                workGeofence = db.getMyGeofence(geofenceId)
                model!!.geofence = workGeofence
            }
            if (requestCode == CREATE_CODE && geofenceId != null) {
                uncheckAll()
                enterButton.isChecked = true
                model!!.geofence = db.getMyGeofence(geofenceId)
            }
        } else {
            uncheckAll()
            toast("Erstellen Abgebrochen")
        }
    }

    private fun uncheckAll() {
        homeGeofenceButton.isChecked = false
        workGeofenceButton.isChecked = false
        enterButton.isChecked = false
        worldGeofenceButton.isChecked = false
        wifiGeofenceButton.isChecked = false
        shopGeofenceButton.isChecked = false
        listGeofenceButton.isChecked = false
        busstopGeofenceButton.isChecked = false
    }

    override fun updateView() {
        situation_text?.setText(model?.situation)
        model?.let {
            with(it) {
                worldGeofenceButton?.isChecked = false
                homeGeofenceButton?.isChecked = false
                workGeofenceButton?.isChecked = false
                homeGeofenceButton?.isChecked = false
                wifiGeofenceButton?.isChecked = false
                if (geofence != null) {
                    if (geofence?.id == homeGeofence?.id) {
                        homeGeofenceButton?.isChecked = true
                    } else if (geofence?.id == workGeofence?.id) {
                        workGeofenceButton?.isChecked = true
                    } else if (wifi != null) {
                        wifiGeofenceButton?.isChecked = true
                    } else {
                        listGeofenceButton?.isChecked = true
                    }
                } else {
                    worldGeofenceButton?.isChecked = true
                }

            }
        }
    }

    fun clickHome() {
        uncheckAll()
        homeGeofenceButton.isChecked = true
        if (homeGeofence == null) {
            val intent = intentFor<GeofenceMapActivity>()
            intent.putExtra(GEOFENCE_NAME, HOME_NAME)
            intent.putExtra(GEOFENCE_IMAGE,
                            HOME_CODE)
            startActivityForResult(intent,
                                   HOME_CODE)
        } else
            model!!.geofence = homeGeofence
    }

    fun clickWork() {
        uncheckAll()
        workGeofenceButton.isChecked = true
        if (workGeofence == null) {
            val intent = intentFor<GeofenceMapActivity>()
            intent.putExtra(GEOFENCE_NAME, WORK_NAME)
            intent.putExtra(GEOFENCE_IMAGE,
                            WORK_CODE)
            startActivityForResult(intent,
                                   WORK_CODE)
        } else
            model!!.geofence = workGeofence
    }

    fun clickMap() {
        uncheckAll()
        worldGeofenceButton.isChecked = true
        val intent = intentFor<GeofenceMapActivity>()
        intent.putExtra(GEOFENCE_IMAGE,
                        CREATE_CODE)
        startActivityForResult(intent,
                               CREATE_CODE)

    }

    fun clickWorld() {
        uncheckAll()
        worldGeofenceButton.isChecked = true
        model!!.geofence = everywhere_geofence()
    }

    fun clickList() {
        val dialog = GeofenceListDialogFragment()
        dialog.show(activity!!.supportFragmentManager, "GeofenceListDialogFragment")
    }

    fun clickShop() {
        //noop
    }

    fun clickBus() {
        //noop
    }

    fun clickWifi() {
        val dialog = WifiListDialogFragment()
        dialog.show(activity!!.supportFragmentManager, "GeofenceListDialogFragment")
    }

    companion object {
        val EVERYWHERE: String = "Überall"
        fun everywhere_geofence() = MyGeofence(-1, Companion.EVERYWHERE, 0.0, 0.0, Float.MAX_VALUE,
                                                   false, false,
                                                   true, TimeUnit.MINUTES.toMillis(5).toInt(),
                                               WORLD_CODE)
    }

}

interface GeofenceDialogListener {
    fun onGeofenceSelected(geofence: MyGeofence)
    fun onNoGeofenceSelected()
}

interface WifiDialogListener {
    fun onWifiSelected(geofence: WifiInfo)
    fun onNoWifiSelected()
}


const val HOME_CODE = 0
const val WORK_CODE = 1
const val CREATE_CODE = 2
const val WORLD_CODE = 3
const val RESTAURANT_CODE = 4
const val SHOP_CODE = 5
const val BUS_CODE = 6
const val WIFI_CODE = 7

/* array geofence_icons
  <item>@drawable/ic_home_white_48dp</item>
  <item>@drawable/ic_work_white_48dp</item>
  <item>@android:drawable/ic_dialog_map</item>
  <item>@drawable/ic_public_white_48dp</item>
  <item>@drawable/ic_local_dining_white_48dp</item>
  <item>@drawable/ic_shopping_cart_white_48dp</item>
  <item>@drawable/ic_bus_stop</item>
  <item>@drawable/ic_wifi_white_48dp</item>
 */