package de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.jitai.MyGeofence
import kotlinx.android.synthetic.main.empty_view.view.*
import kotlinx.android.synthetic.main.geofence_dialog_list_item.view.*
import kotlinx.android.synthetic.main.geofence_list_dialog.view.*

/**
 * Created by Leo on 10.03.2018.
 */
class GeofenceListDialogFragment : DialogFragment() {

    // Use this instance of the interface to deliver action events
    lateinit var mListener: GeofenceDialogListener

    // Override the Fragment.onAttach() method to instantiate the GeofenceDialogListener
    override fun onAttach(activity: Context) {
        super.onAttach(activity);
        // Verify that the host activities implements the callback interface
        try {
            // Instantiate the GeofenceDialogListener so we can send events to the host
            mListener = activity as GeofenceDialogListener
        } catch (e: ClassCastException) {
            // The activities doesn't implement the interface, throw exception
            throw ClassCastException(activity.toString() + " must implement GeofenceDialogListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        dialog?.window?.setTitle("Ihre Orte")
        val view = inflater.inflate(R.layout.geofence_list_dialog, container, false)
        view.add_geofences_button.setOnClickListener { mListener.onCreateGeofence(); dialog?.cancel() }
        val list = JitaiDatabase.getInstance(context!!).getAllMyGeofencesDistinct()
        val empty = view.empty
        if (list.isEmpty()) {
            empty.setText("Keine Orte vorhanden.")
        } else {
            view.geofence_listview.adapter = GeofenceListAdapter(context!!, list)
            view.geofence_listview.onItemClickListener =
                AdapterView.OnItemClickListener { parent, view, position, id ->
                    mListener.onGeofenceSelected(list[position])
                    dismiss()
                }
        }
        view.geofence_listview.emptyView = empty
        return view
    }


    inner class GeofenceListAdapter(context: Context, list: List<MyGeofence>) :
        ArrayAdapter<MyGeofence>(context, R.layout.geofence_dialog_list_item, list) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context!!)
                .inflate(R.layout.geofence_dialog_list_item, parent, false)
            view.icon.setImageDrawable(
                context.resources.obtainTypedArray(R.array.geofence_icons)
                    .getDrawable(getItem(position).imageResId))
            view.geofence_name.setText(getItem(position).name)
            return view
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        mListener.onNoGeofenceSelected()
        super.onCancel(dialog)
    }
}
