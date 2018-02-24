package de.leo.fingerprint.datacollector.datacollection.sensors;

import java.util.HashMap;

/**
 * Created by Yunlong on 4/4/2017.
 */

public interface GooglePlacesListener {
    void onReceivedPlaces(HashMap<String,Float> places);
}
