package com.example.leo.datacollector.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.leo.datacollector.activityRecording.RECORDING_NUMBER
import com.example.leo.datacollector.datacollection.sensors.WeatherCaller
import com.example.leo.datacollector.models.SensorDataSet
import com.example.leo.datacollector.models.Weather
import com.example.leo.datacollector.utils.TimeUtils
import java.util.concurrent.TimeUnit

/**
 * Created by Leo on 18.11.2017.
 */
const val DATABASE_VERSION = 16

class SqliteDatabase private constructor(val context: Context) : SQLiteOpenHelper(context,
                                                                                  "mydb",
                                                                                  null,
                                                                                  DATABASE_VERSION,
                                                                                  null) {

    val TAG: String = SqliteDatabase.javaClass.canonicalName

    companion object {
        @Volatile private var INSTANCE: SqliteDatabase? = null

        fun getInstance(context: Context): SqliteDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                SqliteDatabase(context)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.transaction {
            execSQL(CREATE_TABLE_SENSORDATA)
            execSQL(CREATE_TABLE_EVENTS)
            execSQL(CREATE_TABLE_WEATHER)
        }
    }

    /**
     * Automatically insert stuff necessary for the transaction.
     */
    inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
        try {
            this.beginTransaction()
            this.block()
            this.setTransactionSuccessful()
        } catch (e: SQLiteException) {
            Log.e(TAG, e.message)
            e.printStackTrace()
        } finally {
            this.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WEATHER")
        onCreate(db)
    }

    fun insertSensorDataSet(sensorDataSet: SensorDataSet) {
        val cv = sensorDataSetToContentValues(sensorDataSet)
        writableDatabase.transaction {
            insertOrThrow(TABLE, null, cv)
        }
    }

    fun insertSensorDataBatch(data: Iterable<SensorDataSet>) {
        writableDatabase.transaction {
            for (sensorDataSet in data) {
                val cv = sensorDataSetToContentValues(sensorDataSet)
                insertOrThrow(TABLE, null, cv)
            }
        }
    }

    private fun sensorDataSetToContentValues(sensorDataSet: SensorDataSet): ContentValues {
        val cv = ContentValues()
        with(sensorDataSet) {
            cv.apply {
                put(TIMESTAMP, TimeUtils.getTimeStr(time))
                put(USERNAME, userName)
                put(RECORDING, recordingId)
                put(ACTIVITY, activity.toString())
                put(STEPS, stepsSinceLast)
                put(ABIENT_SOUND, ambientSound)
                put(SCREEN_STATE, screenState)
                put(AMBIENT_LIGHT, ambientLight)
                put(LOCATION, location)
                put(GPS, gps.toString())
                put(GPSlat, gps!!.latitude)
                put(GPSlng, gps!!.longitude)
                put(WIFI_NAME, wifiName)
                put(BLUETOOTH, bluetoothDevices.toString())
                put(AIR_PRESSURE, airPressure)
                put(HUMIDITY_PERCENT, humidityPercent)
                put(TEMPERATURE, temperature)
                put(ACCELEROMETER_X, acc_x)
                put(ACCELEROMETER_Y, acc_y)
                put(ACCELEROMETER_Z, acc_z)
                put(GYRO_X, gyro_x)
                put(GYRO_Y, gyro_y)
                put(GYRO_Z, gyro_z)
                put(PITCH, pitch)
                put(ROLL, roll)
                put(AZIMUTH, azimuth)
                put(MAG_X, mag_x)
                put(MAG_Y, mag_y)
                put(MAG_Z, mag_z)
                put(WEATHER, weather)
            }
        }
        return cv
    }


    val CREATE_TABLE_SENSORDATA =
            "CREATE TABLE if not exists $TABLE (" +
                    "$ID integer PRIMARY KEY, " +
                    "$SESSION integer, " +
                    "$RECORDING integer, " +
                    "$TIMESTAMP date, " +
                    "$USERNAME text, " +
                    "$ACTIVITY string, " +
                    "$STEPS integer, " +
                    "$ABIENT_SOUND real, " +
                    "$SCREEN_STATE bool, " +
                    "$AMBIENT_LIGHT real, " +
                    "$LOCATION text, " +
                    "$GPS text, " +
                    "$GPSlat real, " +
                    "$GPSlng real, " +
                    "$WIFI_NAME text, " +
                    "$BLUETOOTH text, " +
                    "$AIR_PRESSURE real, " +
                    "$HUMIDITY_PERCENT real, " +
                    "$TEMPERATURE real, " +
                    "$ACCELEROMETER_X real, " +
                    "$ACCELEROMETER_Y real, " +
                    "$ACCELEROMETER_Z real, " +
                    "$GYRO_X real, " +
                    "$GYRO_Y real, " +
                    "$GYRO_Z real, " +
                    "$MAG_X real, " +
                    "$MAG_Y real, " +
                    "$MAG_Z real, " +
                    "$PITCH real, " +
                    "$ROLL real, " +
                    "$AZIMUTH real, " +
                    "$WEATHER integer" +
                    ");"

    val CREATE_TABLE_EVENTS =
            "CREATE TABLE if not exists $TABLE_EVENTS (" +
                    "$ID integer PRIMARY KEY, " +
                    "$TIMESTAMP date, " +
                    "$USERNAME text, " +
                    "$EVENT text " +
                    ");"

    val CREATE_TABLE_WEATHER =
            "CREATE TABLE if not exists $TABLE_WEATHER ( " +
                    "$WEATHER_ID INTEGER PRIMARY KEY, " +
                    "$WEATHER_JSON TEXT, " +
                    "$WEATHER_TIMESTAMP INTEGER" +
                    ");"

    /**
     * Returs the highest (latest) recordingId
     */
    fun getLatestRecordingId(): Int {
        val c = writableDatabase.query(true,
                                       TABLE,
                                       arrayOf(RECORDING),
                                       null,
                                       null,
                                       null,
                                       null,
                                       "$RECORDING DESC",
                                       "1")
        if (c.count > 0) {
            c.moveToFirst()
            return c.getInt(c.getColumnIndex(RECORDING))
        } else {
            return 1
        }
    }

    /**
     * Returs the highest (latest) recordingId
     * returns 0 -> no entry in db
     * -1 -> no entry newer than one hour
     */
    fun getLatestWeather(): Weather? {
        val c = writableDatabase.query(true,
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

    fun getWeather(id: Int): Weather? {
        val c = readableDatabase.query(TABLE_WEATHER,
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
        writableDatabase.transaction {
            returnvalue = insertOrThrow(TABLE_WEATHER, null, c)
        }
        return returnvalue
    }

    fun getRecordings(): Cursor {
        return readableDatabase.rawQuery("SELECT DISTINCT $RECORDING AS $RECORDING_NUMBER, $RECORDING AS _id FROM $TABLE ",
                                         null)
    }

    fun getRecording(rec_id: Int): Cursor {
        return readableDatabase.rawQuery(
                "SELECT * FROM $TABLE " +
                        "LEFT JOIN $TABLE_WEATHER " +
                        "ON $TABLE_WEATHER.$WEATHER_ID = $TABLE.$WEATHER " +
                        "WHERE $RECORDING = ? " +
                        "ORDER BY $TIMESTAMP"
                , arrayOf("" + rec_id))
    }


}

// Weather
const val TABLE_WEATHER = "table_weather"
const val WEATHER_ID = "weather_id"
const val WEATHER_JSON = "weather_json"
const val WEATHER_TIMESTAMP = "weather_timestamp"

// SensorData
const val TABLE = "sensorData"
const val ID = "id"
const val SESSION = "session_id"
const val RECORDING = "recording_id"
const val TIMESTAMP = "timestamp"
const val USERNAME = "username"
const val ACTIVITY = "detectedActivity"
const val STEPS = "stepsSinceLast"
const val ABIENT_SOUND = "ambientSound"
const val SCREEN_STATE = "screenState"
const val AMBIENT_LIGHT = "ambienLight"
const val LOCATION = "location"
const val GPS = "gps"
const val GPSlat = "gps_lat"
const val GPSlng = "gps_lng"
const val WIFI_NAME = "wifiName"
const val BLUETOOTH = "bluetooth"
const val WEATHER = "weather"
const val AIR_PRESSURE = "airPressure"
const val HUMIDITY_PERCENT = "humidityInPercent"
const val TEMPERATURE = "temperature"
const val ACCELEROMETER_X = "acc_x"
const val ACCELEROMETER_Y = "acc_y"
const val ACCELEROMETER_Z = "acc_z"
const val GYRO_X = "gyro_x"
const val GYRO_Y = "gyro_y"
const val GYRO_Z = "gyro_z"
const val PITCH = "pitch"
const val AZIMUTH = "azimuth"
const val ROLL = "roll"
const val MAG_X = "mag_x"
const val MAG_Y = "mag_y"
const val MAG_Z = "mag_z"



// events
const val TABLE_EVENTS = "events"
const val EVENT = "event"