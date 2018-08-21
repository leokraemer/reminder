package de.leo.fingerprint.datacollector.ui.naturalTrigger.creation

import android.content.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.models.WifiInfo
import kotlinx.android.synthetic.main.empty_view.view.*
import kotlinx.android.synthetic.main.geofence_dialog_list_item.view.*
import kotlinx.android.synthetic.main.geofence_list_dialog.view.*


/**
 * Created by Leo on 10.03.2018.
 */
class WifiListDialogFragment : DialogFragment() {

    // Use this instance of the interface to deliver action events
    lateinit var mListener: WifiDialogListener

    lateinit var mWifiManager: WifiManager

    lateinit var wifiList: ListView

    var scanResults: MutableList<ScanResult> = mutableListOf()

    // Override the Fragment.onAttach() method to instantiate the GeofenceDialogListener
    override fun onAttach(activity: Context) {
        super.onAttach(activity)
        // Verify that the host activities implements the callback interface
        try {
            // Instantiate the GeofenceDialogListener so we can send events to the host
            mListener = activity as WifiDialogListener
        } catch (e: ClassCastException) {
            // The activities doesn't implement the interface, throw exception
            throw ClassCastException(activity.toString() + " must implement GeofenceDialogListener")
        }
        mWifiManager = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        activity.registerReceiver(mWifiScanReceiver,
                                  IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        mWifiManager.startScan()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        getDialog().getWindow().setTitle("Verfügbare W-LAN Netze")
        val view = inflater.inflate(R.layout.geofence_list_dialog, container, false)
        wifiList = view.geofence_listview
        wifiList.adapter = WifiListAdapter(context!!, scanResults)
        if (scanResults.isEmpty()) {
            val empty = view.empty
            empty.setText(if (scanned) "Keine Netze verfügbar" else "Suche...")
            wifiList.emptyView = empty
        }
        wifiList.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                mListener.onWifiSelected(WifiInfo(
                    scanResults[position]))
                dismiss()
            }
        return view
    }


    inner class WifiListAdapter(context: Context, list: List<ScanResult>) :
        ArrayAdapter<ScanResult>
        (context,
         R.layout.geofence_dialog_list_item,
         list
        ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context!!)
                .inflate(R.layout.geofence_dialog_list_item, parent, false)
            val scanResult = getItem(position)
            /*val configs = mWifiManager.configuredNetworks
            val knownNetwork = configs.filter { it.BSSID == scanResult.BSSID }*/
            val drawableID = when (WifiManager.calculateSignalLevel(scanResult.level, 5)) {
                0    -> R.drawable.wifi_level0
                1    -> R.drawable.wifi_level1
                2    -> R.drawable.wifi_level2
                3    -> R.drawable.wifi_level3
                4    -> R.drawable.wifi_level4
                else -> R.drawable.ic_error_white_48dp
            }
            view.icon.setImageResource(drawableID)
            view.geofence_name.setText("${getItem(position).SSID},\n${getItem(position)
                .BSSID}")
            return view
        }
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        mListener.onNoWifiSelected()
    }

    var scanned = false

    val mWifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                scanResults = mWifiManager.getScanResults()
                //sort so that strongest signal is on top
                scanResults.sortByDescending { it.level }
                with(wifiList.adapter as ArrayAdapter<ScanResult>) {
                    clear()
                    addAll(scanResults)
                }
            }
        }
    }
}


