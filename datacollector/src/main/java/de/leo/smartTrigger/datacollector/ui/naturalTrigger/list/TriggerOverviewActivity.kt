package de.leo.smartTrigger.datacollector.ui.naturalTrigger.list

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.JITAI_ID
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.updateNaturalTriggerReminderCardView
import kotlinx.android.synthetic.main.activity_trigger_overview.*
import kotlinx.android.synthetic.main.naturaltriggerview.*

class TriggerOverviewActivity : AppCompatActivity() {

    val db by lazy { JitaiDatabase.getInstance(this) }
    val model: NaturalTriggerModel by lazy { db.getNaturalTrigger(jitaiId) }
    private val username: String by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
            .getString(getString(R.string.user_name), null)
    }

    val jitaiId: Int by lazy { intent?.getIntExtra(JITAI_ID, -1) ?: -1 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppBaseTheme_Light)
        setContentView(R.layout.activity_trigger_overview)
        title = "Ziel Detailalsicht"
        goal.text = model.goal
        message.text = model.message
        situation.text = model.situation
        updateNaturalTriggerReminderCardView(model, reminder_card)
    }
}