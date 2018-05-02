package de.leo.fingerprint.datacollector.jitai.manage

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.widget.CursorAdapter
import android.widget.ListAdapter
import cz.msebera.android.httpclient.util.Args
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.DataCollectorService
import de.leo.fingerprint.datacollector.utils.UPDATE_JITAI
import de.leo.fingerprint.datacollector.widget.ClassificationWidget
import kotlinx.android.synthetic.main.jitai_list.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import java.util.*
import kotlin.reflect.KFunction1

/**
 * Created by Leo on 15.11.2017.
 */

class JitaiManagingActivity : Activity(), JitaiUpdater {
    override fun deleteJitai(id: Int) {
        if (id == -2)
            toast("Lange drücken um zu löschen.")
        else if (id == -1)
            toast("Jitai muss zuerst deaktiviert werden.")
        else {
            db.deleteJitai(id)
            updateDataset()
        }
        return
    }


    override fun updateJitai(jitai: Int, active: Boolean) {
        db.updateJitai(jitai, active)
        updateDataset()
    }

    private lateinit var listAdapter: ListAdapter

    private fun updateDataset() {
        (jitaiListView.adapter as CursorAdapter).changeCursor(db.allJitai())
        startService(intentFor<DataCollectorService>().setAction(UPDATE_JITAI))
        val appWidgetManager = AppWidgetManager.getInstance(application)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(getApplication(),
                                                                 ClassificationWidget::class.java))
        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_jitai_list);
    }

    private lateinit var cursor: Cursor
    private lateinit var db: JitaiDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.jitai_list)
        db = JitaiDatabase.getInstance(this)
        cursor = db.allJitai()
        listAdapter = JitaiListAdapter(this, cursor, this)
        jitaiListView.adapter = listAdapter
        floatingActionButton2.setOnClickListener { addJitai() }
    }

    private fun addJitai() {
        val intent = Intent(this, AddJitaiActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        (jitaiListView.adapter as CursorAdapter).changeCursor(db.allJitai())
    }



}
