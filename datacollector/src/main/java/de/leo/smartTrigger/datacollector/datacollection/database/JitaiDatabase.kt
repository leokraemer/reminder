package de.leo.smartTrigger.datacollector.datacollection.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteOpenHelper
import android.hardware.SensorEvent
import android.location.Location
import android.os.Environment
import android.preference.PreferenceManager
import android.widget.Toast
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.model.LatLng
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import de.leo.smartTrigger.datacollector.R
import de.leo.smartTrigger.datacollector.datacollection.models.*
import de.leo.smartTrigger.datacollector.datacollection.sensors.WeatherCaller
import de.leo.smartTrigger.datacollector.jitai.JitaiEvent
import de.leo.smartTrigger.datacollector.jitai.MyGeofence
import de.leo.smartTrigger.datacollector.jitai.MyWifiGeofence
import de.leo.smartTrigger.datacollector.jitai.manage.NaturalTriggerJitai
import de.leo.smartTrigger.datacollector.ui.GeofencesWithPlayServices.Constants
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.LocationSelection.Companion.everywhere_geofence
import de.leo.smartTrigger.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import de.leo.smartTrigger.datacollector.utils.fromJson
import de.leo.smartTrigger.datacollector.utils.getObjectListFromCursor
import org.jetbrains.anko.db.transaction
import org.threeten.bp.LocalTime
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.*


/**
 * Created by Leo on 18.11.2017.
 */
const val DATABASE_VERSION = 1024

open class JitaiDatabase protected constructor(protected var context: Context) {

