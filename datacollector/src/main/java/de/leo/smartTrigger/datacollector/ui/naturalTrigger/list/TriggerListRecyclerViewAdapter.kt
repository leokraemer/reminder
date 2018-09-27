package de.leo.smartTrigger.datacollector.ui.naturalTrigger.list

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService
import de.leo.smartTrigger.datacollector.datacollection.database.JITAI_ID
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity.Companion.EDIT
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity.Companion.NATURALTRIGGER_ID
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.updateNaturalTriggerReminderCardView
import de.leo.smartTrigger.datacollector.utils.UPDATE_JITAI
import kotlinx.android.synthetic.main.trigger_list_element.view.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast

class TriggerListRecyclerViewAdapter(private val context: Context,
                                     private val myDataset: MutableList<NaturalTriggerModel>) :
    RecyclerView.Adapter<TriggerListRecyclerViewAdapter.TriggerViewHolder>() {

    private val db: JitaiDatabase by lazy { JitaiDatabase.getInstance(context) }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TriggerViewHolder {
        val triggerListElement = LayoutInflater.from(parent.context)
            .inflate(R.layout.trigger_list_element, parent, false)
        return TriggerViewHolder(triggerListElement)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: TriggerViewHolder, position: Int) {
        holder.bind(myDataset[position])
    }

    override fun getItemId(position: Int): Long = myDataset.get(position).ID.toLong()

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size

    private fun updateService(trigger: Int) {
        context.startService(context.intentFor<DataCollectorService>()
                                 .setAction(UPDATE_JITAI)
                                 .putExtra(JITAI_ID, trigger)
                            )
    }

    inner class TriggerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(model: NaturalTriggerModel) {
            with(itemView) {
                updateNaturalTriggerReminderCardView(model, this)
                goal.text = model.goal
                situation.text = model.situation
                messageCard.text = model.message
                active_toggle_button.isChecked = model.active
            }
        }

        init {
            with(itemView) {
                active_toggle_button.setOnClickListener {
                    adapterPosition.let { position ->
                        db.updateNaturalTrigger(myDataset[position].ID,
                                                active_toggle_button.isChecked)
                        myDataset[position].active = active_toggle_button.isChecked
                        updateService(myDataset[position].ID)
                    }
                }
                delete.setOnLongClickListener {
                    if (!active_toggle_button.isChecked) {
                        adapterPosition.let { position ->
                            db.deleteNaturalTrigger(myDataset[position].ID)
                            updateService(myDataset[position].ID)
                            myDataset.removeAt(position)
                            notifyItemRemoved(position)
                            Log.d("delete", myDataset.toString())
                            getContext().toast("Gelöscht.")
                            true
                        }
                    } else
                        getContext().toast("Muss zuerst deaktiviert werden.")
                    true
                }
                delete.setOnClickListener {
                    getContext().toast("Lange drücken um zu löschen.")
                }
                edit.setOnClickListener {
                    getContext().startActivity(getContext().intentFor<CreateTriggerActivity>(
                        NATURALTRIGGER_ID to myDataset[adapterPosition].ID).setAction(EDIT))
                }
                copy.setOnClickListener { copy() }
            }
        }

        fun copy() {
            val model = db.getNaturalTrigger(myDataset[adapterPosition].ID)
            //id to -1 to copy
            model.ID = -1
            //get new ID
            model.ID = db.enterNaturalTrigger(model)
            myDataset.add(model)
            Log.d("copy", myDataset.toString())
            notifyItemInserted(itemCount - 1)
            updateService(model.ID)
            context.toast("Kopiert.")
        }
    }
}