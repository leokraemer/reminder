package de.leo.fingerprint.datacollector.datacollection.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.hardware.SensorEvent
import android.location.Location
import android.util.Log
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.model.LatLng
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import de.leo.fingerprint.datacollector.datacollection.models.Weather
import de.leo.fingerprint.datacollector.datacollection.models.deSerializeWifi
import de.leo.fingerprint.datacollector.datacollection.models.serializeWifi
import de.leo.fingerprint.datacollector.datacollection.sensors.WeatherCaller
import de.leo.fingerprint.datacollector.jitai.*
import de.leo.fingerprint.datacollector.jitai.Location.GeofenceTrigger
import de.leo.fingerprint.datacollector.jitai.activityDetection.MATCH
import de.leo.fingerprint.datacollector.jitai.activityDetection.NO_MATCH
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
import de.leo.fingerprint.datacollector.jitai.manage.MachineLearningJitai
import de.leo.fingerprint.datacollector.jitai.manage.NaturalTriggerJitai
import de.leo.fingerprint.datacollector.ui.GeofencesWithPlayServices.Constants
import de.leo.fingerprint.datacollector.ui.activityRecording.ActivityRecord
import de.leo.fingerprint.datacollector.ui.naturalTrigger.creation.NaturalTriggerModel
import org.threeten.bp.LocalTime
import java.util.*


/**
 * Created by Leo on 18.11.2017.
 */
const val DATABASE_VERSION = 1015

