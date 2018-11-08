package de.leo.smartTrigger.datacollector.ui.naturalTrigger.list

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.BOTTOM
import androidx.constraintlayout.widget.ConstraintSet.TOP
import androidx.recyclerview.widget.RecyclerView
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

    private var expandedPosition = -1

    init {
        setHasStableIds(true)

    }

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
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

    override fun getItemId(position: Int): Long = myDataset[position].ID.toLong()

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
            if (adapterPosition == expandedPosition) expand() else collapse()
            with(itemView) {
                updateNaturalTriggerReminderCardView(model, this)
                goal.text = model.goal
                situation.text = model.situation
                message.text = model.message
                active_toggle_button.isChecked = model.active
            }
        }

        private fun expand() {
            with(itemView) {
                situation.visibility = View.VISIBLE
                situationDescription.visibility = View.VISIBLE
                message.visibility = View.VISIBLE
                message.maxLines = Integer.MAX_VALUE
                situation.maxLines = Integer.MAX_VALUE
                goal.maxLines = Integer.MAX_VALUE
                messageDescription.visibility = View.VISIBLE
                copy.visibility = View.VISIBLE
                delete.visibility = View.VISIBLE
                edit.visibility = View.VISIBLE
                ConstraintSet().apply {
                    clone(cardLayout as ConstraintLayout)
                    connect(R.id.active_toggle_button, TOP, R.id.situation, BOTTOM)
                    applyTo(cardLayout as ConstraintLayout)
                }
                expandButton.setImageResource(R.drawable.baseline_expand_less_black_36)
            }
        }

        private fun collapse() {
            with(itemView) {
                situation.visibility = View.INVISIBLE
                situationDescription.visibility = View.INVISIBLE
                message.visibility = View.INVISIBLE
                messageDescription.visibility = View.INVISIBLE
                message.maxLines = 1
                situation.maxLines = 1
                goal.maxLines = 5
                copy.visibility = View.INVISIBLE
                delete.visibility = View.INVISIBLE
                edit.visibility = View.INVISIBLE
                ConstraintSet().apply {
                    clone(cardLayout)
                    connect(R.id.active_toggle_button, TOP, R.id.goal, BOTTOM)
                    applyTo(cardLayout)
                }
                expandButton.setImageResource(R.drawable.outline_expand_more_white_36)
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
                expandButton.setOnClickListener {
                    if (expandedPosition == adapterPosition)
                        expandedPosition = -1
                    else
                        expandedPosition = adapterPosition
                    notifyDataSetChanged()
                }
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