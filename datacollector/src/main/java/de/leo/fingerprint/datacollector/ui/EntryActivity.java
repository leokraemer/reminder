package de.leo.fingerprint.datacollector.ui;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.leo.fingerprint.datacollector.R;
import de.leo.fingerprint.datacollector.datacollection.DataCollectorService;
import de.leo.fingerprint.datacollector.ui.settings.FingerPrintSettingsActivity;
import de.leo.fingerprint.datacollector.ui.uiElements.ComboSeekBar.ComboSeekBar;

public class EntryActivity extends AppCompatActivity {
    String TAG = "EntryActivity";
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    final int MY_PERMISSIONS_REQUEST_ACCESS_COURSE_LOCATION = 1;
    final int VIEW_SENSOR_DATA = 0;
    final int START_SERVICE = 1;
    final int START_LABEL = 2;
    final int STOP_LABEL = 3;
    TextView textView;
    int mood = -1;
    int state = -1;
    ComboSeekBar comboStartLabel, comboStopLabel;
    Context context;
    Button buttonStartService, buttonStopService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        context = this;

        textView = (TextView) findViewById(R.id.entry_activity_text);
        buttonStartService = (Button) findViewById(R.id.button_start_service);
        buttonStopService = (Button) findViewById(R.id.button_stop_service);

        startScheduledUpdate();
        updateUI();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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

    public void OnClickStartService(View view) {

        if (!CheckGPS()) {
            state = START_SERVICE;
            return;
        }
        if (!CheckPermission()) {
            return;
        }

        boolean running = isMyServiceRunning(DataCollectorService.class);
        if (!running) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
                    (this);
            String userName = sharedPreferences.getString("fingerprint_user_name", "userName");
            if (userName.equals("userName") || userName.equals("Name")) {
                Toast.makeText(this, "please type in your name in settings", Toast.LENGTH_SHORT)
                        .show();
                Log.d(TAG, "please type in your name in settings");
                return;
            } else {
                Intent intent = new Intent(this, DataCollectorService.class);
                startService(intent);
                updateUI();
            }
        }
    }

    public void OnClickStopService(View view) {
        boolean running = isMyServiceRunning(DataCollectorService.class);
        if (running) {
            Intent intent = new Intent(this, DataCollectorService.class);
            stopService(intent);
            updateUI();
        }
    }

    public void OnClickViewSensorData(View view) {
        if (!CheckGPS()) {
            state = VIEW_SENSOR_DATA;
            return;
        }
        if (!CheckPermission()) {
            return;
        }
    }

    public void OnClickStartLabel(View view) {
        boolean running = isMyServiceRunning(DataCollectorService.class);
        if (running) {
            //Toast.makeText(this, idx + " " + currentLabel, Toast.LENGTH_SHORT).show();
            state = START_LABEL;
        } else {
            Toast.makeText(this, "Please start service first.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer
                .MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startScheduledUpdate() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                }
                            });

                        } catch (Exception e) {
                            System.err.println("error in executing: " + ". It will no longer be " +
                                    "run!");
                            e.printStackTrace();
                            // and re throw it so that the Executor also gets this error so that
                            // it can do what it would
                            throw new RuntimeException(e);
                        }

                    }
                }, 0, 10, TimeUnit.SECONDS);
    }

    private void updateUI() {


        boolean running = isMyServiceRunning(DataCollectorService.class);
        textView.setText("Data Collector Service: " + running);
        if (running) {
            buttonStartService.setEnabled(false);
            buttonStopService.setEnabled(true);

        } else {
            buttonStartService.setEnabled(true);
            buttonStopService.setEnabled(false);
        }

    }

    private boolean CheckGPS() {
        LocationManager locationManager = (LocationManager) getSystemService(Context
                .LOCATION_SERVICE);

        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled || !network_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("GPS is disabled in your device. Would you like to enable it?");
            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                }
            });
            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                }
            });
            dialog.show();
            return false;
        }
        return true;
    }

    private boolean CheckPermission() {
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, Manifest
                .permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission
                        .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COURSE_LOCATION);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean running = isMyServiceRunning(DataCollectorService.class);
                    if (!running) {
                        SharedPreferences sharedPreferences = PreferenceManager
                                .getDefaultSharedPreferences(this);
                        String userName = sharedPreferences.getString("fingerprint_user_name",
                                "userName");
                        if (userName.equals("userName") || userName.equals("Name")) {
                            Toast.makeText(this, "please type in your name in settings", Toast
                                    .LENGTH_SHORT).show();
                            Log.d(TAG, "please type in your name in settings");
                            return;
                        } else {
                            Intent intent = new Intent(this, DataCollectorService.class);
                            startService(intent);
                            updateUI();
                        }
                    }
                } else {
                    return;
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_ACCESS_COURSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (state == START_SERVICE) {
                        boolean running = isMyServiceRunning(DataCollectorService.class);
                        if (!running) {
                            SharedPreferences sharedPreferences = PreferenceManager
                                    .getDefaultSharedPreferences(this);
                            String userName = sharedPreferences.getString
                                    ("fingerprint_user_name", "userName");
                            if (userName.equals("userName") || userName.equals("Name")) {
                                Toast.makeText(this, "please type in your name in settings",
                                        Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "please type in your name in settings");
                                return;
                            } else {
                                Intent intent = new Intent(this, DataCollectorService.class);
                                startService(intent);
                                updateUI();
                            }
                        }
                    } else if (state == VIEW_SENSOR_DATA) {

                    }
                } else {
                    return;
                }
                return;
            }
            default:
                return;
        }
    }
}