package com.example.leo.datacollector.sensors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.example.leo.datacollector.models.Location;
import com.example.leo.datacollector.models.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Yunlong on 8/8/2017.
 */

public class WeatherCaller {
    private static final String city = "Konstanz";
    private static final String TAG = "WeatherCaller";
    private static String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?q=";
    private static String IMG_URL = "http://openweathermap.org/img/w/";
    private static String KEY = "ed45a865440d1dee638c7a55a115aa62";
    private Context context;
    private WeatherCallerListener weatherCallerListener;

    public WeatherCaller(Context context) {
        this.context = context;
        weatherCallerListener = (WeatherCallerListener) context;
        getCurrentWeather();
    }

    public void getCurrentWeather(){
        new JSONWeatherTask().execute(new String[]{city});
    }

    private class JSONWeatherTask extends AsyncTask<String, Void, Weather> {

        @Override
        protected Weather doInBackground(String... params) {
            Weather weather = new Weather();
            String data = ( (new WeatherHttpClient()).getWeatherData(params[0]));

            try {
                if(data != null) {
                    JSONWeatherParser jsonWeatherParser = new JSONWeatherParser();
                    weather = jsonWeatherParser.getWeather(data);

                    // Let's retrieve the icon
                    weather.iconData = ((new WeatherHttpClient()).getImage(weather.currentCondition.getIcon()));
                }else {
                    weather = null;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return weather;

        }

        @Override
        protected void onPostExecute(Weather weather) {
            super.onPostExecute(weather);

            if (weather.iconData != null && weather.iconData.length > 0) {
                Bitmap img = BitmapFactory.decodeByteArray(weather.iconData, 0, weather.iconData.length);
                //imgView.setImageBitmap(img);
            }

            weatherCallerListener.onReceivedWeather(weather.currentCondition.getCondition(),Math.round((weather.temperature.getTemp() - 273.15)));

            Log.i(TAG,weather.currentCondition.getCondition() + "(" + weather.currentCondition.getDescr() + ")");
            Log.i(TAG,Math.round((weather.temperature.getTemp() - 273.15)) + "C");

/*            cityText.setText(weather.location.getCity() + "," + weather.location.getCountry());
            condDescr.setText(weather.currentCondition.getCondition() + "(" + weather.currentCondition.getDescr() + ")");
            temp.setText("" + Math.round((weather.temperature.getTemp() - 273.15)) + "�C");
            hum.setText("" + weather.currentCondition.getHumidity() + "%");
            press.setText("" + weather.currentCondition.getPressure() + " hPa");
            windSpeed.setText("" + weather.wind.getSpeed() + " mps");
            windDeg.setText("" + weather.wind.getDeg() + "�");*/

        }

    }

    class WeatherHttpClient {

        String getWeatherData(String location) {
            HttpURLConnection con = null;
            InputStream is = null;

            try {
                con = (HttpURLConnection) (new URL(BASE_URL + location + "&appid=" + KEY)).openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.setDoOutput(true);
                con.connect();

                // Let's read the response
                StringBuffer buffer = new StringBuffer();
                is = con.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = br.readLine()) != null)
                    buffer.append(line + "\r\n");

                is.close();
                con.disconnect();
                return buffer.toString();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (Throwable t) {
                }
                try {
                    con.disconnect();
                } catch (Throwable t) {
                }
            }

            return null;

        }

        public byte[] getImage(String code) {
            HttpURLConnection con = null;
            InputStream is = null;
            try {
                con = (HttpURLConnection) (new URL(IMG_URL + code)).openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.setDoOutput(true);
                con.connect();

                // Let's read the response
                is = con.getInputStream();
                byte[] buffer = new byte[1024];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                while (is.read(buffer) != -1)
                    baos.write(buffer);

                return baos.toByteArray();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (Throwable t) {
                }
                try {
                    con.disconnect();
                } catch (Throwable t) {
                }
            }

            return null;

        }

    }

    class JSONWeatherParser {

        public Weather getWeather(String data) throws JSONException  {
            Weather weather = new Weather();

            // We create out JSONObject from the data
            JSONObject jObj = new JSONObject(data);

            // We start extracting the info
            Location loc = new Location();

            JSONObject coordObj = getObject("coord", jObj);
            loc.setLatitude(getFloat("lat", coordObj));
            loc.setLongitude(getFloat("lon", coordObj));

            JSONObject sysObj = getObject("sys", jObj);
            loc.setCountry(getString("country", sysObj));
            loc.setSunrise(getInt("sunrise", sysObj));
            loc.setSunset(getInt("sunset", sysObj));
            loc.setCity(getString("name", jObj));
            weather.location = loc;

            // We get weather info (This is an array)
            JSONArray jArr = jObj.getJSONArray("weather");

            // We use only the first value
            JSONObject JSONWeather = jArr.getJSONObject(0);
            weather.currentCondition.setWeatherId(getInt("id", JSONWeather));
            weather.currentCondition.setDescr(getString("description", JSONWeather));
            weather.currentCondition.setCondition(getString("main", JSONWeather));
            weather.currentCondition.setIcon(getString("icon", JSONWeather));

            JSONObject mainObj = getObject("main", jObj);
            weather.currentCondition.setHumidity(getInt("humidity", mainObj));
            weather.currentCondition.setPressure(getInt("pressure", mainObj));
            weather.temperature.setMaxTemp(getFloat("temp_max", mainObj));
            weather.temperature.setMinTemp(getFloat("temp_min", mainObj));
            weather.temperature.setTemp(getFloat("temp", mainObj));

            // Wind
            JSONObject wObj = getObject("wind", jObj);
            weather.wind.setSpeed(getFloat("speed", wObj));
            weather.wind.setDeg(getFloat("deg", wObj));

            // Clouds
            JSONObject cObj = getObject("clouds", jObj);
            weather.clouds.setPerc(getInt("all", cObj));

            // We download the icon to show


            return weather;
        }


        private JSONObject getObject(String tagName, JSONObject jObj)  throws JSONException {
            JSONObject subObj = jObj.getJSONObject(tagName);
            return subObj;
        }

        private String getString(String tagName, JSONObject jObj) throws JSONException {
            return jObj.getString(tagName);
        }

        private float  getFloat(String tagName, JSONObject jObj) throws JSONException {
            return (float) jObj.getDouble(tagName);
        }

        private int  getInt(String tagName, JSONObject jObj) throws JSONException {
            return jObj.getInt(tagName);
        }

    }
}
