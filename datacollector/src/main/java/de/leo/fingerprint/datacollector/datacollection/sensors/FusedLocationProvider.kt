package de.leo.fingerprint.datacollector.datacollection.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
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
    fun startLocationUpdates() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 5000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)
        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(UiThreadExecutor(), OnSuccessListener<LocationSettingsResponse> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            Log.i("location service", "success")
        })

        task.addOnFailureListener(UiThreadExecutor(), OnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    Log.e("location service", "fail")
                    Log.e("location service", "$e")
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

    fun stopLocationUpdates() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 1800000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_NO_POWER
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)
        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(UiThreadExecutor(),
                                  OnSuccessListener<LocationSettingsResponse> {
                                      // All location settings are satisfied. The client can initialize
                                      // location requests here.
                                      // ...
                                      Log.i("location service", "success")
                                  })

        task.addOnFailureListener(UiThreadExecutor(), OnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    Log.e("location service", "fail")
                    Log.e("location service", "$e")
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }

            }
        })
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}