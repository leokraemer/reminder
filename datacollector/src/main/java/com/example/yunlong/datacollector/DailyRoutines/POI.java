package com.example.yunlong.datacollector.DailyRoutines;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.maps.android.SphericalUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leo on 25.08.2017.
 */

public class POI {
    public JSONObject raw;
    public Marker marker;
    public int maxVisits;
    public int popularPlacesClusterIndex;
    public double POIs_center_latitude;
    public double POIs_center_longitude;
    public double POIs_max_latitude;
    public double POIs_min_latitude;
    public double POIs_max_longitude;
    public double POIs_min_longitude;
    public int POIs_visit_times;
    public int POIs_coverage_days;
    public double POIs_average_duration;
    public String locationText = "";
    public double area;
    public double diameter;

    public POI(JSONObject jsonObject) throws JSONException {
        raw = jsonObject;
        popularPlacesClusterIndex = jsonObject.getInt("popularPlacesClusterIndex");
        POIs_center_latitude = jsonObject.getDouble("POIs_center_latitude");
        POIs_center_longitude = jsonObject.getDouble("POIs_center_longitude");
        POIs_max_latitude = jsonObject.getDouble("POIs_max_latitude");
        POIs_min_latitude = jsonObject.getDouble("POIs_min_latitude");
        POIs_max_longitude = jsonObject.getDouble("POIs_max_longitude");
        POIs_min_longitude = jsonObject.getDouble("POIs_min_longitude");
        POIs_visit_times = jsonObject.getInt("POIs_visit_times");
        POIs_coverage_days = jsonObject.getInt("POIs_coverage_days");
        POIs_average_duration = jsonObject.getDouble("POIs_average_duration");
        ArrayList<LatLng> triangle = new ArrayList<>();
        triangle.add(new LatLng(POIs_min_latitude, POIs_min_longitude));  // Should match last point
        triangle.add(new LatLng(POIs_min_latitude, POIs_max_longitude));
        triangle.add(new LatLng(POIs_max_latitude, POIs_max_longitude));
        triangle.add(new LatLng(POIs_max_latitude, POIs_min_longitude));
        area = SphericalUtil.computeArea(triangle);
        diameter = SphericalUtil.computeDistanceBetween(new LatLng(POIs_min_latitude, POIs_min_longitude), new LatLng(POIs_max_latitude, POIs_max_longitude));
    }

    @NonNull
    public LatLng getPOILatLng() {
        return new LatLng(POIs_center_latitude, POIs_center_longitude);
    }


}
