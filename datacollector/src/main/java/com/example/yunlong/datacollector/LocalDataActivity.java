package com.example.yunlong.datacollector;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.example.yunlong.datacollector.models.SensorDataSet;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.ui.ParseLoginBuilder;

import java.util.List;

public class LocalDataActivity extends AppCompatActivity {

    private static final String TAG = "LocalDataActivity";
    private static final int LOGIN_ACTIVITY_CODE = 100;
    private ListView dataListView;
    private ParseQueryAdapter<SensorDataSet> dataListAdapter;
    private LayoutInflater inflater;
    private TextView loggedInInfoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_data);

        // Set up the Parse query to use in the adapter
        ParseQueryAdapter.QueryFactory<SensorDataSet> factory = new ParseQueryAdapter.QueryFactory<SensorDataSet>() {
            public ParseQuery<SensorDataSet> create() {
                ParseQuery<SensorDataSet> query = SensorDataSet.getQuery();
                query.orderByDescending("createdAt");
                query.whereGreaterThan("time","2017-08-14:00-00-00");
                query.fromLocalDatastore();
                //query.setLimit(5);
                return query;
            }
        };
        // Set up the adapter
        inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //dataListAdapter = new DataListAdapter(this, "SensorDataSet");
        dataListAdapter = new DataListAdapter(this, factory);
        dataListView = (ListView) findViewById(R.id.data_list_view);
        dataListView.setAdapter(dataListAdapter);

        loggedInInfoView = (TextView) findViewById(R.id.loggedin_info);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we have a real user
        if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            updateLoggedInInfo();
            loadFromParse();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // An OK result means the pinned dataset changed or
        // log in was successful
        if (resultCode == RESULT_OK) {
            if (requestCode == LOGIN_ACTIVITY_CODE) {
                loadFromParse();
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_local_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            // Log out the current user
            ParseUser.logOut();
            // Create a new anonymous user
            ParseAnonymousUtils.logIn(null);
            // Update the logged in label info
            updateLoggedInInfo();
            // Clear the view
            dataListAdapter.clear();
            SensorDataSet.unpinAllInBackground();

        }

        if (item.getItemId() == R.id.action_login) {
            ParseLoginBuilder builder = new ParseLoginBuilder(this);
            builder.setAppLogo(R.drawable.com_parse_ui_app_logo);
            startActivityForResult(builder.build(), LOGIN_ACTIVITY_CODE);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean realUser = !ParseAnonymousUtils.isLinked(ParseUser
                .getCurrentUser());
        menu.findItem(R.id.action_login).setVisible(!realUser);
        menu.findItem(R.id.action_logout).setVisible(realUser);
        return true;
    }

    private void updateLoggedInInfo() {
        if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            ParseUser currentUser = ParseUser.getCurrentUser();
            loggedInInfoView.setText(getString(R.string.logged_in,
                    currentUser.getString("name")));
        } else {
            loggedInInfoView.setText(getString(R.string.not_logged_in));
        }
    }

    private void loadFromParse() {
        Log.i(TAG,"loadFromParse");
        ParseQuery<SensorDataSet> query = SensorDataSet.getQuery();
        query.whereGreaterThan("time","2017-08-14:00-00-00");
        //query.setLimit(5);
        query.findInBackground(new FindCallback<SensorDataSet>() {
            public void done(final List<SensorDataSet> dataSet, ParseException e) {
                if (e == null) {
                    SensorDataSet.unpinAllInBackground(new DeleteCallback() { // for local data, I have to unpinAll then pinAll to make it work. for server data, it is unnecessary.
                        @Override
                        public void done(ParseException e) {
                            SensorDataSet.pinAllInBackground(dataSet, new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    dataListAdapter.loadObjects();
                                    dataListAdapter.notifyDataSetChanged();
                                    Log.i(TAG,"Result Size: "+dataSet.size());
                                }
                            });
                        }
                    });


                } else {
                    Log.i(TAG,"loadFromParse: Error finding pinned data: " + e.getMessage());
                }
            }
        });
    }

    private class DataListAdapter extends ParseQueryAdapter<SensorDataSet> {

        public DataListAdapter(Context context, String className) {
            super(context, className);
        }

        public DataListAdapter(Context context,
                               ParseQueryAdapter.QueryFactory<SensorDataSet> queryFactory) {
            super(context, queryFactory);
        }

        @Override
        public View getItemView(SensorDataSet dataSet, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view == null) {
                view = inflater.inflate(R.layout.list_item_data, parent, false);
                holder = new ViewHolder();
                holder.dataTime = (TextView) view.findViewById(R.id.data_time);
                holder.dataLocation = (TextView) view.findViewById(R.id.data_location);
                holder.dataSteps = (TextView) view.findViewById(R.id.data_steps);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            holder.dataTime.setText(dataSet.getTime());
            holder.dataLocation.setText(dataSet.getLocation());
            holder.dataSteps.setText(dataSet.getSteps()+"");
            return view;
        }
    }

    private static class ViewHolder {
        TextView dataTime;
        TextView dataLocation;
        TextView dataSteps;
    }

    public void onClickUpdate(View v){
        loadFromParse();
    }
}
