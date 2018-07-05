package de.leo.fingerprint.datacollector.datacollection.sensors;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import de.leo.fingerprint.datacollector.datacollection.models.Location;
import de.leo.fingerprint.datacollector.datacollection.models.Weather;

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

    public void getCurrentWeather() {
        new JSONWeatherTask().execute(new String[]{city});
    }

    private class JSONWeatherTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            return (new WeatherHttpClient()).getWeatherData(params[0]);
        }

        @Override
        protected void onPostExecute(String weatherStr) {
            super.onPostExecute(weatherStr);
            weatherCallerListener.onReceivedWeather(weatherStr);
            Log.d("Weather caller", "Recieved " + weatherStr);
        }
    }

    class WeatherHttpClient {

        String getWeatherData(String location) {
            HttpURLConnection con = null;
            InputStream is = null;

            try {
                con = (HttpURLConnection) (new URL(BASE_URL + location +
                        "&lang=de" + "&appid=" + KEY))
                        .openConnection();
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

    static class JSONWeatherParser {

        //throw when clearly malformed
        public static Weather getWeather(String data) throws JSONException {
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
            weather.currentCondition.weatherId = getInt("id", JSONWeather);
            weather.currentCondition.descr = getString("description", JSONWeather);
            weather.currentCondition.condition = getString("main", JSONWeather);
            weather.currentCondition.icon = getString("icon", JSONWeather);

            JSONObject mainObj = getObject("main", jObj);
            weather.currentCondition.humidity = (getInt("humidity", mainObj));
            weather.currentCondition.pressure = getInt("pressure", mainObj);
            weather.temperature.maxTemp = getFloat("temp_max", mainObj);
            weather.temperature.minTemp = getFloat("temp_min", mainObj);
            weather.temperature.temp = getFloat("temp", mainObj);

            // Wind
            JSONObject wObj = getObject("wind", jObj);
            weather.wind.speed = getFloat("speed", wObj);
            weather.wind.deg = getFloat("deg", wObj);

            // Clouds
            JSONObject cObj = getObject("clouds", jObj);
            weather.clouds.perc = getInt("all", cObj);


            return weather;
        }


        private static JSONObject getObject(String tagName, JSONObject jObj) {
            try {
                JSONObject subObj = jObj.getJSONObject(tagName);
                return subObj;
            } catch (JSONException e) {
                Log.e("JSON Exception", "weather deserialisation tag " + tagName);
                return null;
            }
        }

        private static String getString(String tagName, JSONObject jObj) {
            try {
                return jObj.getString(tagName);
            } catch (JSONException e) {
                Log.e("JSON Exception", "weather deserialisation tag " + tagName);
                return null;
            }
        }

        private static float getFloat(String tagName, JSONObject jObj) {
            try {
                return (float) jObj.getDouble(tagName);
            } catch (JSONException e) {
                Log.e("JSON Exception", "weather deserialisation tag " + tagName);
                return Float.NaN;
            }
        }

        private static int getInt(String tagName, JSONObject jObj) {
            try {
                return jObj.getInt(tagName);
            } catch (JSONException e) {
                Log.e("JSON Exception", "weather deserialisation tag " + tagName);
                return -1;
            }
        }
    }

    public static Weather fromJSON(String weatherStr) {
        Weather weather = null;
        if (weatherStr != null) {
            try {
                weather = JSONWeatherParser.getWeather(weatherStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return weather;
    }
}
