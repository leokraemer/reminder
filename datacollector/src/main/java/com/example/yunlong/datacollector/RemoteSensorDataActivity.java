package com.example.yunlong.datacollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.androidplot.Plot;
import com.androidplot.util.PlotStatistics;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.example.yunlong.datacollector.algorithms.DTW;
import com.example.yunlong.datacollector.services.HelloAccessoryProviderService;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoteSensorDataActivity extends AppCompatActivity {

    private static final int HISTORY_SIZE = 300;            // number of points to plot in history

    private XYPlot aprLevelsPlot = null;
    private XYPlot aprHistoryPlot = null;

    private CheckBox hwAcceleratedCb;
    private CheckBox showFpsCb;
    //private SimpleXYSeries aprLevelsSeries = null;
    private SimpleXYSeries aLvlSeries;
    private SimpleXYSeries pLvlSeries;
    private SimpleXYSeries rLvlSeries;
    private SimpleXYSeries azimuthHistorySeries = null;
    private SimpleXYSeries pitchHistorySeries = null;
    private SimpleXYSeries rollHistorySeries = null;

    private Redrawer redrawer;

    MyDataReceiver myReceiver;

    Button buttonRecord,buttonRecognize,buttonLED;
    TextView textDist;
    boolean isRecording = false;
    boolean isRecognizing = false;

    ArrayList<Float> data_x = new ArrayList<Float>();
    ArrayList<Float> data_y = new ArrayList<Float>();
    ArrayList<Float> data_z = new ArrayList<Float>();

    ArrayList<Float> template_x = new ArrayList<Float>();
    ArrayList<Float> template_y = new ArrayList<Float>();
    ArrayList<Float> template_z = new ArrayList<Float>();

    final int window_size = 10;
    int cnt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orientation_sensor_example);

        initChart();
        initButton();
        textDist = (TextView)findViewById(R.id.text_dist);
    }


    @Override
    public void onResume() {
        super.onResume();
        redrawer.start();
    }

    @Override
    public void onStart(){
        //Register BroadcastReceiver
        //to receive event from our service
        myReceiver = new MyDataReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HelloAccessoryProviderService.TAG);
        registerReceiver(myReceiver, intentFilter);
        super.onStart();
    }

    @Override
    public void onPause() {
        redrawer.pause();
        super.onPause();
    }

    @Override
    public void onStop(){
        unregisterReceiver(myReceiver);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        redrawer.finish();
        super.onDestroy();
    }

    private class MyDataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
