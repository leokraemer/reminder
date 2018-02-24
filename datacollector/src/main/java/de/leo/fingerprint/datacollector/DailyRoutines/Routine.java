package de.leo.fingerprint.datacollector.DailyRoutines;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by Leo on 30.08.2017.
 */

public class Routine {
    public Polyline line;
    public Marker marker;
    public JSONObject walkingInfo;
    public POI from;
    public POI to;
    public int times;
    public Calendar average_leaving_time;
    public Calendar average_arriving_time;

    /**
     * @param line                  visual representation of the routine on map
     * @param marker                marker on map
     * @param walkingInfo           google maps distance information
     * @param from                  from POI
     * @param to                    to POI
     * @param times                 how many times visited
     * @param average_leaving_time
     * @param average_arriving_time
     */
    public Routine(Polyline line, Marker marker, JSONObject walkingInfo, POI from, POI to, int times, Calendar average_leaving_time, Calendar average_arriving_time) {
        this.line = line;
        this.marker = marker;
        this.walkingInfo = walkingInfo;
        this.from = from;
        this.to = to;
        this.times = times;
        this.average_leaving_time = average_leaving_time;
        this.average_arriving_time = average_arriving_time;
    }

    public String getFromText() {
        try {
            return walkingInfo.getJSONArray("origin_addresses")
                    .getString(0);
        } catch (JSONException e) {
            return "";
        }
    }

    public String getToText() {
        try {
            return walkingInfo.getJSONArray("destination_addresses")
                    .getString(0);
        } catch (JSONException e) {
            return "";
        }
    }

    //https://stackoverflow.com/questions/20074496/walking-distance-android-maps
    //see also https://maps.googleapis.com/maps/api/distancematrix/json?origins=41.995908,%2021.431491&destinations=41.996097,%2021.422419&mode=walking&sensor=false
    public String getDistanceText() {
        try {
            return walkingInfo.getJSONArray("rows")
                    .getJSONObject(0)
                    .getJSONArray("elements")
                    .getJSONObject(0)
                    .getJSONObject("distance")
                    .getString("text");
        } catch (JSONException e) {
            return "";
        }
    }

    public int getDistance() {
        try {
            return walkingInfo.getJSONArray("rows")
                    .getJSONObject(0)
                    .getJSONArray("elements")
                    .getJSONObject(0)
                    .getJSONObject("distance")
                    .getInt("value");
        } catch (JSONException e) {
            return 0;
        }
    }
}