package de.leo.smartTrigger.datacollector.datacollection

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import org.jetbrains.anko.toast
import android.os.Build


class OnBootCompleteReciever : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            context.toast("Startin SmartTrigger Service")
            val serviceIntent = Intent(context, DataCollectorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}