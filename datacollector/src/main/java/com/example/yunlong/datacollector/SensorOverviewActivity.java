package com.example.yunlong.datacollector;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yunlong.datacollector.application.DataCollectorApplication;
import com.example.yunlong.datacollector.models.SensorDataSet;
import com.example.yunlong.datacollector.realm.StateRealm;
import com.example.yunlong.datacollector.sensors.AmbientSound;
import com.example.yunlong.datacollector.sensors.AmbientSoundListener;
import com.example.yunlong.datacollector.sensors.FoursquareCaller;
import com.example.yunlong.datacollector.sensors.GoogleFitness;
import com.example.yunlong.datacollector.sensors.GoogleFitnessListener;
import com.example.yunlong.datacollector.sensors.GooglePlacesCaller;
import com.example.yunlong.datacollector.sensors.GooglePlacesListener;
import com.example.yunlong.datacollector.sensors.MyActivity;
import com.example.yunlong.datacollector.sensors.MyActivityListener;
import com.example.yunlong.datacollector.sensors.MyEnvironmentSensor;
import com.example.yunlong.datacollector.sensors.MyEnvironmentSensorListener;
import com.example.yunlong.datacollector.sensors.FourSquareListener;
import com.example.yunlong.datacollector.sensors.MyLocation;
import com.example.yunlong.datacollector.sensors.MyLocationListener;
import com.example.yunlong.datacollector.sensors.MyMotionListener;
import com.example.yunlong.datacollector.sensors.WeatherCaller;
import com.example.yunlong.datacollector.sensors.WeatherCallerListener;
import com.example.yunlong.datacollector.settings.FingerPrintSettingsActivity;
import com.example.yunlong.datacollector.utils.PermissionUtils;
import com.example.yunlong.datacollector.utils.TimeUtils;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;

public class SensorOverviewActivity extends AppCompatActivity implements MyLocationListener,
        MyMotionListener, FourSquareListener,MyActivityListener,MyEnvironmentSensorListener,
        OnMapReadyCallback,GoogleMap.OnMarkerClickListener,GooglePlacesListener,AmbientSoundListener,
        GoogleFitnessListener, WeatherCallerListener{

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "SensorOverviewActivity";

    TextView textViewLocation,textViewRotation,textViewAccelerometer,textViewPlaces,mDetectedActivityTextView,wifiTextView,environmentTextView;
    //MyMotion myMotion;
    MyLocation myLocation;
    MyActivity myActivity;
    FoursquareCaller foursquareCaller;
    GooglePlacesCaller googlePlacesCaller;
    Location currentLocation;
    MyEnvironmentSensor myEnvironmentSensor;
    GoogleFitness googleFitness;
    WeatherCaller weather;
    AmbientSound ambientSound;

    Context context;
    Button buttonChart;
    String wifiName="null";
    String placeName="null";
    int uploadCnt = 0;
    //map
    private GoogleMap mMap;
    private static final LatLng Uni = new LatLng(47.6890, 9.1886);
    private static final LatLng Home = new LatLng(47.681417, 9.189508);
    private static final LatLng Gym = new LatLng(47.694066, 9.189717);
    private static final LatLng Mensa = new LatLng(47.69052,9.18912);
    List<Double> dist2Mensa = new ArrayList<Double>();
    int distCounter = 0;
    boolean goToMensa = false;
    //realm
    private Realm realm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.content_main);
        setContentView(R.layout.activity_main);

