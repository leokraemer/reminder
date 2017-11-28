package com.example.leo.datacollector.DailyRoutines;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.example.leo.datacollector.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
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
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, AdapterView.OnItemClickListener {
    private final static int ALPHA_ADJUSTMENT = 0x77000000;
    public static final String POPULAR_PLACES_CLUSTER_INDEX = "popularPlacesClusterIndex";
    private GoogleMap mMap;
    private ListView mapExtraInfo;
    private Map<Integer, POI> globalPOIs;
    private List<Routine> routines = new ArrayList<>();
    private int largestPOIIndex = 0;
    private JSONArray globalPOI_visits;
    private JSONArray globalPOI_routine_patterns;
    public static final float STEP_LENGTH = 0.7874f;
    private String URLStart = "http://maps.googleapis.com/maps/api/distancematrix/json?origins=";
    private String URLMiddle = "&destinations=";
    private String URLEnd = "&mode=walking&sensor=false";
    private RoutineProvider rp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapExtraInfo = (ListView) findViewById(R.id.mapExtraInfo);
        MapExtraInfoListAdapter mapExtraInfoAdapter = new MapExtraInfoListAdapter(this, R.layout.routinepathinfo, routines);
        mapExtraInfo.setAdapter(mapExtraInfoAdapter);
        mapExtraInfo.setOnItemClickListener(this);
        mapFragment.getMapAsync(this);
        rp = new RoutineProvider(this);
        globalPOI_routine_patterns = rp.getGlobalPOI_routine_patterns();
        globalPOI_visits = rp.getGlobalPOI_visits();
        JSONArray globalPOIsArray = rp.getGlobalPOIs();
        globalPOIs = new HashMap<>();
        for (int i = 0; i < globalPOIsArray.length(); i++) {
            try {
                int index = getPOIClusterIndex(globalPOIsArray, i);
                globalPOIs.put(index, new POI((JSONObject) globalPOIsArray.get(i)));
                if (index > largestPOIIndex)
                    largestPOIIndex = index;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private int getPOIClusterIndex(JSONArray globalPOIsArray, int i) throws JSONException {
        return ((JSONObject) globalPOIsArray.get(i)).getInt(POPULAR_PLACES_CLUSTER_INDEX);
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
            addRoutineEndpointMarkers();
            addRoutineLines();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        mMap.setOnMarkerClickListener(this);
    }

    private void addRoutineLines() throws JSONException, IOException, ParseException {
        List<GlobalPOIsRoutinePattern> routinePatterns = new ArrayList<>();
        for (int i = 0; i < globalPOI_routine_patterns.length(); i++) {
            routinePatterns.add(new GlobalPOIsRoutinePattern(globalPOI_routine_patterns.getJSONObject(i), globalPOIs));
        }
        //get information about routines
        //create sparse visitation matrix
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
        //get maximum visitations of poi
        for (int from : globalPOIs.keySet()) {
            for (int to : globalPOIs.keySet()) {
                if (globalPOIs.get(from).maxVisits < visits[from][to])
                    globalPOIs.get(from).maxVisits = visits[from][to];
            }
        }
        for (int from : globalPOIs.keySet()) {
            for (int to : globalPOIs.keySet()) {
                //add lines
                if (visits[from][to] > 0) {
                    LatLng fromlatlang = globalPOIs.get(from).getPOILatLng();
                    LatLng tolatlang = globalPOIs.get(to).getPOILatLng();
                    Polyline line = mMap.addPolyline(new PolylineOptions()
                            .add(fromlatlang, tolatlang)
                            .width(visits[from][to])
                            .color(Color.RED)
                            .endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.arrow), 32))
                            .clickable(true)
                    );
                    //add step data
                    IconGenerator iconFactory = new IconGenerator(this);
                    Calendar[] averageTimes = getAverageTimes(from, to, routinePatterns);
                    Routine routine = new Routine(line, null, null, globalPOIs.get(from), globalPOIs.get(to), visits[from][to], averageTimes[0], averageTimes[1]);
                    routines.add(routine);
                    MarkerLoader ml = new MarkerLoader(line, iconFactory, routine);
                    ml.execute();
                }
            }
        }
    }

    private Calendar[] getAverageTimes(int from, int to, List<GlobalPOIsRoutinePattern> routinePatterns) throws ParseException {
        List<Long> leaving = new ArrayList<>();
        List<Long> arriving = new ArrayList<>();
        for (GlobalPOIsRoutinePattern g : routinePatterns) {
            if (g.start.popularPlacesClusterIndex == from && g.dest.popularPlacesClusterIndex == to) {
                leaving.add(g.leavingTime.getTimeInMillis());
                arriving.add(g.arrivingTime.getTimeInMillis());
            }
        }
        Long avgLeaving = 0l;
        Long avgAriving = 0l;
        //hacky way to get the average hour, minute and second of day, but way more concise than accessing through Calendar.getHours.
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        for (int i = 0; i < leaving.size(); i++) {
            avgLeaving += sdf.parse(sdf.format(leaving.get(i))).getTime();
            avgAriving += sdf.parse(sdf.format(arriving.get(i))).getTime();
        }
        Calendar arrivingC = new GregorianCalendar();
        Calendar leavingC = new GregorianCalendar();
        arrivingC.setTime(new Date(avgAriving / leaving.size()));
        leavingC.setTime(new Date(avgLeaving / leaving.size()));
        return new Calendar[]{arrivingC, leavingC};
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        boolean returnvalue = false;
        for (int i = 0; i < routines.size(); i++) {
            Routine r = routines.get(i);
            if (r.marker.equals(marker)) {
                returnvalue = true;
                LinearLayout.LayoutParams extraInfoLP = new LinearLayout.LayoutParams((mapExtraInfo.getLayoutParams()));
                extraInfoLP.height = LinearLayout.LayoutParams.MATCH_PARENT;
                extraInfoLP.weight = 1;
                mapExtraInfo.setLayoutParams(extraInfoLP);
                mapExtraInfo.invalidate();
                //scroll to info
                mapExtraInfo.setItemChecked(i, true);
                mapExtraInfo.setSelection(i);
                break;
            }
        }
        return returnvalue;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mapExtraInfo.setItemChecked(position, true);
        mapExtraInfo.setSelection(position);
        Routine r = (Routine) mapExtraInfo.getAdapter().getItem(position);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(r.marker.getPosition()));
    }

    private class MarkerLoader extends AsyncTask<Void, Void, Void> {

        private Polyline line;
        private IconGenerator iconFactory;
        private LatLng start;
        private LatLng dest;
        private Routine routine;
        private JSONObject stepData;

        public MarkerLoader(Polyline line, IconGenerator iconFactory, Routine routine) {
            this.line = line;
            this.iconFactory = iconFactory;
            this.routine = routine;
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
                stepData = new JSONObject(result.toString());
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
            //get mid point.
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(start).include(dest);
            LatLngBounds halfWayBounds = builder.build();
            //get fourth of the way point.
            builder = new LatLngBounds.Builder();
            builder.include(start).include(halfWayBounds.getCenter());
            LatLngBounds fourthOfTheWaybounds = builder.build();
            //save stepData
            routine.walkingInfo = stepData;
            //create marker at one fourth of the way from the start.
            routine.marker = addIcon(iconFactory,
                    routine.from.popularPlacesClusterIndex + " -> " + routine.to.popularPlacesClusterIndex + "\n 0/" + Math.round(routine.getDistance() / MapsActivity.STEP_LENGTH),
                    fourthOfTheWaybounds.getCenter());
            globalPOIs.get(routine.from.popularPlacesClusterIndex).locationText = routine.getFromText();
            globalPOIs.get(routine.to.popularPlacesClusterIndex).locationText = routine.getToText();
            mapExtraInfo.deferNotifyDataSetChanged();
        }
    }


    private Marker addIcon(IconGenerator iconFactory, CharSequence text, LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());
        return mMap.addMarker(markerOptions);
    }

    private void addRoutineEndpointMarkers() throws JSONException {
        for (int i : globalPOIs.keySet()) {
            //Add markers
            POI poi = globalPOIs.get(i);
            LatLng latlang = poi.getPOILatLng();
            poi.marker = mMap.addMarker(new MarkerOptions().position(latlang).title(poi.popularPlacesClusterIndex + ""));
            routines.add(new Routine(null, poi.marker, null, poi, null, poi.POIs_visit_times, null, null));
            ArrayList<LatLng> square = new ArrayList<>();
            square.add(new LatLng(poi.POIs_min_latitude, poi.POIs_min_longitude));  // Should match last point
            square.add(new LatLng(poi.POIs_min_latitude, poi.POIs_max_longitude));
            square.add(new LatLng(poi.POIs_max_latitude, poi.POIs_max_longitude));
            square.add(new LatLng(poi.POIs_max_latitude, poi.POIs_min_longitude));
            mMap.addPolygon(new PolygonOptions()
                    .addAll(square)
                    .fillColor(Color.BLUE - ALPHA_ADJUSTMENT)
                    .strokeColor(Color.BLUE)
                    .strokeWidth(2));
        }
        //zoom too markers
        POI poi = globalPOIs.get(globalPOIs.keySet().toArray()[0]);
        LatLng latlang = poi.getPOILatLng();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlang, 12));
    }
}
