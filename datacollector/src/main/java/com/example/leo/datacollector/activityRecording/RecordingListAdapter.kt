package com.example.leo.datacollector.activityRecording

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import com.example.leo.datacollector.R
import com.example.leo.datacollector.database.ID
import com.example.leo.datacollector.database.RECORDING_NAME
import kotlinx.android.synthetic.main.jitai_list_element.view.*
import kotlinx.android.synthetic.main.recordings_listitem.view.*

/**
 * Created by Leo on 28.11.2017.
 */


class RecordingListAdapter(context: Context, c: Cursor) : CursorAdapter(context, c, true) {


    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        val layoutInflater: LayoutInflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.recordings_listitem, parent, false)
        return view
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        if (view != null) {
            view.recordingNumber.text = cursor!!.getInt(cursor.getColumnIndex(ID)).toString() +
                    ":"
            view.recordingName.text = cursor.getString(cursor.getColumnIndex(RECORDING_NAME))
                    .toString()
            view.intervention.text = "dummy" //cursor!!.getString(cursor!!.getColumnIndex(RECORDING_INTERVENTION)).toString()
        }
    }
}

