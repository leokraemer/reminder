package com.example.leo.datacollector.sensors;

import java.util.HashMap;

/**
 * Created by Yunlong on 4/4/2017.
 */

public interface GooglePlacesListener {
    void onReceivedPlaces(HashMap<String,Float> places);
}