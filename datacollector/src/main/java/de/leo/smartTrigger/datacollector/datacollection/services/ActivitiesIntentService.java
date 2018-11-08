package de.leo.smartTrigger.datacollector.datacollection.services;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import de.leo.smartTrigger.datacollector.utils.Constants;


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

            ArrayList<DetectedActivity> detectedActivities =
                    (ArrayList<DetectedActivity>) result.getProbableActivities();

            i.putExtra(Constants.STRING_EXTRA, detectedActivities);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }
    }
}
