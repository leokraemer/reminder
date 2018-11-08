package de.leo.smartTrigger.datacollector.datacollection.sensors;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;

import java.util.HashMap;

/**
 * Created by Yunlong on 4/4/2017.
 */

public class GooglePlacesCaller implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks{

    private static String TAG = "GooglePlacesCaller";
    private Context context;
    private GoogleApiClient mGoogleApiClient;
    private GooglePlacesListener googlePlacesListener;

    public GooglePlacesCaller(Context context) {
        this.context = context;
        googlePlacesListener = (GooglePlacesListener)context;
        mGoogleApiClient = new GoogleApiClient
                .Builder(context)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                //.enableAutoManage((AppCompatActivity)context, 1, this)
                .build();
        connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG,"onConnectionFailed");
    }

    public void connect(){
        mGoogleApiClient.connect();
    }
    public void disconnect(){
        //mGoogleApiClient.stopAutoManage((AppCompatActivity)context);
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void getCurrentPlace(){
        try {
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                    HashMap<String,Float> places = new HashMap<String, Float>();
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        Log.i(TAG, String.format("Place '%s' has likelihood: %g",
                                placeLikelihood.getPlace().getName(),
                                placeLikelihood.getLikelihood()));
                        places.put( placeLikelihood.getPlace().getName().toString(),placeLikelihood.getLikelihood());
                    }
                    googlePlacesListener.onReceivedPlaces(places);
                    likelyPlaces.release();
                }
            });
        }catch (SecurityException e){
            Log.e(TAG,e.getMessage());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG,"onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG,"onConnectionSuspended");
    }
}
