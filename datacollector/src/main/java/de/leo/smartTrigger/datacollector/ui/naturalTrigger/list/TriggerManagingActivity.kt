package de.leo.smartTrigger.datacollector.ui.naturalTrigger.list

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService
import de.leo.smartTrigger.datacollector.datacollection.database.JITAI_ID
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.ui.ServiceManagingActivity
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity
import de.leo.smartTrigger.datacollector.ui.notifications.NotificationService
import de.leo.smartTrigger.datacollector.utils.PermissionUtils
import de.leo.smartTrigger.datacollector.utils.UPDATE_JITAI
import kotlinx.android.synthetic.main.dialog_enter_user_name.view.*
import kotlinx.android.synthetic.main.activity_trigger_list.*
import org.jetbrains.anko.commit
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast


/**
 * Created by Leo on 15.11.2017.
 */

class TriggerManagingActivity : AppCompatActivity(),
                                TriggerUpdater {
    private val db: JitaiDatabase by lazy { JitaiDatabase.getInstance(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trigger_list)
        setSupportActionBar(findViewById(R.id.trigger_list_toolbar))

        val models = db.allNaturalTriggerModels()
        triggerListView.layoutManager = LinearLayoutManager(this)
        triggerListView.adapter = TriggerListRecyclerViewAdapter(this,
                                                                 models,
                                                                 this)
        floatingActionButton2.setOnClickListener { addTrigger() }
        checkPermission()
        createNotificationChannel()
        postDailyNotifier()
        val userName = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(getString(R.string.user_name), null)
        if (userName == null)
            promptEnterUserName()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_trigger_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.getItemId()) {
            R.id.action_settings -> startActivity(Intent(this, ServiceManagingActivity::class.java))
            else                 -> {
            }
        }
        return true
    }

    override fun deleteNaturalTrigger(id: Int) {
        if (id == -2)
            toast("Lange drücken um zu löschen.")
        else if (id == -1)
            toast("NaturalTrigger muss zuerst deaktiviert werden.")
        else {
            db.deleteNaturalTrigger(id)
            updateDataset()
        }
        return
    }

    override fun updateNaturalTrigger(naturalTrigger: Int, active: Boolean) {
        db.updateNaturalTrigger(naturalTrigger, active)
        updateDataset()
        updateService(naturalTrigger)
    }

    private fun updateDataset() {
        triggerListView.swapAdapter(TriggerListRecyclerViewAdapter(this,
                                                                   db.allNaturalTriggerModels(),
                                                                   this), false)
    }

    private fun addTrigger() {
        val intent = Intent(this, CreateTriggerActivity::class.java)
        startActivity(intent)
    }

    private fun updateService(trigger: Int) {
        startService(intentFor<DataCollectorService>()
                         .setAction(UPDATE_JITAI)
                         .putExtra(JITAI_ID, trigger)
                    )
    }

    override fun onResume() {
        super.onResume()
        updateDataset()
    }

    private fun promptEnterUserName() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_enter_user_name, null)
        view.username.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        view.username.imeOptions = EditorInfo.IME_ACTION_DONE
        builder.setView(view)
        builder.setTitle(getString(R.string.enter_user_name))
        builder.setPositiveButton("OK") { _, _ -> }
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.setOnShowListener {
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener { _ ->
                finishDialog(view, dialog)
            }
        }
        view.username.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                finishDialog(view, dialog)
                true
            }
            false
        }
        dialog.show()
    }

    private fun finishDialog(view: View, dialog: AlertDialog) {
        val name = view.username.text.toString()
        if (name.isNotBlank()) {
            PreferenceManager.getDefaultSharedPreferences(this).commit {
                putString(getString(R.string.user_name), name)
            }
            dialog.dismiss()
        } else {
            toast("Feld darf nicht leer sein.")
        }
    }

    private fun postDailyNotifier() {
        val intent = Intent(this, NotificationService::class.java)
        intent.action = NotificationService.SET_DAILY_REMINDER
        startService(intent)
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NotificationService.CHANNEL, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
        //foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.foreground_channel_name)
            val description = getString(R.string.foreground_channel_description)
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(NotificationService.FOREGROUND_CHANNEL, name,
                                              importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }


    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission
                .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this,
                                              2,
                                              android.Manifest.permission.ACCESS_FINE_LOCATION,
                                              true)
        }
    }

    fun createDummyString(repeat: Int, alpha: Char) = alpha.toString().repeat(repeat)
}
