package de.leo.fingerprint.datacollector;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import de.leo.fingerprint.datacollector.DailyRoutines.MapsActivity;
import de.leo.fingerprint.datacollector.GeofencesWithPlayServices.GeofenceMapActivity;
import de.leo.fingerprint.datacollector.activityRecording.RecordingActivity;
import de.leo.fingerprint.datacollector.activityRecording.RecordingsListActivity;
import de.leo.fingerprint.datacollector.compare.CompareActivity;
import de.leo.fingerprint.datacollector.jitai.manage.JitaiManagingActivity;
import de.leo.fingerprint.datacollector.naturalTriggerCreation.CreateTriggerActivity;
import de.leo.fingerprint.datacollector.utils.PermissionUtils;

public class NavigationActivity extends AppCompatActivity {

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        context = this;

        checkPermission();
        checkPermission();
    }

    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, 1,
                    Manifest.permission.RECORD_AUDIO, true);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, 2,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
    }

    public void onButtonEntry(View view){
        Intent intent = new Intent(context,EntryActivity.class);
        startActivity(intent);
    }
    public void onButtonMaps(View view){
        Intent intent = new Intent(context,MapsActivity.class);
        startActivity(intent);
    }

    public void onButtonRecorder(View view){
        Intent intent = new Intent(context,RecordingActivity.class);
        startActivity(intent);
    }

    public void onButtonGeofence(View view) {
        Intent intent = new Intent(context,GeofenceMapActivity.class);
        startActivity(intent);
    }

    public void onButtonJitai(View view) {
        Intent intent = new Intent(context,JitaiManagingActivity.class);
        startActivity(intent);
    }
    public void onButtonRecList(View view) {
        Intent intent = new Intent(context,RecordingsListActivity.class);
        startActivity(intent);
    }

    public void onButtonCompare(View view) {
        Intent intent = new Intent(context,CompareActivity.class);
        startActivity(intent);
    }

    public void onButtonTrigger(View view) {
        Intent intent = new Intent(context,CreateTriggerActivity.class);
        startActivity(intent);
    }
}
