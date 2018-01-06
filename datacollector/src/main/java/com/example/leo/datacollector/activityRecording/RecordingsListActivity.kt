package com.example.leo.datacollector.activityRecording

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.CursorAdapter
import com.example.leo.datacollector.R
import com.example.leo.datacollector.database.ID
import com.example.leo.datacollector.database.SqliteDatabase
import kotlinx.android.synthetic.main.recordings_activity.*

/**
 * Created by Leo on 28.11.2017.
 */
class RecordingsListActivity : Activity() {
    lateinit var db: SqliteDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recordings_activity)
        db = SqliteDatabase.getInstance(this)
        recordingsList.adapter = RecordingListAdapter(this, db.getRecordings())
        recordingsList.setOnItemClickListener { parent, view, position, id ->
            val cursor = (recordingsList.adapter as RecordingListAdapter).getCursor()
            cursor.moveToPosition(position)
            openRecording(cursor.getInt(cursor.getColumnIndex(ID)))
        }
    }

    override fun onResume() {
        super.onResume()
        val cursor = (recordingsList.adapter as CursorAdapter).swapCursor(db.getRecordings())
        cursor.close()
    }

    private fun openRecording(recordingId: Int) {
        val intent = Intent(this, RecordViewActivity::class.java)
        intent.putExtra(RECORDING_ID, recordingId)
        startActivity(intent)
    }
}