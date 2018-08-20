package de.leo.fingerprint.datacollector.datacollection

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.location.Location
import android.net.wifi.ScanResult
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
import com.google.android.gms.location.DetectedActivity
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.*
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.datacollection.models.WifiInfo
import de.leo.fingerprint.datacollector.datacollection.sensors.*
import de.leo.fingerprint.datacollector.jitai.activityDetection.ActivityRecognizer
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
import de.leo.fingerprint.datacollector.ui.activityRecording.RECORDING_ID
import de.leo.fingerprint.datacollector.ui.application.DataCollectorApplication
import de.leo.fingerprint.datacollector.ui.naturalTrigger.list.TriggerManagingActivity
import de.leo.fingerprint.datacollector.ui.notifications.NotificationService.Companion.CHANNEL
import de.leo.fingerprint.datacollector.utils.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

class DataCollectorService : Service(),
                             MyLocationListener,
                             FourSquareListener,
                             MyActivityListener,
                             GooglePlacesListener,
                             AmbientSoundListener,
                             GoogleFitnessListener,
                             WeatherCallerListener, WifiUpdateListener {

    override fun wifiUpdated(scanResults: List<ScanResult>) {
        currentWifis = scanResults
    }

    companion object {

        val TAG = "DataCollectorService"

        val notificationID = 1001
        val UPDATE_DELAY = SECONDS.toMillis(10L)
        private val WEATHER_UPDATE_DELAY = MINUTES.toMillis(30)

        private val useGooglePlaces = false
    }

    private val sample = 0

    private var weatherUpdateCnt = 0L

    internal lateinit var myMotion: MotionSensors
    //internal lateinit var myLocation: MyLocation
    internal var fusedLocationProviderClient: FusedLocationProvider? = null
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
    internal lateinit var currentPlaceName: String
    internal lateinit var currentLabel: String
    private var currentActivities = listOf(DetectedActivity(DetectedActivity.UNKNOWN, 0))
    private var currentWeatherId: Long = -1
    internal var currentSteps: Long = 0
    private var userName: String? = null
    private var db: JitaiDatabase? = null
    private var recordingId = -1
    private lateinit var activityRecognizer: ActivityRecognizer
    private var wifiScanner: WifiScanner? = null
    private var currentWifiInfo: List<WifiInfo>? = null
    private var currentWifis: List<ScanResult> = listOf()

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
        Log.d(TAG, "service recieved ${intent?.action ?: "unknown signal"}")
        if (!this.isRunning) {
            startDataCollection()
        }
        if (intent != null && intent.action != null)
            when (intent.action) {
                UPDATE_JITAI            -> {
                    val id = intent.getIntExtra(JITAI_ID, -1)
                    if (id != -1)
                        activityRecognizer.updateNaturalTrigger(id)
                    else
                        activityRecognizer = ActivityRecognizer(baseContext)
                }
                START_RECORDING         -> {
                    recordingId = intent.getIntExtra(RECORDING_ID, -1)
                    val answer = Intent(START_RECORDING)
                    answer.putExtra(RECORDING_ID, recordingId)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(answer)
                }
                STOP_RECORDING          -> {
                    recordingId = -1
                    val answer = Intent(STOP_RECORDING)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(answer)
                }
                IS_RECORDING            -> {
                    val answer = Intent(IS_RECORDING)
                    answer.putExtra(RECORDING_ID, recordingId)
                    answer.putExtra(IS_RECORDING, isRunning)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(answer)
                }
                USER_CLASSIFICATION_NOW -> {
                    val jitaiId = intent.getIntExtra(JITAI_ID, -1)
                    if (jitaiId > -1) {
                        doAsync {
                            db!!.enterUserJitaiEvent(jitaiId,
                                                     System.currentTimeMillis(),
                                                     Jitai.NOW,
                                                     uploadDataSet(System.currentTimeMillis()))
                            Log.d(TAG, "entered now event for $jitaiId")
                        }
                        longToast("Aktivität aufgezeichnet")
                    } else
                        baseContext.startActivity(baseContext.intentFor<TriggerManagingActivity>()
                                                      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        return Service.START_STICKY
    }

    private fun startDataCollection() {
        activityRecognizer = ActivityRecognizer(baseContext)
        this.isRunning = true
        if (DataCollectorApplication.LOCATION_ENABLED) {
            //myLocation = MyLocation(this)
            if (fusedLocationProviderClient == null)
                fusedLocationProviderClient = FusedLocationProvider(this, this)
            fusedLocationProviderClient!!.startLocationUpdates()
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
            currentActivities = listOf(DetectedActivity(DetectedActivity.UNKNOWN, 0))
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
        wifiScanner = WifiScanner(this, this)
        currentLabel = "null"



        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                                                                 IntentFilter(
                                                                     DataCollectorApplication.BROADCAST_EVENT))

        startScheduledUpdate()
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = false
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        userName = sharedPreferences.getString("fingerprint_user_name", "userName")
        startNotification()
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
        ambientSound?.stop()
        ambientSound = null
        wifiScanner?.stop()
        wifiScanner = null
    }

    private fun startScheduledUpdate() {
        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                //get the time when the update started to reduce jitter
                val currentTime = System.currentTimeMillis()
                //we assume that the operation finishes before it must be called again
                handler.postDelayed(this, UPDATE_DELAY)
                if (isRunning) {
                    if (weatherUpdateCnt == WEATHER_UPDATE_DELAY) {
                        weatherCaller.getCurrentWeather()
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
        ambientSound?.getAmbientSound()
        googleFitness.readData()
    }

    val wifiMgr by lazy { applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }

    private fun getWiFiName() {
        var wifiInfo: android.net.wifi.WifiInfo? = null
        try {
            wifiInfo = wifiMgr.connectionInfo
        } catch (e: Exception) {
            Log.d(TAG, "get wifi name error")
        }
        currentWifiInfo = currentWifis.map {
            //replace the connected wifi with better information
            if (wifiInfo?.bssid == it.BSSID) {
                de.leo.fingerprint.datacollector.datacollection.models.WifiInfo(wifiInfo!!)
            } else {
                de.leo.fingerprint.datacollector.datacollection.models.WifiInfo(it)
            }
        }
        if (currentWifis.isEmpty()) {
            currentWifiInfo = wifiInfo?.let {
                if (wifiInfo.bssid != null)
                    listOf(WifiInfo(wifiInfo))
                else null
            }
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
            isCurrentScreenOn = pm.isInteractive
        }
    }

    private fun uploadDataSet(currentTime: Long): Long {
        val sensorDataSet = SensorDataSet(currentTime, "username")
        sensorDataSet.recordingId = recordingId
        if (DataCollectorApplication.ACTIVITY_ENABLED) {
            sensorDataSet.activity = currentActivities
        }
        if (DataCollectorApplication.WIFI_NAME_ENABLED) {
            sensorDataSet.wifiInformation = currentWifiInfo
        }
        if (DataCollectorApplication.LOCATION_ENABLED) {
            Log.d(TAG, "lat: ${currentLatitude}")
            Log.d(TAG, "long: $currentLongitude")
            Log.d(TAG, "accuracy $currentAccurate")
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
        activityRecognizer.recognizeActivity(sensorDataSet)
        return sensorDataSet.id
    }

    private fun uploadEnvironmentData() {
        if (DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
            doAsync {
                db!!.enterSingleDimensionDataBatch(recordingId,
                                                   TABLE_REALTIME_AIR,
                                                   myEnvironmentSensor.readPressureData())
                db!!.enterSingleDimensionDataBatch(recordingId,
                                                   TABLE_REALTIME_TEMPERATURE,
                                                   myEnvironmentSensor.readTemperatureData())
                db!!.enterSingleDimensionDataBatch(recordingId,
                                                   TABLE_REALTIME_LIGHT,
                                                   myEnvironmentSensor.readLightData())
                db!!.enterSingleDimensionDataBatch(recordingId,
                                                   TABLE_REALTIME_HUMIDITY,
                                                   myEnvironmentSensor.readHumidityData())
                db!!.enterSingleDimensionDataBatch(recordingId,
                                                   TABLE_REALTIME_PROXIMITY,
                                                   myEnvironmentSensor.readProximityData())
            }
        }
    }

    private fun uploadMotionData() {
        if (DataCollectorApplication.ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED) {
            val accData = myMotion.readAccData()
            val rotData = myMotion.readRotData()
            val magData = myMotion.readMagData()
            val gyroData = myMotion.readGyroData()
            doAsync {
                db!!.enterAccDataBatch(recordingId, accData)
                db!!.enterGyroDataBatch(recordingId, gyroData)
                db!!.enterMagDataBatch(recordingId, magData)
                db!!.enterRotDataBatch(recordingId, rotData)
            }
        }
    }

    override fun activityUpdate(activity: List<DetectedActivity>) {
        currentActivities = activity
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
        //myLocation.stopLocationUpdate()
        fusedLocationProviderClient?.stopLocationUpdates()
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
        val mNotifyBuilder = NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Smart Reminder")
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentText("Reminder service is running.")
            .setSmallIcon(R.drawable.reminder_white)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        startForeground(notificationID, mNotifyBuilder.build())
    }

    fun cancelNotification() {
        stopForeground(true)
    }
}
