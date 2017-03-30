package com.example.yunlong.datacollector.realm;

import io.realm.RealmObject;

/**
 * Created by Yunlong on 2/23/2017.
 */

public class StateRealm extends RealmObject {

    public String userNameStr;
    public String placeStr;
    public String WifiStr;
    public double latitude;
    public double longitude;
    public String activityStr;
    public String timeStr;

    public StateRealm() {
    }


    public String getUserNameStr() {
        return userNameStr;
    }

    public void setUserNameStr(String userNameStr) {
        this.userNameStr = userNameStr;
    }

    public String getPlaceStr() {
        return placeStr;
    }

    public void setPlaceStr(String placeStr) {
        this.placeStr = placeStr;
    }

    public String getWifiStr() {
        return WifiStr;
    }

    public void setWifiStr(String wifiStr) {
        WifiStr = wifiStr;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getActivityStr() {
        return activityStr;
    }

    public void setActivityStr(String activityStr) {
        this.activityStr = activityStr;
    }

    public String getTimeStr() {
        return timeStr;
    }

    public void setTimeStr(String timeStr) {
        this.timeStr = timeStr;
    }
}
