package de.leo.fingerprint.datacollector.ui.DailyRoutines;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.util.Log;

import de.leo.fingerprint.datacollector.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Created by Leo on 24.08.2017.
 */

public class RoutineProvider implements IRoutineProvider {

    private final Context context;

    public RoutineProvider(Context context) {
        this.context= context;
    }

    @Override
    public JSONArray getGlobalPOIs() {
        try {
            return (JSONArray) ((JSONArray) new JSONObject(readRawResourceString(R.raw.globalpois)).get("GlobalPOIs")).get(0);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JSONArray getGlobalPOI_routine_patterns() {
        try {
            return (JSONArray) ((JSONArray) new JSONObject(readRawResourceString(R.raw.globalpois_routine_patterns)).get("GlobalPOIs_routine_patterns")).get(0);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JSONArray getGlobalPOI_visits() {
        try {
            return (JSONArray) ((JSONArray) new JSONObject(readRawResourceString(R.raw.globalpois_visit)).get("GlobalPOIs_visit")).get(0);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    private String readRawResourceString(int resId) {
        try {
            Resources res = context.getResources();
            InputStream in_s = res.openRawResource(resId);
            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            return(new String(b));
        } catch (Exception e) {
            Log.d("GloblaPOIs_visit.json",e.toString());
            return null;
        }
    }


}
