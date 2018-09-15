package de.leo.smartTrigger.datacollector.ui.naturalTrigger.list

import android.content.Context
import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.ID
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.database.NATURAL_TRIGGER_ACTIVE
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity.Companion.EDIT
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity.Companion.EDIT_COPY
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity.Companion.NATURALTRIGGER_ID
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.updateNaturalTriggerReminderCardView
import kotlinx.android.synthetic.main.trigger_list_element.view.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.onLongClick

interface TriggerUpdater {
    fun updateNaturalTrigger(naturalTrigger: Int, active: Boolean)
    fun deleteNaturalTrigger(id: Int)
}

class TriggerListRecyclerViewAdapter(private val context: Context,
                                     private val myDataset: List<NaturalTriggerModel>,
                                     val triggerUpdater: TriggerUpdater) :
    RecyclerView.Adapter<TriggerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TriggerViewHolder {
        val triggerListElement = LayoutInflater.from(parent.context)
            .inflate(R.layout.trigger_list_element, parent, false)
        return TriggerViewHolder(triggerListElement)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: TriggerViewHolder, position: Int) {
        val model = myDataset[position]
        holder.itemView?.apply {
            updateNaturalTriggerReminderCardView(model, this)
            goal.text = model.goal
            situation.text = model.situation
            messageCard.text = model.message
            val jitaiId = model.ID
            active_toggle_button.isChecked = model.active
            //hook to update the jitai when the toggle button is pressed
            active_toggle_button.onClick {
                triggerUpdater.updateNaturalTrigger(jitaiId,
                                                    active_toggle_button.isChecked)
            }
            this.delete.onLongClick {
                if (!active_toggle_button.isChecked)
                    triggerUpdater.deleteNaturalTrigger(jitaiId)
                else
                    triggerUpdater.deleteNaturalTrigger(-1)
            }
            this.delete.onClick {
                triggerUpdater.deleteNaturalTrigger(-2)
            }
            this.edit.onClick {
                val context = this@TriggerListRecyclerViewAdapter.context
                context.alert("Möchten sie diesen Trigger bearbeiten oder eine Kopie?",
                              "Trigger bearbeiten") {
                    positiveButton("Bearbeiten") {
                        context.startActivity(context.intentFor<CreateTriggerActivity>(
                            NATURALTRIGGER_ID to model.ID).setAction(EDIT))
                    }
                    negativeButton("Kopie bearbeiten") {
                        context.startActivity(context.intentFor<CreateTriggerActivity>(
                            NATURALTRIGGER_ID to model.ID).setAction(EDIT_COPY))
                    }
                }.show()
            }
        }
    }


    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}


class TriggerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)