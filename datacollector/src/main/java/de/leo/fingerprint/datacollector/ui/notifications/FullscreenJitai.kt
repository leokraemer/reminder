package de.leo.fingerprint.datacollector.ui.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.*
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
import kotlinx.android.synthetic.main.activity_full_screen_jitai.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk25.coroutines.onClick

class FullscreenJitai : AppCompatActivity() {

    val db by lazy { JitaiDatabase.getInstance(this) }

    val vibrator by lazy { getSystemService(VIBRATOR_SERVICE) as Vibrator }
    val notificationService by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppBaseTheme_Light_Fullscreen)
        setContentView(R.layout.activity_full_screen_jitai)
        val event = intent?.getIntExtra(JITAI_EVENT, -1) ?: -1
        val goalText = intent?.getStringExtra(JITAI_GOAL) ?: ""
        val messageText = intent?.getStringExtra(JITAI_MESSAGE) ?: ""
        val jitaiId = intent?.getIntExtra(JITAI_ID, -1) ?: -1
        val sensorDataId = intent?.getLongExtra(JITAI_EVENT_SENSORDATASET_ID, -1) ?: -1L
        message.text = messageText
        goal.text = goalText
        //cancel the notification when the fullscreen app is launched
        notificationService.cancel(NotificationService.NOTIFICATIONIDMODIFYER + jitaiId)
        close.onClick {
            val intent =
                applicationContext.intentFor<NotificationService>(
                    JITAI_EVENT to Jitai.NOTIFICATION_DELETED,
                    JITAI_ID to jitaiId,
                    JITAI_EVENT_SENSORDATASET_ID to sensorDataId)
                    .setAction("Jitai_user_deleted")
            startService(intent)
            onBackPressed()
        }
        play.onClick {
            val intent =
                applicationContext.intentFor<NotificationService>(
                    JITAI_EVENT to Jitai.NOTIFICATION_SUCCESS,
                    JITAI_ID to jitaiId,
                    JITAI_EVENT_SENSORDATASET_ID to sensorDataId)
                    .setAction("jitai_correct")
            startService(intent)
            onBackPressed()
        }
        snooze.onClick {
            val intent =
                applicationContext.intentFor<NotificationService>(
                    JITAI_EVENT to Jitai.NOTIFICATION_SNOOZE,
                    JITAI_ID to jitaiId,
                    JITAI_EVENT_SENSORDATASET_ID to sensorDataId,
                    JITAI_GOAL to goalText,
                    JITAI_MESSAGE to messageText)
                    .setAction("jitai_correct")
            startService(intent)
            onBackPressed()
        }
    }
}
