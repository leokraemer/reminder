package de.leo.smartTrigger.datacollector.datacollection.sensors

import android.content.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Created by Leo on 24.03.2018.
 */
class WifiScanner(val context: ContextWrapper) {
    val wifiManager: WifiManager

    init {
        wifiManager = context.baseContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }


    suspend fun getCurrentScanResult(): List<ScanResult> =
        suspendCancellableCoroutine { cont ->
            val wifiScanReceiver = object : BroadcastReceiver() {

                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.action?.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) == true) {
                        val scanResults = wifiManager.scanResults
                        //sort so that strongest signal is on top
                        scanResults.sortByDescending { it.level }
                        Log.i("received wifi", scanResults.toString())
                        context.unregisterReceiver(this)
                        cont.resume(scanResults)
                    }
                }
            }
            cont.invokeOnCancellation {
                context.unregisterReceiver(wifiScanReceiver)
                Log.d("wifi", "iscancelled")

            }
            context.registerReceiver(wifiScanReceiver,
                                     IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            wifiManager.startScan()
        }
}