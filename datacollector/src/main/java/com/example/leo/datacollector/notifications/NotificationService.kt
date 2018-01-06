package com.example.leo.datacollector.notifications

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.example.leo.datacollector.R
import com.example.leo.datacollector.datacollection.DataCollectorService.Companion.notificationID
import com.example.leo.datacollector.services.ActivitiesIntentService

/**
 * Created by Leo on 01.01.2018.
 */
class NotificationService(name: String?) : IntentService(name) {
    override fun onHandleIntent(intent: Intent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startNotification(text : String) {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val mNotifyBuilder = NotificationCompat.Builder(this)
                .setContentTitle("DataCollector")
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentText(text)
                .setSmallIcon(R.drawable.fp_s)
                //.addAction(0, "10 minutes", PendingIntent
                //.getService(this, 19921, Intent(this, ActivitiesIntentService::class.java)
                //            , PendingIntent.FLAG_ONE_SHOT))
        mNotificationManager.notify(
                notificationID,
                mNotifyBuilder.build())
    }

    fun cancelNotification() {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(notificationID)
    }
}