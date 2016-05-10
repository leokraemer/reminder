package com.example.yunlong.datacollector;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yunlong.datacollector.application.DataCollectorApplication;
import com.example.yunlong.datacollector.models.SensorDataSet;
import com.example.yunlong.datacollector.sensors.FoursquareCaller;
import com.example.yunlong.datacollector.sensors.MyActivity;
import com.example.yunlong.datacollector.sensors.MyActivityListener;
import com.example.yunlong.datacollector.sensors.MyEnvironmentSensor;
import com.example.yunlong.datacollector.sensors.MyEnvironmentSensorListener;
import com.example.yunlong.datacollector.sensors.MyFourSquareListener;
import com.example.yunlong.datacollector.sensors.MyLocation;
import com.example.yunlong.datacollector.sensors.MyLocationListener;
import com.example.yunlong.datacollector.sensors.MyMotion;
import com.example.yunlong.datacollector.sensors.MyMotionListener;
import com.example.yunlong.datacollector.settings.FingerPrintSettingsActivity;
import com.example.yunlong.datacollector.utils.TimeUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SensorOverviewActivity extends AppCompatActivity implements MyLocationListener, MyMotionListener, MyFourSquareListener,MyActivityListener,MyEnvironmentSensorListener{
    TextView textViewLocation,textViewRotation,textViewAccelerometer,textViewPlaces,mDetectedActivityTextView,wifiTextView,environmentTextView;
    //MyMotion myMotion;
    MyLocation myLocation;
    MyActivity myActivity;
    FoursquareCaller foursquareCaller;
    Location currentLocation;
    MyEnvironmentSensor myEnvironmentSensor;
    Context context;
    Button buttonChart;
    String wifiName="null";
    String placeName="null";
    int uploadCnt = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        context = this;

        textViewLocation = (TextView)findViewById(R.id.location_text);
        //textViewRotation = (TextView)findViewById(R.id.rotation_text);
        //textViewAccelerometer = (TextView)findViewById(R.id.accelerometer_text);
        textViewPlaces = (TextView)findViewById(R.id.places_text);
        mDetectedActivityTextView = (TextView) findViewById(R.id.detected_activities_textview);
        wifiTextView =(TextView) findViewById(R.id.wifi_textview);
        environmentTextView = (TextView) findViewById(R.id.environment_text);

/*        buttonChart = (Button)findViewById(R.id.button_show_chart);
        buttonChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(context,OrientationSensorExampleActivity.class);
                // data from gear 2
                Intent intent = new Intent(context,RemoteSensorDataActivity.class);
                startActivity(intent);
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the state bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle state bar item clicks here. The state bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, FingerPrintSettingsActivity.class);
            startActivity(intent);
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
        textViewAccelerometer.setText("" + accData[0] + "\n" + accData[1] + "\n" + accData[2] + "\n");
        textViewRotation.setText("" + rotData[0] + "\n" + rotData[1] + "\n" + rotData[2] + "\n");
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void stopMotionSensor() {
        //myMotion.stopMotionSensor();
    }

    @Override
    public void stopLocationUpdate() {
        myLocation.stopLocationUpdate();
    }

    @Override
    protected void onPause() {
        //stopMotionSensor();
        stopLocationUpdate();
        stopActivityDetection();
        stopEnvironmentSensor();
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
        myLocation = new MyLocation(this);
        //myMotion =new MyMotion(this);
        myActivity = new MyActivity(this);
        myEnvironmentSensor = new MyEnvironmentSensor(this);
        startScheduledUpdate();
    }

    @Override
    public void placesFound(String place) {
        if(place !=null) {
            textViewPlaces.setText(place);
            String[] places = place.split("\n");
            placeName = places[0];
        }else {
            textViewPlaces.setText("Unknown");
            placeName = "null";
        }
    }

    @Override
    public void activityUpdate(String activity) {
        mDetectedActivityTextView.setText(activity);
    }

    @Override
    public void stopActivityDetection() {
        myActivity.removeActivityUpdates();
        myActivity.disconnect();
        myActivity.unregisterReceiver();
    }

    private void startScheduledUpdate(){
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(uploadCnt==1){ //foursquare limit: 5,000 times requests per hour
                                        getPlaces();
                                    }else if(uploadCnt==10){
                                        uploadCnt=0;
                                    }
                                    uploadCnt++;
                                    getWiFiName();
                                }
                            });

                        }catch (Exception e){
                            System.err.println("error in executing: " + ". It will no longer be run!");
                            e.printStackTrace();
                            // and re throw it so that the Executor also gets this error so that it can do what it would
                            throw new RuntimeException(e);
                        }

                    }
                }, 0, 5, TimeUnit.SECONDS);
    }

    private void getPlaces(){
        try {
            foursquareCaller = new FoursquareCaller(context, currentLocation);
            foursquareCaller.findPlaces();
        }catch (Exception e){
            Toast.makeText(context,"Foursquare API Exception",Toast.LENGTH_SHORT).show();
        }
    }
    private void getWiFiName(){
        try{
            WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            String name = wifiInfo.getSSID();
            if(name == null){
                wifiTextView.setText("no wifi");
                wifiName = "no wifi";
            }else {
                wifiTextView.setText(name);
                wifiName = name;
            }

        }catch (Exception e){
            Toast.makeText(context,"get wifi name error",Toast.LENGTH_SHORT).show();
        }

    }

    private void uploadDataSet(){
        SensorDataSet sensorDataSet = new SensorDataSet();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String userName = sharedPreferences.getString("fingerprint_user_name", "userName");
        if(userName.equals("userName")){
            Toast.makeText(context,"please type in your name in settings",Toast.LENGTH_SHORT).show();
        }else {
            try {
                sensorDataSet.setTitle(DataCollectorApplication.ParseObjectTitle);
                sensorDataSet.setAuthor(ParseUser.getCurrentUser());
                sensorDataSet.setUserName(userName);
                sensorDataSet.setActivity(myActivity.getConfidentActivity());
                sensorDataSet.setWifiName(wifiName);
                sensorDataSet.setHumidity(myEnvironmentSensor.humidity);
                sensorDataSet.setLight(myEnvironmentSensor.light);
                sensorDataSet.setPressure(myEnvironmentSensor.pressure);
                sensorDataSet.setTemperature(myEnvironmentSensor.temperature);
                sensorDataSet.setLocation(placeName);
                sensorDataSet.setTime(TimeUtils.getCurrentTimeStr());

                sensorDataSet.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e==null) {
                            Log.d("DataCollector", "parse save done");
                        }else {
                            Toast.makeText(getApplicationContext(),
                                    "Error saving: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
                /*sensorDataSet.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e==null) {
                            Log.d("DataCollector", "parse save done");
                        }else {
                            Toast.makeText(getApplicationContext(),
                                    "Error saving: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });*/
            }catch (Exception e){
                Toast.makeText(context,"upload data exception",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void environmentSensorDataChanged(float light, float temperature, float pressure, float humidity) {
        environmentTextView.setText("Light: " + light + " lx"+ "\n" +"Temperature: " + temperature + " C"+ "\n"+"Pressure: " + pressure +" hPa"+  "\n"+"Humidity: " + humidity+" %");
    }

    @Override
    public void stopEnvironmentSensor() {
        myEnvironmentSensor.stopEnvironmentSensor();
    }

    public void uploadDataSet(View view){
        uploadDataSet();
    }
}
