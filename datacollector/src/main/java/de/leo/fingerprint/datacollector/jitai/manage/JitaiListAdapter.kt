package de.leo.fingerprint.datacollector.jitai.manage

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.database.*
import de.leo.fingerprint.datacollector.jitai.Location.GeofenceTrigger
import de.leo.fingerprint.datacollector.jitai.TimeTrigger
import com.fatboyindustrial.gsonjavatime.Converters
import kotlinx.android.synthetic.main.jitai_list_element.view.*
import com.google.gson.GsonBuilder
import de.leo.fingerprint.datacollector.datacollection.database.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.onLongClick


/**
 * Created by Leo on 22.01.2018.
 */
class JitaiListAdapter(context: Context, c: Cursor, val jitaiUpdater: JitaiUpdater) : CursorAdapter
                                                                                      (context,
                                                                                       c,
                                                                                       true) {
    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        val layoutInflater: LayoutInflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.jitai_list_element, parent, false)
        return view
    }

    private val gson = Converters.registerAll(GsonBuilder()).create()

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        if (view != null) {
            view.jitai_name.text = cursor!!.getString(cursor.getColumnIndex(JITAI_GOAL))
            view.geofence.text = gson.fromJson(cursor.getString(cursor.getColumnIndex
            (JITAI_GEOFENCE)), GeofenceTrigger::class.java).toString()
            view.time.text = gson.fromJson(
                cursor.getString(cursor.getColumnIndex(JITAI_TIME_TRIGGER)),
                TimeTrigger::class.java).toString()
            view.weather.text = cursor.getInt(cursor.getColumnIndex(JITAI_WEATHER)).toString()
            view.message.text = cursor.getString(cursor.getColumnIndex(JITAI_MESSAGE))
            val jitaiId = cursor.getInt(cursor.getColumnIndex(ID))
            view.active_toggle_button.isChecked = cursor.getInt(cursor.getColumnIndex(
                JITAI_ACTIVE)) > 0
            //hook to update the jitai when the toggle button is pressed
            view.active_toggle_button.onClick {
                jitaiUpdater.updateJitai(jitaiId, view.active_toggle_button.isChecked)
            }
            view.delete.onLongClick {
                if(!view.active_toggle_button.isChecked)
                    jitaiUpdater.deleteJitai(jitaiId)
                else
                    jitaiUpdater.deleteJitai(-1) }
            view.delete.onClick {
                    jitaiUpdater.deleteJitai(-2) }
        }
    }
}

interface JitaiUpdater {
    fun updateJitai(jitai: Int, active: Boolean)
    fun deleteJitai(id: Int)
}