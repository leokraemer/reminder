package de.leo.smartTrigger.datacollector.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import kotlinx.android.synthetic.main.activity_entry.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ServiceManagingActivity : AppCompatActivity() {
    internal var TAG = "EntryActivity"
    internal val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0
    internal val MY_PERMISSIONS_REQUEST_ACCESS_COURSE_LOCATION = 1
    internal val START_SERVICE = 1
    internal var state = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        startScheduledUpdate()
        updateUI()
    }

    private val REYOUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 12312

    fun exportDB(view: View?) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                                              arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                              REYOUEST_PERMISSION_WRITE_EXTERNAL_STORAGE)

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.

        } else {
            val db = JitaiDatabase.getInstance(this)
            db.exportDb()
        }

    }

    fun onClickStartService(view: View) {

        if (!CheckGPS()) {
            state = START_SERVICE
            return
        }
        if (!checkPermission()) {
            return
        }

        val running = isMyServiceRunning(DataCollectorService::class.java)
        if (!running) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val userName = sharedPreferences.getString(getString(R.string.user_name), "userName")
            if (userName == "userName" || userName == "Name") {
                Toast.makeText(this, "please type in your name in settings", Toast.LENGTH_SHORT)
                    .show()
                Log.d(TAG, "please type in your name in settings")
                return
            } else {
                val intent = Intent(this, DataCollectorService::class.java)
                startService(intent)
                updateUI()
            }
        }
    }

    fun onClickStopService(view: View) {
        val running = isMyServiceRunning(DataCollectorService::class.java)
        if (running) {
            val intent = Intent(this, DataCollectorService::class.java)
            stopService(intent)
            updateUI()
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer
                                                       .MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startScheduledUpdate() {
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.scheduleAtFixedRate({
                                          try {
                                              runOnUiThread { updateUI() }

                                          } catch (e: Exception) {
                                              System.err.println("error in executing: " + ". It will no longer be " +
                                                                     "run!")
                                              e.printStackTrace()
                                              // and re throw it so that the Executor also gets this error so that
                                              // it can do what it would
                                              throw RuntimeException(e)
                                          }
                                      }, 0, 10, TimeUnit.SECONDS)
    }

    private fun updateUI() {
        val running = isMyServiceRunning(DataCollectorService::class.java)
        entry_activity_text.text = "Data Collector Service: $running"
        if (running) {
            button_start_service.isEnabled = false
            button_stop_service.isEnabled = true

        } else {
            button_start_service.isEnabled = true
            button_stop_service.isEnabled = false
        }
    }

    private fun CheckGPS(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        var gps_enabled = false
        var network_enabled = false

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }

        if (!gps_enabled || !network_enabled) {
            // notify user
            val dialog = AlertDialog.Builder(this)
            dialog.setMessage("GPS is disabled in your device. Would you like to enable it?")
            dialog.setPositiveButton("Yes") { paramDialogInterface, paramInt ->
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(myIntent)
            }
            dialog.setNegativeButton("No") { paramDialogInterface, paramInt ->
            }
            dialog.show()
            return false
        }
        return true
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest
                .permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                                              arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                              MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
            return false
        } else {
            return true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val running = isMyServiceRunning(DataCollectorService::class.java)
                    if (!running) {
                        startDatacollectorService()
                    }
                } else {
                    return
                }
                return
            }
            REYOUEST_PERMISSION_WRITE_EXTERNAL_STORAGE  -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    exportDB(null)
                } else {
                    Toast.makeText(this, "Daten können nicht exportiert werden", Toast.LENGTH_LONG)
                        .show()
                }
                return
            }

            else                                        -> return
        }
    }

    fun startDatacollectorService() {
        val sharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(this)
        val userName = sharedPreferences.getString(getString(R.string.user_name),
                                                   null)
        if (userName == null) {
            Toast.makeText(this, "username not set", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "username not set")
            return
        } else {
            val intent = Intent(this, DataCollectorService::class.java)
            startService(intent)
            updateUI()
        }
    }
}