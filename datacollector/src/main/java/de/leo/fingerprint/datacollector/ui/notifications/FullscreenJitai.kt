package de.leo.fingerprint.datacollector.ui.notifications

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.*
import kotlinx.android.synthetic.main.activity_full_screen_jitai.*

class FullscreenJitai : AppCompatActivity() {

    val db: JitaiDatabase by lazy { JitaiDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_full_screen_jitai)
        val event = intent?.getIntExtra(JITAI_EVENT, -1) ?: -1
        val goalText = intent?.getStringExtra(JITAI_GOAL) ?: ""
        val messageText = intent?.getStringExtra(JITAI_MESSAGE) ?: ""
        val jitaiId = intent?.getIntExtra(JITAI_ID, -1) ?: -1
        val sensorDataId = intent?.getLongExtra(JITAI_EVENT_SENSORDATASET_ID, -1) ?: -1L
        message.text = messageText
        goal.text = goalText
    }

}
