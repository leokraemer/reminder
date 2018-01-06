package com.example.leo.datacollector.activityRecording

import android.database.Cursor
import com.example.leo.datacollector.database.*
import com.example.leo.datacollector.datacollection.sensors.WeatherCaller
import com.example.leo.datacollector.models.Weather
import com.example.leo.datacollector.utils.TimeUtils
import com.google.android.gms.maps.model.LatLng
import org.threeten.bp.LocalTime

/**
 * Created by Leo on 29.11.2017.
 */
class ActivityRecord(var name : String, val cursor: Cursor) {
    val recordLength: Int
    var beginTime: Long
    var timestamps: MutableList<Long> = mutableListOf()
    var endTime: Long
    val weathers = mutableListOf<String>()
    val wifis = mutableListOf<String>()
    val bluetooth = mutableListOf<String>()
    var locations: MutableSet<String> = mutableSetOf()
    var activities: MutableList<String> = mutableListOf()
    var geolocations: MutableList<LatLng> = mutableListOf()
    var ambientSound: MutableList<Double> = mutableListOf()
    var ambientLight: MutableList<Double> = mutableListOf()
    var proximity: MutableList<Double> = mutableListOf()
    var pressure: MutableList<Double> = mutableListOf()
    var screenState: MutableList<Int> = mutableListOf()
    var steps: MutableList<Double> = mutableListOf()
    var accelerometerData: MutableList<FloatArray> = mutableListOf()
    var rawAccelerometerData: MutableList<FloatArray> = mutableListOf()
    var gyroscopData: MutableList<FloatArray> = mutableListOf()
    var orientationData: MutableList<FloatArray> = mutableListOf()
    var magnetData: MutableList<FloatArray> = mutableListOf()
    var weather: Weather?

    init {
        recordLength = cursor.count
        cursor.moveToFirst()
        beginTime = cursor.getLong(cursor.getColumnIndex(TIMESTAMP))
        do {
            timestamps.add(cursor.getLong(cursor.getColumnIndex(TIMESTAMP)))
            pressure.add(cursor.getDouble(cursor.getColumnIndex(AIR_PRESSURE)))
            screenState.add(cursor.getInt(cursor.getColumnIndex(SCREEN_STATE)))
            activities.add(cursor.getString(cursor.getColumnIndex(ACTIVITY)))
            ambientSound.add(cursor.getDouble(cursor.getColumnIndex(ABIENT_SOUND)))
            ambientLight.add(cursor.getDouble(cursor.getColumnIndex(AMBIENT_LIGHT)))
            proximity.add(cursor.getDouble(cursor.getColumnIndex(PROXIMITY)))
            geolocations.add(
                    LatLng(
                            cursor.getDouble(cursor.getColumnIndex(GPSlat)),
                            cursor.getDouble(cursor.getColumnIndex(GPSlng)))
                            )
            steps.add((cursor.getDouble(cursor.getColumnIndex(STEPS))))
            locations.add(cursor.getString(cursor.getColumnIndex(LOCATION)))
            weathers.add(cursor.getString(cursor.getColumnIndex(WEATHER_JSON)))
            wifis.add(cursor.getString(cursor.getColumnIndex(WIFI_NAME)))
            bluetooth.add(cursor.getString(cursor.getColumnIndex(WIFI_NAME)))
            addAccMagOriGyroData()
        } while (cursor.moveToNext())
        cursor.moveToLast()
        endTime = cursor.getLong(cursor.getColumnIndex(TIMESTAMP))
        weather = getMostPrevalentWeather(weathers)
        cursor.close()
    }

    private fun addAccMagOriGyroData() {
        val accelerometerDataPoint = FloatArray(3)
        val rawAccelerometerDataPoint = FloatArray(3)
        val gyroscopeDataPoint = FloatArray(3)
        val orientationDataPoint = FloatArray(3)
        val magnetometerDataPoint = FloatArray(3)
        accelerometerDataPoint[0] = cursor.getFloat(cursor.getColumnIndex(ACCELEROMETER_X))
        accelerometerDataPoint[1] = cursor.getFloat(cursor.getColumnIndex(ACCELEROMETER_Y))
        accelerometerDataPoint[2] = cursor.getFloat(cursor.getColumnIndex(ACCELEROMETER_Z))
        rawAccelerometerDataPoint[0] = cursor.getFloat(cursor.getColumnIndex(RAW_ACCELEROMETER_X))
        rawAccelerometerDataPoint[1] = cursor.getFloat(cursor.getColumnIndex(RAW_ACCELEROMETER_Y))
        rawAccelerometerDataPoint[2] = cursor.getFloat(cursor.getColumnIndex(RAW_ACCELEROMETER_Z))
        gyroscopeDataPoint[0] = cursor.getFloat(cursor.getColumnIndex(GYRO_X))
        gyroscopeDataPoint[1] = cursor.getFloat(cursor.getColumnIndex(GYRO_Y))
        gyroscopeDataPoint[2] = cursor.getFloat(cursor.getColumnIndex(GYRO_Z))
        orientationDataPoint[0] = cursor.getFloat(cursor.getColumnIndex(AZIMUTH))
        orientationDataPoint[1] = cursor.getFloat(cursor.getColumnIndex(PITCH))
        orientationDataPoint[2] = cursor.getFloat(cursor.getColumnIndex(ROLL))
        magnetometerDataPoint[0] = cursor.getFloat(cursor.getColumnIndex(MAG_X))
        magnetometerDataPoint[1] = cursor.getFloat(cursor.getColumnIndex(MAG_Y))
        magnetometerDataPoint[2] = cursor.getFloat(cursor.getColumnIndex(MAG_Z))
        rawAccelerometerData.add(rawAccelerometerDataPoint)
        accelerometerData.add(accelerometerDataPoint)
        gyroscopData.add(gyroscopeDataPoint)
        orientationData.add(orientationDataPoint)
        magnetData.add(magnetometerDataPoint)
    }

    private fun getMostPrevalentWeather(weather: List<String>): Weather? =
            getWeatherList(weather).first()


    //get list of weathers, sorted by most appearances
    private fun getWeatherList(weather: List<String>): List<Weather> {
        val list: MutableList<Pair<String, Int>> = mutableListOf()
        weather.fold(list, { list, next ->
            val indexOfFirst = list.indexOfFirst { it.component1() == next }
            if (indexOfFirst > -1)
                list.get(indexOfFirst).component2().inc()
            else
                list.add(Pair(next, 1))
            list
        })
        list.sortBy { it.second }
        return list.map { i -> WeatherCaller.fromJSON(i.first) }
    }
}