package de.leo.smartTrigger.datacollector.ui.notifications

import android.app.AlarmManager
import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color.parseColor
import android.media.RingtoneManager
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.*
import de.leo.smartTrigger.datacollector.jitai.manage.Jitai
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity
import de.leo.smartTrigger.datacollector.utils.TimeUtils.toEpochMillis
import org.jetbrains.anko.intentFor
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


/**
 * Created by Leo on 01.01.2018.
 */
class NotificationService : IntentService("NotificationIntentService") {
    companion object {
        const val NOTIFICATIONIDMODIFYER = 19921
        const val DAILY_REMINDER_REQUEST_CODE = 19034
        const val TRIGGERNOTIFICATIONIDMODIFYER = 4711
        private val TIMEOUT = TimeUnit.SECONDS.toMillis(5)
        private val TIMEOUT_LONG = TimeUnit.MINUTES.toMillis(1)
        private val TIMEOUT_SNOOZE = TimeUnit.MINUTES.toMillis(15)
        private const val TAG = "notification service"
        private val notificationStore: HashMap<Int, Long> = hashMapOf()
        const val CHANNEL = "naturalTriggerReminder"
        const val FOREGROUND_CHANNEL = "naturalTriggerReminderForegroundSerciveChannel"
        const val SET_DAILY_REMINDER = "dailyReminder"
        const val PUSH_DAILY_REMINDER_TO_USER = "dailyReminderToUser"
    }

