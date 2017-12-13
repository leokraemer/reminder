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
class ActivityRecord(val cursor: Cursor) {
    val recordLength: Int
    var beginTime: LocalTime
    var timestamps: MutableList<LocalTime> = mutableListOf()
    var endTime: LocalTime
    val weathers = mutableListOf<String>()
    var locations: MutableSet<String> = mutableSetOf()
    var activities: MutableList<String> = mutableListOf()
    var geolocations: MutableList<LatLng> = mutableListOf()
    var ambientSound: MutableList<Double> = mutableListOf()
    var preassure: MutableList<Double> = mutableListOf()
    var accelerometerData: MutableList<FloatArray> = mutableListOf()
    var gyroscopData: MutableList<FloatArray> = mutableListOf()
    var orientationData: MutableList<FloatArray> = mutableListOf()
    var magnetData: MutableList<FloatArray> = mutableListOf()
    var weather: Weather?

    init {
        recordLength = cursor.count
        cursor.moveToFirst()
        beginTime = TimeUtils.getDateFromString(cursor.getString(cursor.getColumnIndex(TIMESTAMP))).toLocalTime()
        do {
            timestamps.add(TimeUtils.getDateFromString(cursor.getString(cursor.getColumnIndex(
                    TIMESTAMP))).toLocalTime())
            preassure.add(cursor.getDouble(cursor.getColumnIndex(AIR_PRESSURE)))
            activities.add(cursor.getString(cursor.getColumnIndex(ACTIVITY)))
            ambientSound.add(cursor.getDouble(cursor.getColumnIndex(ABIENT_SOUND)))
            geolocations.add(
                    LatLng(
                            cursor.getDouble(cursor.getColumnIndex(GPSlat)),
                            cursor.getDouble(cursor.getColumnIndex(GPSlng)))
                            )
            locations.add(cursor.getString(cursor.getColumnIndex(LOCATION)))
            weathers.add(cursor.getString(cursor.getColumnIndex(WEATHER_JSON)))
            addAccMagOriGyroData()
        } while (cursor.moveToNext())
        cursor.moveToLast()
        endTime = TimeUtils.getDateFromString(cursor.getString(cursor.getColumnIndex(TIMESTAMP))).toLocalTime()
        weather = getMostPrevalentWeather(weathers)
        cursor.close()
    }

    private fun addAccMagOriGyroData() {
        val accelerometerDataPoint = FloatArray(3)
        val gyroscopeDataPoint = FloatArray(3)
        val orientationDataPoint = FloatArray(3)
        val magnetometerDataPoint = FloatArray(3)
        accelerometerDataPoint[0] = cursor.getFloat(cursor.getColumnIndex(ACCELEROMETER_X))
        accelerometerDataPoint[1] = cursor.getFloat(cursor.getColumnIndex(ACCELEROMETER_Y))
        accelerometerDataPoint[2] = cursor.getFloat(cursor.getColumnIndex(ACCELEROMETER_Z))
        gyroscopeDataPoint[0] = cursor.getFloat(cursor.getColumnIndex(GYRO_X))
        gyroscopeDataPoint[1] = cursor.getFloat(cursor.getColumnIndex(GYRO_Y))
        gyroscopeDataPoint[2] = cursor.getFloat(cursor.getColumnIndex(GYRO_Z))
        orientationDataPoint[0] = cursor.getFloat(cursor.getColumnIndex(AZIMUTH))
        orientationDataPoint[1] = cursor.getFloat(cursor.getColumnIndex(PITCH))
        orientationDataPoint[2] = cursor.getFloat(cursor.getColumnIndex(ROLL))
        magnetometerDataPoint[0] = cursor.getFloat(cursor.getColumnIndex(MAG_X))
        magnetometerDataPoint[1] = cursor.getFloat(cursor.getColumnIndex(MAG_Y))
        magnetometerDataPoint[2] = cursor.getFloat(cursor.getColumnIndex(MAG_Z))
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