package de.leo.smartTrigger.datacollector.ui.naturalTrigger.list

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.testing.createTestNaturalTriggers
import de.leo.smartTrigger.datacollector.ui.ServiceManagingActivity
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import de.leo.smartTrigger.datacollector.ui.notifications.NotificationService
import de.leo.smartTrigger.datacollector.utils.PermissionUtils
import kotlinx.android.synthetic.main.activity_trigger_list.*
import kotlinx.android.synthetic.main.dialog_enter_user_name.view.*
import org.jetbrains.anko.commit
import org.jetbrains.anko.toast


/**
 * Created by Leo on 15.11.2017.
 */

class TriggerManagingActivity : AppCompatActivity() {

    private val db: JitaiDatabase by lazy { JitaiDatabase.getInstance(this) }

    private lateinit var dataset: MutableList<NaturalTriggerModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trigger_list)
        setSupportActionBar(findViewById(R.id.trigger_list_toolbar))

        dataset = db.allNaturalTriggerModels().toMutableList()

        triggerListView.layoutManager = LinearLayoutManager(this)
        triggerListView.adapter = TriggerListRecyclerViewAdapter(this, dataset)
        //triggerListView.itemAnimator = SlideInLeftAnimator(OvershootInterpolator())
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
            R.id.action_settings  -> startActivity(Intent(this,
                                                          ServiceManagingActivity::class.java))
            R.id.action_test_data -> {
                createTestNaturalTriggers().forEach {
                    db.enterNaturalTrigger(it)
                }
                updateDataset()
            }
            else                  -> {
            }
        }
        return true
    }

    private fun updateDataset() {
        val newDataset = db.allNaturalTriggerModels().toMutableList()
        if (newDataset != dataset) {
            dataset = newDataset
            triggerListView.adapter = TriggerListRecyclerViewAdapter(this, dataset)
        }
    }

    private fun addTrigger() {
        val intent = Intent(this, CreateTriggerActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateDataset()
    }

    ////////////////////////////// app start handling

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
            // Permission to access the locationName is missing.
            PermissionUtils.requestPermission(this,
                                              2,
                                              android.Manifest.permission.ACCESS_FINE_LOCATION,
                                              true)
        }
    }
}
