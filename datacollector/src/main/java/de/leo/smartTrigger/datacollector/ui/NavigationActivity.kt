package de.leo.smartTrigger.datacollector.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService
import de.leo.smartTrigger.datacollector.ui.GeofencesWithPlayServices.GeofenceMapActivity
import de.leo.smartTrigger.datacollector.ui.activityRecording.RecordingActivity
import de.leo.smartTrigger.datacollector.ui.activityRecording.RecordingsListActivity
import de.leo.smartTrigger.datacollector.ui.compare.CompareActivity
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.list.TriggerManagingActivity
import de.leo.smartTrigger.datacollector.ui.notifications.NotificationService
import de.leo.smartTrigger.datacollector.ui.notifications.NotificationService.Companion.CHANNEL
import de.leo.smartTrigger.datacollector.ui.notifications.NotificationService.Companion.FOREGROUND_CHANNEL
import de.leo.smartTrigger.datacollector.ui.notifications.NotificationService.Companion.SET_DAILY_REMINDER
import de.leo.smartTrigger.datacollector.utils.PermissionUtils
import kotlinx.android.synthetic.main.dialog_enter_user_name.view.*
import org.jetbrains.anko.commit
import org.jetbrains.anko.toast


class NavigationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        checkPermission()
        createNotificationChannel()
        postDailyNotifier()
        val userName = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(getString(R.string.user_name), null)
        if (userName == null)
            promptEnterUserName()
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
        intent.action = SET_DAILY_REMINDER
        startService(intent)
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL, name, importance)
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
            val channel = NotificationChannel(FOREGROUND_CHANNEL, name,
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

    fun onButtonEntry(view: View) {
        val intent = Intent(this, ServiceManagingActivity::class.java)
        startActivity(intent)
    }

    fun onButtonRecorder(view: View) {
        val intent = Intent(this, RecordingActivity::class.java)
        startActivity(intent)
    }

    fun onButtonGeofence(view: View) {
        val intent = Intent(this, GeofenceMapActivity::class.java)
        startActivity(intent)
    }

    fun onButtonRecList(view: View) {
        val intent = Intent(this, RecordingsListActivity::class.java)
        startActivity(intent)
    }

    fun onButtonCompare(view: View) {
        val intent = Intent(this, CompareActivity::class.java)
        startActivity(intent)
    }

    fun onButtonTrigger(view: View) {
        val intent = Intent(this, CreateTriggerActivity::class.java)
        startActivity(intent)
    }

    fun onButtonTriggerList(view: View) {
        val intent = Intent(this, TriggerManagingActivity::class.java)
        startActivity(intent)
    }
}
