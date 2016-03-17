package com.example.yunlong.datacollector;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements MyLocationListener, MyMotionListener, MyFourSquareListener {
    TextView textViewLocation;
    TextView textViewRotation;
    TextView textViewAccelerometer;
    TextView textViewPlaces;
    Button button;
    MyMotion myMotion;
    MyLocation myLocation;
    FoursquareCaller foursquareCaller;
    Location currentLocation;
    Context context;
    Button buttonChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        context = this;

        textViewLocation = (TextView)findViewById(R.id.location_text);
        textViewRotation = (TextView)findViewById(R.id.rotation_text);
        textViewAccelerometer = (TextView)findViewById(R.id.accelerometer_text);
        textViewPlaces = (TextView)findViewById(R.id.places_text);
        button = (Button)findViewById(R.id.places_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                foursquareCaller = new FoursquareCaller(context,currentLocation);
                foursquareCaller.findPlaces();
            }
        });
        buttonChart = (Button)findViewById(R.id.button_show_chart);
        buttonChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(context,OrientationSensorExampleActivity.class);
                Intent intent = new Intent(context,RemoteSensorDataActivity.class);
                startActivity(intent);

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void locationChanged(Location location) {
        if(location != null) {
            currentLocation = location;
            textViewLocation.setText("" + location.getLatitude() + "\n" + location.getLongitude() + "\n" + location.getAccuracy());
        }
    }

    @Override
    public void motionDataChanged(float[] accData, float[] rotData) {
        textViewAccelerometer.setText("" + accData[0] + "\n" +accData[1] + "\n" +accData[2] + "\n" );
        textViewRotation.setText("" + rotData[0] + "\n" +rotData[1] + "\n" +rotData[2] + "\n" );
    }

    @Override
    public void stopSensor() {
        myMotion.stopSensor();
    }

    @Override
    public void stopLocationUpdate() {
        myLocation.stopLocationUpdate();
    }

    @Override
    protected void onPause() {
        stopSensor();
        stopLocationUpdate();
        super.onPause();
    }

    @Override
    protected void onPostResume() {
        myLocation = new MyLocation(this);
        myMotion = new MyMotion(this);
        super.onPostResume();
    }

    @Override
    public void placesFound(String place) {
        textViewPlaces.setText(place);
    }
}
