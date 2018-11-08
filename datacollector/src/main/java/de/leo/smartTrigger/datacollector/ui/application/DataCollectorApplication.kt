package de.leo.smartTrigger.datacollector.ui.application

import android.app.Application
import android.net.wifi.p2p.WifiP2pManager
import com.jakewharton.threetenabp.AndroidThreeTen


/**
 * Created by Yunlong on 4/22/2016.
 */
class DataCollectorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }

    companion object {

        var WIFI_NAME_ENABLED = true
        var LOCATION_ENABLED = true
        var LOCATION_NAME_ENABLED = false
        var ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED = false
        var ACTIVITY_ENABLED = true
        var ENVIRONMENT_SENSOR_ENABLED = false
        var GOOGLE_FITNESS_ENABLED = false
        var SCREEN_ON_ENABLED = false
        var AMBIENT_SOUND_ENABLED = false
        var WEATHER_ENABLED = false
    }
}