    val TAG: String = JitaiDatabase::class.java.canonicalName
    val userName: String by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.user_name), "userName")
    }

    companion object {
        @Volatile
        private var INSTANCE: JitaiDatabase? = null

        fun getInstance(context: Context): JitaiDatabase =
            INSTANCE
                ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(
                        context).also {
                        INSTANCE = it
                    }
                }

        private fun buildDatabase(context: Context) =
            JitaiDatabase(context)

        const val NAME = "mydb.1018"
    }

    val gson = Converters.registerAll(GsonBuilder()).create()

    open protected fun initializeDatabase(context: Context): SQLiteOpenHelper {
        val db = JitaiDatabaseOpenHelper(context)
       // db.setWriteAheadLoggingEnabled(true)
        return db
    }

    protected var db: SQLiteOpenHelper = initializeDatabase(context)
    fun close() = db.close()


    fun insertSensorDataSet(sensorDataSet: SensorDataSet): Long {
        val cv = sensorDataSetToContentValues(sensorDataSet)
        var id = -1L
        db.writableDatabase.transaction {
            id = insertOrThrow(TABLE_SENSORDATA, null, cv)
        }
        return id
    }

    fun insertSensorDataBatch(data: Iterable<SensorDataSet>) {
        db.writableDatabase.transaction {
            for (sensorDataSet in data) {
                val cv = sensorDataSetToContentValues(sensorDataSet)
                insertOrThrow(TABLE_SENSORDATA, null, cv)
            }
        }
    }

    /**
     * All sensordata between begin and end, sorted by timestamp.
     */
    fun getSensorDataSets(begin: Long, end: Long): MutableList<SensorDataSet> {
        val c = db.readableDatabase.query(TABLE_SENSORDATA, null,
                                          "$TIMESTAMP BETWEEN $begin AND $end",
                                          null,
                                          null,
                                          null,
                                          TIMESTAMP,
                                          null)
        return getObjectListFromCursor<SensorDataSet>(c, ::sensorDataSetFromCursor)
    }

    fun sensorDataSetFromCursor(c: Cursor): SensorDataSet {
        val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
        val user = c.getString(c.getColumnIndex(USERNAME))
        val id = c.getLong(c.getColumnIndex(ID))
        val activity = gson.fromJson<List<DetectedActivity>>(c.getString(c.getColumnIndex(ACTIVITY)))
        val totalStepsToday = c.getLong(c.getColumnIndex(STEPS))
        val ambientSound = c.getDouble(c.getColumnIndex(ABIENT_SOUND))
        val location = c.getString(c.getColumnIndex(LOCATION))
        val gps = Location("from db")
        gps.latitude = c.getDouble(c.getColumnIndex(GPSlat))
        gps.longitude = c.getDouble(c.getColumnIndex(GPSlng))
        val wifiName = gson.fromJson<List<WifiInfo>>(c.getString(c.getColumnIndex(WIFI_NAME)))
        val bluetoothDevices = gson.fromJson<List<String>>(c.getString(c.getColumnIndex(BLUETOOTH)))
        val weather = c.getLong(c.getColumnIndex(WEATHER))
        val screenState = c.getInt(c.getColumnIndex(SCREEN_STATE)) > 0
        val s = SensorDataSet(timestamp,
                              user,
                              id,
                              activity,
                              totalStepsToday,
                              ambientSound,
                              location,
                              gps,
                              wifiName,
                              bluetoothDevices,
                              screenState,
                              weather)
        return s
    }

    private fun sensorDataSetToContentValues(sensorDataSet: SensorDataSet): ContentValues {
        val cv = ContentValues()
        with(sensorDataSet) {
            cv.apply {
                put(TIMESTAMP, time)
                put(USERNAME, userName)
                put(ACTIVITY, gson.toJson(activity))
                put(STEPS, totalStepsToday)
                put(ABIENT_SOUND, ambientSound)
                put(LOCATION, locationName)
                //it is never deserialized -> not needed
                //put(GPS, gps.toString())
                put(GPSlat, gps?.latitude)
                put(GPSlng, gps?.longitude)
                put(WIFI_NAME, gson.toJson(wifiInformation))
                put(BLUETOOTH, gson.toJson(bluetoothDevices))
                put(WEATHER, weather)
                put(SCREEN_STATE, screenState)
            }
        }
        return cv
    }

    fun enterSingleDimensionData(table: String, value: SensorEvent) {
        val cv = ContentValues()
        cv.put(X, value.values[0])
        cv.put(TIMESTAMP, value.timestamp)
        cv.put(ACCURACY, value.accuracy)
        db.writableDatabase.insert(table, null, cv)
    }

    fun enterSingleDimensionDataBatch(table: String,
                                      values: Iterable<Pair<Long, Float>>) {
        db.writableDatabase.transaction {
            values.forEach { entry ->
                val cv = ContentValues()
                cv.put(X, entry.second)
                cv.put(TIMESTAMP, entry.first)
                //cv.put(ACCURACY, entry.accuracy)
                insert(table, null, cv)
            }
        }
    }

    fun enterAccDataBatch(values: Iterable<Pair<Long, FloatArray>>) {
        insert3DSensorValues(TABLE_REAL_TIME_ACC, values)
    }

    fun enterMagDataBatch(values: Iterable<Pair<Long, FloatArray>>) {
        insert3DSensorValues(TABLE_REAL_TIME_MAG, values)
    }

    fun enterGyroDataBatch(values: Iterable<Pair<Long, FloatArray>>) {
        insert3DSensorValues(TABLE_REAL_TIME_GYRO, values)
    }

    fun enterRotDataBatch(values: ArrayDeque<Pair<Long, FloatArray>>) {
        db.writableDatabase.transaction {
            values.forEach { entry ->
                val cv = ContentValues()
                cv.put(TIMESTAMP, entry.first)
                cv.put(X, entry.second[0])
                cv.put(Y, entry.second[1])
                cv.put(Z, entry.second[2])
                cv.put(SCALAR, entry.second[4])
                insert(TABLE_REAL_TIME_ROT, null, cv)
            }
        }
    }

    private fun insert3DSensorValues(table: String, values: Iterable<Pair<Long, FloatArray>>) {
        db.writableDatabase.transaction {
            values.forEach { entry ->
                val cv = ContentValues()
                cv.put(TIMESTAMP, entry.first)
                cv.put(X, entry.second[0])
                cv.put(Y, entry.second[1])
                cv.put(Z, entry.second[2])
                insert(table, null, cv)
            }
        }
    }

    fun getALL3DSensorValues(start: Long, end: Long, table: String): MutableList<Pair<Long,
        FloatArray>> {
        val c = db.readableDatabase.query(table,
                                          arrayOf(X,
                                                  Y,
                                                  Z,
                                                  TIMESTAMP),
                                          "$TIMESTAMP > ? AND " + "$TIMESTAMP < ?",
                                          arrayOf(start.toString(), end.toString()),
                                          null,
                                          null,
                                          null,
                                          null)
        val list = mutableListOf<Pair<Long, FloatArray>>()
        if (c.count > 0) {
            c.moveToFirst()
            do {
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val array = FloatArray(3)
                array[0] = c.getFloat(c.getColumnIndex(X))
                array[1] = c.getFloat(c.getColumnIndex(Y))
                array[2] = c.getFloat(c.getColumnIndex(Z))
                val pair = Pair<Long, FloatArray>(timestamp, array)
                list.add(pair)
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getALL3DSensorValuesNoTimestamp(start: Long,
                                        end: Long,
                                        table: String): MutableList<DoubleArray> {
        val c = db.readableDatabase.query(table,
                                          arrayOf(X,
                                                  Y,
                                                  Z),
                                          "$TIMESTAMP > ? AND " + "$TIMESTAMP < ?",
                                          arrayOf(start.toString(), end.toString()),
                                          null,
                                          null,
                                          null,
                                          null)
        val list = mutableListOf<DoubleArray>()
        if (c.count > 0) {
            c.moveToFirst()
            val XcolumnIndex = c.getColumnIndex(X)
            val YcolumnIndex = c.getColumnIndex(Y)
            val ZcolumnIndex = c.getColumnIndex(Z)
            do {
                val array = DoubleArray(3)
                array[0] = c.getDouble(XcolumnIndex)
                array[1] = c.getDouble(YcolumnIndex)
                array[2] = c.getDouble(ZcolumnIndex)
                list.add(array)
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getStepData(until: Long): MutableList<Pair<Long, Double>> {
        val c = db.readableDatabase.query(TABLE_SENSORDATA,
                                          arrayOf(STEPS,
                                                  TIMESTAMP),
                                          "$TIMESTAMP > ?",
                                          arrayOf(until.toString()),
                                          null,
                                          null,
                                          "$TIMESTAMP ASC",
                                          "1")

        val list = mutableListOf<Pair<Long, Double>>()
        if (c.count > 0) {
            c.moveToFirst()
            do {
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val pair = Pair<Long, Double>(timestamp, c.getDouble(c.getColumnIndex(STEPS)))
                list.add(pair)
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getSoundData(begin: Long, end: Long): MutableList<Pair<Long, Double>> {
        val c = db.readableDatabase.query(TABLE_SENSORDATA,
                                          arrayOf(ABIENT_SOUND, TIMESTAMP),
                                          "$TIMESTAMP >= ? and $TIMESTAMP <= ?",
                                          arrayOf(begin.toString(), end.toString()),
                                          null,
                                          null,
                                          null,
                                          null)

        val list = mutableListOf<Pair<Long, Double>>()
        if (c.count > 0) {
            c.moveToFirst()
            do {
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val pair = Pair<Long, Double>(timestamp,
                                              c.getDouble(c.getColumnIndex(ABIENT_SOUND)))
                list.add(pair)
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getScreenState(begin: Long, end: Long): MutableList<Pair<Long, Boolean>> {
        val c = db.readableDatabase.query(TABLE_SENSORDATA,
                                          arrayOf(SCREEN_STATE, TIMESTAMP),
                                          "$TIMESTAMP >= ? and $TIMESTAMP <= ?",
                                          arrayOf(begin.toString(), end.toString()),
                                          null,
                                          null,
                                          null,
                                          null)

        val list = mutableListOf<Pair<Long, Boolean>>()
        if (c.count > 0) {
            c.moveToFirst()
            do {
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val pair = Pair<Long, Boolean>(timestamp,
                                               c.getInt(c.getColumnIndex(SCREEN_STATE)) > 0)
                list.add(pair)
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getSensorValues(begin: Long, end: Long, table: String): MutableList<Pair<Long,
        Double>> {
        val c = db.readableDatabase.query(table,
                                          arrayOf(X, TIMESTAMP),
                                          "$TIMESTAMP >= ? and $TIMESTAMP <= ?",
                                          arrayOf(begin.toString(), end.toString()),
                                          null,
                                          null,
                                          null,
                                          null)

        val list = mutableListOf<Pair<Long, Double>>()
        if (c.count > 0) {
            c.moveToFirst()
            do {
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val pair = Pair<Long, Double>(timestamp, c.getDouble(c.getColumnIndex(X)))
                list.add(pair)
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getProximity(time: Long): MutableList<Pair<Long,
        Double>> {
        val c = db.readableDatabase.query(TABLE_REALTIME_PROXIMITY,
                                          arrayOf(X, TIMESTAMP),
                                          "$TIMESTAMP <= ?",
                                          arrayOf(time.toString()),
                                          null,
                                          null,
                                          "$TIMESTAMP DESC",
                                          "1")

        val list = mutableListOf<Pair<Long, Double>>()
        if (c.count > 0) {
            c.moveToFirst()
            do {
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val pair = Pair<Long, Double>(timestamp, c.getDouble(c.getColumnIndex(X)))
                list.add(pair)
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    /**
     * Returs the highest (latest) recordingId
     * returns 0 -> no entry in db
     * -1 -> no entry newer than one hour
     */
    fun getLatestWeather(): Weather? {
        val c = db.writableDatabase.query(true,
                                          TABLE_WEATHER,
                                          null,
                                          null, null, null, null,
                                          "$WEATHER_TIMESTAMP DESC",
                                          "1")
        if (c.count > 0) {
            c.moveToFirst()
            val weather = WeatherCaller.fromJSON(
                c.getString(c.getColumnIndex(WEATHER_JSON)))
            weather.timestamp = c.getLong(c.getColumnIndex(WEATHER_TIMESTAMP))
            weather.id = c.getInt(c.getColumnIndex(WEATHER_ID))
            return weather
        }
        //in case no previous weather is there start with 0
        val weather = Weather()
        weather.id = 1
        return weather
    }

    fun getWeather(id: Long): Weather? {
        val c = db.readableDatabase.query(TABLE_WEATHER,
                                          arrayOf(WEATHER_JSON),
                                          "$WEATHER_ID = ?",
                                          arrayOf(id.toString()),
                                          null,
                                          null,
                                          null)
        if (c.count == 1) {
            c.moveToFirst()
            val json = c.getString(c.getColumnIndex(WEATHER_JSON))
            return WeatherCaller.fromJSON(json)
        }
        return null
    }

    fun enterWeather(weather: String, id: Long): Long {
        val c = ContentValues()
        c.put(WEATHER_JSON, weather)
        c.put(WEATHER_TIMESTAMP, System.currentTimeMillis())
        var returnvalue = -1L
        db.writableDatabase.transaction {
            returnvalue = insertOrThrow(TABLE_WEATHER, null, c)
        }
        return returnvalue
    }

    private fun getOrientationSensorValues(begin: Long,
                                           end: Long): MutableList<Pair<Long, FloatArray>> {
        val c = db.readableDatabase.query(TABLE_REAL_TIME_ROT,
                                          arrayOf(X,
                                                  Y,
                                                  Z,
                                                  SCALAR,
                                                  TIMESTAMP),
                                          "$TIMESTAMP >= ? and $TIMESTAMP <= ?",
                                          arrayOf(begin.toString(), end.toString()),
                                          null,
                                          null,
                                          null,
                                          null)
        val list = mutableListOf<Pair<Long, FloatArray>>()
        if (c.count > 0) {
            c.moveToFirst()
            do {
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val array = FloatArray(4)
                array[0] = c.getFloat(c.getColumnIndex(X))
                array[1] = c.getFloat(c.getColumnIndex(Y))
                array[2] = c.getFloat(c.getColumnIndex(Z))
                array[3] = c.getFloat(c.getColumnIndex(SCALAR))
                val pair = Pair<Long, FloatArray>(timestamp, array)
                list.add(pair)
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getAllGeofences(): List<Pair<Int, MyGeofence>> {
        val c = db.readableDatabase.query(TABLE_GEOFENCE, null, null, null,
                                          null, null, null)
        val list = mutableListOf<Pair<Int, MyGeofence>>()
        if (c.moveToFirst()) {
            do {
                list.add(Pair(c.getInt(c.getColumnIndex(ID)), extractMyGeofenceFromCursor(c)))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getAllMyGeofencesDistinct(): List<MyGeofence> {
        val c = db.readableDatabase.query(true, TABLE_GEOFENCE, null, null, null, GEOFENCE_NAME,
                                          null, null, null)
        val list = mutableListOf<MyGeofence>()
        if (c.moveToFirst()) {
            do {
                list.add(extractMyGeofenceFromCursor(c).copy(enter = false,
                                                             exit = false,
                                                             dwellInside = true,
                                                             loiteringDelay = 0))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun enterGeofence(id: Int,
                      name: String,
                      latLng: LatLng,
                      radius: Float,
                      enter: Boolean,
                      exit: Boolean,
                      dwell: Boolean,
                      dwellOutside: Boolean,
                      loiteringDelay: Long,
                      icon: Int): Int {
        val cv = ContentValues()
        cv.put(ID, id)
        cv.put(GEOFENCE_NAME, name)
        cv.put(GEOFENCE_LAT, latLng.latitude)
        cv.put(GEOFENCE_LONG, latLng.longitude)
        cv.put(GEOFENCE_RADIUS, radius)
        cv.put(GEOFENCE_ENTER, enter)
        cv.put(GEOFENCE_EXIT, exit)
        cv.put(GEOFENCE_DWELL, dwell)
        cv.put(GEOFENCE_DWELL_OUTSIDE, dwellOutside)
        cv.put(GEOFENCE_VALIDITY, Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
        cv.put(GEOFENCE_LOITERING_DELAY, loiteringDelay)
        cv.put(GEOFENCE_IMAGE, icon)
        var returnval = -1
        db.writableDatabase.transaction {
            returnval = insertWithOnConflict(TABLE_GEOFENCE, null, cv, CONFLICT_REPLACE).toInt()
        }
        return returnval
    }

    fun enterGeofence(geofence: MyGeofence): Int {
        return enterGeofence(geofence.name,
                             geofence.latitude,
                             geofence.longitude,
                             geofence.radius,
                             geofence.enter,
                             geofence.exit,
                             geofence.dwellInside,
                             geofence.dwellOutside,
                             geofence.loiteringDelay,
                             geofence.imageResId)
    }

    fun enterMyWifiGeofence(geofence: MyWifiGeofence): Int {
        val cv = ContentValues()
        with(geofence) {
            cv.put(WIFI_GEOFENCE_NAME, name)
            cv.put(WIFI_GEOFENCE_BSSID, bssid)
            cv.put(WIFI_GEOFENCE_RSSI, rssi)
            cv.put(WIFI_GEOFENCE_ENTER, enter)
            cv.put(WIFI_GEOFENCE_EXIT, exit)
            cv.put(WIFI_GEOFENCE_DWELL, dwellInside)
            cv.put(WIFI_GEOFENCE_DWELL_OUTSIDE, dwellOutside)
            cv.put(WIFI_GEOFENCE_LOITERING_DELAY, loiteringDelay)
        }
        var returnval = -1L
        db.writableDatabase.transaction {
            returnval = insert(TABLE_WIFI_GEOFENCE, null, cv)
        }
        return returnval.toInt()
    }

    fun
        enterGeofence(name: String,
                      latitude: Double,
                      longitude: Double,
                      radius: Float,
                      enter: Boolean,
                      exit: Boolean,
                      dwell: Boolean,
                      dwellOutside: Boolean,
                      loiteringDelay: Long,
                      icon: Int): Int {
        val cv = ContentValues()
        cv.put(GEOFENCE_NAME, name)
        cv.put(GEOFENCE_LAT, latitude)
        cv.put(GEOFENCE_LONG, longitude)
        cv.put(GEOFENCE_RADIUS, radius)
        cv.put(GEOFENCE_ENTER, enter)
        cv.put(GEOFENCE_EXIT, exit)
        cv.put(GEOFENCE_DWELL, dwell)
        cv.put(GEOFENCE_DWELL_OUTSIDE, dwellOutside)
        cv.put(GEOFENCE_VALIDITY, Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
        cv.put(GEOFENCE_LOITERING_DELAY, loiteringDelay)
        cv.put(GEOFENCE_IMAGE, icon)
        var returnval = -1L
        db.writableDatabase.transaction {
            returnval = insert(TABLE_GEOFENCE, null, cv)
        }
        return returnval.toInt()
    }

    fun enterGeofence(name: String,
                      latLng: LatLng,
                      radius: Float,
                      enter: Boolean,
                      exit: Boolean,
                      dwell: Boolean,
                      dwellOutside: Boolean,
                      loiteringDelay: Long,
                      icon: Int): Int {
        return enterGeofence(name, latLng.latitude, latLng.longitude, radius, enter, exit, dwell,
                             dwellOutside, loiteringDelay, icon)
    }

    fun getGeofence(id: Int): Geofence {
        val c = db.readableDatabase.query(TABLE_GEOFENCE, null, "$ID = ?", arrayOf(id.toString()),
                                          null, null, null)
        c.moveToFirst()
        val geofece = extractGeofenceFromCursor(c)
        c.close()
        return geofece
    }

    fun getMyGeofence(id: Int): MyGeofence {
        val c = db.readableDatabase.query(TABLE_GEOFENCE, null, "$ID = ?", arrayOf(id.toString()),
                                          null, null, null)
        var geofece: MyGeofence? = null
        if (c.moveToFirst())
            geofece = extractMyGeofenceFromCursor(c)
        c.close()
        return geofece ?: everywhere_geofence()
    }

    fun getMyWifiGeofence(id: Int): MyWifiGeofence? {
        val c = db.readableDatabase.query(TABLE_WIFI_GEOFENCE,
                                          null,
                                          "$ID = ?",
                                          arrayOf(id.toString()),
                                          null,
                                          null,
                                          null)
        var geofece: MyWifiGeofence? = null
        if (c.moveToFirst())
            geofece = extractMyWifiGeofenceFromCursor(c)
        c.close()
        return geofece
    }

    //HACK the image code is not necessarily unique. Works only for HOME_CODE and WORK_CODE,
// because they can be created only once
    fun getMyGeofenceByCode(imageCode: Int): MyGeofence? {
        val c = db.readableDatabase.query(TABLE_GEOFENCE,
                                          null,
                                          "$GEOFENCE_IMAGE = ?",
                                          arrayOf(imageCode.toString
                                          ()),
                                          null,
                                          null,
                                          null)
        var geofece: MyGeofence? = null
        if (c.moveToFirst())
            geofece = extractMyGeofenceFromCursor(c)
        c.close()
        return geofece
    }

    private fun extractGeofenceFromCursor(c: Cursor): Geofence {
        val name = c.getString(c.getColumnIndex(GEOFENCE_NAME))
        val latitude = c.getDouble(c.getColumnIndex(GEOFENCE_LAT))
        val longitude = c.getDouble(c.getColumnIndex(GEOFENCE_LONG))
        val radius = c.getFloat(c.getColumnIndex(GEOFENCE_RADIUS))
        val enter = c.getInt(c.getColumnIndex(GEOFENCE_ENTER)) > 0
        val exit = c.getInt(c.getColumnIndex(GEOFENCE_EXIT)) > 0
        val dwell = c.getInt(c.getColumnIndex(GEOFENCE_DWELL)) > 0
        val validity = c.getLong(c.getColumnIndex(GEOFENCE_VALIDITY))
        return getGeofenceForLatlng(name, latitude, longitude, radius, enter, exit, dwell, validity)
    }

    private fun extractMyGeofenceFromCursor(c: Cursor): MyGeofence {
        val latitude = c.getDouble(c.getColumnIndex(GEOFENCE_LAT))
        val longitude = c.getDouble(c.getColumnIndex(GEOFENCE_LONG))
        val radius = c.getFloat(c.getColumnIndex(GEOFENCE_RADIUS))
        val name = c.getString(c.getColumnIndex(GEOFENCE_NAME))
        val id = c.getInt(c.getColumnIndex(ID))
        val imageResId = c.getInt(c.getColumnIndex(GEOFENCE_IMAGE))
        val enter = c.getInt(c.getColumnIndex(GEOFENCE_ENTER)) > 0
        val exit = c.getInt(c.getColumnIndex(GEOFENCE_EXIT)) > 0
        val dwellInside = c.getInt(c.getColumnIndex(GEOFENCE_DWELL)) > 0
        val dwellOutside = c.getInt(c.getColumnIndex(GEOFENCE_DWELL_OUTSIDE)) > 0
        val loiteringDelay = c.getLong(c.getColumnIndex(GEOFENCE_LOITERING_DELAY))
        return MyGeofence(id,
                          name,
                          latitude,
                          longitude,
                          radius,
                          enter,
                          exit,
                          dwellInside,
                          dwellOutside,
                          loiteringDelay,
                          imageResId)
    }

    private fun extractMyWifiGeofenceFromCursor(c: Cursor): MyWifiGeofence {
        val bssid = c.getString(c.getColumnIndex(WIFI_GEOFENCE_BSSID))
        val rssi = c.getInt(c.getColumnIndex(WIFI_GEOFENCE_RSSI))
        val name = c.getString(c.getColumnIndex(WIFI_GEOFENCE_NAME))
        val id = c.getInt(c.getColumnIndex(ID))
        val enter = c.getInt(c.getColumnIndex(WIFI_GEOFENCE_ENTER)) > 0
        val exit = c.getInt(c.getColumnIndex(WIFI_GEOFENCE_EXIT)) > 0
        val dwellInside = c.getInt(c.getColumnIndex(WIFI_GEOFENCE_DWELL)) > 0
        val dwellOutside = c.getInt(c.getColumnIndex(WIFI_GEOFENCE_DWELL_OUTSIDE)) > 0
        val loiteringDelay = c.getLong(c.getColumnIndex(WIFI_GEOFENCE_LOITERING_DELAY))
        return MyWifiGeofence(id,
                              name,
                              bssid,
                              rssi,
                              enter,
                              exit,
                              dwellInside,
                              dwellOutside,
                              loiteringDelay)
    }

    private fun getGeofenceForLatlng(name: String,
                                     latitude: Double,
                                     longitude: Double,
                                     radius: Float,
                                     enter: Boolean,
                                     exit: Boolean,
                                     dwell: Boolean,
                                     loiteringDelay: Long): Geofence {
        val enterI = if (enter) Geofence.GEOFENCE_TRANSITION_ENTER else 0;
        val exitI = if (exit) Geofence.GEOFENCE_TRANSITION_EXIT else 0;
        val dwellI = if (dwell) Geofence.GEOFENCE_TRANSITION_DWELL else 0;
        return Geofence.Builder()
            .setRequestId(name)
            .setCircularRegion(latitude, longitude, radius)
            .setLoiteringDelay(loiteringDelay.toInt())
            .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(enterI or exitI or dwellI)
            .build()
    }

    /**
     * Enter a jitai and get it returned back with the Id it has in the database.
     */
    fun enterNaturalTriggerJitai(jitai: NaturalTriggerJitai): NaturalTriggerJitai {
        val cv = ContentValues()
        cv.put(JITAI_GOAL, jitai.goal)
        cv.put(JITAI_MESSAGE, jitai.message)
        cv.put(JITAI_WEATHER, gson.toJson(jitai.weatherTrigger))
        cv.put(JITAI_ACTIVE, jitai.active)
        cv.put(JITAI_GEOFENCE, gson.toJson(jitai.geofenceTrigger))
        cv.put(JITAI_TIME_TRIGGER, gson.toJson(jitai.timeTrigger))
        var id = 0L
        db.writableDatabase.transaction { id = insert(TABLE_JITAI, null, cv) }
        jitai.id = id.toInt()
        return jitai
    }

    fun getAllActiveNaturalTriggerJitai(): MutableList<NaturalTriggerJitai> {
        val c = db.readableDatabase.query(TABLE_NATURAL_TRIGGER,
                                          null,
                                          "$NATURAL_TRIGGER_ACTIVE = 1 and $NATURAL_TRIGGER_DELETED" +
                                              " < 1",
                                          null,
                                          null,
                                          null,
                                          null)
        return getObjectListFromCursor(c, ::getNaturalTriggerJitaiFromCursor)
    }

    fun getActiveNaturalTriggerJitai(id: Int): NaturalTriggerJitai? {
        val c = db.readableDatabase.query(TABLE_NATURAL_TRIGGER,
                                          null,
                                          "$ID = $id " +
                                              "AND $NATURAL_TRIGGER_ACTIVE = 1 " +
                                              "AND $NATURAL_TRIGGER_DELETED < 1",
                                          null,
                                          null,
                                          null,
                                          null)
        var naturalTriggerJitai: NaturalTriggerJitai? = null
        if (c.moveToFirst()) {
            naturalTriggerJitai = getNaturalTriggerJitaiFromCursor(c)
        }
        c.close()
        return naturalTriggerJitai
    }

    private fun getNaturalTriggerJitaiFromCursor(c: Cursor): NaturalTriggerJitai {
        val naturalTrigger = getNaturalTrigger(c)
        val id = c.getInt(c.getColumnIndex(ID))
        val jitai = NaturalTriggerJitai(id, context, naturalTrigger)
        jitai.active = c.getInt(c.getColumnIndex(NATURAL_TRIGGER_ACTIVE)) > 0
        return jitai
    }

    fun enterUserJitaiEvent(id: Int, timestamp: Long, username: String, eventName: Int,
                            sensorDatasetId: Long, triggerRating: Int, momentRating: Int,
                            surveyText: String) {
        val cv = ContentValues()
        cv.put(JITAI_ID, id)
        cv.put(TIMESTAMP, timestamp)
        cv.put(USERNAME, username)
        cv.put(JITAI_EVENT, eventName)
        cv.put(JITAI_SURVEY_TRIGGER_RATING, triggerRating)
        cv.put(JITAI_SURVEY_MOMENT_RATING, momentRating)
        cv.put(JITAI_SURVEY_TEXT, surveyText)
        cv.put(JITAI_EVENT_SENSORDATASET_ID, sensorDatasetId)
        db.writableDatabase.transaction { insert(TABLE_JITAI_EVENTS, null, cv) }
    }

    fun enterNaturalTrigger(naturalTrigger: NaturalTriggerModel): Int {
        val cv = ContentValues()
        if (naturalTrigger.ID != -1)
            cv.put(ID, naturalTrigger.ID)
        cv.put(NATURAL_TRIGGER_GOAL, naturalTrigger.goal)
        cv.put(NATURAL_TRIGGER_MESSAGE, naturalTrigger.message)
        cv.put(NATURAL_TRIGGER_SITUATION, naturalTrigger.situation)
        cv.put(NATURAL_TRIGGER_BEGIN_TIME, naturalTrigger.beginTime!!.toSecondOfDay())
        cv.put(NATURAL_TRIGGER_END_TIME, naturalTrigger.endTime!!.toSecondOfDay())
        cv.put(NATURAL_TRIGGER_ACTIVITY, gson.toJson(naturalTrigger.activity))
        cv.put(NATURAL_TRIGGER_ACTIVITY_DURATION, naturalTrigger.timeInActivity)
        naturalTrigger.wifi?.let {
            val wifiID = enterMyWifiGeofence(it)
            cv.put(NATURAL_TRIGGER_WIFI, wifiID)
        }
        naturalTrigger.geofence?.let {
            val geofenceID = enterGeofence(it)
            cv.put(NATURAL_TRIGGER_GEOFENCE, geofenceID)
        }
        var returnval = -1L
        db.writableDatabase.transaction {
            returnval = insertWithOnConflict(TABLE_NATURAL_TRIGGER, null, cv, CONFLICT_REPLACE)
        }
        return returnval.toInt()
    }

    fun getNaturalTrigger(id: Int): NaturalTriggerModel {
        val cursor = db.readableDatabase.query(TABLE_NATURAL_TRIGGER,
                                               null,
                                               "$ID = ?",
                                               arrayOf(id.toString()),
                                               null,
                                               null,
                                               null)
        if (cursor.moveToFirst()) {
            return getNaturalTrigger(cursor)
        }
        return NaturalTriggerModel()
    }

    fun deleteNaturalTrigger(id: Int) {
        val cv = ContentValues()
        cv.put(NATURAL_TRIGGER_DELETED, true)
        db.writableDatabase.transaction {
            update(TABLE_NATURAL_TRIGGER,
                   cv,
                   "$ID = ?",
                   arrayOf(id.toString()))
        }
    }

    fun updateNaturalTrigger(id: Int, active: Boolean) {
        val cv = ContentValues()
        cv.put(NATURAL_TRIGGER_ACTIVE, active)
        db.writableDatabase.transaction {
            update(TABLE_NATURAL_TRIGGER,
                   cv,
                   "$ID = ?",
                   arrayOf(id.toString()))
        }
    }

    //cursor is already at position
    private fun getNaturalTrigger(cursor: Cursor): NaturalTriggerModel {
        val naturalTrigger = NaturalTriggerModel()
        with(cursor) {
            naturalTrigger.ID = getInt(getColumnIndex(ID))
            naturalTrigger.geofence = getMyGeofence(getInt(getColumnIndex(NATURAL_TRIGGER_GEOFENCE)))
            naturalTrigger.beginTime = LocalTime.ofSecondOfDay(getLong(getColumnIndex(
                NATURAL_TRIGGER_BEGIN_TIME)))
            naturalTrigger.endTime = LocalTime.ofSecondOfDay(getLong(getColumnIndex(
                NATURAL_TRIGGER_END_TIME)))
            naturalTrigger.goal = getString(getColumnIndex(NATURAL_TRIGGER_GOAL))
            naturalTrigger.message = getString(getColumnIndex(NATURAL_TRIGGER_MESSAGE))
            naturalTrigger.situation = getString(getColumnIndex(NATURAL_TRIGGER_SITUATION))
            naturalTrigger.wifi = getMyWifiGeofence(getInt(getColumnIndex(NATURAL_TRIGGER_WIFI)))
            naturalTrigger.timeInActivity = getLong(getColumnIndex(NATURAL_TRIGGER_ACTIVITY_DURATION))
            gson.fromJson<HashSet<Int>>(
                getString(getColumnIndex(NATURAL_TRIGGER_ACTIVITY)),
                object : TypeToken<HashSet<Int>>() {}.getType()).forEach {
                naturalTrigger.addActivity(it)
            }
            naturalTrigger.active = getInt(getColumnIndex(NATURAL_TRIGGER_ACTIVE)) > 0
        }
        return naturalTrigger
    }

    fun allNaturalTrigger(): Cursor {
        return db.readableDatabase.query(TABLE_NATURAL_TRIGGER,
                                         null,
                                         "$NATURAL_TRIGGER_DELETED < 1",
                                         null,
                                         null,
                                         null,
                                         null)
    }

    fun allNaturalTriggerModels(): List<NaturalTriggerModel> {
        val c = db.readableDatabase.query(TABLE_NATURAL_TRIGGER,
                                          null,
                                          "$NATURAL_TRIGGER_DELETED < 1",
                                          null,
                                          null,
                                          null,
                                          null)
        return getObjectListFromCursor(c, this::getNaturalTrigger)
    }

    fun getJitaiEvents(id: Int): MutableList<JitaiEvent> {
        val c = db.readableDatabase.query(TABLE_JITAI_EVENTS, null, "$JITAI_ID = ?", arrayOf(id
                                                                                                 .toString()),
                                          null, null, null)
        val list = mutableListOf<JitaiEvent>()
        if (c.moveToFirst()) {
            do {
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val username = c.getString(c.getColumnIndex(USERNAME))
                val eventType = c.getInt(c.getColumnIndex(JITAI_EVENT))
                val sensorDatasetId = c.getLong(c.getColumnIndex(JITAI_EVENT_SENSORDATASET_ID))
                list.add(JitaiEvent(id, timestamp, username, eventType, sensorDatasetId))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun enterGeofenceEvent(timestamp: Long, geofenceId: Int, geofenceName: String,
                           geofenceEvent: String) {
        val cv = ContentValues()
        cv.put(TIMESTAMP, timestamp)
        cv.put(GEOFENCE_ID, geofenceId)
        cv.put(GEOFENCE_NAME, geofenceName)
        cv.put(GEOFENCE_EVENT, geofenceEvent)
        cv.put(USERNAME, userName)
        db.writableDatabase.transaction {
            insert(TABLE_GEOFENCE_EVENT, null, cv)
        }
    }

    internal fun exportDb() {
        val externalStorageDirectory = Environment.getExternalStorageDirectory()
        val dataDirectory = Environment.getDataDirectory()

        var source: FileChannel? = null
        var destination: FileChannel? = null

        val currentDBPath =
            "/data/${context.applicationContext.applicationInfo.packageName}/databases/$NAME"
        val backupDBPath = "$NAME.$DATABASE_VERSION.$userName.sqlite"
        val currentDB = File(dataDirectory, currentDBPath)
        val backupDB = File(externalStorageDirectory, backupDBPath)

        try {
            source = FileInputStream(currentDB).getChannel()
            destination = FileOutputStream(backupDB).getChannel()
            destination!!.transferFrom(source, 0, source!!.size())

            Toast.makeText(context, "DB Exported!", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                if (source != null) source.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            try {
                if (destination != null) destination.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }
}



