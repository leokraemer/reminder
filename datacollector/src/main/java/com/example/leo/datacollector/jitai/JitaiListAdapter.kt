package com.example.leo.datacollector.jitai

import android.content.Context
import android.content.SharedPreferences
import android.database.DataSetObserver
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import com.example.leo.datacollector.R
import kotlinx.android.synthetic.main.jitai_list_element.view.*
import com.google.gson.Gson


/**
 * Created by Leo on 15.11.2017.
 */
class JitaiListAdapter(val context: Context?) : ListAdapter {
    companion object {
        val JITAIS: Array<String> = arrayOf("JITAI1", "JITAI2", "JITAI3", "JITAI4", "JITAI5")
    }

    private var jitais: MutableList<Jitai> = mutableListOf()

    val observers: MutableList<DataSetObserver> = mutableListOf<DataSetObserver>()

    init {
        val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val gson = Gson()
        for (jitai_name in JITAIS) {
            val json: String = sp.getString(jitai_name, "")
            if (json != "")
                jitais.add(gson.fromJson<Jitai>(json, Jitai::class.java))
        }
    }

    override fun isEmpty(): Boolean = jitais.isEmpty()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        if (convertView != null) {
            view = convertView
        } else {
            val layoutInflater: LayoutInflater = this.context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = layoutInflater.inflate(R.layout.jitai_list_element, parent, false)
        }
        view.jitai_name.text = jitais[position].name
        view.geofence.text = jitais[position].getGeogenceString()
        view.text.text = jitais[position].message
        return view
    }

    override fun getItemViewType(position: Int): Int = 0

    override fun getItem(position: Int): Jitai = jitais[position]

    override fun getViewTypeCount(): Int = 1

    override fun isEnabled(position: Int): Boolean = true

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    override fun areAllItemsEnabled(): Boolean = true


    override fun registerDataSetObserver(observer: DataSetObserver?) {
        observers.add(observer!!)
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver?) {
        observers.remove(observer)
    }

    override fun getCount(): Int = jitais.size

    fun add(jitai: Jitai) {
        jitais.add(jitai)
        observers.map { it.onChanged() }
    }

    fun remove(jitai: Jitai) {
        if (jitais.remove(jitai)) {
            observers.map { it.onChanged() }
        }
    }

}
