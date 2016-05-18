package com.example.yunlong.datacollector.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Yunlong on 4/24/2016.
 */
public class TimeUtils {

    public static String getCurrentTimeStr(){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String currentDateStr = df.format(c.getTime());
        df = new SimpleDateFormat("HH-mm-ss");
        String currentTimeStr = df.format(c.getTime());
        return currentDateStr+" "+currentTimeStr;
    }
}