class JitaiDatabase private constructor(val context: Context) : SQLiteOpenHelper(context,
                                                                                 NAME,
                                                                                 null,
                                                                                 DATABASE_VERSION,
                                                                                 null) {

    val TAG: String = JitaiDatabase.javaClass.canonicalName
    val gson = Converters.registerAll(GsonBuilder()).create()

    companion object {
        @Volatile
        private var INSTANCE: JitaiDatabase? = null

        fun getInstance(context: Context): JitaiDatabase =
            INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: buildDatabase(
                            context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
            JitaiDatabase(context)

        const val NAME = "mydb"

        fun serializeDetectedActivitys(activities: List<DetectedActivity>): String {
            return activities.fold("", { r, f -> r + f.toString() })
        }

        fun deSerializeActivitys(list: String): List<DetectedActivity> {
            //split into list and split entries into parts
            val activities = list.split(']', '[').map { it.split(',', '=') } as ArrayList
            //remove elements that do not represent detected activities
            activities.removeAll { it.size != 4 }
            //[[type, asdf,  confidence, 7], [type, qwer,  confidence, 8]]
            return activities.map {
                DetectedActivity(mapActivities(
                    it[1]), it[3].toInt())
            }
        }

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
    }

    override fun onCreate(db: SQLiteDatabase?) {
        Log.d(TAG, "onCreate to ${db?.version}")
        db!!.transaction {
            execSQL(CREATE_TABLE_SENSORDATA)
            execSQL(CREATE_TABLE_EVENTS)
            execSQL(CREATE_TABLE_WEATHER)
            execSQL(CREATE_TABLE_RECORDINGS)
            execSQL(CREATE_TABLE_ACC)
            execSQL(CREATE_TABLE_GYRO)
            execSQL(CREATE_TABLE_ROT)
            execSQL(CREATE_TABLE_MAG)
            execSQL(CREATE_TABLE_JITAI)
            execSQL(CREATE_TABLE_GEOFENCE)
            execSQL(CREATE_TABLE_WIFI_GEOFENCE)
            execSQL(CREATE_TABLE_JITAI_EVENTS)
            execSQL(CREATE_TABLE_NATURAL_TRIGGER)
            createSingleDimensionRealtimeTables(db)
            execSQL("CREATE INDEX acc_timestamp_index ON $TABLE_REAL_TIME_ACC ($TIMESTAMP)")
            execSQL("CREATE INDEX mag_timestamp_index ON $TABLE_REAL_TIME_MAG ($TIMESTAMP)")
            execSQL("CREATE INDEX gyro_timestamp_index ON $TABLE_REAL_TIME_GYRO ($TIMESTAMP)")
            execSQL("CREATE INDEX air_timestamp_index ON $TABLE_REALTIME_AIR ($TIMESTAMP)")
            execSQL("CREATE INDEX prox_timestamp_index ON $TABLE_REALTIME_PROXIMITY ($TIMESTAMP)")
            execSQL("CREATE INDEX light_timestamp_index ON $TABLE_REALTIME_LIGHT ($TIMESTAMP)")
            execSQL("CREATE INDEX sensorDataSet_timestamp_index ON $TABLE_SENSORDATA ($TIMESTAMP)")
        }
        insertFirstEvent(db)
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
        Log.d(TAG, "onUpgrade from $oldVersion to $newVersion")
        db!!.transaction {
            execSQL("DROP TABLE IF EXISTS $TABLE_SENSORDATA")
            execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
            execSQL("DROP TABLE IF EXISTS $TABLE_WEATHER")
            execSQL("DROP TABLE IF EXISTS $TABLE_RECORDINGS")
            execSQL("DROP TABLE IF EXISTS $TABLE_GEOFENCE")
            execSQL("DROP TABLE IF EXISTS $TABLE_WIFI_GEOFENCE")
            execSQL("DROP TABLE IF EXISTS $TABLE_JITAI")
            execSQL("DROP TABLE IF EXISTS $TABLE_JITAI_EVENTS")
            execSQL("DROP TABLE IF EXISTS $TABLE_NATURAL_TRIGGER")
            execSQL("DROP INDEX IF EXISTS acc_timestamp_index")
            execSQL("DROP INDEX IF EXISTS mag_timestamp_index")
            execSQL("DROP INDEX IF EXISTS gyro_timestamp_index")
            execSQL("DROP INDEX IF EXISTS air_timestamp_index")
            execSQL("DROP INDEX IF EXISTS prox_timestamp_index")
            execSQL("DROP INDEX IF EXISTS light_timestamp_index")
            deleteRealtimeTables(db)
        }
        Log.d(TAG, "onUpgrade deleting finished")
        onCreate(db)
    }

    fun insertSensorDataSet(sensorDataSet: SensorDataSet): Long {
        val cv = sensorDataSetToContentValues(sensorDataSet)
        var id = -1L
        writableDatabase.transaction {
            id = insertOrThrow(TABLE_SENSORDATA, null, cv)
        }
        return id
    }

    fun insertSensorDataBatch(data: Iterable<SensorDataSet>) {
        writableDatabase.transaction {
            for (sensorDataSet in data) {
                val cv = sensorDataSetToContentValues(sensorDataSet)
                insertOrThrow(TABLE_SENSORDATA, null, cv)
            }
        }
    }

    private fun sensorDataSetToContentValues(sensorDataSet: SensorDataSet): ContentValues {
        val cv = ContentValues()
        with(sensorDataSet) {
            cv.apply {
                put(TIMESTAMP, time)
                put(USERNAME, userName)
                put(RECORDING, recordingId)
                put(ACTIVITY, activity.toString())
                put(STEPS, totalStepsToday)
                put(ABIENT_SOUND, ambientSound)
                put(LOCATION, location)
                put(GPS, gps.toString())
                put(GPSlat, gps?.latitude)
                put(GPSlng, gps?.longitude)
                put(WIFI_NAME, wifiInformation?.let { serializeWifi(it) } ?: "null")
                put(BLUETOOTH, bluetoothDevices.toString())
                put(WEATHER, weather)
                put(SCREEN_STATE, screenState)
            }
        }
        return cv
    }

    /**
     * get the sensordatasets for these events
     */
    fun getSensorDatasets(events: List<JitaiEvent>): List<Pair<SensorDataSet, String>> {
        val sb = StringBuilder()
        events.forEachIndexed { i, value ->
            if (value.sensorDatasetId > -1) {
                sb.append(value.sensorDatasetId)
                if (i < events.size - 1)
                    sb.append(", ")
            }
        }
        val ids = sb.toString()
        val c = readableDatabase.query(TABLE_SENSORDATA, null, "$ID IN ( $ids )",
                                       null,
                                       null,
                                       null,
                                       null)
        val list = mutableListOf<Pair<SensorDataSet, String>>()
        if (c.moveToFirst()) {
            do {
                val s = sensorDataSetFromCursor(c)
                val event = events[events.indexOfFirst { it.sensorDatasetId == s.id }]
                list.add(Pair(s, if (event.event in arrayOf(Jitai.NOTIFICATION_SUCCESS, Jitai.NOW))
                    MATCH else NO_MATCH))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    /**
     * get the sensordatasets for these events
     */
    fun getSensorDatasetForTimestamp(timestamp: Long): SensorDataSet? {
        val c = readableDatabase.query(TABLE_SENSORDATA, null,
                                       "$TIMESTAMP BETWEEN ${timestamp - 7500} AND $timestamp",
                                       null,
                                       null,
                                       null,
                                       TIMESTAMP,
                                       "1")
        val list = mutableListOf<SensorDataSet>()
        if (c.moveToFirst()) {
            do {
                list.add(sensorDataSetFromCursor(c))
            } while (c.moveToNext())
        }
        c.close()
        return list.firstOrNull()
    }

    /**
     * Get the sensordatasets for the provided events. Use id to find matches. Used to get the
     * events from other Jitai as true negatives.
     * Equivalent to [getSensorDatasets] when events has events from only one jitai.
     */
    fun getSensorDatasetsFromAll(events: List<JitaiEvent>, id: Int): List<Pair<SensorDataSet,
        String>> {
        val sb = StringBuilder()
        events.forEachIndexed { i, value ->
            if (value.sensorDatasetId > -1) {
                sb.append(value.sensorDatasetId)
                if (i < events.size - 1)
                    sb.append(", ")
            }
        }
        val ids = sb.toString()
        val c = readableDatabase.query(TABLE_SENSORDATA, null, "$ID IN ( $ids )",
                                       null,
                                       null,
                                       null,
                                       null)
        val list = mutableListOf<Pair<SensorDataSet, String>>()
        if (c.moveToFirst()) {
            do {
                val s = sensorDataSetFromCursor(c)
                val event = events[events.indexOfFirst { it.sensorDatasetId == s.id }]
                list.add(Pair(s,
                              if (event.JITAI_ID == id
                                  && event.event in arrayOf(Jitai.NOTIFICATION_SUCCESS, Jitai.NOW))
                                  MATCH else NO_MATCH))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getSensorDataset(id: Long): SensorDataSet? {
        val c = readableDatabase.query(TABLE_SENSORDATA, null, "$ID = ?",
                                       arrayOf(id.toString()),
                                       null,
                                       null,
                                       null)
        var s: SensorDataSet? = null
        if (c.moveToFirst()) {
            s = sensorDataSetFromCursor(c)
        }
        c.close()
        return s
    }

    private fun sensorDataSetFromCursor(c: Cursor): SensorDataSet {
        val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
        val user = c.getString(c.getColumnIndex(USERNAME))
        val recording = c.getInt(c.getColumnIndex(RECORDING_ID))
        val id = c.getLong(c.getColumnIndex(ID))
        //TODO fix hack
        val activity = c.getString(c.getColumnIndex(ACTIVITY))
        //"DetectedActivity [type=").append(type).append(", confidence=").append(confidence).append("]"
        val totalStepsToday = c.getLong(c.getColumnIndex(
            STEPS))
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
                              recording,
                              id,
                              deSerializeActivitys(activity),
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


    fun enterSingleDimensionData(rec_id: Int, table: String, value: SensorEvent) {
        val cv = ContentValues()
        cv.put(X, value.values[0])
        cv.put(TIMESTAMP, value.timestamp)
        cv.put(RECORDING_ID, rec_id)
        cv.put(ACCURACY, value.accuracy)
        writableDatabase.insert(table, null, cv)
    }

    fun enterSingleDimensionDataBatch(rec_id: Int,
                                      table: String,
                                      values: Iterable<Pair<Long, Float>>) {
        writableDatabase.transaction {
            values.forEach { entry ->
                val cv = ContentValues()
                cv.put(X, entry.second)
                cv.put(TIMESTAMP, entry.first)
                cv.put(RECORDING_ID, rec_id)
                //cv.put(ACCURACY, entry.accuracy)
                insert(table, null, cv)
            }
        }
    }

    fun enterAccDataBatch(rec_id: Int, values: Iterable<Pair<Long, FloatArray>>) {
        insert3DSensorValues(TABLE_REAL_TIME_ACC, values, rec_id)
    }

    fun enterMagDataBatch(rec_id: Int, values: Iterable<Pair<Long, FloatArray>>) {
        insert3DSensorValues(TABLE_REAL_TIME_MAG, values, rec_id)
    }

    fun enterGyroDataBatch(rec_id: Int, values: Iterable<Pair<Long, FloatArray>>) {
        insert3DSensorValues(TABLE_REAL_TIME_GYRO, values, rec_id)
    }

    fun enterRotDataBatch(rec_id: Int, values: ArrayDeque<Pair<Long, FloatArray>>) {
        writableDatabase.transaction {
            values.forEach { entry ->
                val cv = ContentValues()
                cv.put(TIMESTAMP, entry.first)
                cv.put(X, entry.second[0])
                cv.put(Y, entry.second[1])
                cv.put(Z, entry.second[2])
                cv.put(RECORDING_ID, rec_id)
                cv.put(SCALAR, entry.second[4])
                insert(TABLE_REAL_TIME_ROT, null, cv)
            }
        }
    }

    private fun insert3DSensorValues(table: String, values: Iterable<Pair<Long, FloatArray>>,
                                     rec_id: Int) {
        writableDatabase.transaction {
            values.forEach { entry ->
                val cv = ContentValues()
                cv.put(TIMESTAMP, entry.first)
                cv.put(X, entry.second[0])
                cv.put(Y, entry.second[1])
                cv.put(Z, entry.second[2])
                cv.put(RECORDING_ID, rec_id)
                insert(table, null, cv)
            }
        }
    }

    fun get3DSensorValues(rec_id: Int, table: String): MutableList<Pair<Long, FloatArray>> {
        val c = readableDatabase.query(table, arrayOf(X,
                                                      Y,
                                                      Z,
                                                      TIMESTAMP), "$RECORDING_ID = ?",
                                       arrayOf(rec_id.toString()), null, null, null, null)
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

    fun getALL3DSensorValues(rec_id: Int, table: String): MutableList<Pair<Long, FloatArray>> {
        val c = readableDatabase.query(table, arrayOf(X,
                                                      Y,
                                                      Z,
                                                      TIMESTAMP), "$RECORDING_ID != ?",
                                       arrayOf(rec_id.toString()), null,
                                       null, null, null)
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


    fun getALL3DSensorValues(start: Long, end: Long, table: String): MutableList<Pair<Long,
        FloatArray>> {
        val c = readableDatabase.query(table,
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
        val c = readableDatabase.query(table,
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

    fun getSensorValues(rec_id: Int, table: String): MutableList<Pair<Long, Double>> {
        val c = readableDatabase.query(table, arrayOf(X,
                                                      TIMESTAMP), "$RECORDING_ID = ?",
                                       arrayOf(rec_id.toString()), null, null, null, null)

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

    fun getStepData(until: Long): MutableList<Pair<Long, Double>> {
        val c = readableDatabase.query(TABLE_SENSORDATA,
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

    /*fun getStepData(begin: Long, end: Long): MutableList<Pair<Long, Double>> {
        val c = readableDatabase.query(TABLE,
                                       arrayOf(STEPS, TIMESTAMP),
                                       "$TIMESTAMP > ? AND $TIMESTAMP < ?",
                                       arrayOf(begin.toString(), end.toString()),
                                       null,
                                       null,
                                       "$TIMESTAMP ASC",
                                       null)

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
    }*/


    fun getSoundData(begin: Long, end: Long): MutableList<Pair<Long, Double>> {
        val c = readableDatabase.query(TABLE_SENSORDATA,
                                       arrayOf(ABIENT_SOUND,
                                               TIMESTAMP),
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
        val c = readableDatabase.query(TABLE_SENSORDATA,
                                       arrayOf(SCREEN_STATE,
                                               TIMESTAMP),
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
        val c = readableDatabase.query(table,
                                       arrayOf(X,
                                               TIMESTAMP),
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
        val c = readableDatabase.query(TABLE_REALTIME_PROXIMITY,
                                       arrayOf(X,
                                               TIMESTAMP),
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


    fun getRecordingIds(): List<Int> {
        val list = mutableListOf<Int>()
        val c = getRecordings()
        if (c.count > 0) {
            c.moveToFirst()
            do {
                list.add(c.getInt(c.getColumnIndex(ID)))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }


    fun createSingleDimensionRealtimeTables(db: SQLiteDatabase) {
        for (i in 0 until REALTIME_TABLES.size) {
            val table = REALTIME_TABLES[i]
            db.execSQL("CREATE TABLE $table ( " +
                           "$ID integer PRIMARY KEY, " +
                           "$RECORDING_ID INTEGER, " +
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

    /**
     * Returs the highest (latest) recordingId
     */
    fun getLatestRecordingId(): Int {
        val c = writableDatabase.query(true,
                                       TABLE_SENSORDATA,
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
            return 0
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

    fun getWeather(id: Long): Weather? {
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
        return readableDatabase.rawQuery("SELECT DISTINCT $TABLE_SENSORDATA.$RECORDING AS '_id'," +
                                             " $RECORDING_NAME " +
                                             " FROM $TABLE_RECORDINGS " +
                                             " LEFT JOIN $TABLE_SENSORDATA " +
                                             " ON $TABLE_SENSORDATA.$RECORDING " +
                                             "= $TABLE_RECORDINGS.$RECORDING_ID",
                                         null)
    }

    fun getRecording(rec_id: Int): ActivityRecord {
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_SENSORDATA " +
                "LEFT JOIN $TABLE_WEATHER " +
                "ON $TABLE_WEATHER.$WEATHER_ID = $TABLE_SENSORDATA.$WEATHER " +
                "WHERE $RECORDING = ? " +
                "ORDER BY $TIMESTAMP"
            , arrayOf("" + rec_id))

        val name = getRecordingName(rec_id)
        val record = ActivityRecord(name, c)
        c.close()
        record.accelerometerData = getALL3DSensorValues(record.beginTime, record.endTime,
                                                        TABLE_REAL_TIME_ACC)
        record.magnetData = getALL3DSensorValues(record.beginTime,
                                                 record.endTime,
                                                 TABLE_REAL_TIME_MAG)
        record.gyroscopData = getALL3DSensorValues(record.beginTime,
                                                   record.endTime,
                                                   TABLE_REAL_TIME_GYRO)
        record.proximity = getSensorValues(record.beginTime,
                                           record.endTime,
                                           TABLE_REALTIME_PROXIMITY)
        record.orientationData = getOrientationSensorValues(record.beginTime, record.endTime)
        record.ambientLight = getSensorValues(record.beginTime,
                                              record.endTime,
                                              TABLE_REALTIME_LIGHT)
        record.pressure = getSensorValues(record.beginTime, record.endTime,
                                          TABLE_REALTIME_AIR)
        return record
    }

    private fun getOrientationSensorValues(begin: Long,
                                           end: Long): MutableList<Pair<Long, FloatArray>> {
        val c = readableDatabase.query(TABLE_REAL_TIME_ROT,
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

    private fun getRecordingName(rec_id: Int): String {
        val d = readableDatabase.query(TABLE_RECORDINGS,
                                       null,
                                       "$RECORDING_ID = ?",
                                       arrayOf(rec_id.toString()),
                                       null,
                                       null,
                                       null,
                                       null)
        d.moveToFirst()
        val name = d.getString(d.getColumnIndex(RECORDING_NAME))
        d.close()
        return name
    }

    fun getReferenceRecording(rec_id: Int, limit: Int): ActivityRecord {
        val c = readableDatabase.rawQuery(
            "SELECT * FROM ( " +
                "SELECT * FROM $TABLE_SENSORDATA " +
                "LEFT JOIN $TABLE_WEATHER " +
                "ON $TABLE_WEATHER.$WEATHER_ID = $TABLE_SENSORDATA.$WEATHER " +
                "WHERE $RECORDING = ? " +
                "ORDER BY $TIMESTAMP DESC " +
                "LIMIT '$limit' ) " +
                "ORDER BY $TIMESTAMP"
            , arrayOf("" + rec_id))
        val name = getRecordingName(rec_id)
        val record = ActivityRecord(name, c)
        record.accelerometerData = get3DSensorValues(rec_id,
                                                     TABLE_REAL_TIME_ACC)
        record.magnetData = get3DSensorValues(rec_id,
                                              TABLE_REAL_TIME_MAG)
        record.gyroscopData = get3DSensorValues(rec_id,
                                                TABLE_REAL_TIME_GYRO)
        record.proximity = getSensorValues(rec_id,
                                           TABLE_REALTIME_PROXIMITY)
        record.orientationData = getOrientationSensorValues(record.beginTime, record.endTime)
        record.ambientLight = getSensorValues(rec_id,
                                              TABLE_REALTIME_LIGHT)
        record.pressure = getSensorValues(rec_id,
                                          TABLE_REALTIME_AIR)
        c.close()
        return record
    }


    fun insertEvent(event: Int): Long {
        val cv = ContentValues()
        cv.put(EVENT, event)
        cv.put(USERNAME,
               RECOGNIZE)
        cv.put(TIMESTAMP, System.currentTimeMillis())
        var id = 0L
        writableDatabase.transaction { id = insertOrThrow(TABLE_EVENTS, null, cv) }
        return id
    }

    fun insertEvent(event: Int, user: String): Long {
        val cv = ContentValues()
        cv.put(EVENT, event)
        cv.put(USERNAME, user)
        cv.put(TIMESTAMP, System.currentTimeMillis())
        var id = 0L
        writableDatabase.transaction { id = insertOrThrow(TABLE_EVENTS, null, cv) }
        return id
    }

    private fun insertFirstEvent(db: SQLiteDatabase): Long {
        val cv = ContentValues()
        cv.put(EVENT, -1)
        cv.put(USERNAME, "dummy")
        cv.put(TIMESTAMP, 0)
        var id = 0L
        db.transaction { id = insertOrThrow(TABLE_EVENTS, null, cv) }
        return id
    }

    fun getRecognizedActivitiesId(): Int {
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_EVENTS WHERE " +
                                                   "$USERNAME = \"$RECOGNIZE\" ORDER BY " +
                                                   "$TIMESTAMP " +
                                                   "DESC LIMIT \"1\"", null)

        var returnval = -1
        if (cursor.count > 0) {
            cursor.moveToFirst()
            returnval = cursor.getInt(cursor.getColumnIndex(EVENT))
        }
        cursor.close()
        return returnval
    }

    fun setRecordingName(s: String, rec_id: Int) {
        val cv = ContentValues()
        cv.put(RECORDING_NAME, s)
        cv.put(RECORDING_ID, rec_id)
        writableDatabase.replace(TABLE_RECORDINGS,
                                 null, cv)
    }

    fun getAllGeofences(): List<Pair<Int, Geofence>> {
        val c = readableDatabase.query(TABLE_GEOFENCE, null, null, null,
                                       null, null, null)
        val list = mutableListOf<Pair<Int, Geofence>>()
        if (c.moveToFirst()) {
            do {
                list.add(Pair(c.getInt(c.getColumnIndex(ID)), extractGeofenceFromCursor(c)))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getAllMyGeofences(): List<MyGeofence> {
        val c = readableDatabase.query(true, TABLE_GEOFENCE, null, null, null, GEOFENCE_NAME,
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
        writableDatabase.transaction {
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
        writableDatabase.transaction {
            returnval = insert(TABLE_WIFI_GEOFENCE, null, cv)
        }
        return returnval.toInt()
    }

    fun enterGeofence(name: String,
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
        writableDatabase.transaction {
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
        val c = readableDatabase.query(TABLE_GEOFENCE, null, "$ID = ?", arrayOf(id.toString()),
                                       null, null, null)
        c.moveToFirst()
        val geofece = extractGeofenceFromCursor(c)
        c.close()
        return geofece
    }

    fun getMyGeofence(id: Int): MyGeofence? {
        val c = readableDatabase.query(TABLE_GEOFENCE, null, "$ID = ?", arrayOf(id.toString()),
                                       null, null, null)
        var geofece: MyGeofence? = null
        if (c.moveToFirst())
            geofece = extractMyGeofenceFromCursor(c)
        c.close()
        return geofece
    }

    fun getMyWifiGeofence(id: Int): MyWifiGeofence? {
        val c = readableDatabase.query(TABLE_WIFI_GEOFENCE, null, "$ID = ?", arrayOf(id.toString()),
                                       null, null, null)
        var geofece: MyWifiGeofence? = null
        if (c.moveToFirst())
            geofece = extractMyWifiGeofenceFromCursor(c)
        c.close()
        return geofece
    }

    //HACK the image code is not necessarily unique. Works only for HOME_CODE and WORK_CODE,
// because they can be created only once
    fun getMyGeofenceByCode(imageCode: Int): MyGeofence? {
        val c = readableDatabase.query(TABLE_GEOFENCE,
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
    fun enterJitai(jitai: Jitai): Jitai {
        val cv = ContentValues()
        cv.put(JITAI_GOAL, jitai.goal)
        cv.put(JITAI_MESSAGE, jitai.message)
        cv.put(JITAI_WEATHER, gson.toJson(jitai.weatherTrigger))
        cv.put(JITAI_ACTIVE, jitai.active)
        cv.put(JITAI_GEOFENCE, gson.toJson(jitai.geofenceTrigger))
        cv.put(JITAI_TIME_TRIGGER, gson.toJson(jitai.timeTrigger))
        var id = 0L
        writableDatabase.transaction { id = insert(TABLE_JITAI, null, cv) }
        jitai.id = id.toInt()
        return jitai
    }


    fun updateJitai(jitai: Jitai): Jitai {
        val cv = ContentValues()
        cv.put(JITAI_GOAL, jitai.goal)
        cv.put(JITAI_MESSAGE, jitai.message)
        //TODO expand
        val id = writableDatabase.update(TABLE_JITAI, cv, "$ID = ?", arrayOf(jitai.id.toString()))
        jitai.id = id.toInt()
        return jitai
    }

    fun updateJitai(jitai: Int, active: Boolean) {
        val cv = ContentValues()
        cv.put(JITAI_ACTIVE, active)
        writableDatabase.update(TABLE_JITAI, cv, "$ID = ?", arrayOf(jitai.toString()))
    }

    fun allJitai(): Cursor {
        return readableDatabase.query(TABLE_JITAI, null, "$JITAI_DELETED < 1", null, null,
                                      null, null)
    }

    fun getJitai(id: Int): Jitai {
        val c = readableDatabase.query(TABLE_JITAI, null, "$ID = ? and $JITAI_DELETED < 1",
                                       arrayOf(id.toString()), null, null, null)
        val jitai = getJitaiFromCursor(c)
        c.close()
        return jitai
    }

    fun getActiveMachineLearningJitai(): MutableList<Jitai> {
        val c = readableDatabase.query(TABLE_JITAI,
                                       null,
                                       "$JITAI_ACTIVE = ? and $JITAI_DELETED < 1",
                                       arrayOf("1"),
                                       null,
                                       null,
                                       null)
        val jitais = mutableListOf<Jitai>()
        if (c.moveToFirst()) {
            do {
                jitais.add(getJitaiFromCursor(c))
            } while (c.moveToNext())
        }
        c.close()
        return jitais
    }

    fun getAllActiveNaturalTriggerJitai(): MutableList<NaturalTriggerJitai> {
        val c = readableDatabase.query(TABLE_NATURAL_TRIGGER,
                                       null,
                                       "$NATURAL_TRIGGER_ACTIVE = 1 and $NATURAL_TRIGGER_DELETED" +
                                           " < 1",
                                       null,
                                       null,
                                       null,
                                       null)
        val jitais = mutableListOf<NaturalTriggerJitai>()
        if (c.moveToFirst()) {
            do {
                jitais.add(getNaturalTriggerJitaiFromCursor(c))
            } while (c.moveToNext())
        }
        c.close()
        return jitais
    }

    fun getActiveNaturalTriggerJitai(id: Int): NaturalTriggerJitai? {
        val c = readableDatabase.query(TABLE_NATURAL_TRIGGER,
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
        val jitai = NaturalTriggerJitai(context, naturalTrigger)
        jitai.id = c.getInt(c.getColumnIndex(ID))
        jitai.active = c.getInt(c.getColumnIndex(NATURAL_TRIGGER_ACTIVE)) > 0
        return jitai
    }

    private fun getJitaiFromCursor(c: Cursor): Jitai {
        val jitai = MachineLearningJitai(context)
        jitai.goal = c.getString(c.getColumnIndex(JITAI_GOAL))
        jitai.message = c.getString(c.getColumnIndex(JITAI_MESSAGE))
        jitai.timeTrigger = gson.fromJson(c.getString(c.getColumnIndex(JITAI_TIME_TRIGGER)),
                                          TimeTrigger::class.java)
        jitai.geofenceTrigger = gson.fromJson(c.getString(c.getColumnIndex(JITAI_GEOFENCE)),
                                              GeofenceTrigger::class.java)
        jitai.weatherTrigger = gson.fromJson(c.getString(c.getColumnIndex(JITAI_WEATHER)),
                                             WeatherTrigger::class.java)
        jitai.id = c.getInt(c.getColumnIndex(ID))
        jitai.active = c.getInt(c.getColumnIndex(JITAI_ACTIVE)) > 0
        return jitai
    }

    fun deleteJitai(id: Int) {
        val cv = ContentValues()
        cv.put(JITAI_DELETED, true)
        writableDatabase.transaction { update(TABLE_JITAI, cv, "$ID = ?", arrayOf(id.toString())) }
    }

    fun enterJitaiEvent(id: Int, timestamp: Long, eventName: Int, sensorDatasetId: Long) {
        val cv = ContentValues()
        cv.put(JITAI_ID, id)
        cv.put(TIMESTAMP, timestamp)
        cv.put(JITAI_EVENT, eventName)
        cv.put(JITAI_EVENT_SENSORDATASET_ID, sensorDatasetId)
        writableDatabase.transaction { insert(TABLE_JITAI_EVENTS, null, cv) }
    }

    fun getJitaiEvents(id: Int): MutableList<JitaiEvent> {
        val c = readableDatabase.query(TABLE_JITAI_EVENTS, null, "$JITAI_ID = ?", arrayOf(id
                                                                                              .toString()),
                                       null, null, null)
        val list = mutableListOf<JitaiEvent>()
        if (c.moveToFirst()) {
            do {
                val id = c.getInt(c.getColumnIndex(JITAI_ID))
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val eventName = c.getInt(c.getColumnIndex(JITAI_EVENT))
                val sensorDatasetId = c.getLong(c.getColumnIndex(JITAI_EVENT_SENSORDATASET_ID))
                list.add(JitaiEvent(id, timestamp, eventName, sensorDatasetId))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun enterNaturalTrigger(naturalTrigger: NaturalTriggerModel): Int {
        val cv = ContentValues()
        if (naturalTrigger.ID != -1)
            cv.put(ID, naturalTrigger.ID)
        cv.put(NATURAL_TRIGGER_GOAL, naturalTrigger.goal)
        cv.put(NATURAL_TRIGGER_MESSAGE, naturalTrigger.message)
        cv.put(NATURAL_TRIGGER_SITUATION, naturalTrigger.situation)
        cv.put(NATURAL_TRIGGER_BEGIN_TIME, naturalTrigger.beginTime.toSecondOfDay())
        cv.put(NATURAL_TRIGGER_END_TIME, naturalTrigger.endTime.toSecondOfDay())
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
        writableDatabase.transaction {
            returnval = insertWithOnConflict(TABLE_NATURAL_TRIGGER, null, cv, CONFLICT_REPLACE)
        }
        return returnval.toInt()
    }

    fun getNaturalTrigger(id: Int): NaturalTriggerModel {
        val cursor = readableDatabase.query(TABLE_NATURAL_TRIGGER,
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
        writableDatabase.transaction {
            update(TABLE_NATURAL_TRIGGER,
                   cv,
                   "$ID = ?",
                   arrayOf(id.toString()))
        }
    }

    fun updateNaturalTrigger(id: Int, active: Boolean) {
        val cv = ContentValues()
        cv.put(NATURAL_TRIGGER_ACTIVE, active)
        writableDatabase.transaction {
            update(TABLE_NATURAL_TRIGGER,
                   cv,
                   "$ID = ?",
                   arrayOf(id.toString()))
        }
    }

    //cursor is already at position
    fun getNaturalTrigger(cursor: Cursor): NaturalTriggerModel {
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
            naturalTrigger.activity = gson.fromJson<HashSet<Int>>(
                getString(getColumnIndex(NATURAL_TRIGGER_ACTIVITY)),
                object : TypeToken<HashSet<Int>>() {}.getType())
        }
        return naturalTrigger
    }

    fun allNaturalTrigger(): Cursor {
        return readableDatabase.query(TABLE_NATURAL_TRIGGER, null, "$NATURAL_TRIGGER_DELETED < " +
            "1", null, null, null, null)
    }

    fun getJitaiTriggerEvents(id: Int): MutableList<JitaiEvent> {
        val c = readableDatabase.query(TABLE_JITAI_EVENTS, null, "$JITAI_ID = ?", arrayOf(id
                                                                                              .toString()),
                                       null, null, null)
        val list = mutableListOf<JitaiEvent>()
        if (c.moveToFirst()) {
            do {
                val id = c.getInt(c.getColumnIndex(JITAI_ID))
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val eventName = c.getInt(c.getColumnIndex(JITAI_EVENT))
                list.add(JitaiEvent(id, timestamp, eventName, -1L))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getJitaiEventsForTraining(id: Int): MutableList<JitaiEvent> {
        val c = readableDatabase.query(TABLE_JITAI_EVENTS, null, "$JITAI_ID = ? AND " +
            "$JITAI_EVENT IN (" +
            "'${Jitai.NOTIFICATION_SUCCESS}' ," +
            "'${Jitai.NOW}' , " +
            "'${Jitai.NOTIFICATION_FAIL}'" +
            //"," +
            //"'${Jitai.NOTIFICATION_DELETED}'" +
            ")",
                                       arrayOf(id.toString()),
                                       null, null, null)
        val list = mutableListOf<JitaiEvent>()
        if (c.moveToFirst()) {
            do {
                val id = c.getInt(c.getColumnIndex(JITAI_ID))
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val eventName = c.getInt(c.getColumnIndex(JITAI_EVENT))
                val sensorDatasetId = c.getLong(c.getColumnIndex(JITAI_EVENT_SENSORDATASET_ID))
                list.add(JitaiEvent(id, timestamp, eventName, sensorDatasetId))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getAllJitaiEventsForTraining(): MutableList<JitaiEvent> {
        val c = readableDatabase.query(TABLE_JITAI_EVENTS, null, "$JITAI_EVENT IN (" +
            "'${Jitai.NOTIFICATION_SUCCESS}' ," +
            "'${Jitai.NOW}' , " +
            "'${Jitai.NOTIFICATION_FAIL}'" +
            //"," +
            //"'${Jitai.NOTIFICATION_DELETED}'" +
            ")",
                                       null, null, null, null)
        val list = mutableListOf<JitaiEvent>()
        if (c.moveToFirst()) {
            do {
                val id = c.getInt(c.getColumnIndex(JITAI_ID))
                val timestamp = c.getLong(c.getColumnIndex(TIMESTAMP))
                val eventName = c.getInt(c.getColumnIndex(JITAI_EVENT))
                val sensorDatasetId = c.getLong(c.getColumnIndex(JITAI_EVENT_SENSORDATASET_ID))
                list.add(JitaiEvent(id, timestamp, eventName, sensorDatasetId))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }


    fun getSensorDataSets(begin: Long, end: Long): List<SensorDataSet> {
        val c = readableDatabase.query(TABLE_SENSORDATA, null,
                                       "$TIMESTAMP BETWEEN $begin AND $end",
                                       null,
                                       null,
                                       null,
                                       TIMESTAMP,
                                       null)
        val list = mutableListOf<SensorDataSet>()
        if (c.moveToFirst()) {
            do {
                list.add(sensorDataSetFromCursor(c))
            } while (c.moveToNext())
        }
        c.close()
        return list
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
const val TABLE_WIFI_GEOFENCE = "table_geofences"
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

// events
const val TABLE_EVENTS = "events"
const val EVENT = "event"
const val RECOGNIZE = "recognize"

//recordings
const val TABLE_RECORDINGS = "recordings"
const val RECORDING_ID = RECORDING
const val RECORDING_NAME = "recording_name"

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
const val JITAI_EVENT_SENSORDATASET_ID = "jitai_sensorDataSet_id"

//Create table statements
const val CREATE_TABLE_JITAI =
    "CREATE TABLE if not exists $TABLE_JITAI (" +
        "$ID integer PRIMARY KEY, " +
        "$JITAI_GOAL text, " +
        "$JITAI_ACTIVE bool, " +
        "$JITAI_MESSAGE text, " +
        "$JITAI_GEOFENCE text, " +
        "$JITAI_TIME_TRIGGER text, " +
        "$JITAI_WEATHER text, " +
        "$JITAI_DELETED Boolean default 0 " +
        ");"

const val CREATE_TABLE_SENSORDATA =
    "CREATE TABLE if not exists $TABLE_SENSORDATA (" +
        "$ID integer PRIMARY KEY, " +
        "$SESSION integer, " +
        "$RECORDING integer, " +
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

const val CREATE_TABLE_EVENTS =
    "CREATE TABLE if not exists $TABLE_EVENTS (" +
        "$ID integer PRIMARY KEY, " +
        "$TIMESTAMP DATE, " +
        "$USERNAME TEXT, " +
        "$EVENT TEXT " +
        ");"

const val CREATE_TABLE_JITAI_EVENTS =
    "CREATE TABLE if not exists $TABLE_JITAI_EVENTS (" +
        "$ID integer PRIMARY KEY, " +
        "$TIMESTAMP date, " +
        //see Jitai.companion for codes
        "$JITAI_EVENT INTEGER, " +
        "$JITAI_ID INTEGER, " +
        "$JITAI_EVENT_SENSORDATASET_ID INTEGER " +
        ");"


const val CREATE_TABLE_WEATHER =
    "CREATE TABLE if not exists $TABLE_WEATHER ( " +
        "$WEATHER_ID INTEGER PRIMARY KEY, " +
        "$WEATHER_JSON TEXT, " +
        "$WEATHER_TIMESTAMP INTEGER" +
        ");"

const val CREATE_TABLE_RECORDINGS =
    "CREATE TABLE if not exists $TABLE_RECORDINGS ( " +
        "$RECORDING_ID INTEGER PRIMARY KEY, " +
        "$RECORDING_NAME text " +
        ");"

const val CREATE_TABLE_ACC =
    "CREATE TABLE if not exists $TABLE_REAL_TIME_ACC ( " +
        "$ID integer PRIMARY KEY, " +
        "$RECORDING_ID INTEGER, " +
        "$TIMESTAMP date, " +
        "$X real , " +
        "$Y real , " +
        "$Z real , " +
        "$ACCURACY real )"

const val CREATE_TABLE_MAG =
    "CREATE TABLE if not exists $TABLE_REAL_TIME_MAG ( " +
        "$ID integer PRIMARY KEY, " +
        "$RECORDING_ID INTEGER, " +
        "$TIMESTAMP date, " +
        "$X real , " +
        "$Y real , " +
        "$Z real , " +
        "$ACCURACY real )"

const val CREATE_TABLE_GYRO =
    "CREATE TABLE if not exists $TABLE_REAL_TIME_GYRO ( " +
        "$ID integer PRIMARY KEY, " +
        "$RECORDING_ID INTEGER, " +
        "$TIMESTAMP date, " +
        "$X real , " +
        "$Y real , " +
        "$Z real , " +
        "$ACCURACY real )"

const val CREATE_TABLE_ROT =
    "CREATE TABLE if not exists $TABLE_REAL_TIME_ROT ( " +
        "$ID integer PRIMARY KEY, " +
        "$RECORDING_ID INTEGER, " +
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
    "CREATE TABLE if not exists $TABLE_GEOFENCE ( " +
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