package de.leo.fingerprint.datacollector.notifications

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.util.Log
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.database.*
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
import org.jetbrains.anko.intentFor
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


/**
 * Created by Leo on 01.01.2018.
 */
class NotificationService : IntentService("NotificationIntentService") {
    companion object {
        private const val notificationIdModifyer = 19921
        private val TIMEOUT = TimeUnit.SECONDS.toMillis(5)
        private val TIMEOUT_LONG = TimeUnit.MINUTES.toMillis(1)
        private const val TAG = "notification service"
        private val notificationStore: HashMap<Int, Long> = hashMapOf()
    }

    @Transient
    private var jitaiDatabase: JitaiDatabase? = null

    override fun onHandleIntent(intent: Intent?) {
        if (jitaiDatabase == null) {
            jitaiDatabase = JitaiDatabase.getInstance(applicationContext)
        }
        val event = intent?.getIntExtra(JITAI_EVENT, -1) ?: -1
        val goal = intent?.getStringExtra(JITAI_GOAL) ?: ""
        val message = intent?.getStringExtra(JITAI_MESSAGE) ?: ""
        val id = intent?.getIntExtra(JITAI_ID, -1) ?: -1
        val sensorDataId = intent?.getLongExtra(JITAI_EVENT_SENSORDATASET_ID, -1) ?: -1L
        if (event > 0) {
            when (event) {
            //already in db
                Jitai.CONDITION_MET                   -> {
                    startNotification(id, goal, message, sensorDataId)
                    Log.d(TAG, "start  $sensorDataId")
                }
            //already in db
                Jitai.NOTIFICATION_NOT_VALID_ANY_MORE -> {
                    cancelNotification(id)
                    Log.d(TAG, "not valid $sensorDataId")
                }

                Jitai.TOO_FREQUENT_NOTIFICATIONS      -> {
                    Log.d(TAG, "too many $sensorDataId")
                    jitaiDatabase!!.enterJitaiEvent(id,
                                                    System.currentTimeMillis(),
                                                    Jitai.TOO_FREQUENT_NOTIFICATIONS,
                                                    sensorDataId)
                }
                Jitai.NOTIFICATION_DELETED            -> {
                    Log.d(TAG, "deleted $sensorDataId")
                    jitaiDatabase!!.enterJitaiEvent(id,
                                                    System.currentTimeMillis(),
                                                    Jitai.NOTIFICATION_DELETED, sensorDataId)
                    //if the notification was deleted by the user set one minte timeout
                    notificationStore.put(id, System.currentTimeMillis() + TIMEOUT_LONG)
                }
                Jitai.NOTIFICATION_FAIL               -> {
                    Log.d(TAG, "incorrect $sensorDataId")
                    cancelNotification(id)
                    jitaiDatabase!!.enterJitaiEvent(id,
                                                    System.currentTimeMillis(),
                                                    Jitai.NOTIFICATION_FAIL, sensorDataId)
                }
                Jitai.NOTIFICATION_SUCCESS            -> {
                    Log.d(TAG, "success $sensorDataId")
                    cancelNotification(id)
                    jitaiDatabase!!.enterJitaiEvent(id,
                                                    System.currentTimeMillis(),
                                                    Jitai.NOTIFICATION_SUCCESS, sensorDataId)
                }
                else                                  -> {
                    Log.d("NotificationService", "I dont know how I ended up here o.0")
                    return
                }
            }
        }
    }

    fun startNotification(id: Int, goal: String, message: String, sensorDataId: Long) {
        val timestamp = notificationStore.get(id)
        if (timestamp == null || timestamp < System.currentTimeMillis()) {
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()),
                                               ZoneId.systemDefault())
            val mNotifyBuilder = NotificationCompat.Builder(this)
                .setContentTitle("${time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}: " +
                                     "$goal")
                .setAutoCancel(false)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setWhen(0)
                .setVibrate(longArrayOf(0L, 150L, 50L, 150L, 50L, 150L))
                .setContentText(message)
                .setSmallIcon(R.drawable.fp_s)
                .addAction(R.drawable.check, "Korrekt", PendingIntent
                    .getService(this,
                                notificationIdModifyer + id,
                                winIntent(id, sensorDataId)
                                , PendingIntent.FLAG_ONE_SHOT))
                .addAction(R.drawable.cancel, "Falsch", PendingIntent
                    .getService(this,
                                notificationIdModifyer + id,
                                failIntent(id, sensorDataId)
                                , PendingIntent.FLAG_ONE_SHOT))
                .setDeleteIntent(PendingIntent.getService(this,
                                                          notificationIdModifyer + id,
                                                          deleteIntent(id, sensorDataId)
                                                          , PendingIntent.FLAG_ONE_SHOT))
            notificationStore.put(id, System.currentTimeMillis() + TIMEOUT)
            mNotificationManager.notify(notificationIdModifyer + id, mNotifyBuilder.build())
        } else {
            //call self in most complicated way
            startService(tooFrequentIntent(id, sensorDataId))
        }
    }

    private fun failIntent(id: Int, sensorDataId: Long): Intent =
        applicationContext.intentFor<NotificationService>(
            JITAI_EVENT to Jitai.NOTIFICATION_FAIL, JITAI_ID to id,
            JITAI_EVENT_SENSORDATASET_ID to sensorDataId).setAction("jitai_fail")

    private fun tooFrequentIntent(id: Int, sensorDataId: Long): Intent =
        applicationContext.intentFor<NotificationService>(
            JITAI_EVENT to Jitai.TOO_FREQUENT_NOTIFICATIONS, JITAI_ID to id,
            JITAI_EVENT_SENSORDATASET_ID to sensorDataId).setAction("Jitai_too_many")

    private fun deleteIntent(id: Int, sensorDataId: Long): Intent =
        applicationContext.intentFor<NotificationService>(
            JITAI_EVENT to Jitai.NOTIFICATION_DELETED, JITAI_ID to id,
            JITAI_EVENT_SENSORDATASET_ID to sensorDataId).setAction("Jitai_user_deleted")

    private fun winIntent(id: Int, sensorDataId: Long): Intent =
        applicationContext.intentFor<NotificationService>(
            JITAI_EVENT to Jitai.NOTIFICATION_SUCCESS, JITAI_ID to id,
            JITAI_EVENT_SENSORDATASET_ID to sensorDataId).setAction("jitai_correct")


    fun cancelNotification(id: Int) {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(notificationIdModifyer + id)
    }
}