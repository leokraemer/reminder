package de.leo.smartTrigger.datacollector.ui.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.*
import de.leo.smartTrigger.datacollector.jitai.manage.Jitai.Companion.NOTIFICATION_DELETED
import de.leo.smartTrigger.datacollector.jitai.manage.Jitai.Companion.NOTIFICATION_SNOOZE
import de.leo.smartTrigger.datacollector.jitai.manage.Jitai.Companion.NOTIFICATION_SUCCESS
import de.leo.smartTrigger.datacollector.jitai.manage.Jitai.Companion.SURVEY_ABORD
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.updateNaturalTriggerReminderCardView
import kotlinx.android.synthetic.main.activity_fullscreen_jitai_dialog.*
import kotlinx.android.synthetic.main.naturaltriggerview.*

class FullscreenJitaiSurvey : AppCompatActivity() {

    val db by lazy { JitaiDatabase.getInstance(this) }
    private val username: String by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
            .getString(getString(R.string.user_name), null)
    }

    var jitaiId = -1
    var sensorDataId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppBaseTheme_Light)
        setContentView(R.layout.activity_fullscreen_jitai_dialog)
        val event = intent?.getIntExtra(JITAI_EVENT, -1) ?: -1
        title = when (event) {
            NOTIFICATION_DELETED -> "Falscher Moment"
            NOTIFICATION_SUCCESS -> "Richtiger Moment"
            NOTIFICATION_SNOOZE  -> "Snooze"
            else                 -> "unbekannter status"
        }
        val goalText = intent?.getStringExtra(JITAI_GOAL) ?: ""
        val messageText = intent?.getStringExtra(JITAI_MESSAGE) ?: ""
        jitaiId = intent?.getIntExtra(JITAI_ID, -1) ?: -1
        sensorDataId = intent?.getLongExtra(JITAI_EVENT_SENSORDATASET_ID, -1) ?: -1L
        val model = db.getNaturalTrigger(jitaiId)
        situation.text = model.situation
        updateNaturalTriggerReminderCardView(model, reminder_card)
        submit_survey.setOnClickListener {
            db.enterUserJitaiEvent(jitaiId, System.currentTimeMillis(),
                                   username,
                                   event,
                                   sensorDataId,
                                   scale(triggerRating.checkedRadioButtonId),
                                   scale(momentRating.checkedRadioButtonId),
                                   surveyText.text.toString())
            finish()
        }
    }

    private fun scale(id: Int): Int =
        when (id) {
            R.id.strong_disagree or R.id.strong_disagree1 -> 1
            R.id.disagree or R.id.disagree                -> 2
            R.id.neutral or R.id.neutral                  -> 3
            R.id.agree or R.id.agree1                     -> 4
            R.id.strong_agree or R.id.strong_agree1       -> 5
            else                                          -> -1
        }

    override fun onBackPressed() {
        db.enterUserJitaiEvent(jitaiId, System.currentTimeMillis(),
                               username,
                               SURVEY_ABORD,
                               sensorDataId,
                               scale(triggerRating.checkedRadioButtonId),
                               scale(momentRating.checkedRadioButtonId),
                               surveyText.text.toString())
        super.onBackPressed()
    }
}
