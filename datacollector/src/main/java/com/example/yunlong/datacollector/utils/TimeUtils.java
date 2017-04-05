package com.example.yunlong.datacollector.utils;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Yunlong on 4/24/2016.
 */
public class TimeUtils {

    public static String getCurrentTimeStr(){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");
        String currentTimeStr = df.format(c.getTime());
        return currentTimeStr;
    }

    public static Date getDateFromString(String timeStr){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");
        Date d = new Date();
        try {
            d = sdf.parse(timeStr);
        } catch (ParseException ex) {
            Log.d("TimeUtils",ex.getMessage());
        }
        return d;
    }
}
