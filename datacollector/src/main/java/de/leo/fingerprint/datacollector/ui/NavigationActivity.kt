package de.leo.fingerprint.datacollector.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.ui.GeofencesWithPlayServices.GeofenceMapActivity
import de.leo.fingerprint.datacollector.ui.activityRecording.RecordingActivity
import de.leo.fingerprint.datacollector.ui.activityRecording.RecordingsListActivity
import de.leo.fingerprint.datacollector.ui.compare.CompareActivity
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity
import de.leo.fingerprint.datacollector.ui.naturalTrigger.list.TriggerManagingActivity
import de.leo.fingerprint.datacollector.ui.notifications.NotificationService
import de.leo.fingerprint.datacollector.ui.notifications.NotificationService.Companion.CHANNEL
import de.leo.fingerprint.datacollector.ui.notifications.NotificationService.Companion.FOREGROUND_CHANNEL
import de.leo.fingerprint.datacollector.ui.notifications.NotificationService.Companion.SET_DAILY_REMINDER
import de.leo.fingerprint.datacollector.utils.PermissionUtils
import kotlinx.android.synthetic.main.dialog_enter_user_name.view.*
import org.jetbrains.anko.commit
import org.jetbrains.anko.toast


class NavigationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        checkPermission()
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
        builder.setView(view)
        builder.setTitle(getString(R.string.enter_user_name))
        builder.setPositiveButton("OK") { _, _ -> }
        val dialog = builder.create()
        dialog.setOnShowListener(DialogInterface.OnShowListener {
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener { _ ->
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
        })
        dialog.show()
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
        if (ContextCompat.checkSelfPermission(this,
                                              Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, 1,
                                              Manifest.permission.RECORD_AUDIO, true)
        }
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
        val intent = Intent(this, EntryActivity::class.java)
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
