package de.leo.smartTrigger.datacollector.datacollection.sensors;

import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
 * Created by Yunlong on 4/21/2016.
 */
public interface MyActivityListener {
    void activityUpdate(List<DetectedActivity> activity);
    void stopActivityDetection();
}
