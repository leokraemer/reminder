package de.leo.fingerprint.datacollector.datacollection.sensors;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import de.leo.fingerprint.datacollector.datacollection.models.FoursquareModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.util.ByteArrayBuffer;

/**
 * Created by Yunlong on 3/3/2016.
 */
public class FoursquareCaller {

    // ============== YOU SHOULD MAKE NEW KEYS ====================//
    final String CLIENT_ID = "NW2CAEYT43VSJEKJ2XUYYE4CXI1VNKATYUZVIWZ4MP1PITYU";
    final String CLIENT_SECRET = "5ILSTDHJEVBKRLWQLYFP4DBNP4135PNSKDKY4IXTVHHDKV1D";

    Context context;
    ArrayList<FoursquareModel> resultList;
    FourSquareListener fourSquareListener;
    Location location;

    public FoursquareCaller(Context context, Location location) {
        this.context = context;
        fourSquareListener = (FourSquareListener)context;
        resultList = new ArrayList<FoursquareModel>();
        this.location = location;
    }

    public void findPlaces(){
        new fourquare().execute(location);
    }

    private class fourquare extends AsyncTask<Location, Void, String> {

        String temp;


        @Override
        protected String doInBackground(Location... location) {

            if(location[0] != null) {
                // make Call to the url
                temp = makeCall("https://api.foursquare.com/v2/venues/search?client_id="
                        + CLIENT_ID
                        + "&client_secret="
                        + CLIENT_SECRET
                        + "&v=20130815&ll="
                        + String.valueOf(location[0].getLatitude())
                        + ","
                        + String.valueOf(location[0].getLongitude()));
                //Log.e("Link ---- > ", temp);
            }
            return "";
        }

        @Override
        protected void onPreExecute() {
            // we can start a progress bar here
        }

        @Override
        protected void onPostExecute(String result) {
            String name = null;
            if (temp == null) {
                // we have an error to the call
                // we can also stop the progress bar
                Log.d("foursquare","no places found");
                name = "No place found";
            } else {
                // all things went right

                // parseFoursquare venues search result
                resultList = (ArrayList<FoursquareModel>) parseFoursquare(temp);
                for(FoursquareModel f:resultList){
                    if(name == null){
                        name = f.getName();
                    }else {
                        name = name + "\n" + f.getName();
                    }
                }

            }
            fourSquareListener.placesFound(name);
        }
    }

    public static String makeCall(String url) {

        // string buffers the url
        StringBuffer buffer_string = new StringBuffer(url);
        String replyString = "";

        // instanciate an HttpClient
        HttpClient httpclient = HttpClientBuilder.create().build();        // instanciate an HttpGet

        HttpGet httpget = new HttpGet(buffer_string.toString());

        try {
            // get the responce of the httpclient execution of the url
            HttpResponse response = httpclient.execute(httpget);
            InputStream is = response.getEntity().getContent();

            // buffer input stream the result
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayBuffer baf = new ByteArrayBuffer(20);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            // the result as a string is ready for parsing
            replyString = new String(baf.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // trim the whitespaces
        return replyString.trim();
    }

    private static ArrayList<FoursquareModel> parseFoursquare(
            final String response) {

        ArrayList<FoursquareModel> temp = new ArrayList<FoursquareModel>();
        try {

            // make an jsonObject in order to parse the response
            JSONObject jsonObject = new JSONObject(response);

            // make an jsonObject in order to parse the response
            if (jsonObject.has("response")) {
                if (jsonObject.getJSONObject("response").has("venues")) {
                    JSONArray jsonArray = jsonObject.getJSONObject("response")
                            .getJSONArray("venues");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        FoursquareModel poi = new FoursquareModel();

                        try {
                            if (jsonArray.getJSONObject(i).has("name")) {
                                poi.setName(jsonArray.getJSONObject(i)
                                        .getString("name"));

                                // We will get only those locations which has
                                // address
                                if (jsonArray.getJSONObject(i).has("location")) {
                                    if (jsonArray.getJSONObject(i)
                                            .getJSONObject("location")
                                            .has("address")) {
                                        poi.setAddress(jsonArray
                                                .getJSONObject(i)
                                                .getJSONObject("location")
                                                .getString("address"));

                                        if (jsonArray.getJSONObject(i)
                                                .getJSONObject("location")
                                                .has("lat")) {
                                            poi.setLatitude(jsonArray
                                                    .getJSONObject(i)
                                                    .getJSONObject("location")
                                                    .getString("lat"));
                                        }
                                        if (jsonArray.getJSONObject(i)
                                                .getJSONObject("location")
                                                .has("lng")) {
                                            poi.setLongtitude(jsonArray
                                                    .getJSONObject(i)
                                                    .getJSONObject("location")
                                                    .getString("lng"));
                                        }

                                        if (jsonArray.getJSONObject(i)
                                                .getJSONObject("location")
                                                .has("city")) {
                                            poi.setCity(jsonArray
                                                    .getJSONObject(i)
                                                    .getJSONObject("location")
                                                    .getString("city"));
                                        }
                                        if (jsonArray.getJSONObject(i)
                                                .getJSONObject("location")
                                                .has("country")) {
                                            poi.setCountry(jsonArray
                                                    .getJSONObject(i)
                                                    .getJSONObject("location")
                                                    .getString("country"));
                                        }
                                        if (jsonArray.getJSONObject(i).has(
                                                "categories")) {
                                            if (jsonArray.getJSONObject(i)
                                                    .getJSONArray("categories")
                                                    .length() > 0) {
                                                if (jsonArray
                                                        .getJSONObject(i)
                                                        .getJSONArray(
                                                                "categories")
                                                        .getJSONObject(0)
                                                        .has("name")) {
                                                    poi.setCategory(jsonArray
                                                            .getJSONObject(i)
                                                            .getJSONArray(
                                                                    "categories")
                                                            .getJSONObject(0)
                                                            .getString("name"));
                                                }
                                                if (jsonArray
                                                        .getJSONObject(i)
                                                        .getJSONArray(
                                                                "categories")
                                                        .getJSONObject(0)
                                                        .has("id")) {
                                                    poi.setCategoryID(jsonArray
                                                            .getJSONObject(i)
                                                            .getJSONArray(
                                                                    "categories")
                                                            .getJSONObject(0)
                                                            .getString("id"));
                                                }
                                                if (jsonArray
                                                        .getJSONObject(i)
                                                        .getJSONArray(
                                                                "categories")
                                                        .getJSONObject(0)
                                                        .has("icon")) {

                                                    poi.setCategoryIcon(jsonArray
                                                            .getJSONObject(i)
                                                            .getJSONArray(
                                                                    "categories")
                                                            .getJSONObject(0)
                                                            .getJSONObject(
                                                                    "icon")
                                                            .getString("prefix")
                                                            + "bg_32.png");
                                                }
                                            }
                                        }
                                        temp.add(poi);

                                    }
                                }

                            }
                        } catch (Exception e) {

                        }

                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;

    }

}
