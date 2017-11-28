package com.example.leo.datacollector.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.example.leo.datacollector.R;
import com.example.leo.datacollector.utils.Constants;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

import static com.example.leo.datacollector.services.DataCollectorService.ACTIVITY;
import static com.example.leo.datacollector.services.DataCollectorService.DELETED;
import static com.example.leo.datacollector.services.DataCollectorService.MINUTES;
import static com.example.leo.datacollector.services.DataCollectorService.SNACK;
import static com.example.leo.datacollector.services.DataCollectorService.TYPE;
import static com.example.leo.datacollector.services.DataCollectorService.notificationID;

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
                notificationID+1,
                mNotifyBuilder.build());
    }

    public void cancelNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationID+1);
    }
}
