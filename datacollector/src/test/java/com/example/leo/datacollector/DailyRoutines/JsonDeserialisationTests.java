package com.example.leo.datacollector.DailyRoutines;


import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Leo on 30.08.2017.
 */

public class JsonDeserialisationTests {
    @Test
    public void testDateDeserialisation() throws JSONException {
        String poi2 = "{" +
                "\"popularPlacesClusterIndex\": 2," +
                "\"POIs_center_latitude\": 47.69106255," +
                "\"POIs_center_longitude\": 9.186875704," +
                "\"POIs_max_latitude\": 47.69136404," +
                "\"POIs_min_latitude\": 47.68993432," +
                "\"POIs_max_longitude\": 9.18954601," +
                "\"POIs_min_longitude\": 9.18632309," +
                "\"POIs_visit_times\": 11," +
                "\"POIs_coverage_days\": 12," +
                "\"POIs_average_duration\": 5.564671717" +
                "}";
        String poi3 = "{ " +
                "\"popularPlacesClusterIndex\": 3," +
                "\"POIs_center_latitude\": 47.68156933," +
                "\"POIs_center_longitude\": 9.18949009," +
                "\"POIs_max_latitude\": 47.68254778," +
                "\"POIs_min_latitude\": 47.68105862," +
                "\"POIs_max_longitude\": 9.1905084," +
                "\"POIs_min_longitude\": 9.18807941," +
                "\"POIs_visit_times\": 18," +
                "\"POIs_coverage_days\": 12," +
                "\"POIs_average_duration\": 9.100478395" +
                "}";
        String json = "{" +
                "\"poi_start\": 2," +
                "\"leaving_time\": {" +
                "\"Format\": \"dd-MMM-uuuu HH:mm:ss\"," +
                "\"TimeZone\": \"\"," +
                "\"Year\": 2017," +
                "\"Month\": 4," +
                "\"Day\": 5," +
                "\"Hour\": 18," +
                "\"Minute\": 32," +
                "\"Second\": 13," +
                "\"SystemTimeZone\": \"Europe\\/Berlin\"" +
                "}," +
                "\"poi_end\": 3," +
                "\"arriving_time\": {" +
                "\"Format\": \"dd-MMM-uuuu HH:mm:ss\"," +
                "\"TimeZone\": \"\"," +
                "\"Year\": 2017," +
                "\"Month\": 4," +
                "\"Day\": 5," +
                "\"Hour\": 23," +
                "\"Minute\": 45," +
                "\"Second\": 58," +
                "\"SystemTimeZone\": \"Europe\\/Berlin\"" +
                "}," +
                "\"duration\": 10.26833333" +
                "}";
        Map<Integer, POI> poiMap = new HashMap<>();
        poiMap.put(2, new POI(new JSONObject(poi2)));
        poiMap.put(3, new POI(new JSONObject(poi3)));
        GlobalPOIsRoutinePattern routine = new GlobalPOIsRoutinePattern(new JSONObject(json), poiMap);
        Assert.assertEquals(2, routine.start.popularPlacesClusterIndex);
        Assert.assertEquals(3, routine.dest.popularPlacesClusterIndex);
        Assert.assertEquals(10.26833333, routine.duration, 0.001);
        Assert.assertEquals(23, routine.arrivingTime.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(18, routine.leavingTime.get(Calendar.HOUR_OF_DAY));
    }
}
