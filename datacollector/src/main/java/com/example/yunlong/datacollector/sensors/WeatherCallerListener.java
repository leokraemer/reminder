package com.example.yunlong.datacollector.sensors;

import com.example.yunlong.datacollector.models.Weather;

/**
 * Created by Yunlong on 8/10/2017.
 */

public interface WeatherCallerListener {
    void onReceivedWeather(String condition,float temperature);
}
