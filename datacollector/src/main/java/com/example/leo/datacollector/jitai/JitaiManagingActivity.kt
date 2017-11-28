package com.example.leo.datacollector.jitai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ListAdapter
import android.widget.Toast

import com.example.leo.datacollector.R

import kotlinx.android.synthetic.main.jitai_list.*

/**
 * Created by Leo on 15.11.2017.
 */

class JitaiManagingActivity : Activity() {
    private lateinit var listAdapter: ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.jitai_list)
        listAdapter = JitaiListAdapter(this)
        jitaiListView.adapter = listAdapter
        floatingActionButton2.setOnClickListener { addJitai() }
    }

    private fun addJitai() {
        if (listAdapter.count - 1 < JitaiListAdapter.JITAIS.size) {
            val intent = Intent(this, AddJitaiActivity::class.java)
            intent.putExtra(AddJitaiActivity.INTENT_KEY, JitaiListAdapter.JITAIS[listAdapter.count])
            startActivity(intent)
        } else {
            Toast.makeText(this, "Zu viele Jitai", Toast.LENGTH_SHORT).show()
        }
    }
}
