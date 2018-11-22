package de.leo.smartTrigger.datacollector.datacollection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.jetbrains.anko.toast


class RestartServiceIntentReciever : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED  -> {
                context.toast("Starting SmartTrigger Service")
                val serviceIntent = Intent(context, DataCollectorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}