package de.leo.smartTrigger.datacollector.ui.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.os.Bundle
import android.os.PowerManager
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.*
import de.leo.smartTrigger.datacollector.jitai.manage.Jitai
import kotlinx.android.synthetic.main.activity_full_screen_jitai.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.intentFor
import java.util.concurrent.TimeUnit


class FullscreenJitai : AppCompatActivity() {

    val STOP_SERVICE_REQUEST_CODE = 21470
    val db by lazy { JitaiDatabase.getInstance(this) }

    val vibrator by lazy { getSystemService(VIBRATOR_SERVICE) as Vibrator }
    val notificationService by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private var jitaiId: Int = -1
    private var sensorDataId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //handle vibration
        if (!(getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) {
            vibrator.vibrate(longArrayOf(100L, 200L, 100L, 200L, 100L, 200L, 500L),
                             1,
                             AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            val mReceiver = ScreenEventReceiver()
            registerReceiver(mReceiver, filter)
            GlobalScope.launch {
                delay(TimeUnit.MINUTES.toMillis(2))
                vibrator.cancel()
            }


        } else {
            vibrator.cancel()
        }
        setTheme(R.style.AppBaseTheme_Light_Fullscreen)
        setContentView(R.layout.activity_full_screen_jitai)
        val event = intent?.getIntExtra(JITAI_EVENT, -1) ?: -1
        val goalText = intent?.getStringExtra(JITAI_GOAL) ?: ""
        val messageText = intent?.getStringExtra(JITAI_MESSAGE) ?: ""
        jitaiId = intent?.getIntExtra(JITAI_ID, -1) ?: -1
        sensorDataId = intent?.getLongExtra(JITAI_EVENT_SENSORDATASET_ID, -1) ?: -1L
        message.text = messageText
        goal.text = goalText
        //cancel the notification when the fullscreen app is launched
        notificationService.cancel(NotificationService.NOTIFICATIONIDMODIFYER + jitaiId)
        close.setOnClickListener {
            val intent =
                applicationContext.intentFor<FullscreenJitaiSurvey>(
                    JITAI_EVENT to Jitai.NOTIFICATION_DELETED,
                    JITAI_ID to jitaiId,
                    JITAI_EVENT_SENSORDATASET_ID to sensorDataId)
                    .setAction("Jitai_user_deleted")
            startActivity(intent)
            finish()
        }
        play.setOnClickListener {
            val intent =
                applicationContext.intentFor<FullscreenJitaiSurvey>(
                    JITAI_EVENT to Jitai.NOTIFICATION_SUCCESS,
                    JITAI_ID to jitaiId,
                    JITAI_EVENT_SENSORDATASET_ID to sensorDataId)
                    .setAction("jitai_correct")
            startActivity(intent)
            finish()
        }
        snooze.setOnClickListener {
            val intent =
                applicationContext.intentFor<FullscreenJitaiSurvey>(
                    JITAI_EVENT to Jitai.NOTIFICATION_SNOOZE,
                    JITAI_ID to jitaiId,
                    JITAI_EVENT_SENSORDATASET_ID to sensorDataId,
                    JITAI_GOAL to goalText,
                    JITAI_MESSAGE to messageText)
                    .setAction("jitai_correct")
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        val intent =
            applicationContext.intentFor<FullscreenJitaiSurvey>(
                JITAI_EVENT to Jitai.NOTIFICATION_DELETED,
                JITAI_ID to jitaiId,
                JITAI_EVENT_SENSORDATASET_ID to sensorDataId)
                .setAction("Jitai_user_deleted")
        startActivity(intent)
        finish()
    }

    inner class ScreenEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                //noop
            } else if (intent.action == Intent.ACTION_SCREEN_ON) {
                // and do whatever you need to do here
                vibrator.cancel()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        vibrator.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        vibrator.cancel()
    }
}