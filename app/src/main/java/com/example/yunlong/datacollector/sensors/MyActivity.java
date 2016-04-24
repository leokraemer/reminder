package com.example.yunlong.datacollector.sensors;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.yunlong.datacollector.MainActivity;
import com.example.yunlong.datacollector.R;
import com.example.yunlong.datacollector.services.ActivitiesIntentService;
import com.example.yunlong.datacollector.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

/**
 * Created by Yunlong on 4/21/2016.
 */
public class MyActivity implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,ResultCallback<Status> {

    private ActivityDetectionBroadcastReceiver mBroadcastReceiver;
    private GoogleApiClient mGoogleApiClient;
    private Context context;
    private MyActivityListener myActivityListener;
    public String confidentActivity;

    public MyActivity(Context context) {
        this.context = context;
        myActivityListener = (MyActivityListener)context;
        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();
        registerReceiver();

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                        //.addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
        connect();
    }

    public void connect(){
        mGoogleApiClient.connect();
    }
    public void disconnect(){
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    public void unregisterReceiver(){
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mBroadcastReceiver);
    }
    public void registerReceiver(){
        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.STRING_ACTION));
    }

    public void requestActivityUpdates() {
        if (!mGoogleApiClient.isConnected()) {
            //Toast.makeText(context, "GoogleApiClient not yet connected", Toast.LENGTH_SHORT).show();
        } else {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0, getActivityDetectionPendingIntent()).setResultCallback(this);
        }
    }

    public void removeActivityUpdates() {
        if (!mGoogleApiClient.isConnected()) {
            //Toast.makeText(context, "GoogleApiClient not yet connected", Toast.LENGTH_SHORT).show();
        } else {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, getActivityDetectionPendingIntent()).setResultCallback(this);
        }
    }


    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(context, ActivitiesIntentService.class);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("GooglePlayAPI","onConnected");
        requestActivityUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("GooglePlayAPI","onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("GooglePlayAPI","onConnectionFailed");
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            Log.e("Activity Detection", "Successfully added activity detection.");

        } else {
            Log.e("Activity Detection", "Error: " + status.getStatusMessage());
        }
    }

    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> detectedActivities = intent.getParcelableArrayListExtra(Constants.STRING_EXTRA);
            String activityString = "";
            if(detectedActivities.get(0).getConfidence()>60){
                setConfidentActivity(getDetectedActivity(detectedActivities.get(0).getType())+"");
            }else {
                setConfidentActivity("unknown");
            }
            for(DetectedActivity activity: detectedActivities){
                activityString +=  "Activity: " + getDetectedActivity(activity.getType()) + ", Confidence: " + activity.getConfidence() + "%\n";
            }
            myActivityListener.activityUpdate(activityString);
        }
    }

    public String getConfidentActivity() {
        if(confidentActivity==null) {
            return "null";
        }
        return confidentActivity;
    }

    public void setConfidentActivity(String confidentActivity) {
        this.confidentActivity = confidentActivity;
    }

    public String getDetectedActivity(int detectedActivityType) {
        Resources resources = context.getResources();
        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.on_bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.on_foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.walking);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity.UNKNOWN:
                return resources.getString(R.string.unknown);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }


    }
}
