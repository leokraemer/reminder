package de.leo.fingerprint.datacollector.ui.naturalTrigger.list

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.*
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.updateNaturalTriggerReminderCardView
import kotlinx.android.synthetic.main.trigger_list_element.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.onLongClick


/**
 * Created by Leo on 22.01.2018.
 */
class TriggerListAdapter(context: Context, c: Cursor, val triggerUpdater: TriggerUpdater) :
    CursorAdapter
    (context,
     c,
     true) {
    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        val layoutInflater: LayoutInflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.trigger_list_element, parent, false)
        return view
    }

    private val db: JitaiDatabase by lazy { JitaiDatabase.getInstance(context) }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        view?.apply {
            cursor?.let {
                val model = db.getNaturalTrigger(it)
                updateNaturalTriggerReminderCardView(model, view)
                goal.text = model.goal
                situation.text = model.situation
                message.text = model.message
                val jitaiId = cursor.getInt(cursor.getColumnIndex(ID))
                view.active_toggle_button.isChecked = cursor.getInt(cursor.getColumnIndex(
                    NATURAL_TRIGGER_ACTIVE)) > 0
                //hook to update the jitai when the toggle button is pressed
                view.active_toggle_button.onClick {
                    triggerUpdater.updateNaturalTrigger(jitaiId,
                                                        view.active_toggle_button.isChecked)
                }
                view.delete.onLongClick {
                    if (!view.active_toggle_button.isChecked)
                        triggerUpdater.deleteNaturalTrigger(jitaiId)
                    else
                        triggerUpdater.deleteNaturalTrigger(-1)
                }
                view.delete.onClick {
                    triggerUpdater.deleteNaturalTrigger(-2)
                }
            }
        }
    }
}

interface TriggerUpdater {
    fun updateNaturalTrigger(jitai: Int, active: Boolean)
    fun deleteNaturalTrigger(id: Int)
}