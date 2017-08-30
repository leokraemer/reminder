package com.example.yunlong.datacollector.DailyRoutines;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.yunlong.datacollector.R;

import java.text.SimpleDateFormat;
import java.util.List;

import static com.example.yunlong.datacollector.DailyRoutines.MapsActivity.STEP_LENGTH;

/**
 * Created by Leo on 25.08.2017.
 */

class MapExtraInfoListAdapter extends ArrayAdapter<Routine> {

    public MapExtraInfoListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Routine> routines) {
        super(context, resource, routines);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Routine routine = getItem(position);
        if (routine.to != null) {
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.routinepathinfo, parent, false);
            }
            TextView routineTV = (TextView) convertView.findViewById(R.id.routine);
            TextView start = (TextView) convertView.findViewById(R.id.start);
            TextView dest = (TextView) convertView.findViewById(R.id.dest);
            TextView time = (TextView) convertView.findViewById(R.id.time);
            TextView duration = (TextView) convertView.findViewById(R.id.duration);
            TextView steps = (TextView) convertView.findViewById(R.id.steps);
            TextView distance = (TextView) convertView.findViewById(R.id.distance);
            TextView visits = (TextView) convertView.findViewById(R.id.visits);
            routineTV.setText("Routine " + routine.from.popularPlacesClusterIndex + " -> " + routine.to.popularPlacesClusterIndex);
            start.setText(routine.getFromText());
            dest.setText(routine.getToText());
            distance.setText(routine.getDistanceText());
            steps.setText("0 / " + Math.round(routine.getDistance() / STEP_LENGTH));
            visits.setText(routine.times + " / 12");
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
            time.setText(sdf.format(routine.average_leaving_time.getTimeInMillis()) + " Uhr");
            long diff = routine.average_arriving_time.getTimeInMillis() - routine.average_leaving_time.getTimeInMillis();
            SimpleDateFormat stf = new SimpleDateFormat("H:mm");
            duration.setText(stf.format(diff) + " h");
        } else {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.poiinfo, parent, false);
                TextView routineTV = (TextView) convertView.findViewById(R.id.routine);
                TextView start = (TextView) convertView.findViewById(R.id.start);
                TextView duration = (TextView) convertView.findViewById(R.id.duration);
                TextView visits = (TextView) convertView.findViewById(R.id.visits);
                routineTV.setText("Ort " + routine.from.popularPlacesClusterIndex);
                start.setText(routine.from.locationText);
                visits.setText(routine.times + " / 12");
                int hours = ((int) Math.floor(routine.from.POIs_average_duration));
                long minutes = Math.round((routine.from.POIs_average_duration - hours) * 60);
                duration.setText(hours + ":" + (minutes < 10 ? "0" : "") + minutes + " h");
            }
        }
        // Return the completed view to render on screen
        return convertView;

    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        Routine routine = getItem(position);
        if (routine.to == null)
            return 1;
        return 0;
    }
}
