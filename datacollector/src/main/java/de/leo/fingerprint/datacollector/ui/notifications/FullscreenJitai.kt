package de.leo.fingerprint.datacollector.ui.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.*
import kotlinx.android.synthetic.main.activity_full_screen_jitai.*

class FullscreenJitai : AppCompatActivity() {

    val db by lazy { JitaiDatabase.getInstance(this) }

    val vibrator by lazy { getSystemService(VIBRATOR_SERVICE) as Vibrator }
    val notificationService by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppBaseTheme_Light)
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
    }


    private fun vibrate() {
        // 0 : Start without a delay
        // 150 : Vibrate for 150 milliseconds
        // 200 : Pause for 200 milliseconds
        // 150 : Vibrate for 150 milliseconds
        val mVibratePattern = longArrayOf(0, 150, 200, 150)
        // -1 : Do not repeat this pattern
        // pass 0 if you want to repeat this pattern from 0th index
        vibrator.vibrate(mVibratePattern, -1)
    }

}
