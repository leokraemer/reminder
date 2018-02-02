package com.example.leo.datacollector.jitai.manage

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.CursorAdapter
import android.widget.ListAdapter

import com.example.leo.datacollector.R
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.datacollection.DataCollectorService
import com.example.leo.datacollector.utils.START_RECORDING
import com.example.leo.datacollector.utils.STOP_RECORDING
import com.example.leo.datacollector.utils.UPDATE_JITAI
import com.example.leo.datacollector.widget.ClassificationWidget

import kotlinx.android.synthetic.main.jitai_list.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import android.content.ComponentName


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
                                                                 ClassificationWidget::class.java!!))
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
