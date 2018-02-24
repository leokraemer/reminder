package de.leo.fingerprint.datacollector.DailyRoutines;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by Leo on 30.08.2017.
 */

public class GlobalPOIsRoutinePattern {
    public POI start;
    public POI dest;
    public Calendar leavingTime;
    public Calendar arrivingTime;
    public double duration;

    public GlobalPOIsRoutinePattern(JSONObject jsonObject, Map<Integer, POI> pois) throws JSONException {
        start = pois.get(jsonObject.getInt("poi_start"));
        dest = pois.get(jsonObject.getInt("poi_end"));
        leavingTime = getDateTime(jsonObject.getJSONObject("leaving_time"));
        arrivingTime = getDateTime(jsonObject.getJSONObject("arriving_time"));
        duration = jsonObject.getDouble("duration");
    }

    private Calendar getDateTime(JSONObject time) throws JSONException {
        Calendar calendar = new GregorianCalendar();
        calendar.set(
                time.getInt("Year"),
                time.getInt("Month"),
                time.getInt("Day"),
                time.getInt("Hour"),
                time.getInt("Minute"),
                time.getInt("Second"));
        calendar.setTimeZone(TimeZone.getTimeZone(time.getString("SystemTimeZone")));
        return calendar;
    }

}
