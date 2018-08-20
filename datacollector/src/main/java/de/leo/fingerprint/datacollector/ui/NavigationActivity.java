package de.leo.fingerprint.datacollector.ui;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import de.leo.fingerprint.datacollector.R;
import de.leo.fingerprint.datacollector.ui.DailyRoutines.MapsActivity;
import de.leo.fingerprint.datacollector.ui.GeofencesWithPlayServices.GeofenceMapActivity;
import de.leo.fingerprint.datacollector.ui.activityRecording.RecordingActivity;
import de.leo.fingerprint.datacollector.ui.activityRecording.RecordingsListActivity;
import de.leo.fingerprint.datacollector.ui.compare.CompareActivity;
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.CreateTriggerActivity;
import de.leo.fingerprint.datacollector.ui.naturalTrigger.list.TriggerManagingActivity;
import de.leo.fingerprint.datacollector.ui.notifications.NotificationService;
import de.leo.fingerprint.datacollector.utils.PermissionUtils;

import static de.leo.fingerprint.datacollector.ui.notifications.NotificationService.CHANNEL;
import static de.leo.fingerprint.datacollector.ui.notifications.NotificationService
        .FOREGROUND_CHANNEL;
import static de.leo.fingerprint.datacollector.ui.notifications.NotificationService
        .SET_DAILY_REMINDER;

public class NavigationActivity extends AppCompatActivity {

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        context = this;

        checkPermission();
        checkPermission();
        createNotificationChannel();
        postDailyNotifier();
    }

    private void postDailyNotifier() {
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(SET_DAILY_REMINDER);
        startService(intent);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        //foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.foreground_channel_name);
            String description = getString(R.string.foreground_channel_description);
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(FOREGROUND_CHANNEL, name,
                    importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, 1,
                    Manifest.permission.RECORD_AUDIO, true);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission
                .ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, 2,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
    }

    public void onButtonEntry(View view) {
        Intent intent = new Intent(context, EntryActivity.class);
        startActivity(intent);
    }

    public void onButtonMaps(View view) {
        Intent intent = new Intent(context, MapsActivity.class);
        startActivity(intent);
    }

    public void onButtonRecorder(View view) {
        Intent intent = new Intent(context, RecordingActivity.class);
        startActivity(intent);
    }

    public void onButtonGeofence(View view) {
        Intent intent = new Intent(context, GeofenceMapActivity.class);
        startActivity(intent);
    }

    public void onButtonRecList(View view) {
        Intent intent = new Intent(context, RecordingsListActivity.class);
        startActivity(intent);
    }

    public void onButtonCompare(View view) {
        Intent intent = new Intent(context, CompareActivity.class);
        startActivity(intent);
    }

    public void onButtonTrigger(View view) {
        Intent intent = new Intent(context, CreateTriggerActivity.class);
        startActivity(intent);
    }

    public void onButtonTriggerList(View view) {
        Intent intent = new Intent(context, TriggerManagingActivity.class);
        startActivity(intent);
    }
}
