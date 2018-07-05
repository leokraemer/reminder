package de.leo.fingerprint.datacollector.ui.notifications

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color.parseColor
import android.support.v4.app.NotificationCompat
import android.util.Log
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.*
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
import kotlinx.android.synthetic.main.fragment_reminder_selection.view.*
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
        private const val triggerNotificationIdModifyer = 19922
        private val TIMEOUT = TimeUnit.SECONDS.toMillis(5)
        private val TIMEOUT_LONG = TimeUnit.MINUTES.toMillis(1)
        private const val TAG = "notification service"
        private val notificationStore: HashMap<Int, Long> = hashMapOf()
    }

    private val jitaiDatabase: JitaiDatabase by lazy { JitaiDatabase.getInstance(applicationContext) }

    override fun onHandleIntent(intent: Intent?) {
        val event = intent?.getIntExtra(JITAI_EVENT, -1) ?: -1
        val goal = intent?.getStringExtra(JITAI_GOAL) ?: ""
        val message = intent?.getStringExtra(JITAI_MESSAGE) ?: ""
        val jitaiId = intent?.getIntExtra(JITAI_ID, -1) ?: -1
        val sensorDataId = intent?.getLongExtra(JITAI_EVENT_SENSORDATASET_ID, -1) ?: -1L
        if (event > 0) {
            when (event) {
            //already in db
                Jitai.CONDITION_MET                   -> {
                    startNotification(jitaiId, goal, message, sensorDataId)
                    Log.d(TAG, "start  $sensorDataId")
                }
            //already in db
                Jitai.NOTIFICATION_NOT_VALID_ANY_MORE -> {
                    cancelNotification(jitaiId)
                    Log.d(TAG, "not valid $sensorDataId")
                }

                Jitai.TOO_FREQUENT_NOTIFICATIONS      -> {
                    Log.d(TAG, "too many $sensorDataId")
                    jitaiDatabase!!.enterJitaiEvent(jitaiId,
                                                    System.currentTimeMillis(),
                                                    Jitai.TOO_FREQUENT_NOTIFICATIONS,
                                                    sensorDataId)
                }
                Jitai.NOTIFICATION_DELETED            -> {
                    Log.d(TAG, "deleted $sensorDataId")
                    jitaiDatabase!!.enterJitaiEvent(jitaiId,
                                                    System.currentTimeMillis(),
                                                    Jitai.NOTIFICATION_DELETED, sensorDataId)
                    //if the notification was deleted by the user set one minte timeout
                    notificationStore.put(jitaiId, System.currentTimeMillis() + TIMEOUT_LONG)
                }
                Jitai.NOTIFICATION_FAIL               -> {
                    Log.d(TAG, "incorrect $sensorDataId")
                    cancelNotification(jitaiId)
                    jitaiDatabase!!.enterJitaiEvent(jitaiId,
                                                    System.currentTimeMillis(),
                                                    Jitai.NOTIFICATION_FAIL, sensorDataId)
                }
                Jitai.NOTIFICATION_SUCCESS            -> {
                    Log.d(TAG, "success $sensorDataId")
                    cancelNotification(jitaiId)
                    jitaiDatabase!!.enterJitaiEvent(jitaiId,
                                                    System.currentTimeMillis(),
                                                    Jitai.NOTIFICATION_SUCCESS, sensorDataId)
                }

            //trigger notifications
                Jitai.NOTIFICATION_TRIGGER            -> {
                    Log.d(TAG, "$goal notification trigger, asking user for confirmation of " +
                        "situation")
                    startNotificationTrigger(jitaiId, goal, message)
                }
                Jitai.NOTIFICATION_TRIGGER_YES        -> {
                    Log.d(TAG, "$goal notification trigger yes")
                    cancelTriggerNotification(jitaiId)
                    jitaiDatabase!!.enterJitaiEvent(jitaiId,
                                                    System.currentTimeMillis(),
                                                    Jitai.NOTIFICATION_TRIGGER_YES, -1)

                }
                Jitai.NOTIFICATION_TRIGGER_NO         -> {
                    Log.d(TAG, "$goal notification trigger, asking user for confirmation of " +
                        "situation")
                    cancelTriggerNotification(jitaiId)
                    jitaiDatabase!!.enterJitaiEvent(jitaiId,
                                                    System.currentTimeMillis(),
                                                    Jitai.NOTIFICATION_TRIGGER_NO, -1)

                }
                Jitai.NOTIFICATION_TRIGGER_DELETE     -> {
                    Log.d(TAG,
                          "$goal notification trigger, asking user for confirmation of situation")
                    cancelTriggerNotification(jitaiId)
                    jitaiDatabase!!.enterJitaiEvent(jitaiId,
                                                    System.currentTimeMillis(),
                                                    Jitai.NOTIFICATION_TRIGGER_DELETE, -1)

                }

                else                                  -> {
                    Log.d("NotificationService", "I dont know how I ended up here o.0")
                    return
                }
            }
        }
    }

    fun startNotificationTrigger(id: Int, goal: String, message: String) {
        val timestamp = notificationStore.get(id)
        if (timestamp == null || timestamp < System.currentTimeMillis()) {
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()),
                                               ZoneId.systemDefault())
            val mNotifyBuilder = NotificationCompat.Builder(this)
                .setContentTitle("${time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}: " +
                                     "$goal")
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setWhen(0)
                .setVibrate(longArrayOf(0L, 150L, 50L, 150L, 50L, 150L, 50L, 150L))
                .setContentText(message)
                .setSmallIcon(R.drawable.fp_s)
                .setColor(parseColor("blue"))
                .addAction(R.drawable.check, "Ja", PendingIntent
                    .getService(this,
                                triggerNotificationIdModifyer + id,
                                triggerYesIntent(id)
                                , PendingIntent.FLAG_ONE_SHOT))
                .addAction(R.drawable.cancel, "Nein", PendingIntent
                    .getService(this,
                                triggerNotificationIdModifyer + id,
                                triggerNoIntent(id)
                                , PendingIntent.FLAG_ONE_SHOT))
                .setDeleteIntent(PendingIntent.getService(this,
                                                          triggerNotificationIdModifyer + id,
                                                          triggerDeleteIntent(id)
                                                          , PendingIntent.FLAG_ONE_SHOT))
            notificationStore.put(id, System.currentTimeMillis() + TIMEOUT)
            mNotificationManager.notify(triggerNotificationIdModifyer + id, mNotifyBuilder.build())
        } else {
            //call self in most complicated way
            startService(tooFrequentIntent(id, -1))
        }
    }

    private fun triggerYesIntent(jitaiId: Int): Intent =
        applicationContext.intentFor<NotificationService>(
            JITAI_EVENT to Jitai.NOTIFICATION_TRIGGER_YES, JITAI_ID to jitaiId)
            .setAction("jitai_trigger_yes")


    private fun triggerNoIntent(jitaiId: Int): Intent =
        applicationContext.intentFor<NotificationService>(
            JITAI_EVENT to Jitai.NOTIFICATION_TRIGGER_NO, JITAI_ID to jitaiId)
            .setAction("jitai_trigger_no")

    private fun triggerDeleteIntent(jitaiId: Int): Intent =
        applicationContext.intentFor<NotificationService>(
            JITAI_EVENT to Jitai.NOTIFICATION_TRIGGER_DELETE, JITAI_ID to jitaiId)
            .setAction("jitai_trigger_delete")


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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(0)
                .setVibrate(longArrayOf(0L, 150L, 50L, 150L, 50L, 150L))
                .setContentText(message)
                .setSmallIcon(R.drawable.fp_s)
                .addAction(R.drawable.baseline_play_arrow_24_white, "Korrekt", PendingIntent
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
                .setFullScreenIntent(PendingIntent
                                         .getActivity(this,
                                                     notificationIdModifyer + id,
                                                     fullScreenIntent(id, sensorDataId)
                                                     , PendingIntent.FLAG_ONE_SHOT), true)
            notificationStore.put(id, System.currentTimeMillis() + TIMEOUT)
            mNotificationManager.notify(notificationIdModifyer + id, mNotifyBuilder.build())
        } else {
            //call self in most complicated way
            startService(tooFrequentIntent(id, sensorDataId))
        }
    }

    fun cancelTriggerNotification(id: Int) {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(triggerNotificationIdModifyer + id)
    }

    private fun failIntent(id: Int, sensorDataId: Long): Intent =
        applicationContext.intentFor<NotificationService>(
            JITAI_EVENT to Jitai.NOTIFICATION_FAIL, JITAI_ID to id,
            JITAI_EVENT_SENSORDATASET_ID to sensorDataId).setAction("jitai_fail")

    private fun fullScreenIntent(id: Int, sensorDataId: Long): Intent =
        applicationContext.intentFor<FullscreenJitai>(
            JITAI_EVENT to Jitai.NOTIFICATION_FULL_SCREEN, JITAI_ID to id,
            JITAI_EVENT_SENSORDATASET_ID to sensorDataId).setAction("jitai_fullscreen")

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