/*        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/

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
        });
*/

        //map
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_in);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();

        // Add a marker in Sydney and move the camera
        // Add some markers to the map, and add a data object to each marker.
        Marker mUni = mMap.addMarker(new MarkerOptions()
                .position(Uni)
                .title("Uni"));
        mUni.setTag(0);

        Marker mHome = mMap.addMarker(new MarkerOptions()
                .position(Home)
                .title("Home"));
        mHome.setTag(0);

        Marker mGym = mMap.addMarker(new MarkerOptions()
                .position(Gym)
                .title("Gym"));
        mGym.setTag(0);

        // Set a listener for marker click.
        mMap.setOnMarkerClickListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
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
            if(goToMensa){
                textViewLocation.setText("" + location.getLatitude() + ", " + location.getLongitude() + ", " + location.getAccuracy() + ": GoToMensa" );
            }else {
                textViewLocation.setText("" + location.getLatitude() + ", " + location.getLongitude() + ", " + location.getAccuracy());
            }
            if(mMap!=null) {
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                  // Showing the current location in Google Map
              CameraPosition camPos = new CameraPosition.Builder()
                .target(current)
                .zoom(15)
                .bearing(location.getBearing())
                .tilt(10)
                .build();
              CameraUpdate camUpd3 = CameraUpdateFactory.newCameraPosition(camPos);
              mMap.animateCamera(camUpd3);
            }
            if(location.getAccuracy()<20) {
                double dist = Math.abs(location.getLatitude() - Mensa.latitude) + Math.abs(location.getLongitude() - Mensa.longitude);
                dist2Mensa.add(dist);
                distCounter++;
                final int maxCounter = 5;
                if (distCounter == maxCounter) {
                    double direct = 1;
                    for (int i = 0; i < maxCounter - 1; i++) {
                        double temp = dist2Mensa.get(i + 1) - dist2Mensa.get(i);
                        if (temp > 0) {
                            direct = 0;
                        }
                    }
                    if (direct == 1) {
                        goToMensa = true;
                    } else {
                        goToMensa = false;
                    }
                    dist2Mensa.clear();
                    distCounter = 0;
                }
            }
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
        googlePlacesCaller.disconnect();
    }

    @Override
    protected void onPause() {
        //stopMotionSensor();
        stopLocationUpdate();
        stopActivityDetection();
        stopEnvironmentSensor();
        googleFitness.disconnect();//FIXME: put into interface
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
        myLocation = new MyLocation(this);
        //myMotion =new MyMotion(this);
        myActivity = new MyActivity(this);
        myEnvironmentSensor = new MyEnvironmentSensor(this);
        foursquareCaller = new FoursquareCaller(this, currentLocation);
        googlePlacesCaller = new GooglePlacesCaller(this);
        googleFitness = new GoogleFitness(this);
        weather = new WeatherCaller(this);//FIXME: init here, call somewhere else
        ambientSound = new AmbientSound(this);//FIXME: init here, call somewhere else
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
            //show both
            googlePlacesCaller.getCurrentPlace();
            foursquareCaller.findPlaces();

        }catch (Exception e){
            Toast.makeText(context,"Foursquare API Exception",Toast.LENGTH_SHORT).show();
        }
    }
    private void getWiFiName(){
        try{
            WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            String wifi = wifiInfo.getSSID()+":"+wifiInfo.getBSSID()+ ":" +wifiInfo.getIpAddress()+":"+wifiInfo.getNetworkId()+":"+wifiInfo.getRssi();
            if(wifi == null){
                wifiTextView.setText("null");
                wifiName = "null";
            }else {
                wifiTextView.setText(wifi);
                wifiName = wifi;
            }

        }catch (Exception e){
            Toast.makeText(context,"get wifi name error",Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isScreenOn(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //noinspection deprecation
            return pm.isScreenOn();
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
                //sensorDataSet.setHumidity(myEnvironmentSensor.humidity);
                //sensorDataSet.setLight(myEnvironmentSensor.light);
                //sensorDataSet.setPressure(myEnvironmentSensor.pressure);
                //sensorDataSet.setTemperature(myEnvironmentSensor.temperature);
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

    private void storeStateReal(){
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String userName = sharedPreferences.getString("fingerprint_user_name", "userName");
        if(userName.equals("userName")){
            Toast.makeText(context,"please type in your name in settings",Toast.LENGTH_SHORT).show();
        }else {
            try {
                // Create the Realm instance
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        StateRealm stateRealm = realm.createObject(StateRealm.class);
                        stateRealm.setUserNameStr(userName);
                        stateRealm.setActivityStr(myActivity.getConfidentActivity());
                        stateRealm.setWifiStr(wifiName);
                        stateRealm.setPlaceStr(placeName);
                        stateRealm.setLatitude(currentLocation.getLatitude());
                        stateRealm.setLongitude(currentLocation.getLongitude());
                        stateRealm.setTimeStr(TimeUtils.getCurrentTimeStr());
                    }
                });
            }catch (Exception e){
                Toast.makeText(context,"upload data exception",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void environmentSensorDataChanged(float light, float temperature, float pressure, float humidity) {
        //environmentTextView.setText("Light: " + light + " lx"+ "\n" +"Temperature: " + temperature + " C"+ "\n"+"Pressure: " + pressure +" hPa"+  "\n"+"Humidity: " + humidity+" %");
        //environmentTextView.setText("Light: " + light + " lx"+ "\n"+"Pressure: " + pressure +" hPa");
    }

    @Override
    public void stopEnvironmentSensor() {
        myEnvironmentSensor.stopEnvironmentSensor();
    }

    public void uploadDataSet(View view){
        //uploadDataSet();
        //test realm
        storeStateReal();
    }

    // google places handler
    @Override
    public void onReceivedPlaces(HashMap<String, Float> places) {
        Iterator it = places.entrySet().iterator();
        String place  = "";
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if((float)pair.getValue()>0) {
                place +=  pair.getKey() + " : " + pair.getValue() + "\n";
            }
            it.remove(); // avoids a ConcurrentModificationException

        }

        if(place.isEmpty()) {
            if(places.entrySet().size()>0){
                place = "Unknown Places";
            }else {
                place = "null";
            }
        }else {
            placeName = place;
        }

        environmentTextView.setText(place);
    }

    @Override
    public void onReceivedAmbientSound(double volume) {

    }

    @Override
    public void onReceivedStepsCounter(long steps) {

    }

    @Override
    public void onReceivedWeather(String condition, float temperature) {

    }
}
