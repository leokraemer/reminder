package de.leo.smartTrigger.datacollector.datacollection.sensors

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import de.leo.smartTrigger.datacollector.datacollection.DataCollectorService.Companion.UPDATE_DELAY
import java.util.concurrent.Executor


/**
 * Created by Leo on 11.02.2018.
 */
class FusedLocationProvider(val context: Context, val locationListener: MyLocationListener) {

    val fusedLocationProviderClient: FusedLocationProviderClient

    init {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(interval: Long, fastestInterval: Long = UPDATE_DELAY) {
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = interval
        mLocationRequest.fastestInterval = fastestInterval
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)
        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(UiThreadExecutor(), OnSuccessListener<LocationSettingsResponse> {
            // All locationName settings are satisfied. The client can initialize
            // locationName requests here.
            // ...
            Log.i("start location service", "success")
            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
                                                               locationCallback,
                                                               null)
        })

        task.addOnFailureListener(UiThreadExecutor(), OnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    Log.e("locationName service", "fail")
                    Log.e("locationName service", "$e")
                    Toast.makeText(context, "could not start location service",
                                   Toast.LENGTH_LONG).show()
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }

            }
        })
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, null)
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            if (locationResult != null)
                locationListener.locationChanged(locationResult.lastLocation)
        }
    }

    internal inner class UiThreadExecutor : Executor {
        private val mHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            mHandler.post(command)
        }
    }

    @SuppressLint("MissingPermission")
    fun changeUpdateInterval(interval: Long, fastestInterval: Long = UPDATE_DELAY) {
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = interval
        mLocationRequest.fastestInterval = fastestInterval
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, null)
    }

    fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}