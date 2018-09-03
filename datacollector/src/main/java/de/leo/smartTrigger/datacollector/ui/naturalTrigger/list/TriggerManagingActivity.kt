package de.leo.smartTrigger.datacollector.ui.naturalTrigger.list

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.CursorAdapter
import android.widget.ListAdapter
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService
import de.leo.smartTrigger.datacollector.datacollection.database.JITAI_ID
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity
import de.leo.smartTrigger.datacollector.utils.UPDATE_JITAI
import kotlinx.android.synthetic.main.trigger_list.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast

/**
 * Created by Leo on 15.11.2017.
 */

class TriggerManagingActivity : Activity(),
                                TriggerUpdater {
    override fun deleteNaturalTrigger(id: Int) {
        if (id == -2)
            toast("Lange drücken um zu löschen.")
        else if (id == -1)
            toast("NaturalTrigger muss zuerst deaktiviert werden.")
        else {
            db.deleteNaturalTrigger(id)
            updateDataset()
        }
        return
    }


    override fun updateNaturalTrigger(trigger: Int, active: Boolean) {
        db.updateNaturalTrigger(trigger, active)
        updateDataset()
        updateService(trigger)
    }

    private lateinit var listAdapter: ListAdapter

    private fun updateDataset() {
        (triggerListView.adapter as CursorAdapter).changeCursor(db.allNaturalTrigger())
    }

    private lateinit var cursor: Cursor
    private val db: JitaiDatabase by lazy { JitaiDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.trigger_list)
        cursor = db.allNaturalTrigger()
        listAdapter = TriggerListAdapter(this,
                                         cursor,
                                         this)
        triggerListView.adapter = listAdapter
        floatingActionButton2.setOnClickListener { addTrigger() }
    }

    private fun addTrigger() {
        val intent = Intent(this, CreateTriggerActivity::class.java)
        startActivity(intent)
    }

    private fun updateService(trigger: Int) {
        startService(intentFor<DataCollectorService>()
                         .setAction(UPDATE_JITAI)
                         .putExtra(JITAI_ID, trigger)
                    )
    }

    override fun onResume() {
        super.onResume()
        (triggerListView.adapter as CursorAdapter).changeCursor(db.allNaturalTrigger())
    }


}
