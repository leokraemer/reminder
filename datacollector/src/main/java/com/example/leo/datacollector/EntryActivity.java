package com.example.leo.datacollector;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.leo.datacollector.ComboSeekBar.ComboSeekBar;
import com.example.leo.datacollector.application.DataCollectorApplication;
import com.example.leo.datacollector.datacollection.DataCollectorService;
import com.example.leo.datacollector.settings.FingerPrintSettingsActivity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EntryActivity extends AppCompatActivity {
    String TAG = "EntryActivity";
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    final int MY_PERMISSIONS_REQUEST_ACCESS_COURSE_LOCATION = 1;
    final int VIEW_SENSOR_DATA = 0;
    final int START_SERVICE = 1;
    final int START_LABEL = 2;
    final int STOP_LABEL = 3;
    TextView textView;
    RadioGroup radioGroupLabelType, radioGroupStopLabel;
    String currentLabel = null;
    int mood = -1;
    int state = -1;
    ComboSeekBar comboStartLabel, comboStopLabel;
    Context context;
    Button buttonStartService, buttonStopService, buttonStartLabel, buttonStopLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        context = this;

        textView = (TextView) findViewById(R.id.entry_activity_text);
        radioGroupLabelType = (RadioGroup) findViewById(R.id.radio_group);
        buttonStartService = (Button) findViewById(R.id.button_start_service);
        buttonStopService = (Button) findViewById(R.id.button_stop_service);
        buttonStartLabel = (Button) findViewById(R.id.button_start_label);
        buttonStopLabel = (Button) findViewById(R.id.button_stop_label);

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
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String userName = sharedPreferences.getString("fingerprint_user_name", "userName");
            if (userName.equals("userName") || userName.equals("Name")) {
                Toast.makeText(this, "please type in your name in settings", Toast.LENGTH_SHORT).show();
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
            radioGroupLabelType.clearCheck();
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

        Intent intent = new Intent(this, SensorOverviewActivity.class);
        startActivity(intent);
    }

    public void OnClickStartLabel(View view) {
        boolean running = isMyServiceRunning(DataCollectorService.class);
        if (running) {
            int id = radioGroupLabelType.getCheckedRadioButtonId();
            View radioButton = radioGroupLabelType.findViewById(id);
            int idx = radioGroupLabelType.indexOfChild(radioButton);
            if (idx < 0) {
                Toast.makeText(this, "Please select one of the options.", Toast.LENGTH_SHORT).show();
                return;
            }
            currentLabel = (String) ((RadioButton) radioButton).getText();
            //Toast.makeText(this, idx + " " + currentLabel, Toast.LENGTH_SHORT).show();
            state = START_LABEL;
            ShowDialog(state);
        } else {
            Toast.makeText(this, "Please start service first.", Toast.LENGTH_SHORT).show();
        }
    }

    public void OnClickStopLabel(View view) {
        if (currentLabel != null) {
            state = STOP_LABEL;
            ShowDialog(state);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
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
                            System.err.println("error in executing: " + ". It will no longer be run!");
                            e.printStackTrace();
                            // and re throw it so that the Executor also gets this error so that it can do what it would
                            throw new RuntimeException(e);
                        }

                    }
                }, 0, 10, TimeUnit.SECONDS);
    }

    private void updateUI() {

        if (state == START_LABEL) {
            buttonStartLabel.setEnabled(false);
            buttonStopLabel.setEnabled(true);
        } else {
            buttonStartLabel.setEnabled(true);
            buttonStopLabel.setEnabled(false);
        }

        boolean running = isMyServiceRunning(DataCollectorService.class);
        textView.setText("Data Collector Service: " + running);
        if (running) {
            buttonStartService.setEnabled(false);
            buttonStopService.setEnabled(true);

        } else {
            buttonStartService.setEnabled(true);
            buttonStopService.setEnabled(false);
            buttonStartLabel.setEnabled(false);
            buttonStopLabel.setEnabled(false);
        }

    }

    private boolean CheckGPS() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (state == START_SERVICE) {
                        boolean running = isMyServiceRunning(DataCollectorService.class);
                        if (!running) {
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                            String userName = sharedPreferences.getString("fingerprint_user_name", "userName");
                            if (userName.equals("userName") || userName.equals("Name")) {
                                Toast.makeText(this, "please type in your name in settings", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "please type in your name in settings");
                                return;
                            } else {
                                Intent intent = new Intent(this, DataCollectorService.class);
                                startService(intent);
                                updateUI();
                            }
                        }
                    } else if (state == VIEW_SENSOR_DATA) {
                        Intent intent = new Intent(this, SensorOverviewActivity.class);
                        startActivity(intent);
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
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                            String userName = sharedPreferences.getString("fingerprint_user_name", "userName");
                            if (userName.equals("userName") || userName.equals("Name")) {
                                Toast.makeText(this, "please type in your name in settings", Toast.LENGTH_SHORT).show();
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

    private void ShowDialog(int action) {
        if (action == START_LABEL) {
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_start_label);
            setUpSeekBarStartLabel(dialog);

            Button button = (Button) dialog.findViewById(R.id.button_done_dialog_start_label);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (currentLabel != null && mood > 0) {
                        sendMessage2Service(currentLabel);

                    } else {
                        Toast.makeText(context, "Please finish the questions.", Toast.LENGTH_SHORT).show();
                    }

                    dialog.dismiss();
                }
            });
            dialog.show();
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    state = -1;
                    updateUI();
                }
            });

        } else if (action == STOP_LABEL) {
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_stop_label);
            setUpSeekBarStopLabel(dialog);
            radioGroupStopLabel = (RadioGroup) dialog.findViewById(R.id.radio_group_typical_routine_dialog_stop_label);

            Button button = (Button) dialog.findViewById(R.id.button_done_dialog_stop_label);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = radioGroupStopLabel.getCheckedRadioButtonId();
                    View radioButton = radioGroupStopLabel.findViewById(id);
                    int idx = radioGroupStopLabel.indexOfChild(radioButton);
                    if (idx < 0) {
                        Toast.makeText(context, "Please select one of the options.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String isTypicalRoutine = (String) ((RadioButton) radioButton).getText();
                    if (currentLabel != null && mood > 0) {
                        sendMessage2Service("null");
                        currentLabel = null;
                        radioGroupLabelType.clearCheck();
                    } else {
                        Toast.makeText(context, "Please finish the questions.", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                }
            });
            dialog.show();
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    state = -1;
                    updateUI();
                }
            });
        }
    }

    private void setUpSeekBarStartLabel(Dialog view) {
        final float scale = getResources().getDisplayMetrics().density;
        comboStartLabel = new ComboSeekBar(this);
        List<String> seekBarStep = Arrays.asList("very | bad", " ", " ", " ", " ", " ", "very |good ");
        comboStartLabel.setAdapter(seekBarStep);
        comboStartLabel.setSelection(3);
        comboStartLabel.setColor(Color.WHITE);
        int textSize = (int) (15 * scale + 0.5f);
        comboStartLabel.setTextSize(textSize);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        comboStartLabel.setLayoutParams(layoutParams);
        comboStartLabel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                comboStartLabel.setColor(Color.BLUE);
                mood = i;
            }
        });
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.bar_holder_dialog_start_label);
        linearLayout.addView(comboStartLabel);
        mood = -1;
    }

    private void setUpSeekBarStopLabel(Dialog view) {
        final float scale = getResources().getDisplayMetrics().density;
        comboStopLabel = new ComboSeekBar(this);
        List<String> seekBarStep = Arrays.asList("very | bad", " ", " ", " ", " ", " ", "very |good ");
        comboStopLabel.setAdapter(seekBarStep);
        comboStopLabel.setSelection(3);
        comboStopLabel.setColor(Color.WHITE);
        int textSize = (int) (15 * scale + 0.5f);
        comboStopLabel.setTextSize(textSize);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        comboStopLabel.setLayoutParams(layoutParams);
        comboStopLabel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                comboStopLabel.setColor(Color.BLUE);
                mood = i;
            }
        });
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.bar_holder_dialog_stop_label);
        linearLayout.addView(comboStopLabel);
        mood = -1;
    }


    private void sendMessage2Service(String label) {
        Intent intent = new Intent(DataCollectorApplication.BROADCAST_EVENT);
        // add data
        intent.putExtra("label", label);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
