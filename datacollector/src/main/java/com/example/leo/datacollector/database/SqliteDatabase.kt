package com.example.leo.datacollector.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.leo.datacollector.activityRecording.RECORDING_ID
import com.example.leo.datacollector.models.SensorDataSet
import com.example.leo.datacollector.utils.TimeUtils

/**
 * Created by Leo on 18.11.2017.
 */
class SqliteDatabase private constructor(val context: Context) : SQLiteOpenHelper(context, "mydb", null, 9) {

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
        db!!.execSQL("DROP TABLE IF EXISTS ${TABLE}")
        db!!.execSQL("DROP TABLE IF EXISTS ${TABLE_EVENTS}")
        onCreate(db!!)
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
                put(WIFI_NAME, wifiName)
                put(BLUETOOTH, bluetoothDevices.toString())
                put(AIR_PRESSURE, airPressure)
                put(HUMIDITY_PERCENT, humidityPercent)
                put(TEMPERATURE, temperature)
                put(WEATHER, weather)
            }
        }
        return cv
    }

    val TABLE = "sensorData"
    val ID = "id"
    val SESSION = "session_id"
    val RECORDING = "recording_id"
    val TIMESTAMP = "timestamp"
    val USERNAME = "username"
    val ACTIVITY = "detectedActivity"
    val STEPS = "stepsSinceLast"
    val ABIENT_SOUND = "ambientSound"
    val SCREEN_STATE = "screenState"
    val AMBIENT_LIGHT = "ambienLight"
    val LOCATION = "location"
    val GPS = "gps"
    val WIFI_NAME = "wifiName"
    val BLUETOOTH = "bluetooth"
    val WEATHER = "weather"
    val AIR_PRESSURE = "airPressure"
    val HUMIDITY_PERCENT = "humidityInPercent"
    val TEMPERATURE = "temperature"

    val TABLE_EVENTS = "events"
    val EVENT = "event"

    val CREATE_TABLE_SENSORDATA =
            "CREATE TABLE if not exists ${TABLE} (" +
                    "${ID} integer PRIMARY KEY autoincrement, " +
                    "${SESSION} integer, " +
                    "${RECORDING} integer, " +
                    "${TIMESTAMP} date, " +
                    "${USERNAME} text, " +
                    "${ACTIVITY} string, " +
                    "${STEPS} integer, " +
                    "${ABIENT_SOUND} real, " +
                    "${SCREEN_STATE} bool, " +
                    "${AMBIENT_LIGHT} real, " +
                    "${LOCATION} text, " +
                    "${GPS} text, " +
                    "${WIFI_NAME} text, " +
                    "${BLUETOOTH} text, " +
                    "${AIR_PRESSURE} real, " +
                    "${HUMIDITY_PERCENT} real, " +
                    "${TEMPERATURE} real, " +
                    "${WEATHER} text" +
                    ");"

    val CREATE_TABLE_EVENTS =
            "CREATE TABLE if not exists ${TABLE_EVENTS} (" +
                    "${ID} integer PRIMARY KEY autoincrement, " +
                    "${TIMESTAMP} date, " +
                    "${USERNAME} text, " +
                    "${EVENT} text " +
                    ");"

    /**
     * Returs the highest (latest) recordingId
     */
    fun getLatestRecordingId(): Int {
        val c = writableDatabase.query(true, TABLE, arrayOf(RECORDING), null, null, null, null, "${RECORDING} DESC", "1")
        if (c.count > 0) {
            c.moveToFirst()
            return c.getInt(c.getColumnIndex(RECORDING))
        } else {
            return 1
        }
    }
}