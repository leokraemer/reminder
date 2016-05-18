package com.example.yunlong.datacollector.sensors;

/**
 * Created by Yunlong on 3/2/2016.
 */
public interface MyMotionListener {
    void motionDataChanged(float[] accData,float[] rotData);
    void stopMotionSensor();
}
