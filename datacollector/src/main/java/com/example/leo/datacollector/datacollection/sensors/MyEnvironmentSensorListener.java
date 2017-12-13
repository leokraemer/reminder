package com.example.leo.datacollector.datacollection.sensors;

/**
 * Created by Yunlong on 4/23/2016.
 */
public interface MyEnvironmentSensorListener {
    void environmentSensorDataChanged(float light,float temperature,float pressure,float humidity);
    void stopEnvironmentSensor();
}
