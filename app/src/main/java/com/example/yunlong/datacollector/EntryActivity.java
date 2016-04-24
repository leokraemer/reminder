package com.example.yunlong.datacollector;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.yunlong.datacollector.MainActivity;
import com.example.yunlong.datacollector.R;
import com.example.yunlong.datacollector.services.DataCollectorService;
import com.example.yunlong.datacollector.settings.FingerPrintSettingsActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EntryActivity extends AppCompatActivity {

    TextView textView;
    Button buttonActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);


        textView = (TextView)findViewById(R.id.entry_activity_text);
        buttonActivity = (Button)findViewById(R.id.button_activity);
        startScheduledUpdate();
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
            Intent intent = new Intent(this, FingerPrintSettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void startActivity(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    public void startService(View view){
        Intent intent = new Intent(this, DataCollectorService.class);
        startService(intent);
        updateUI();
    }

    public void stopService(View view){
        Intent intent = new Intent(this, DataCollectorService.class);
        stopService(intent);
        updateUI();
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
                                  updateUI();
                                }
                            });

                        }catch (Exception e){
                            System.err.println("error in executing: " + ". It will no longer be run!");
                            e.printStackTrace();
                            // and re throw it so that the Executor also gets this error so that it can do what it would
                            throw new RuntimeException(e);
                        }

                    }
                }, 0, 10, TimeUnit.SECONDS);
    }

    private void updateUI(){
        boolean running = isMyServiceRunning(DataCollectorService.class);
        textView.setText("Data Collector Service: " + running);
        if(running){
            if(buttonActivity.isEnabled()){
                buttonActivity.setEnabled(false);
            }
        }else{
            if(!buttonActivity.isEnabled()){
                buttonActivity.setEnabled(true);
            }
        };
    }
}
