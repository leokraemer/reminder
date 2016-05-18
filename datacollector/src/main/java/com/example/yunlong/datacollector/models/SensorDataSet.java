package com.example.yunlong.datacollector.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

/**
 * Created by Yunlong on 4/22/2016.
 */
@ParseClassName("SensorDataSet")
public class SensorDataSet extends ParseObject {

    public SensorDataSet(){

    }

    public String getTitle() {
        return getString("title");
    }

    public void setTitle(String title) {
        put("title", title);
    }

    public String getUserName() {
        return getString("userName");
    }

    public void setUserName(String userName) {
        put("userName", userName);
    }

    public ParseUser getAuthor() {
        return getParseUser("author");
    }

    public void setAuthor(ParseUser user) {
        put("author", user);
    }

    public String getActivity() {
        return getString("activity");
    }

    public void setActivity(String activity) {
        put("activity", activity);
    }

    public String getWifiName() {
        return getString("wifiName");
    }

    public void setWifiName(String wifiName) {
        put("wifiName", wifiName);
    }
    public void setTime(String time){
        put("time",time);
    }
    public void setLocation(String location){
        put("location",location);
    }
    public void setGPS(String GPS){
        put("GPS",GPS);
    }
    public void setTemperature(float temperature){
        put("temperature",temperature);
    }
    public void setPressure(float pressure){
        put("pressure",pressure);
    }
    public void setLight(float light){
        put("light",light);
    }
    public void setHumidity(float humidity){
        put("humidity",humidity);
    }
    public void setLabel(String label){
        put("label",label);
    }

}
