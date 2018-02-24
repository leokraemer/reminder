package de.leo.fingerprint.datacollector.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import de.leo.fingerprint.datacollector.utils.Constants;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;


public class ActivitiesIntentService extends IntentService {
    private static final String TAG = "ActivitiesIntentService";

    public ActivitiesIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        if (result != null) {
            Intent i = new Intent(Constants.STRING_ACTION);

            ArrayList<DetectedActivity> detectedActivities = (ArrayList) result
                    .getProbableActivities();

            i.putExtra(Constants.STRING_EXTRA, detectedActivities);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }
    }
}
