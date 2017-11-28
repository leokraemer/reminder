package com.example.leo.datacollector.DailyRoutines;

import org.json.JSONArray;

/**
 * Created by Leo on 24.08.2017.
 */

public interface IRoutineProvider {
    public JSONArray getGlobalPOIs();
    public JSONArray getGlobalPOI_routine_patterns();
    public JSONArray getGlobalPOI_visits();
}
