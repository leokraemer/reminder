package de.leo.smartTrigger.datacollector.datacollection

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context


class OnBootCompleteReciever : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val serviceIntent = Intent(context, DataCollectorService::class.java)
            context.startService(serviceIntent)
        }
    }
}