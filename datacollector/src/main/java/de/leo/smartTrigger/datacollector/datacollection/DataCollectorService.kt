package de.leo.smartTrigger.datacollector.datacollection

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.database.*
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.datacollection.models.WifiInfo
import de.leo.smartTrigger.datacollector.datacollection.sensors.*
import de.leo.smartTrigger.datacollector.jitai.UNKNOWN_LOCATION
import de.leo.smartTrigger.datacollector.jitai.activityDetection.ActivityRecognizer
import de.leo.smartTrigger.datacollector.ui.application.DataCollectorApplication
import de.leo.smartTrigger.datacollector.ui.application.DataCollectorApplication.Companion.ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED
import de.leo.smartTrigger.datacollector.ui.application.DataCollectorApplication.Companion.ACTIVITY_ENABLED
import de.leo.smartTrigger.datacollector.ui.application.DataCollectorApplication.Companion.AMBIENT_SOUND_ENABLED
import de.leo.smartTrigger.datacollector.ui.application.DataCollectorApplication.Companion.ENVIRONMENT_SENSOR_ENABLED
import de.leo.smartTrigger.datacollector.ui.application.DataCollectorApplication.Companion.GOOGLE_FITNESS_ENABLED
import de.leo.smartTrigger.datacollector.ui.application.DataCollectorApplication.Companion.LOCATION_ENABLED
import de.leo.smartTrigger.datacollector.ui.application.DataCollectorApplication.Companion.LOCATION_NAME_ENABLED
import de.leo.smartTrigger.datacollector.ui.application.DataCollectorApplication.Companion.SCREEN_ON_ENABLED
import de.leo.smartTrigger.datacollector.ui.application.DataCollectorApplication.Companion.WEATHER_ENABLED
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.list.TriggerManagingActivity
import de.leo.smartTrigger.datacollector.ui.notifications.NotificationService.Companion.CHANNEL
import de.leo.smartTrigger.datacollector.utils.UPDATE_JITAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class DataCollectorService : Service(),
                             FourSquareListener,
                             MyActivityListener,
                             GooglePlacesListener,
                             AmbientSoundListener,
                             GoogleFitnessListener,
                             WeatherCallerListener {


    companion object {

        val TAG = "DataCollectorService"

        val notificationID = 1001
        val UPDATE_DELAY = SECONDS.toMillis(10L)
        private val WEATHER_UPDATE_DELAY = MINUTES.toMillis(30)

        private val useGooglePlaces = false
    }

    private var weatherUpdateCnt = 0L

    private lateinit var myMotion: MotionSensors

    private lateinit var myActivity: MyActivity

    private var googlePlacesCaller: GooglePlacesCaller? = null
    private var currentLocation: Location = Location(UNKNOWN_LOCATION)

    private var myEnvironmentSensor: MyEnvironmentSensor? = null

    private var ambientSound: AmbientSound? = null
    private lateinit var googleFitness: GoogleFitness
    private lateinit var weatherCaller: WeatherCaller

    private var currentAmbientSound: Double = 0.0
    private var isRunning: Boolean = false
    private var currentPlaceName: String = ""
    private var currentActivities = listOf(DetectedActivity(DetectedActivity.UNKNOWN, 0))
    private var currentWeatherId: Long = -1
    private var currentSteps: Long = 0
    private val userName: String by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
            .getString(getString(R.string.user_name), null)
    }
    private var db: JitaiDatabase? = null
    private lateinit var activityRecognizer: ActivityRecognizer
    private lateinit var wifiScanner: WifiScanner

    private val pm: PowerManager by lazy { applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager }

    private var updateDelay: Long = UPDATE_DELAY
        set(value) {
            if (field != value) {
                field = value
                startNotification(field)
            }
        }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!this::wifiScanner.isInitialized)
            wifiScanner = WifiScanner(this)
        db = JitaiDatabase.getInstance(applicationContext)
        currentWeatherId = db!!.getLatestWeather()!!.id.toLong()
        Log.d(TAG, "service recieved ${intent?.action ?: "unknown signal"}")
        if (!this.isRunning) {
            startDataCollection()
        } else {
            //reschedule to update updatedelay
            startScheduledUpdate()
        }
        if (intent != null && intent.action != null)
            when (intent.action) {
                UPDATE_JITAI -> {
                    val id = intent.getIntExtra(JITAI_ID, -1)
                    if (id != -1)
                        activityRecognizer.updateNaturalTrigger(id)
                    else
                        activityRecognizer = ActivityRecognizer(baseContext)
                }
            }
        return Service.START_STICKY
    }

    private fun startDataCollection() {
        activityRecognizer = ActivityRecognizer(baseContext)
        this.isRunning = true
        if (LOCATION_NAME_ENABLED) {
            googlePlacesCaller = GooglePlacesCaller(this)
        }
        if (ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED) {
            myMotion = MotionSensors(this)
        }
        if (ACTIVITY_ENABLED) {
            myActivity = MyActivity(this)
            currentActivities = listOf(DetectedActivity(DetectedActivity.UNKNOWN, 0))
        }
        if (ENVIRONMENT_SENSOR_ENABLED) {
            myEnvironmentSensor = MyEnvironmentSensor(this)
        }
        if (GOOGLE_FITNESS_ENABLED) {
            googleFitness = GoogleFitness(this)
            currentSteps = 0
        }
        if (AMBIENT_SOUND_ENABLED) {
            ambientSound = AmbientSound(this)
            currentAmbientSound = 0.0
        }
        if (WEATHER_ENABLED) {
            weatherCaller = WeatherCaller(this)
        }
        startScheduledUpdate()
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = false
        startNotification(updateDelay)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        cancelNotification()

        if (DataCollectorApplication.ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED) {
            myMotion.stopMotionSensor()
        }
        if (DataCollectorApplication.ACTIVITY_ENABLED) {
            stopActivityDetection()
        }
        if (DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
            myEnvironmentSensor?.stopEnvironmentSensor()
        }
        if (DataCollectorApplication.GOOGLE_FITNESS_ENABLED) {
            stopGoogleFitness()
        }
        ambientSound?.stop()
        ambientSound = null
    }

    private fun startScheduledUpdate() {
        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                if (isRunning) {
                    uploadDataSet(currentTime)
                }
                updateDelay = Math.max(
                    Math.min(activityRecognizer.nextUpdate(), TimeUnit.MINUTES.toMillis(5)),
                    UPDATE_DELAY)
                handler.removeCallbacks { this }
                handler.postDelayed(this, updateDelay)
            }
        })
    }

    private val wifiMgr by lazy {
        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    suspend fun getWifi(): List<WifiInfo>? {
        val currentWifis = wifiScanner.getCurrentScanResult()
        var wifiInfo: android.net.wifi.WifiInfo? = null
        try {
            wifiInfo = wifiMgr.connectionInfo
        } catch (e: Exception) {
            Log.d(TAG, "get wifi name error")
        }
        if (currentWifis.isEmpty()) {
            return wifiInfo?.let {
                if (wifiInfo.bssid != null)
                    listOf(WifiInfo(wifiInfo))
                else null
            }
        } else {
            return currentWifis.map {
                //replace the connected wifi with better information
                if (wifiInfo?.bssid == it.BSSID) {
                    WifiInfo(wifiInfo!!)
                } else {
                    WifiInfo(it)
                }
            }
        }
    }


    fun checkScreenOn(): Boolean = pm.isInteractive

    private fun uploadDataSet(currentTime: Long) {
        //trigger asynchronus processes
        if (WEATHER_ENABLED) {
            if (weatherUpdateCnt == WEATHER_UPDATE_DELAY) {
                weatherCaller.getCurrentWeather()
                weatherUpdateCnt = 0
            }
            weatherUpdateCnt++
        }
        if (AMBIENT_SOUND_ENABLED) ambientSound?.getAmbientSound()
        if (GOOGLE_FITNESS_ENABLED) googleFitness.readData()
        //retrieve most recent data and store
        val sensorDataSet = SensorDataSet(currentTime, userName)
        if (ACTIVITY_ENABLED) sensorDataSet.activity = currentActivities
        if (GOOGLE_FITNESS_ENABLED) {
            sensorDataSet.totalStepsToday = currentSteps
        }
        if (AMBIENT_SOUND_ENABLED) {
            sensorDataSet.ambientSound = currentAmbientSound
        }
        if (WEATHER_ENABLED) {
            sensorDataSet.weather = currentWeatherId
        }
        if (SCREEN_ON_ENABLED) sensorDataSet.screenState = checkScreenOn()
        GlobalScope.launch(Dispatchers.Main) {
            val location = async { getLocation() }
            val wifi = async { getWifi() }
            currentLocation = location.await()
            val currentWifiInfo = wifi.await()
            Log.d(TAG, "lat: ${currentLocation.latitude}," +
                " long: ${currentLocation.longitude}," +
                " accuracy ${currentLocation.accuracy}")
            sensorDataSet.gps = currentLocation
            sensorDataSet.wifiInformation = currentWifiInfo
            sensorDataSet.locationName = currentPlaceName
            sensorDataSet.id = db!!.insertSensorDataSet(sensorDataSet)
            activityRecognizer.recognizeActivity(sensorDataSet)
            sensorDataSet.id = db!!.insertSensorDataSet(sensorDataSet)
            activityRecognizer.recognizeActivity(sensorDataSet)
        }
        uploadMotionData()
        uploadEnvironmentData()
    }

    suspend fun getLocation(): Location =
        if (LOCATION_ENABLED)
            getCurrentLocation(this) ?: Location(UNKNOWN_LOCATION)
        else
            Location(UNKNOWN_LOCATION)

    private fun uploadEnvironmentData() {
        if (DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
            doAsync {
                db!!.enterSingleDimensionDataBatch(TABLE_REALTIME_AIR,
                                                   myEnvironmentSensor!!.readPressureData())
                db!!.enterSingleDimensionDataBatch(TABLE_REALTIME_TEMPERATURE,
                                                   myEnvironmentSensor!!.readTemperatureData())
                db!!.enterSingleDimensionDataBatch(TABLE_REALTIME_LIGHT,
                                                   myEnvironmentSensor!!.readLightData())
                db!!.enterSingleDimensionDataBatch(TABLE_REALTIME_HUMIDITY,
                                                   myEnvironmentSensor!!.readHumidityData())
                db!!.enterSingleDimensionDataBatch(TABLE_REALTIME_PROXIMITY,
                                                   myEnvironmentSensor!!.readProximityData())
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
                db!!.enterAccDataBatch(accData)
                db!!.enterGyroDataBatch(gyroData)
                db!!.enterMagDataBatch(magData)
                db!!.enterRotDataBatch(rotData)
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
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = suspendCoroutine { cont ->
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.numUpdates = 1
        val listener = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                cont.resume(locationResult?.lastLocation)
            }
        }
        val task = fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                                                                      listener,
                                                                      null)
        task.addOnFailureListener { cont.resumeWithException(it) }
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

    fun startNotification(updateDelay: Long) {
        val contentIntent = intentFor<TriggerManagingActivity>().setAction("OPEN_TRIGGER_LIST")
        val mNotifyBuilder = NotificationCompat.Builder(this, CHANNEL)
            .setContentIntent(PendingIntent.getActivity(this,
                                                        6874,
                                                        contentIntent,
                                                        PendingIntent.FLAG_UPDATE_CURRENT))
            .setContentTitle("Smart Trigger")
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentText("Studienanwendung läuft."// Current rate: ${TimeUnit.MILLISECONDS
                // .toSeconds(updateDelay)}s"
                           )
            .setSmallIcon(R.drawable.ic_reminder_notification)
            .setColor(resources.getColor(R.color.green_800))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
        startForeground(notificationID, mNotifyBuilder.build())
    }

    fun cancelNotification() {
        stopForeground(true)
    }
}
