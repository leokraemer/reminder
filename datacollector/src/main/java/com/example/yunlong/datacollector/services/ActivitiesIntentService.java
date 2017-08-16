package com.example.yunlong.datacollector.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.yunlong.datacollector.R;
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

import static com.example.yunlong.datacollector.services.DataCollectorService.ACTIVITY;
import static com.example.yunlong.datacollector.services.DataCollectorService.DELETED;
import static com.example.yunlong.datacollector.services.DataCollectorService.MINUTES;
import static com.example.yunlong.datacollector.services.DataCollectorService.SNACK;
import static com.example.yunlong.datacollector.services.DataCollectorService.TYPE;
import static com.example.yunlong.datacollector.services.DataCollectorService.notificationID;

public class ActivitiesIntentService extends IntentService {
    private static final String TAG = "ActivitiesIntentService";

    public ActivitiesIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String type = (String) intent.getExtras().get(TYPE);
        if(type == null)
            type = "";
        switch (type) {
            case ACTIVITY:
                //fall through
            case MINUTES:
                //fall through
            case SNACK:
                //fall through
            case DELETED:
                uploadLabel("now", type, 0, "no");
                cancelNotification();
                startNotification();
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String userName = sharedPreferences.getString("fingerprint_user_name", "userName");
        label.setTitle(DataCollectorApplication.ParseObjectTitle);
        label.setAuthor(ParseUser.getCurrentUser());
        label.setUserName(userName);
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

    public void startNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("DataCollector")
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentText("Background service is running.")
                .setSmallIcon(R.drawable.fp_s).addAction(0, "10 minutes", PendingIntent.getService(this, 19921, new Intent(this, ActivitiesIntentService.class).putExtra(TYPE, MINUTES), PendingIntent.FLAG_ONE_SHOT))
                .addAction(0, "activity", PendingIntent.getService(this, 19922, new Intent(this, ActivitiesIntentService.class).putExtra(TYPE, ACTIVITY), PendingIntent.FLAG_ONE_SHOT))
                .addAction(0, "snack", PendingIntent.getService(this, 19923, new Intent(this, ActivitiesIntentService.class).putExtra(TYPE, SNACK), PendingIntent.FLAG_ONE_SHOT));


        mNotificationManager.notify(
                notificationID,
                mNotifyBuilder.build());
    }

    public void cancelNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationID);
    }
}
