package com.example.leo.datacollector.sensors;

import com.google.android.gms.location.DetectedActivity;

/**
 * Created by Yunlong on 4/21/2016.
 */
public interface MyActivityListener {
    void activityUpdate(DetectedActivity activity);
    void stopActivityDetection();
}