    private val db: JitaiDatabase by lazy { JitaiDatabase.getInstance(applicationContext) }
    private val username: String by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
            .getString(getString(R.string.user_name), null)
    }

    override fun onHandleIntent(intent: Intent?) {
        val event = intent?.getIntExtra(JITAI_EVENT, -1) ?: -1
        val goal = intent?.getStringExtra(JITAI_GOAL) ?: ""
        val message = intent?.getStringExtra(JITAI_MESSAGE) ?: ""
        val jitaiId = intent?.getIntExtra(JITAI_ID, -1) ?: -1
        val sensorDataId = intent?.getLongExtra(JITAI_EVENT_SENSORDATASET_ID, -1) ?: -1L
        when (intent?.action) {
            PUSH_DAILY_REMINDER_TO_USER -> dailyReminderNotification()
            SET_DAILY_REMINDER          -> {
                val am = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = applicationContext.intentFor<NotificationService>()
                intent.action = PUSH_DAILY_REMINDER_TO_USER
                val pendingIntent = PendingIntent.getService(
                    applicationContext,
                    DAILY_REMINDER_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
                //wake up tomorrow at 5:00 am
                var date: LocalDate = LocalDate.now().plusDays(1)
                val time = LocalTime.of(5, 0)
                val dateTime = LocalDateTime.of(date, time).atZone(ZoneId.systemDefault())
                am.set(AlarmManager.RTC_WAKEUP, dateTime.toEpochMillis(), pendingIntent)
            }
        }
        if (event > 0) {
            when (event) {
                //already in db
                Jitai.CONDITION_MET                   -> {
                    naturalTriggerNotification(jitaiId, goal, message, sensorDataId)
                    naturalTriggerFullScreenReminder(jitaiId, goal, message, sensorDataId)
                    Log.d(TAG, "start  $sensorDataId")
                }
                Jitai.NOTIFICATION_SNOOZE_FINISHED    -> {
                    naturalTriggerNotification(jitaiId, goal, message, sensorDataId)
                    naturalTriggerFullScreenReminder(jitaiId, goal, message, sensorDataId)
                    Log.d(TAG, "start  $sensorDataId")
                }
                //already in db
                Jitai.NOTIFICATION_NOT_VALID_ANY_MORE -> {
                    cancelNotification(jitaiId)
                    Log.d(TAG, "not valid $sensorDataId")
                }

                Jitai.TOO_FREQUENT_NOTIFICATIONS      -> {
                    Log.d(TAG, "too many $sensorDataId")
                    db.enterUserJitaiEvent(jitaiId,
                                           System.currentTimeMillis(),
                                           username,
                                           Jitai.TOO_FREQUENT_NOTIFICATIONS,
                                           sensorDataId, -1,
                                           -1, "")
                }
                Jitai.NOTIFICATION_DELETED            -> {
                    Log.d(TAG, "deleted $sensorDataId")
                    db.enterUserJitaiEvent(jitaiId,
                                           System.currentTimeMillis(),
                                           username,
                                           Jitai.NOTIFICATION_DELETED, sensorDataId, -1,
                                           -1, "")
                    //if the notification was deleted by the user set one minte timeout
                    notificationStore.put(jitaiId, System.currentTimeMillis() + TIMEOUT_LONG)
                }
                Jitai.NOTIFICATION_FAIL               -> {
                    Log.d(TAG, "incorrect $sensorDataId")
                    cancelNotification(jitaiId)
                    db.enterUserJitaiEvent(jitaiId,
                                           System.currentTimeMillis(),
                                           username,
                                           Jitai.NOTIFICATION_FAIL, sensorDataId, -1,
                                           -1, "")
                }
                Jitai.NOTIFICATION_SNOOZE             -> {
                    Log.d(TAG, "snooze $sensorDataId")
                    cancelNotification(jitaiId)
                    db.enterUserJitaiEvent(jitaiId,
                                           System.currentTimeMillis(),
                                           username,
                                           Jitai.NOTIFICATION_FAIL, sensorDataId, -1,
                                           -1, "")
                    //set alarm to re-post notification in 15 minutes
                    val am = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = applicationContext.intentFor<NotificationService>(
                        JITAI_EVENT to Jitai.CONDITION_MET,
                        JITAI_ID to jitaiId,
                        JITAI_EVENT_SENSORDATASET_ID to sensorDataId,
                        JITAI_GOAL to goal,
                        JITAI_MESSAGE to message)
                    val pendingIntent = PendingIntent.getService(applicationContext,
                                                                 12356,
                                                                 intent,
                                                                 PendingIntent.FLAG_UPDATE_CURRENT)
                    am.setExact(AlarmManager.RTC,
                                System.currentTimeMillis() + TIMEOUT_SNOOZE,
                                pendingIntent)
                }
                Jitai.NOTIFICATION_SUCCESS            -> {
                    Log.d(TAG, "success $sensorDataId")
                    cancelNotification(jitaiId)
                    db.enterUserJitaiEvent(jitaiId,
                                           System.currentTimeMillis(),
                                           username,
                                           Jitai.NOTIFICATION_SUCCESS, sensorDataId, -1,
                                           -1, "")
                }

                //trigger notifications
                Jitai.NOTIFICATION_TRIGGER            -> {
                    Log.d(TAG, "$goal notification trigger, asking user for confirmation of " +
                        "situation")
                    startNotificationMachineLearningJitai(jitaiId, goal, message)
                }
                Jitai.NOTIFICATION_TRIGGER_YES        -> {
                    Log.d(TAG, "$goal notification trigger yes")
                    cancelTriggerNotification(jitaiId)
                    db.enterUserJitaiEvent(jitaiId,
                                           System.currentTimeMillis(),
                                           username,
                                           Jitai.NOTIFICATION_TRIGGER_YES, -1, -1,
                                           -1, "")

                }
                Jitai.NOTIFICATION_TRIGGER_NO         -> {
                    Log.d(TAG, "$goal notification trigger, asking user for confirmation of " +
                        "situation")
                    cancelTriggerNotification(jitaiId)
                    db.enterUserJitaiEvent(jitaiId,
                                           System.currentTimeMillis(),
                                           username,
                                           Jitai.NOTIFICATION_TRIGGER_NO, -1, -1,
                                           -1, "")

                }
                Jitai.NOTIFICATION_TRIGGER_DELETE     -> {
                    Log.d(TAG,
                          "$goal notification trigger, asking user for confirmation of situation")
                    cancelTriggerNotification(jitaiId)
                    db.enterUserJitaiEvent(jitaiId,
                                           System.currentTimeMillis(),
                                           username,
                                           Jitai.NOTIFICATION_TRIGGER_DELETE, -1, -1,
                                           -1, "")

                }

                else                                  -> {
                    Log.d("NotificationService", "I dont know how I ended up here o.0")
                    return
                }
            }
        }
    }

    fun startNotificationMachineLearningJitai(id: Int, goal: String, message: String) {
        val timestamp = notificationStore.get(id)
        if (timestamp == null || timestamp < System.currentTimeMillis()) {
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()),
                                               ZoneId.systemDefault())
            val mNotifyBuilder = NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle("${time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}: " +
                                     "$goal")
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setWhen(0)
                .setVibrate(longArrayOf(0L, 150L, 50L, 150L, 50L, 150L, 50L, 150L))
                .setContentText(message)
                .setSmallIcon(R.drawable.reminder_ico)
                .setColor(parseColor("blue"))
                .addAction(R.drawable.check, "Ja", PendingIntent
                    .getService(this,
                                TRIGGERNOTIFICATIONIDMODIFYER + id,
                                triggerYesIntent(id)
                                , PendingIntent.FLAG_ONE_SHOT))
                .addAction(R.drawable.cancel, "Nein", PendingIntent
                    .getService(this,
                                TRIGGERNOTIFICATIONIDMODIFYER + id,
                                triggerNoIntent(id)
                                , PendingIntent.FLAG_ONE_SHOT))
                .setDeleteIntent(PendingIntent.getService(this,
                                                          TRIGGERNOTIFICATIONIDMODIFYER + id,
                                                          triggerDeleteIntent(id)
                                                          , PendingIntent.FLAG_ONE_SHOT))
            notificationStore.put(id, System.currentTimeMillis() + TIMEOUT)
            mNotificationManager.notify(TRIGGERNOTIFICATIONIDMODIFYER + id, mNotifyBuilder.build())
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


    fun naturalTriggerFullScreenReminder(id: Int,
                                         goal: String,
                                         message: String,
                                         sensorDataId: Long) {
        val timestamp = notificationStore.get(id)
        if (timestamp == null || timestamp < System.currentTimeMillis()) {
            notificationStore.put(id, System.currentTimeMillis() + TIMEOUT)
            startActivity(fullScreenIntent(id, sensorDataId, goal, message))
        } else {
            //call self in most complicated way
            startService(tooFrequentIntent(id, sensorDataId))
        }
    }

    fun naturalTriggerNotification(id: Int, goal: String, message: String, sensorDataId: Long) {
        val timestamp = notificationStore.get(id)
        if (timestamp == null || timestamp < System.currentTimeMillis()) {
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()),
                                               ZoneId.systemDefault())
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            //notification on default channel to get the priority_max for the heads up notification
            val mNotifyBuilder = NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle("${time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}: " +
                                     "$goal")
                .setAutoCancel(true)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setWhen(0)
                .setSound(alarmSound)
                .setVibrate(longArrayOf(0L, 150L, 50L, 150L, 50L, 150L))
                .setContentText(message)
                .setSmallIcon(R.drawable.reminder_ico)
            mNotificationManager.notify(NOTIFICATIONIDMODIFYER + id, mNotifyBuilder.build())
        }
    }

    fun dailyReminderNotification() {
        if (db.getAllActiveNaturalTriggerJitai().size < 4) {
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            //notification on default channel to get the priority_max for the heads up notification
            val mNotifyBuilder = NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle("Morgentliche Erinnerung")
                .setAutoCancel(true)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setWhen(0)
                .setContentText("Setzen sie sich ihr Ziel für heute")
                .setSmallIcon(R.drawable.reminder_white)
                .setContentIntent(PendingIntent.getActivity(this,
                                                            DAILY_REMINDER_REQUEST_CODE,
                                                            createIntent(),
                                                            PendingIntent.FLAG_UPDATE_CURRENT))
            mNotificationManager.notify(NOTIFICATIONIDMODIFYER + DAILY_REMINDER_REQUEST_CODE,
                                        mNotifyBuilder.build())
        }
    }

    fun cancelTriggerNotification(id: Int) {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(TRIGGERNOTIFICATIONIDMODIFYER + id)
    }

    private fun failIntent(id: Int, sensorDataId: Long): Intent =
        applicationContext.intentFor<NotificationService>(
            JITAI_EVENT to Jitai.NOTIFICATION_FAIL, JITAI_ID to id,
            JITAI_EVENT_SENSORDATASET_ID to sensorDataId).setAction("jitai_fail")

    private fun createIntent(): Intent =
        applicationContext.intentFor<CreateTriggerActivity>().setAction("create")

    private fun fullScreenIntent(id: Int,
                                 sensorDataId: Long,
                                 goal: String,
                                 message: String): Intent =
        applicationContext.intentFor<FullscreenJitai>(
            JITAI_EVENT to Jitai.NOTIFICATION_FULL_SCREEN,
            JITAI_ID to id,
            JITAI_GOAL to goal,
            JITAI_MESSAGE to message,
            JITAI_EVENT_SENSORDATASET_ID to sensorDataId)
            .setAction("jitai_fullscreen").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

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
        mNotificationManager.cancel(NOTIFICATIONIDMODIFYER + id)
    }
}