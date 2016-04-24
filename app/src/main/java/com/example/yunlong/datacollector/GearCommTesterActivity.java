package com.example.yunlong.datacollector;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.yunlong.datacollector.services.HelloAccessoryProviderService;

import java.io.IOException;

public class GearCommTesterActivity extends AppCompatActivity {

    MyReceiver myReceiver;
    Context context;
    String text;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gear_comm_tester);
        context = this;
        textView = (TextView)findViewById(R.id.text_remote_data);
    }

    @Override
    protected void onStart() {
        //Register BroadcastReceiver
        //to receive event from our service
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HelloAccessoryProviderService.TAG);
        registerReceiver(myReceiver, intentFilter);

//	      //Start our own service
//	      Intent intent = new Intent(main.this,
//	      com.AndroidServiceTest.MyService.class);
//	      startService(intent);

        super.onStart();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        unregisterReceiver(myReceiver);
        super.onStop();
    }

    public void send2Gear2(){
        new Thread(new Runnable() {
            public void run() {
                try {
                    HelloAccessoryProviderService.connection.send(HelloAccessoryProviderService.HELLOACCESSORY_CHANNEL_ID, text.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void triggerPropt(){
        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(R.layout.prompts, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        // set prompts.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setView(promptView);
        final EditText input = (EditText) promptView.findViewById(R.id.userInput);
        // setup a dialog window
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // get user input and set it to result
                        //editTextMainScreen.setText(input.getText());
                        text = input.getText().toString();
                        send2Gear2();
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,	int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();

        alertD.show();
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
/*            int datapassed = arg1.getIntExtra("DATAPASSED", 0);
            Toast.makeText(context,
                    "Triggered by Service!\n"
                            + "Data passed: " + String.valueOf(datapassed),
                    Toast.LENGTH_LONG).show();
            triggerPropt();*/
            String data = arg1.getStringExtra("data");
            textView.setText("remote sensor data:" + "\n" + data);
        }
    }
}
