package com.example.leo.datacollector.datacollection

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.Display

import com.example.leo.datacollector.R
import com.example.leo.datacollector.activityDetection.ActivityRecognizer
import com.example.leo.datacollector.application.DataCollectorApplication
import com.example.leo.datacollector.datacollection.sensors.AmbientSound
import com.example.leo.datacollector.datacollection.sensors.AmbientSoundListener
import com.example.leo.datacollector.datacollection.sensors.FourSquareListener
import com.example.leo.datacollector.datacollection.sensors.GoogleFitness
import com.example.leo.datacollector.datacollection.sensors.GoogleFitnessListener
import com.example.leo.datacollector.datacollection.sensors.GooglePlacesCaller
import com.example.leo.datacollector.datacollection.sensors.GooglePlacesListener
import com.example.leo.datacollector.datacollection.sensors.MyActivity
import com.example.leo.datacollector.datacollection.sensors.MyActivityListener
import com.example.leo.datacollector.datacollection.sensors.MyEnvironmentSensor
import com.example.leo.datacollector.datacollection.sensors.MyLocation
import com.example.leo.datacollector.datacollection.sensors.MyLocationListener
import com.example.leo.datacollector.datacollection.sensors.MotionSensors
import com.example.leo.datacollector.datacollection.sensors.WeatherCaller
import com.example.leo.datacollector.datacollection.sensors.WeatherCallerListener
import com.example.leo.datacollector.models.SensorDataSet
import com.google.android.gms.location.DetectedActivity

import java.util.HashMap
import java.util.concurrent.TimeUnit

import com.example.leo.datacollector.activityRecording.RECORDING_ID
import com.example.leo.datacollector.database.*
import com.example.leo.datacollector.jitai.manage.Jitai
import com.example.leo.datacollector.jitai.manage.JitaiManagingActivity
import com.example.leo.datacollector.utils.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

