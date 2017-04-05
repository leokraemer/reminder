package com.example.yunlong.datacollector;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class NavigationActivity extends AppCompatActivity {

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        context = this;
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
    public void onButtonSensorFigur(View view){
        Intent intent = new Intent(context,OrientationSensorExampleActivity.class);
        startActivity(intent);
    }
}
