package de.leo.smartTrigger.datacollector.datacollection.sensors

import android.content.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Handler
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService.Companion.UPDATE_DELAY

/**
 * Created by Leo on 24.03.2018.
 */
class WifiScanner(val wifiUpdateListener: WifiUpdateListener, val activity: ContextWrapper,
                  private var updateDelay: Long = UPDATE_DELAY) {
    lateinit var wifiManager: WifiManager
    val handler = Handler()

    val mWifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                val scanResults = wifiManager.getScanResults()
                //sort so that strongest signal is on top
                scanResults.sortByDescending { it.level }
                wifiUpdateListener.wifiUpdated(scanResults)
            }
            //initiate re-scan in UPDATE_DELAY seconds
            handler.postDelayed(scanRunnable, updateDelay)
        }
    }

    init {
        wifiManager = activity.baseContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        activity.registerReceiver(mWifiScanReceiver,
                                  IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
    }

    fun stop() {
        handler.removeCallbacks(scanRunnable)
        activity.unregisterReceiver(mWifiScanReceiver)
    }

    private val scanRunnable = Runnable { wifiManager.startScan() }

    fun changeUpdateDelay(updateDelay: Long) {
        if (this.updateDelay != updateDelay) {
            this.updateDelay = updateDelay
            handler.removeCallbacks(scanRunnable)
            handler.postDelayed(scanRunnable, updateDelay)
        }
    }
}

interface WifiUpdateListener {
    fun wifiUpdated(scanResults: List<ScanResult>)
}