class DataCollectorService : Service(),
                             MyLocationListener,
                             FourSquareListener,
                             MyActivityListener,
                             GooglePlacesListener,
                             AmbientSoundListener,
                             GoogleFitnessListener,
                             WeatherCallerListener {

    companion object {

        val TAG = "DataCollectorService"

        val notificationID = 1001
        private val updatesPerSecond = 5

        private val useGooglePlaces = true
    }

    private val numberOfSamples: Int = TimeUnit.MINUTES.toSeconds(5).toInt() *
            updatesPerSecond

    private val sample = 0

    private var weatherUpdateCnt = 0

    internal lateinit var myMotion: MotionSensors
    internal lateinit var myLocation: MyLocation
    internal lateinit var myActivity: MyActivity
    //internal lateinit var foursquareCaller: FoursquareCaller
    internal lateinit var googlePlacesCaller: GooglePlacesCaller
    internal lateinit var currentLocation: Location
    internal lateinit var myEnvironmentSensor: MyEnvironmentSensor
    internal var ambientSound: AmbientSound? = null
    internal lateinit var googleFitness: GoogleFitness
    internal lateinit var weatherCaller: WeatherCaller

    internal var currentLatitude: Double = 0.0
    internal var currentLongitude: Double = 0.0
    internal var currentAccurate: Double = 0.0
    internal var currentAmbientSound: Double = 0.0
    internal var ifLocationChanged: Boolean = false
    internal var isRunning: Boolean = false
    internal var isCurrentScreenOn: Boolean = false
    internal lateinit var currentWifiName: String
    internal lateinit var currentPlaceName: String
    internal lateinit var currentLabel: String
    private var currentActivity = DetectedActivity(DetectedActivity.UNKNOWN, 0)
    private var currentWeatherId: Long = -1
    internal var currentSteps: Long = 0
    private var userName: String? = null
    private var db: JitaiDatabase? = null
    private var recordingId = -1
    private var activityRecognizer: ActivityRecognizer? = null

    private lateinit var pm: PowerManager
    private lateinit var dm: DisplayManager

    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            currentLabel = intent.getStringExtra("label")
            //Log.d("receiver", "Got message: " + message);
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        dm = applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        db = JitaiDatabase.getInstance(applicationContext)
        currentWeatherId = db!!.getLatestWeather()!!.id.toLong()
        Log.d(TAG, "service started")
        if (!this.isRunning) {
            startDataCollection()
        }
        if (intent != null && intent.action != null)
            when (intent.action) {
                UPDATE_JITAI -> {
                    activityRecognizer = ActivityRecognizer(baseContext)
                } START_RECORDING -> {
                    recordingId = intent.getIntExtra(RECORDING_ID, -1)
                    val answer = Intent(START_RECORDING)
                    answer.putExtra(RECORDING_ID, recordingId)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(answer)
                }
                STOP_RECORDING -> {
                    recordingId = -1
                    val answer = Intent(STOP_RECORDING)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(answer)
                }
                IS_RECORDING -> {
                    val answer = Intent(IS_RECORDING)
                    answer.putExtra(RECORDING_ID, recordingId)
                    answer.putExtra(IS_RECORDING, isRunning)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(answer)
                }
                USER_CLASSIFICATION_NOW -> {
                    val jitaiId = intent.getIntExtra(JITAI_ID, -1)
                    if (jitaiId > -1) {
                        db!!.enterJitaiEvent(jitaiId,
                                             System.currentTimeMillis(),
                                             Jitai.NOW,
                                             uploadDataSet(System.currentTimeMillis()))
                        Log.d(TAG, "entered now event for $jitaiId")
                        toast("Aktivität aufgezeichnet")
                    } else
                        baseContext.startActivity(baseContext.intentFor<JitaiManagingActivity>()
                                                          .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        return Service.START_STICKY
    }

    private fun startDataCollection() {
        activityRecognizer = ActivityRecognizer(baseContext)
        this.isRunning = true
        if (DataCollectorApplication.LOCATION_ENABLED) {
            myLocation = MyLocation(this)
            //foursquareCaller = FoursquareCaller(this, currentLocation)
            googlePlacesCaller = GooglePlacesCaller(this)
            currentPlaceName = "null"
            ifLocationChanged = true
        }
        if (DataCollectorApplication.ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED) {
            myMotion = MotionSensors(this)
        }
        if (DataCollectorApplication.ACTIVITY_ENABLED) {
            myActivity = MyActivity(this)
            currentActivity = DetectedActivity(DetectedActivity.UNKNOWN, 0)
        }
        if (DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
            myEnvironmentSensor = MyEnvironmentSensor(this)
        }
        if (DataCollectorApplication.GOOGLE_FITNESS_ENABLED) {
            googleFitness = GoogleFitness(this)
            currentSteps = 0
        }
        if (DataCollectorApplication.AMBIENT_SOUND_ENABLED) {
            ambientSound = AmbientSound(this)
            currentAmbientSound = 0.0
        }
        if (DataCollectorApplication.WEATHER_ENABLED) {
            weatherCaller = WeatherCaller(this)
        }
        currentWifiName = "null"
        currentLabel = "null"

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                                                                 IntentFilter(
                                                                         DataCollectorApplication.BROADCAST_EVENT))

        startScheduledUpdate()
        startNotification()
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = false
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        userName = sharedPreferences.getString("fingerprint_user_name", "userName")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        cancelNotification()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)

        if (DataCollectorApplication.ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED) {
            myMotion.stopMotionSensor()
        }
        if (DataCollectorApplication.LOCATION_ENABLED) {
            stopLocationUpdate()
        }
        if (DataCollectorApplication.ACTIVITY_ENABLED) {
            stopActivityDetection()
        }
        if (DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
            myEnvironmentSensor.stopEnvironmentSensor()
        }
        if (DataCollectorApplication.GOOGLE_FITNESS_ENABLED) {
            stopGoogleFitness()
        }
        if (ambientSound != null) {
            ambientSound!!.stop()
            ambientSound = null
        }
    }

    private fun startScheduledUpdate() {
        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                val currentTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()
                //we assume that the operation finishes before it must be called again
                handler.postDelayed(this, 5000)
                if (isRunning) {
                    if (weatherUpdateCnt == 1) {
                        weatherCaller.getCurrentWeather()
                    } else if (weatherUpdateCnt == 3600 * updatesPerSecond) {//update
                        // per hour
                        weatherUpdateCnt = 0
                    }
                    weatherUpdateCnt++
                    updateUnAutomaticData()
                    uploadDataSet(currentTime)
                }
            }
        })
    }

    private fun getPlaces() {
        try {
            if (useGooglePlaces) {
                googlePlacesCaller.getCurrentPlace()
            } else {
                //foursquareCaller.findPlaces()
            }
        } catch (e: Exception) {
            Log.d(TAG, "getPlaces Exception")
        }

    }

    private fun updateUnAutomaticData() {
        getWiFiName()
        checkScreenOn()
        ambientSound!!.getAmbientSound()
        googleFitness.readData()
    }

    private fun getWiFiName() {
        try {
            val wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiMgr.connectionInfo
            val wifi = wifiInfo.ssid + ":" + wifiInfo.bssid + ":" + wifiInfo
                    .ipAddress + ":" + wifiInfo.networkId + ":" + wifiInfo.rssi
            val level = WifiManager.calculateSignalLevel(wifiInfo.rssi, 100)
            //String wifi = wifiInfo.getSSID()+":"+wifiInfo.getIpAddress()+":"+wifiInfo
            // .getNetworkId();
            if (wifiInfo.ssid == null) {
                currentWifiName = "null"
            } else {
                currentWifiName = wifi
            }

        } catch (e: Exception) {
            Log.d(TAG, "get wifi name error")
        }

    }

    fun checkScreenOn() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            for (display in dm.displays) {
                isCurrentScreenOn = false
                if (display.state != Display.STATE_OFF) {
                    isCurrentScreenOn = true
                }
            }
        } else {
            isCurrentScreenOn = pm.isScreenOn
        }
    }

    private fun uploadDataSet(currentTime: Long): Long {
        val sensorDataSet = SensorDataSet(currentTime, "username")
        sensorDataSet.recordingId = recordingId
        if (DataCollectorApplication.ACTIVITY_ENABLED) {
            sensorDataSet.activity = currentActivity
        }
        if (DataCollectorApplication.WIFI_NAME_ENABLED) {
            sensorDataSet.wifiName = currentWifiName
        }
        if (DataCollectorApplication.LOCATION_ENABLED) {
            Log.d(TAG, currentLatitude.toString() + "")
            Log.d(TAG, currentLongitude.toString() + "")
            Log.d(TAG, "" + currentAccurate)
            val location = Location("gps")
            location.latitude = currentLatitude
            location.longitude = currentLongitude
            location.accuracy = currentAccurate.toFloat()
            sensorDataSet.gps = location
            sensorDataSet.location = currentPlaceName
        }
        if (DataCollectorApplication.GOOGLE_FITNESS_ENABLED) {
            sensorDataSet.totalStepsToday = currentSteps
        }
        if (DataCollectorApplication.AMBIENT_SOUND_ENABLED) {
            sensorDataSet.ambientSound = currentAmbientSound
        }
        if (DataCollectorApplication.WEATHER_ENABLED) {
            sensorDataSet.weather = currentWeatherId
        }
        sensorDataSet.screenState = isCurrentScreenOn
        sensorDataSet.id = db!!.insertSensorDataSet(sensorDataSet)
        uploadMotionData()
        uploadEnvironmentData()
        activityRecognizer!!.recognizeActivity(sensorDataSet)
        return sensorDataSet.id
    }

    private fun uploadEnvironmentData() {
        db!!.enterSingleDimensionDataBatch(recordingId,
                                           TABLE_REALTIME_AIR,
                                           myEnvironmentSensor.readPressureData())
        db!!.enterSingleDimensionDataBatch(recordingId,
                                           TABLE_REALTIME_TEMPERATURE,
                                           myEnvironmentSensor.readTemperatureData())
        db!!.enterSingleDimensionDataBatch(recordingId,
                                           TABLE_REALTIME_LIGHT,
                                           myEnvironmentSensor.readLightData())
        db!!.enterSingleDimensionDataBatch(recordingId, TABLE_REALTIME_HUMIDITY,
                                           myEnvironmentSensor.readHumidityData())
        db!!.enterSingleDimensionDataBatch(recordingId, TABLE_REALTIME_PROXIMITY,
                                           myEnvironmentSensor.readProximityData())
    }

    private fun uploadMotionData() {
        val accData = myMotion.readAccData()
        /*val rotData =  myMotion.readRotData()
        val magData =  myMotion.readMagData()
        val gyroData =  myMotion.readGyroData()*/
        db!!.enterAccDataBatch(recordingId, accData)
        /*db!!.enterGyroDataBatch(recordingId, gyroData)
        db!!.enterMagDataBatch(recordingId, magData)
        db!!.enterRotDataBatch(recordingId, rotData)*/
    }

    override fun activityUpdate(activity: DetectedActivity) {
        currentActivity = activity
    }

    override fun stopActivityDetection() {
        myActivity.removeActivityUpdates()
        myActivity.disconnect()
        myActivity.unregisterReceiver()
    }

    override fun locationChanged(location: Location?) {
        if (location != null) {
            currentLocation = location
            currentLatitude = location.latitude
            currentLongitude = location.longitude
            currentAccurate = location.accuracy.toDouble()
            getPlaces()
        }
    }

    // google places handler
    override fun onReceivedPlaces(places: HashMap<String, Float>) {
        //get the most likely one
        var placeName: String? = null
        var probability = 0f
        for ((key, value) in places) {
            if (value >= probability) {
                probability = value
                placeName = key
            }
        }

        if (placeName == null) {
            currentPlaceName = "null"
        } else {
            currentPlaceName = placeName + ":" + probability
        }

        //get all potential places
        /*        Iterator it = places.entrySet().iterator();
        String placeName  = "";
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if((float)pair.getValue()>0) {
                placeName += pair.getKey() + " : " + pair.getValue() + ";";
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        if(placeName.isEmpty()) {
            if(places.entrySet().size()>0){
                placeName = "Unknown Places";
            }else {
                placeName = "null";
            }
        }else {
            currentPlaceName = placeName;
        }*/

    }

    //foursquare place handler
    override fun placesFound(place: String?) {
        if (place != null) {
            val places = place.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            currentPlaceName = places[0]
            return
        }
        currentPlaceName = "null"
    }

    override fun stopLocationUpdate() {
        myLocation.stopLocationUpdate()
        googlePlacesCaller.disconnect()
    }

    fun stopGoogleFitness() {
        googleFitness.disconnect()
    }

    override fun onReceivedAmbientSound(volume: Double) {
        currentAmbientSound = volume
    }

    override fun onReceivedStepsCounter(steps: Long) {
        currentSteps = steps
    }

    override fun onReceivedWeather(weather: String?) {
        if (weather != null)
            currentWeatherId = db!!.enterWeather(weather, currentWeatherId + 1)
    }

    fun startNotification() {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val mNotifyBuilder = NotificationCompat.Builder(this)
                .setContentTitle("DataCollector")
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentText("Background service is running.")
                .setSmallIcon(R.drawable.fp_s)/*.addAction(0, "10 minutes", PendingIntent
                .getService(this, 19921, Intent(this, ActivitiesIntentService::class.java)
                        .putExtra(TYPE, MINUTES), PendingIntent.FLAG_ONE_SHOT))
                .addAction(0, "activity", PendingIntent.getService(this, 19922, Intent(this,
                                                                                       ActivitiesIntentService::class.java).putExtra(
                        TYPE,
                        ACTIVITY), PendingIntent
                                                                           .FLAG_ONE_SHOT))
                .addAction(0, "snack", PendingIntent.getService(this, 19923, Intent(this,
                                                                                    ActivitiesIntentService::class.java).putExtra(
                        TYPE,
                        SNACK), PendingIntent
                                                                        .FLAG_ONE_SHOT))
*/

        mNotificationManager.notify(
                notificationID,
                mNotifyBuilder.build())
    }

    fun cancelNotification() {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(notificationID)
    }
}
