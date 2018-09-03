package de.leo.smartTrigger.datacollector.datacollection.sensors

import android.content.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Handler

/**
 * Created by Leo on 24.03.2018.
 */
class WifiScanner(val wifiUpdateListener: WifiUpdateListener, val activity: ContextWrapper) {
    lateinit var mWifiManager: WifiManager
    val handler = Handler()
    val mWifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                val scanResults = mWifiManager.getScanResults()
                //sort so that strongest signal is on top
                scanResults.sortByDescending { it.level }
                wifiUpdateListener.wifiUpdated(scanResults)
            }
            //initiate re-scan in 5 seconds
            handler.postDelayed({ mWifiManager.startScan() }, 5000)
        }
    }


    init {
        mWifiManager = activity.baseContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        activity.registerReceiver(mWifiScanReceiver,
                                  IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        mWifiManager.startScan()
    }

    fun stop() {
        activity.unregisterReceiver(mWifiScanReceiver)
    }
}

interface WifiUpdateListener {
    fun wifiUpdated(scanResults: List<ScanResult>)
}