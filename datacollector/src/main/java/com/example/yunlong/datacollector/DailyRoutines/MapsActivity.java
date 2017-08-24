package com.example.yunlong.datacollector.DailyRoutines;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.yunlong.datacollector.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Cap;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String POI_S_CENTER_LONGITUDE = "POIs_center_longitude";
    public static final String POI_S_CENTER_LATITUDE = "POIs_center_latitude";
    public static final String POPULAR_PLACES_CLUSTER_INDEX = "popularPlacesClusterIndex";
    private GoogleMap mMap;
    private Map<Integer, JSONObject> globalPOIs;
    private int largestPOIIndex = 0;
    private JSONArray globalPOI_visits;
    private JSONArray globalPOI_routine_patterns;
    public static final float STEP_LENGTH = 0.7874f;
    private String URLStart = "http://maps.googleapis.com/maps/api/distancematrix/json?origins=";
    private String URLMiddle = "&destinations=";
    private String URLEnd = "&mode=walking&sensor=false";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        RoutineProvider rp = new RoutineProvider(this);
        globalPOI_routine_patterns = rp.getGlobalPOI_routine_patterns();
        globalPOI_visits = rp.getGlobalPOI_visits();
        JSONArray globalPOIsArray = rp.getGlobalPOIs();
        globalPOIs = new HashMap<>();
        for (int i = 0; i < globalPOIsArray.length(); i++) {
            try {
                int index = ((JSONObject) globalPOIsArray.get(i)).getInt(POPULAR_PLACES_CLUSTER_INDEX);
                globalPOIs.put(index, ((JSONObject) globalPOIsArray.get(i)));
                if (index > largestPOIIndex)
                    largestPOIIndex = index;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        try {
            addRoutineMarkers();
            addRoutineLines();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addRoutineLines() throws JSONException, IOException {
        //get information about routines
        int[][] visits = new int[largestPOIIndex + 1][largestPOIIndex + 1];
        for (int i = 0; i < globalPOI_routine_patterns.length(); i++) {
            try {
                Integer from = ((JSONObject) globalPOI_routine_patterns.get(i))
                        .getInt("poi_start");
                Integer to = ((JSONObject) globalPOI_routine_patterns.get(i))
                        .getInt("poi_end");
                visits[from][to]++;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        for (int from : globalPOIs.keySet()) {
            for (int to : globalPOIs.keySet()) {
                if (globalPOIs.get(from).optInt("count", 0) < visits[from][to])
                    globalPOIs.get(from).put("count", visits[from][to]);
            }
        }
        for (int from : globalPOIs.keySet()) {
            for (int to : globalPOIs.keySet()) {
                //add lines
                if (visits[from][to] > 0) {
                    JSONObject fromPOI = globalPOIs.get(from);
                    JSONObject toPOI = globalPOIs.get(to);
                    LatLng fromlatlang = getPOILatLng(fromPOI);
                    LatLng tolatlang = getPOILatLng(toPOI);
                    Polyline line = mMap.addPolyline(new PolylineOptions()
                            .add(fromlatlang, tolatlang)
                            .width(globalPOIs.get(from).getInt("count"))
                            .color(Color.RED)
                            .endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.arrow), 32))
                            .clickable(true)
                    );
                    //add step data
                    IconGenerator iconFactory = new IconGenerator(this);
                    MarkerLoader ml = new MarkerLoader(line, iconFactory);
                    ml.execute();
                }
            }
        }
    }

    private class MarkerLoader extends AsyncTask<Void, Void, Void> {

        private Polyline line;
        private IconGenerator iconFactory;
        private int walkinDistanceMeters;
        private LatLng start;
        private LatLng dest;

        public MarkerLoader(Polyline line, IconGenerator iconFactory) {
            this.line = line;
            this.iconFactory = iconFactory;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            start = line.getPoints().get(0);
            dest = line.getPoints().get(line.getPoints().size() - 1);
        }

        @Override
        protected Void doInBackground(Void... params) {
            String startLatLng = Double.toString(start.latitude) + ",%20" + Double.toString(start.longitude);
            String destLatLng = Double.toString(dest.latitude) + ",%20" + Double.toString(dest.longitude);
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(URLStart + startLatLng + URLMiddle + destLatLng + URLEnd);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                JSONObject stepData = new JSONObject(result.toString());
                //https://stackoverflow.com/questions/20074496/walking-distance-android-maps
                //see also https://maps.googleapis.com/maps/api/distancematrix/json?origins=41.995908,%2021.431491&destinations=41.996097,%2021.422419&mode=walking&sensor=false
                walkinDistanceMeters = stepData.getJSONArray("rows")
                        .getJSONObject(0)
                        .getJSONArray("elements")
                        .getJSONObject(0)
                        .getJSONObject("distance")
                        .getInt("value");
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            LatLng start = line.getPoints().get(0);
            LatLng dest = line.getPoints().get(line.getPoints().size() - 1);
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(start).include(dest);
            LatLngBounds bounds = builder.build();
            bounds.getCenter();
            addIcon(iconFactory, "0/" + Math.round(walkinDistanceMeters / MapsActivity.STEP_LENGTH), bounds.getCenter());
        }
    }

    @NonNull
    private LatLng getPOILatLng(JSONObject fromPOI) throws JSONException {
        return new LatLng(fromPOI.getDouble(POI_S_CENTER_LATITUDE), fromPOI.getDouble(POI_S_CENTER_LONGITUDE));
    }

    private void addIcon(IconGenerator iconFactory, CharSequence text, LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());
        mMap.addMarker(markerOptions);
    }

    private void addRoutineMarkers() throws JSONException {
        for (int i : globalPOIs.keySet()) {
            //Add markers
            JSONObject poi = globalPOIs.get(i);
            LatLng latlang = getPOILatLng(poi);
            mMap.addMarker(new MarkerOptions().position(latlang).title(poi.getString(POPULAR_PLACES_CLUSTER_INDEX)));
        }
        //zoom too markers
        JSONObject poi = globalPOIs.get(globalPOIs.keySet().toArray()[0]);
        LatLng latlang = getPOILatLng(poi);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlang, 12));
    }
}