/*            int datapassed = arg1.getIntExtra("DATAPASSED", 0);
            Toast.makeText(context,
                    "Triggered by Service!\n"
                            + "Data passed: " + String.valueOf(datapassed),
                    Toast.LENGTH_LONG).show();
            triggerPropt();*/
            String data = arg1.getStringExtra("data");
            String[] dataArray = data.split(",");
            float x = Float.valueOf(dataArray[0]);
            float y = Float.valueOf(dataArray[1]);
            float z = Float.valueOf(dataArray[2]);

            // update level data:
            aLvlSeries.setModel(Arrays.asList(
                            new Number[]{x}),
                    SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

            pLvlSeries.setModel(Arrays.asList(
                            new Number[]{y}),
                    SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

            rLvlSeries.setModel(Arrays.asList(
                            new Number[]{z}),
                    SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

            // get rid the oldest sample in history:
            if (rollHistorySeries.size() > HISTORY_SIZE) {
                rollHistorySeries.removeFirst();
                pitchHistorySeries.removeFirst();
                azimuthHistorySeries.removeFirst();
            }

            // add the latest history sample:
            azimuthHistorySeries.addLast(null, x);
            pitchHistorySeries.addLast(null, y);
            rollHistorySeries.addLast(null, z);

            if(isRecording){
                template_x.add(x);
                template_y.add(y);
                template_z.add(z);

            }else if(isRecognizing){
                data_x.add(x);
                data_y.add(y);
                data_z.add(z);

                if(data_x.size()>template_x.size()*3){
                    cnt++;
                    if(cnt>=window_size) {
                        cnt = 0;
                        float[] datax = getDataArray(data_x.subList(data_x.size() - template_x.size() * 2 - 1, data_x.size() - 1));
                        float[] datay = getDataArray(data_y.subList(data_y.size() - template_y.size() * 2 - 1, data_y.size() - 1));
                        float[] dataz = getDataArray(data_z.subList(data_z.size() - template_z.size() * 2 - 1, data_z.size() - 1));
                        DTW dtw1 = new DTW(datax, getDataArray(template_x));
                        DTW dtw2 = new DTW(datay, getDataArray(template_y));
                        DTW dtw3 = new DTW(dataz, getDataArray(template_z));
                        double dis = dtw1.getDistance() + dtw2.getDistance() + dtw3.getDistance();
                        textDist.setText("dist: " + dis);
                        if(dis<10){
                            buttonLED.setBackgroundColor(Color.BLUE);
                        }else {
                            buttonLED.setBackgroundColor(Color.GRAY);
                        }
                    }
                }
            }


        }
    }

    public void initChart(){
        // setup the APR Levels plot:
        aprLevelsPlot = (XYPlot) findViewById(R.id.aprLevelsPlot);
        aprLevelsPlot.setDomainBoundaries(-1, 1, BoundaryMode.FIXED);
        aprLevelsPlot.getGraphWidget().getDomainTickLabelPaint().setColor(Color.TRANSPARENT);

        aLvlSeries = new SimpleXYSeries("A");
        pLvlSeries = new SimpleXYSeries("P");
        rLvlSeries = new SimpleXYSeries("R");

        aprLevelsPlot.addSeries(aLvlSeries,
                new BarFormatter(Color.rgb(0, 200, 0), Color.rgb(0, 80, 0)));
        aprLevelsPlot.addSeries(pLvlSeries,
                new BarFormatter(Color.rgb(200, 0, 0), Color.rgb(0, 80, 0)));
        aprLevelsPlot.addSeries(rLvlSeries,
                new BarFormatter(Color.rgb(0, 0, 200), Color.rgb(0, 80, 0)));

        aprLevelsPlot.setDomainStepValue(3);
        aprLevelsPlot.setTicksPerRangeLabel(3);

        // per the android documentation, the minimum and maximum readings we can get from
        // any of the orientation sensors is -180 and 359 respectively so we will fix our plot's
        // boundaries to those values.  If we did not do this, the plot would auto-range which
        // can be visually confusing in the case of dynamic plots.
        aprLevelsPlot.setRangeBoundaries(-20, 20, BoundaryMode.FIXED);

        // update our domain and range axis labels:
        aprLevelsPlot.setDomainLabel("");
        aprLevelsPlot.getDomainLabelWidget().pack();
        aprLevelsPlot.setRangeLabel("Angle (Degs)");
        aprLevelsPlot.getRangeLabelWidget().pack();
        aprLevelsPlot.setGridPadding(5, 0, 5, 0);
        aprLevelsPlot.setRangeValueFormat(new DecimalFormat("#"));

        // setup the APR History plot:
        aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);

        azimuthHistorySeries = new SimpleXYSeries("Az.");
        azimuthHistorySeries.useImplicitXVals();
        pitchHistorySeries = new SimpleXYSeries("Pitch");
        pitchHistorySeries.useImplicitXVals();
        rollHistorySeries = new SimpleXYSeries("Roll");
        rollHistorySeries.useImplicitXVals();

        aprHistoryPlot.setRangeBoundaries(-20, 20, BoundaryMode.FIXED);
        aprHistoryPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        aprHistoryPlot.addSeries(azimuthHistorySeries,
                new LineAndPointFormatter(
                        Color.rgb(100, 100, 200), null, null, null));
        aprHistoryPlot.addSeries(pitchHistorySeries,
                new LineAndPointFormatter(
                        Color.rgb(100, 200, 100), null, null, null));
        aprHistoryPlot.addSeries(rollHistorySeries,
                new LineAndPointFormatter(
                        Color.rgb(200, 100, 100), null, null, null));
        aprHistoryPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        aprHistoryPlot.setDomainStepValue(HISTORY_SIZE/10);
        aprHistoryPlot.setTicksPerRangeLabel(3);
        aprHistoryPlot.setDomainLabel("Sample Index");
        aprHistoryPlot.getDomainLabelWidget().pack();
        aprHistoryPlot.setRangeLabel("Angle (Degs)");
        aprHistoryPlot.getRangeLabelWidget().pack();

        aprHistoryPlot.setRangeValueFormat(new DecimalFormat("#"));
        aprHistoryPlot.setDomainValueFormat(new DecimalFormat("#"));

        // setup checkboxes:
        hwAcceleratedCb = (CheckBox) findViewById(R.id.hwAccelerationCb);
        final PlotStatistics levelStats = new PlotStatistics(1000, false);
        final PlotStatistics histStats = new PlotStatistics(1000, false);

        aprLevelsPlot.addListener(levelStats);
        aprHistoryPlot.addListener(histStats);
        hwAcceleratedCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    aprLevelsPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                    aprHistoryPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                } else {
                    aprLevelsPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    aprHistoryPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
            }
        });

        showFpsCb = (CheckBox) findViewById(R.id.showFpsCb);
        showFpsCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                levelStats.setAnnotatePlotEnabled(b);
                histStats.setAnnotatePlotEnabled(b);
            }
        });

        // get a ref to the BarRenderer so we can make some changes to it:
        BarRenderer barRenderer = (BarRenderer) aprLevelsPlot.getRenderer(BarRenderer.class);
        if(barRenderer != null) {
            // make our bars a little thicker than the default so they can be seen better:
            barRenderer.setBarWidth(25);
        }

        redrawer = new Redrawer(
                Arrays.asList(new Plot[]{aprHistoryPlot, aprLevelsPlot}),
                100, false);
    }

    public void initButton(){
        buttonRecord = (Button)findViewById(R.id.button_record);
        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecording){//stop
                    buttonRecord.setText("Record");
                    isRecording = false;
                    buttonRecognize.setEnabled(true);
                }else {
                    buttonRecord.setText("Stop");
                    isRecording = true;
                    buttonRecognize.setEnabled(false);
                    template_x.clear();
                    template_y.clear();
                    template_z.clear();
                }
            }
        });

        buttonRecognize = (Button)findViewById(R.id.button_recognize);
        buttonRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecognizing){
                    buttonRecognize.setText("Recognize");
                    isRecognizing = false;
                    buttonRecord.setEnabled(true);
                }else {
                    buttonRecognize.setText("Stop");
                    isRecognizing = true;
                    buttonRecord.setEnabled(false);
                    data_x.clear();
                    data_y.clear();
                    data_z.clear();
                }
            }
        });

        buttonLED = (Button)findViewById(R.id.button_led);
        buttonLED.setBackgroundColor(Color.GRAY);
    }

    private float[] getDataArray(List<Float> dataArrayList){
        float[] floatArray = new float[dataArrayList.size()];
        int i = 0;
        for (Float f : dataArrayList) {
            floatArray[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }
        return  floatArray;
    }

}
