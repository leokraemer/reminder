package com.example.yunlong.datacollector.sensors;

import android.location.Location;

/**
 * Created by Yunlong on 3/2/2016.
 */
public interface MyLocationListener {
    void locationChanged(Location location);
    void stopLocationUpdate();
}