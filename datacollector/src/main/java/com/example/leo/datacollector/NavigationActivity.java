package com.example.leo.datacollector;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.leo.datacollector.DailyRoutines.MapsActivity;
import com.example.leo.datacollector.GeofencesWithPlayServices.GeofenceMapActivity;
import com.example.leo.datacollector.activityRecording.RecordingActivity;
import com.example.leo.datacollector.jitai.JitaiManagingActivity;
import com.example.leo.datacollector.utils.PermissionUtils;

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
    public void onButtonSensorOverview(View view){
        Intent intent = new Intent(context,SensorOverviewActivity.class);
        startActivity(intent);
    }
    public void onButtonSensorFigure(View view){
        Intent intent = new Intent(context,OrientationSensorExampleActivity.class);
        startActivity(intent);
    }
    public void onButtonStepCounter(View view){
        Intent intent = new Intent(context,StepCounterActivity.class);
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
}
