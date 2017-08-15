package com.example.yunlong.datacollector.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.yunlong.datacollector.application.DataCollectorApplication;
import com.example.yunlong.datacollector.models.LabelData;
import com.example.yunlong.datacollector.utils.Constants;
import com.example.yunlong.datacollector.utils.TimeUtils;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;


public class ActivitiesIntentService extends IntentService {
    private static final String TAG = "ActivitiesIntentService";

    public ActivitiesIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String type = (String) intent.getExtras().get(DataCollectorService.TYPE);
        switch (type) {
            case DataCollectorService.ACTIVITY:
                //fall through
            case DataCollectorService.MINUTES:
                //fall through
            case DataCollectorService.SNACK:
                uploadLabel("now", type, 0, null);
                break;
            default:
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                Intent i = new Intent(Constants.STRING_ACTION);

                ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

                i.putExtra(Constants.STRING_EXTRA, detectedActivities);
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }
    }

    private void uploadLabel(String type, String activityLabel, int mood, String isTypicalRoutine) {
        LabelData label = new LabelData();
        label.setTitle(DataCollectorApplication.ParseObjectTitle);
        label.setAuthor(ParseUser.getCurrentUser());
        label.setUserName("leo");
        label.setTime(TimeUtils.getCurrentTimeStr());
        label.setType(type);
        label.setMood(mood);
        label.setIsTypicalRoutine(isTypicalRoutine);
        label.setActivityLabel(activityLabel);
        label.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("DataCollector", "parse save done");
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error saving: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

}
