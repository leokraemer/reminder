package com.example.leo.datacollector.activityRecording

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.leo.datacollector.R
import com.example.leo.datacollector.database.SqliteDatabase
import kotlinx.android.synthetic.main.recordings_activity.*

/**
 * Created by Leo on 28.11.2017.
 */
class RecordingsListActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recordings_activity)
        val db = SqliteDatabase.getInstance(this)
        val cursor = db.getRecordings();
        recordingsList.adapter = RecordingListAdapter(this, cursor)
        recordingsList.setOnItemClickListener { parent, view, position, id ->
            cursor.moveToPosition(position)
            openRecording(cursor.getInt(cursor.getColumnIndex(RECORDING_NUMBER)))
        }
    }

    private fun openRecording(recordingId: Int) {
        val intent = Intent(this, RecordViewActivity::class.java)
        intent.putExtra(RECORDING_ID, recordingId)
        startActivity(intent)
    }
}