package de.leo.fingerprint.datacollector.datacollection.sensors

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.services.ActivitiesIntentService
import de.leo.fingerprint.datacollector.utils.Constants
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.DetectedActivity

/**
 * Created by Yunlong on 4/21/2016.
 */
class MyActivity(private val context: Context) : GoogleApiClient.ConnectionCallbacks,
                                                 GoogleApiClient.OnConnectionFailedListener,
                                                 ResultCallback<Status> {

    private val mBroadcastReceiver: ActivityDetectionBroadcastReceiver
    private val mGoogleApiClient: GoogleApiClient
    private val myActivityListener: MyActivityListener

    private val activityDetectionPendingIntent: PendingIntent
        get() {
            val intent = Intent(context, ActivitiesIntentService::class.java)
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    init {
        myActivityListener = context as MyActivityListener
        mBroadcastReceiver = ActivityDetectionBroadcastReceiver()
        registerReceiver()

        mGoogleApiClient = GoogleApiClient.Builder(context)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            //.addApi(LocationServices.API)
            .addApi(ActivityRecognition.API)
            //.enableAutoManage((AppCompatActivity)context, 0, this)
            .build()
        connect()
    }

    fun connect() {
        mGoogleApiClient.connect()
    }

    fun disconnect() {
        //mGoogleApiClient.stopAutoManage((AppCompatActivity)context);
        if (mGoogleApiClient.isConnected) {
            mGoogleApiClient.disconnect()
        }
    }

    fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mBroadcastReceiver)
    }

    fun registerReceiver() {
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(mBroadcastReceiver, IntentFilter(Constants.STRING_ACTION))
    }

    fun requestActivityUpdates() {
        if (!mGoogleApiClient.isConnected) {
            //Toast.makeText(context, "GoogleApiClient not yet connected", Toast.LENGTH_SHORT).show();
        } else {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient,
                                                                              5000,
                                                                              activityDetectionPendingIntent)
                .setResultCallback(this)
        }
    }

    fun removeActivityUpdates() {
        if (!mGoogleApiClient.isConnected) {
            //Toast.makeText(context, "GoogleApiClient not yet connected", Toast.LENGTH_SHORT).show();
        } else {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient,
                                                                             activityDetectionPendingIntent)
                .setResultCallback(this)
        }
    }

    override fun onConnected(bundle: Bundle?) {
        Log.d("GooglePlayAPI", "onConnected")
        requestActivityUpdates()
    }

    override fun onConnectionSuspended(i: Int) {
        Log.d("GooglePlayAPI", "onConnectionSuspended")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d("GooglePlayAPI", "onConnectionFailed")
    }

    override fun onResult(status: Status) {
        if (status.isSuccess) {
            Log.d("Activity Detection", "Successfully added activity detection.")

        } else {
            Log.e("Activity Detection", "Error: " + status.statusMessage!!)
        }
    }

    inner class ActivityDetectionBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val detectedActivities = intent.getParcelableArrayListExtra<DetectedActivity>(Constants.STRING_EXTRA)
            myActivityListener.activityUpdate(detectedActivities)
        }

        fun getDetectedActivity(detectedActivityType: Int): String {
            val resources = context.resources
            return getDetectedActivity(resources, detectedActivityType)
        }

        fun getDetectedActivity(resources: Resources, detectedActivityType: Int): String {
            when (detectedActivityType) {
                DetectedActivity.IN_VEHICLE -> return resources.getString(R.string.in_vehicle)
                DetectedActivity.ON_BICYCLE -> return resources.getString(R.string.on_bicycle)
                DetectedActivity.ON_FOOT    -> return resources.getString(R.string.on_foot)
                DetectedActivity.RUNNING    -> return resources.getString(R.string.running)
                DetectedActivity.WALKING    -> return resources.getString(R.string.walking)
                DetectedActivity.STILL      -> return resources.getString(R.string.still)
                DetectedActivity.TILTING    -> return resources.getString(R.string.tilting)
                DetectedActivity.UNKNOWN    -> return resources.getString(R.string.unknown)
                else                        -> return resources.getString(R.string.unidentifiable_activity,
                                                                          detectedActivityType)
            }
        }
    }

    companion object {
        private val TAG = "MyActivity"
    }
}
