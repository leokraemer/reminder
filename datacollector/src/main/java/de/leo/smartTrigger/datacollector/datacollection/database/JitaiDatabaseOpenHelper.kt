package de.leo.smartTrigger.datacollector.datacollection.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import android.util.Log
import android.widget.TableLayout
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.android.gms.location.DetectedActivity
import com.google.gson.GsonBuilder
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.datacollection.models.WifiInfo
import de.leo.smartTrigger.datacollector.utils.getObjectListFromCursor
import org.jetbrains.anko.db.transaction

open class JitaiDatabaseOpenHelper(context: Context,
                                   name: String? = JitaiDatabase.NAME,
                                   version: Int = DATABASE_VERSION) : SQLiteOpenHelper
                                                                      (context,
                                                                       name,
                                                                       null,
                                                                       version,
                                                                       null) {

    val TAG: String = JitaiDatabase::class.java.canonicalName
    override fun onCreate(db: SQLiteDatabase?) {
        Log.d(TAG, "onCreate to ${db?.version}")
        db!!.transaction {
            execSQL(CREATE_TABLE_SENSORDATA)
            execSQL(CREATE_TABLE_WEATHER)
            execSQL(CREATE_TABLE_ACC)
            execSQL(CREATE_TABLE_GYRO)
            execSQL(CREATE_TABLE_ROT)
            execSQL(CREATE_TABLE_MAG)
            execSQL(CREATE_TABLE_GEOFENCE)
            execSQL(CREATE_TABLE_WIFI_GEOFENCE)
            execSQL(CREATE_TABLE_JITAI_EVENTS)
            execSQL(CREATE_TABLE_NATURAL_TRIGGER)
            execSQL(CREATE_GEOFENCE_EVENT_TABLE)
            createSingleDimensionRealtimeTables(db)
            execSQL("CREATE INDEX IF NOT EXISTS acc_timestamp_index ON $TABLE_REAL_TIME_ACC ($TIMESTAMP)")
            execSQL("CREATE INDEX IF NOT EXISTS mag_timestamp_index ON $TABLE_REAL_TIME_MAG ($TIMESTAMP)")
            execSQL("CREATE INDEX IF NOT EXISTS gyro_timestamp_index ON $TABLE_REAL_TIME_GYRO ($TIMESTAMP)")
            execSQL("CREATE INDEX IF NOT EXISTS air_timestamp_index ON $TABLE_REALTIME_AIR ($TIMESTAMP)")
            execSQL("CREATE INDEX IF NOT EXISTS prox_timestamp_index ON $TABLE_REALTIME_PROXIMITY ($TIMESTAMP)")
            execSQL("CREATE INDEX IF NOT EXISTS light_timestamp_index ON $TABLE_REALTIME_LIGHT ($TIMESTAMP)")
            execSQL("CREATE INDEX IF NOT EXISTS sensorDataSet_timestamp_index ON $TABLE_SENSORDATA ($TIMESTAMP)")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "onUpgrade from $oldVersion to $newVersion")
        db!!.transaction {
            if (oldVersion < 1024) {
                val time = System.currentTimeMillis()
                Log.d("db update start", "$time")
                val gson = Converters.registerAll(GsonBuilder()).create()

                fun mapActivities(value: String): Int {
                    if (value.contains("IN_VEHICLE")) return 0
                    if (value.contains("ON_BICYCLE")) return 1
                    if (value.contains("ON_FOOT")) return 2
                    if (value.contains("STILL")) return 3
                    if (value.contains("UNKNOWN")) return 4
                    if (value.contains("TILTING")) return 5
                    if (value.contains("WALKING")) return 6
                    if (value.contains("RUNNING")) return 7
                    if (value.contains("IN_ROAD_VEHICLE")) return 8
                    if (value.contains("IN_RAIL_VEHICLE")) return 9
                    else return 4 //unknown
                }

                fun deSerializeActivitys(list: String): List<DetectedActivity> {
                    //split into list and split entries into parts
                    //remove elements that do not represent detected activities
                    //[[type, asdf,  confidence, 7], [type, qwer,  confidence, 8]]
                    val activities = list.split(']', '[').asSequence().map {
                        it.split(',', '=')
                    }.filter { it.size == 4 }.map {
                        DetectedActivity(mapActivities(it[1]), it[3].toInt())
                    }
                    return activities.toList()
                }

                /**
                 * @return List<bssid, rssi, ssid, ip, networkId>
                 */
                fun deSerializeWifi(wifiNames: String?): List<WifiInfo> {
                    if (wifiNames == null || wifiNames.isEmpty() || wifiNames == "null")
                        return emptyList<WifiInfo>()
                            //val wifis = wifiNames.replace("[", "", false).replace("]", "", false).split(",")
                            .toMutableList()
                    val wifis = wifiNames
                        .split(Regex("\\], \\["), 0)
                        .map {
                            it.replace("[", "", false)
                                .replace("]", "", false)
                        }
                        .map { it.split(";") }
                    return wifis.map {
                        try {
                            WifiInfo(it[0], it[1].toInt(), it[2], it[3], it[4].toInt())
                        } catch (e: Exception) {
                            Log.e("wifi deserilaisation", e.toString())
                            WifiInfo("null", -100, "null", "null", -1)
                        }
                    }
                }

                fun sensorDataSetFromCursor(c: Cursor): SensorDataSet {
                    val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                    val user = c.getString(c.getColumnIndex(USERNAME))
                    val id = c.getLong(c.getColumnIndex(ID))
                    val activity = deSerializeActivitys(c.getString(c.getColumnIndex(ACTIVITY)))
                    val totalStepsToday = c.getLong(c.getColumnIndex(STEPS))
                    val ambientSound = c.getDouble(c.getColumnIndex(ABIENT_SOUND))
                    val location = c.getString(c.getColumnIndex(LOCATION))
                    val gps = Location("from db")
                    gps.latitude = c.getDouble(c.getColumnIndex(GPSlat))
                    gps.longitude = c.getDouble(c.getColumnIndex(GPSlng))
                    val wifiName = deSerializeWifi(c.getString(c.getColumnIndex(WIFI_NAME)))
                    val bluetoothDevices = listOf(c.getString(c.getColumnIndex(BLUETOOTH)))
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

                fun sensorDataSetToContentValues(sensorDataSet: SensorDataSet): ContentValues {
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
                Log.d("db update start", "${System.currentTimeMillis() - time}")
                val c = rawQuery("SELECT * FROM $TABLE_SENSORDATA ORDER BY $ID", null, null)
                val sensordata = getObjectListFromCursor(c, ::sensorDataSetFromCursor)
                Log.d("db end get", "${System.currentTimeMillis() - time}")
                setTransactionSuccessful()
                endTransaction()
                Log.d("db start insert", "${System.currentTimeMillis() - time}")
                beginTransaction()
                execSQL("DROP TABLE IF EXISTS $TABLE_SENSORDATA")
                execSQL(CREATE_TABLE_SENSORDATA)
                sensordata.forEach {
                    insert(TABLE_SENSORDATA,
                           null,
                           sensorDataSetToContentValues(it))
                }
                Log.d("db update finished", "${System.currentTimeMillis() - time}")
            }
        }
        Log.d(TAG, "onUpgrade deleting finished")
    }


    fun createSingleDimensionRealtimeTables(db: SQLiteDatabase) {
        for (i in 0 until REALTIME_TABLES.size) {
            val table = REALTIME_TABLES[i]
            db.execSQL("CREATE TABLE  IF NOT EXISTS $table ( " +
                           "$ID integer PRIMARY KEY, " +
                           "$TIMESTAMP date, " +
                           "$X real ," +
                           "$ACCURACY real )")
        }
    }

    fun deleteRealtimeTables(db: SQLiteDatabase) {
        for (i in 0 until REALTIME_TABLES.size) {
            val table = REALTIME_TABLES[i]
            db.execSQL("DROP TABLE if exists $table")
        }
        db.execSQL("DROP TABLE if exists $TABLE_REAL_TIME_ACC")
        db.execSQL("DROP TABLE if exists $TABLE_REAL_TIME_MAG")
        db.execSQL("DROP TABLE if exists $TABLE_REAL_TIME_ROT")
        db.execSQL("DROP TABLE if exists $TABLE_REAL_TIME_GYRO")
    }
}


//GEOFENCE
const val TABLE_GEOFENCE = "table_geofences"
const val GEOFENCE_NAME = "geofenceName"
const val GEOFENCE_LAT = "geofenceLat"
const val GEOFENCE_LONG = "geofenceLong"
const val GEOFENCE_RADIUS = "geofenceRadius"
const val GEOFENCE_ENTER = "geofenceEnter"
const val GEOFENCE_EXIT = "geofenceExit"
const val GEOFENCE_DWELL = "geofenceDwellInside"
const val GEOFENCE_DWELL_OUTSIDE = "geofenceDwellOutside"
const val GEOFENCE_DATE_ADDED = "geofenceTimestamp"
const val GEOFENCE_VALIDITY = "geofenceValidity"
const val GEOFENCE_LOITERING_DELAY = "geofenceLoiteringDelay"
const val GEOFENCE_IMAGE = "geofenceImage"

//GEOFENCE
const val TABLE_WIFI_GEOFENCE = "table_wifi_geofences"
const val WIFI_GEOFENCE_NAME = "geofenceName"
const val WIFI_GEOFENCE_BSSID = "geofenceBssid"
const val WIFI_GEOFENCE_RSSI = "geofenceRssi"
const val WIFI_GEOFENCE_ENTER = "geofenceEnter"
const val WIFI_GEOFENCE_EXIT = "geofenceExit"
const val WIFI_GEOFENCE_DWELL = "geofenceDwellInside"
const val WIFI_GEOFENCE_DWELL_OUTSIDE = "geofenceDwellOutside"
const val WIFI_GEOFENCE_LOITERING_DELAY = "geofenceLoiteringDelay"

// Weather
const val TABLE_WEATHER = "table_weather"
const val WEATHER_ID = "weather_id"
const val WEATHER_JSON = "weather_json"
const val WEATHER_TIMESTAMP = "weather_timestamp"

// SensorData
const val TABLE_SENSORDATA = "sensorData"
const val ID = "_id"
const val SESSION = "session_id"
const val RECORDING = "recording_id"
const val TIMESTAMP = "timestamp"
const val USERNAME = "username"
const val ACTIVITY = "detectedActivity"
const val STEPS = "totalStepsToday"
const val ABIENT_SOUND = "ambientSound"
const val LOCATION = "location"
const val GPS = "gps"
const val GPSlat = "gps_lat"
const val GPSlng = "gps_lng"
const val WIFI_NAME = "wifiName"
const val BLUETOOTH = "bluetooth"
const val WEATHER = "weather"
const val SCREEN_STATE = "screenState"

//unused

const val X = "x_axis"
const val Y = "y_axis"
const val Z = "z_axis"

// RealTimeSensorData

const val TABLE_REAL_TIME_ACC = "table_realtime_acc"
const val TABLE_REAL_TIME_GYRO = "table_realtime_gyro"
const val TABLE_REAL_TIME_MAG = "table_realtime_mag"
const val TABLE_REAL_TIME_ROT = "table_realtime_rot"

const val TABLE_REALTIME_LIGHT = "table_realtime_light"

const val TABLE_REALTIME_PROXIMITY = "table_realtime_proximity"

const val TABLE_REALTIME_AIR = "table_realtime_air"

const val TABLE_REALTIME_HUMIDITY = "table_humdity"

const val TABLE_REALTIME_TEMPERATURE = "table_realtime_temp"

val REALTIME_TABLES = arrayOf(TABLE_REALTIME_LIGHT,
                              TABLE_REALTIME_PROXIMITY,
                              TABLE_REALTIME_AIR,
                              TABLE_REALTIME_HUMIDITY,
                              TABLE_REALTIME_TEMPERATURE)

const val ACCURACY = "accuracy"
const val SCALAR = "scalar"

//jitai
const val TABLE_JITAI = "tableJitai"
const val JITAI_ACTIVE = "jitaiActive"
const val JITAI_GOAL = "jitaiName"
const val JITAI_MESSAGE = "jitaiMessage"
const val JITAI_GEOFENCE = "jitaiGeofence"

const val JITAI_WEATHER = "jitaiWeather"
const val JITAI_TIME_TRIGGER = "jitaiTimeTrigger"
const val JITAI_DELETED = "jitaiDeleted"

//jitai events
const val TABLE_JITAI_EVENTS = "table_jitai_events"
const val JITAI_EVENT = "jitai_event"
const val JITAI_ID = "jitai_id"
//how good was the time
const val JITAI_SURVEY_MOMENT_RATING = "jitaiMomentRating"
//how good was the trigger with  the conditions
const val JITAI_SURVEY_TRIGGER_RATING = "jitaiTriggerRating"
const val JITAI_SURVEY_TEXT = "jitaiSurveyText"
const val JITAI_EVENT_SENSORDATASET_ID = "jitai_sensorDataSet_id"

//Create table statements
const val CREATE_TABLE_SENSORDATA =
    "CREATE TABLE if not exists $TABLE_SENSORDATA (" +
        "$ID integer PRIMARY KEY, " +
        "$SESSION integer, " +
        "$TIMESTAMP date, " +
        "$USERNAME text, " +
        "$ACTIVITY string, " +
        "$STEPS integer, " +
        "$LOCATION text, " +
        "$GPS text, " +
        "$GPSlat real, " +
        "$GPSlng real, " +
        "$ABIENT_SOUND real, " +
        "$WIFI_NAME text, " +
        "$BLUETOOTH text, " +
        "$WEATHER integer," +
        "$SCREEN_STATE int" +
        ");"

const val CREATE_TABLE_JITAI_EVENTS =
    "CREATE TABLE if not exists $TABLE_JITAI_EVENTS (" +
        "$ID INTEGER PRIMARY KEY, " +
        "$TIMESTAMP DATE, " +
        "$USERNAME TEXT, " +
        //see Jitai.companion for codes
        "$JITAI_EVENT INTEGER, " +
        "$JITAI_ID INTEGER, " +
        "$JITAI_SURVEY_MOMENT_RATING INTEGER, " +
        "$JITAI_SURVEY_TRIGGER_RATING INTEGER, " +
        "$JITAI_SURVEY_TEXT TEXT, " +
        "$JITAI_EVENT_SENSORDATASET_ID INTEGER " +
        ");"


const val CREATE_TABLE_WEATHER =
    "CREATE TABLE if not exists $TABLE_WEATHER ( " +
        "$WEATHER_ID INTEGER PRIMARY KEY, " +
        "$WEATHER_JSON TEXT, " +
        "$WEATHER_TIMESTAMP INTEGER" +
        ");"

const val CREATE_TABLE_ACC =
    "CREATE TABLE if not exists $TABLE_REAL_TIME_ACC ( " +
        "$ID integer PRIMARY KEY, " +
        "$TIMESTAMP date, " +
        "$X real , " +
        "$Y real , " +
        "$Z real , " +
        "$ACCURACY real )"

const val CREATE_TABLE_MAG =
    "CREATE TABLE if not exists $TABLE_REAL_TIME_MAG ( " +
        "$ID integer PRIMARY KEY, " +
        "$TIMESTAMP date, " +
        "$X real , " +
        "$Y real , " +
        "$Z real , " +
        "$ACCURACY real )"

const val CREATE_TABLE_GYRO =
    "CREATE TABLE if not exists $TABLE_REAL_TIME_GYRO ( " +
        "$ID integer PRIMARY KEY, " +
        "$TIMESTAMP date, " +
        "$X real , " +
        "$Y real , " +
        "$Z real , " +
        "$ACCURACY real )"

const val CREATE_TABLE_ROT =
    "CREATE TABLE if not exists $TABLE_REAL_TIME_ROT ( " +
        "$ID integer PRIMARY KEY, " +
        "$TIMESTAMP date, " +
        "$X real , " +
        "$Y real , " +
        "$Z real , " +
        "$SCALAR real )"

const val CREATE_TABLE_GEOFENCE =
    "CREATE TABLE if not exists $TABLE_GEOFENCE ( " +
        "$ID integer PRIMARY KEY, " +
        "$GEOFENCE_NAME text, " +
        "$GEOFENCE_LAT real, " +
        "$GEOFENCE_LONG real, " +
        "$GEOFENCE_RADIUS INTEGER, " +
        "$GEOFENCE_ENTER bool, " +
        "$GEOFENCE_EXIT bool, " +
        "$GEOFENCE_DWELL bool, " +
        "$GEOFENCE_DWELL_OUTSIDE bool, " +
        "$GEOFENCE_DATE_ADDED time, " +
        "$GEOFENCE_LOITERING_DELAY number, " +
        "$GEOFENCE_VALIDITY LONG," +
        "$GEOFENCE_IMAGE INTEGER )"


const val CREATE_TABLE_WIFI_GEOFENCE =
    "CREATE TABLE if not exists $TABLE_WIFI_GEOFENCE ( " +
        "$ID integer PRIMARY KEY, " +
        "$WIFI_GEOFENCE_NAME text, " +
        "$WIFI_GEOFENCE_BSSID String, " +
        "$WIFI_GEOFENCE_RSSI INTEGER, " +
        "$WIFI_GEOFENCE_ENTER bool, " +
        "$WIFI_GEOFENCE_EXIT bool, " +
        "$WIFI_GEOFENCE_DWELL bool, " +
        "$WIFI_GEOFENCE_DWELL_OUTSIDE bool, " +
        "$WIFI_GEOFENCE_LOITERING_DELAY number )"

const val TABLE_NATURAL_TRIGGER = "table_natural_trigger"
const val NATURAL_TRIGGER_GOAL = "natural_trigger_goal"
const val NATURAL_TRIGGER_MESSAGE = "natural_trigger_message"
const val NATURAL_TRIGGER_SITUATION = "natural_trigger_situation"
const val NATURAL_TRIGGER_GEOFENCE = "natural_trigger_geofence"
const val NATURAL_TRIGGER_WIFI = "natural_trigger_wifi"
const val NATURAL_TRIGGER_BEGIN_TIME = "natural_trigger_begin_time"
const val NATURAL_TRIGGER_END_TIME = "natural_trigger_end_time"
const val NATURAL_TRIGGER_DELETED = "natural_trigger_deleted"
const val NATURAL_TRIGGER_ACTIVE = "natural_trigger_active"
const val NATURAL_TRIGGER_ACTIVITY = "natural_trigger_activity"
const val NATURAL_TRIGGER_ACTIVITY_DURATION = "natural_trigger_activity_duration"

const val CREATE_TABLE_NATURAL_TRIGGER =
    "CREATE TABLE if not exists $TABLE_NATURAL_TRIGGER ( " +
        "$ID INTEGER PRIMARY KEY, " +
        "$NATURAL_TRIGGER_GOAL TEXT, " +
        "$NATURAL_TRIGGER_MESSAGE TEXT, " +
        "$NATURAL_TRIGGER_SITUATION TEXT, " +
        "$NATURAL_TRIGGER_GEOFENCE INTEGER, " +
        "$NATURAL_TRIGGER_WIFI INTEGER, " +
        "$NATURAL_TRIGGER_BEGIN_TIME TIME, " +
        "$NATURAL_TRIGGER_END_TIME TIME, " +
        "$NATURAL_TRIGGER_DELETED BOOLEAN DEFAULT 0, " + //false
        "$NATURAL_TRIGGER_ACTIVE BOOLEAN DEFAULT 1, " +  //true
        "$NATURAL_TRIGGER_ACTIVITY TEXT, " +
        "$NATURAL_TRIGGER_ACTIVITY_DURATION INTEGER )"

//table to store enter/exit/dwell events of a geofence or wifi
const val TABLE_GEOFENCE_EVENT = "table_geofence_event"
const val GEOFENCE_EVENT = "geofence_event"
const val GEOFENCE_ID = "geofence_id"

const val CREATE_GEOFENCE_EVENT_TABLE =
    "CREATE TABLE if not exists $TABLE_GEOFENCE_EVENT ( " +
        "$ID INTEGER PRIMARY KEY, " +
        "$TIMESTAMP DATE, " +
        "$USERNAME TEXT, " +
        "$GEOFENCE_ID INTEGER, " +
        "$GEOFENCE_NAME TEXT, " +
        "$GEOFENCE_EVENT TEXT )"