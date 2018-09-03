package de.leo.smartTrigger.datacollector.ui.activityRecording

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.CursorAdapter
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.ID
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import kotlinx.android.synthetic.main.recordings_activity.*

/**
 * Created by Leo on 28.11.2017.
 */
class RecordingsListActivity : Activity() {
    lateinit var db: JitaiDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recordings_activity)
        db = JitaiDatabase.getInstance(this)
        recordingsList.adapter = RecordingListAdapter(this, db.getRecordings())
        recordingsList.setOnItemClickListener { _, _, position, _ ->
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