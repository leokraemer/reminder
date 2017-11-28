package com.example.leo.datacollector.sensors;

/**
 * Created by Yunlong on 8/10/2017.
 */

public interface WeatherCallerListener {
    void onReceivedWeather(String condition,float temperature);
}
