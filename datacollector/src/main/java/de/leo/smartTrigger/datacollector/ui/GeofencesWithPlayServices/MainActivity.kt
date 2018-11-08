/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.leo.smartTrigger.datacollector.ui.GeofencesWithPlayServices

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import de.leo.smartTrigger.datacollector.BuildConfig
import de.leo.smartTrigger.datacollector.R
import java.util.concurrent.TimeUnit

val fiveMinutes = TimeUnit.MINUTES.toMillis(5)

/**
 * Demonstrates how to create and remove geofences using the GeofencingApi. Uses an IntentService
 * to monitor geofence transitions and creates notifications whenever a device enters or exits
 * a geofence.
 *
 *
 * This sample requires a device's Location settings to be turned on. It also requires
 * the ACCESS_FINE_LOCATION permission, as specified in AndroidManifest.xml.
 *
 *
 */
open class MainActivity : AppCompatActivity(), OnCompleteListener<Void> {

    /**
     * Provides access to the Geofencing API.
     */
    protected lateinit var mGeofencingClient: GeofencingClient

    /**
     * Used when requesting to add or remove geofences.
     */
    private var mGeofencePendingIntent: PendingIntent? = null

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    protected// Reuse the PendingIntent if we already have it.
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
            // addGeofences() and removeGeofences().
    val geofencePendingIntent: PendingIntent
        get() {
            if (mGeofencePendingIntent != null) {
                return mGeofencePendingIntent as PendingIntent
            }
            val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
            return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null
        mGeofencingClient = LocationServices.getGeofencingClient(this)
    }

    public override fun onStart() {
        super.onStart()

        if (!checkPermissions()) {
            requestPermissions()
        }
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        val builder = GeofencingRequest.Builder()

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofence(geofence)

        // Return a GeofencingRequest.
        return builder.build()
    }

    /**
     * Removes geofences. This method should be called after the user has granted the locationName
     * permission.
     */
    private fun removeGeofences() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions))
            return
        }
        mGeofencingClient.removeGeofences(geofencePendingIntent).addOnCompleteListener(this)
    }

    @SuppressWarnings("MissingPermission")
    protected fun addGeofence(geofenceName: String, latLng: LatLng, radius: Float) {
        mGeofencingClient.addGeofences(getGeofencingRequest(getGeofenceForLatlng(geofenceName, latLng, radius, true, true, false,
                                                                                 fiveMinutes)),
                                       geofencePendingIntent)
                .addOnCompleteListener(this)
    }

    @SuppressWarnings("MissingPermission")
    protected fun addGeofence(geofenceName: String, latLng: LatLng, radius: Float, enter: Boolean, exit: Boolean, dwell: Boolean) {
        mGeofencingClient.addGeofences(getGeofencingRequest(getGeofenceForLatlng(geofenceName, latLng, radius, enter, exit, dwell,
                                                                                 fiveMinutes)),
                                       geofencePendingIntent)
                .addOnCompleteListener(this)
    }

    /**
     * Runs when the result of calling [.addGeofence] and/or [.removeGeofences]
     * is available.
     *
     * @param task the resulting Task, containing either a result or error.
     */
    override fun onComplete(task: Task<Void>) {
        if (task.isSuccessful) {

            Toast.makeText(this, "Geofence updated", Toast.LENGTH_SHORT).show()
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            val errorMessage = GeofenceErrorMessages.getErrorString(this, task.exception!!)
            Log.w(TAG, errorMessage)
        }
    }

    fun getGeofenceForLatlng(name: String, latLng: LatLng, radius: Float, enter: Boolean, exit:
    Boolean, dwell: Boolean, loiteringDelay: Long): Geofence {
        val enterI = if (enter) Geofence.GEOFENCE_TRANSITION_ENTER else 0;
        val exitI = if (exit) Geofence.GEOFENCE_TRANSITION_EXIT else 0;
        val dwellI = if (dwell) Geofence.GEOFENCE_TRANSITION_DWELL else 0;
        return Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(name)

                // Set the circular region of this geofence.
                .setCircularRegion(
                        latLng.latitude,
                        latLng.longitude,
                        radius
                )

                .setLoiteringDelay(loiteringDelay.toInt())

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(enterI or exitI or dwellI)

                // Create the geofence.
                .build()
    }


    /**
     * Shows a [Snackbar] using `text`.
     *
     * @param text The Snackbar text.
     */
    private fun showSnackbar(text: String) {
        val container = findViewById<View>(android.R.id.content)
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * Shows a [Snackbar].
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private fun showSnackbar(mainTextStringId: Int, actionStringId: Int,
                             listener: View.OnClickListener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show()
    }

    /**
     * Return the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    View.OnClickListener {
                        // Request permission
                        ActivityCompat.requestPermissions(this@MainActivity,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                REQUEST_PERMISSIONS_REQUEST_CODE)
                    })
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted.")
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        View.OnClickListener {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null)
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        })
            }
        }
    }

    companion object {

        private val TAG = MainActivity::class.java.simpleName

        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 4711
    }
}

