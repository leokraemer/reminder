package de.leo.smartTrigger.datacollector.datacollection.sensors;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * Created by Yunlong on 3/2/2016.
 */
public class MyLocation {
    MyLocationListener myLocationListener;
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 5; //

    private static final long TWO_MINUTES = TimeUnit.MINUTES.toMillis(2);

    public Location currentBestLocation = null;
    private LocationManager locationManager;
    private LocationListener locationListener;

    public MyLocation(Context context) {
        myLocationListener = (MyLocationListener) context;
        initLocation(context);
    }

    private void initLocation(final Context context) {
        // Acquire a reference to the system MyLocation Manager
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to myLocation updates
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
                if (isBetterLocation(location, currentBestLocation)) {
                    currentBestLocation = location;
                    myLocationListener.locationChanged(location);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

        };

        try {
            Location lastKnownLocation_byNetwork = locationManager.getLastKnownLocation
                    (LocationManager.GPS_PROVIDER);
            if (lastKnownLocation_byNetwork != null) {
                currentBestLocation = lastKnownLocation_byNetwork;
                myLocationListener.locationChanged(currentBestLocation);
            }
        } catch (SecurityException e) {
            Log.e("MyLocation", "SecurityException");
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
        } catch (SecurityException e) {
            Log.e("MyLocation", "SecurityException");
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
        } catch (SecurityException e) {
            Log.e("MyLocation", "SecurityException");
        }

    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new myLocation is always better than no myLocation
            return true;
        }

        // Check whether the new myLocation fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current myLocation, use the new myLocation
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new myLocation is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new myLocation fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new myLocation are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine myLocation quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else return isNewer && !isSignificantlyLessAccurate && isFromSameProvider;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public void stopLocationUpdate() {
        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException e) {
            Log.e("MyLocation", "SecurityException");
        }
    }


}
