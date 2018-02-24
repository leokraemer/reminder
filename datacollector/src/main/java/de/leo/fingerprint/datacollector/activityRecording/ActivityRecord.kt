package de.leo.fingerprint.datacollector.activityRecording

import android.database.Cursor
import de.leo.fingerprint.datacollector.database.*
import de.leo.fingerprint.datacollector.datacollection.sensors.WeatherCaller
import de.leo.fingerprint.datacollector.models.SensorDataSet
import de.leo.fingerprint.datacollector.models.Weather
import com.google.android.gms.maps.model.LatLng
import java.util.*

/**
 * Created by Leo on 29.11.2017.
 */
class ActivityRecord {
    var name: String
    var recordLength: Int
    var beginTime: Long
    var timestamps: MutableList<Long> = mutableListOf()
    var endTime: Long
    val weathers = mutableListOf<String>()
    val wifis = mutableListOf<String>()
    val bluetooth = mutableListOf<String>()
    //ODO evaluate if set is correct data type
    var locations: MutableSet<String> = mutableSetOf()
    var activities: MutableCollection<String> = mutableListOf()
    var geolocations: MutableCollection<LatLng> = mutableListOf()
    var ambientSound: MutableCollection<Double> = mutableListOf()

    var screenState: MutableList<Int> = mutableListOf()
    var steps: MutableList<Double> = mutableListOf()

    //realtime with own "timestamps" (the timestamps do not correspond to system time)
    var ambientLight: MutableCollection<Pair<Long, Double>> = mutableListOf()
    var proximity: MutableCollection<Pair<Long, Double>> = mutableListOf()
    var pressure: MutableCollection<Pair<Long, Double>> = mutableListOf()
    var accelerometerData: MutableCollection<Pair<Long, FloatArray>> = mutableListOf()
    var gyroscopData: MutableCollection<Pair<Long, FloatArray>> = mutableListOf()
    var orientationData: MutableCollection<Pair<Long, FloatArray>> = mutableListOf()
    var magnetData: MutableCollection<Pair<Long, FloatArray>> = mutableListOf()
    var weather: Weather?

    constructor(name: String, cursor: Cursor) {
        this.name = name
        recordLength = cursor.count
        cursor.moveToFirst()
        beginTime = cursor.getLong(cursor.getColumnIndex(TIMESTAMP))
        do {
            timestamps.add(cursor.getLong(cursor.getColumnIndex(TIMESTAMP)))
            screenState.add(cursor.getInt(cursor.getColumnIndex(SCREEN_STATE)))
            activities.add(cursor.getString(cursor.getColumnIndex(ACTIVITY)))
            ambientSound.add(cursor.getDouble(cursor.getColumnIndex(ABIENT_SOUND)))
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
        } while (cursor.moveToNext())
        cursor.moveToLast()
        endTime = cursor.getLong(cursor.getColumnIndex(TIMESTAMP))
        weather = getMostPrevalentWeather(weathers)
        cursor.close()
    }

    constructor(sensorDataSet: SensorDataSet,
                ambientLight: ArrayDeque<Pair<Long, Double>>,
                proximity: ArrayDeque<Pair<Long, Double>>,
                pressure: ArrayDeque<Pair<Long, Double>>,
                acc: ArrayDeque<Pair<Long, FloatArray>>,
                gyroscope: ArrayDeque<Pair<Long, FloatArray>>,
                orientation: ArrayDeque<Pair<Long, FloatArray>>,
                mag: ArrayDeque<Pair<Long, FloatArray>>) {
        this.name = "dynamically created record"
        recordLength = 1
        beginTime = sensorDataSet.time
        endTime = beginTime
        timestamps.add(beginTime)
        //TODO weathers
        weather = null
        wifis.add(sensorDataSet.wifiName!!)
        //TODO bluetooth
        locations.add(sensorDataSet.location!!)
        activities.add(sensorDataSet.activity.toString())
        geolocations.add(LatLng(sensorDataSet.gps!!.latitude, sensorDataSet.gps!!.longitude))
        ambientSound.add(sensorDataSet.ambientSound!!)
        if (sensorDataSet.screenState)
            screenState.add(1)
        else
            screenState.add(0)
        steps.add(sensorDataSet.totalStepsToday!!.toDouble())
        this.ambientLight = ambientLight
        this.proximity = proximity
        this.pressure = pressure
        this.accelerometerData = acc
        this.gyroscopData = gyroscope
        this.orientationData = orientation
        this.magnetData = mag
    }

    fun update(sensorDataSet: SensorDataSet,
               ambientLight: ArrayDeque<Pair<Long, Double>>,
               proximity: ArrayDeque<Pair<Long, Double>>,
               pressure: ArrayDeque<Pair<Long, Double>>,
               acc: ArrayDeque<Pair<Long, FloatArray>>,
               gyroscope: ArrayDeque<Pair<Long, FloatArray>>,
               orientation: ArrayDeque<Pair<Long, FloatArray>>,
               mag: ArrayDeque<Pair<Long, FloatArray>>) {
        recordLength++
        endTime = sensorDataSet.time
        timestamps.add(sensorDataSet.time)
        //TODO weathers
        weather = null
        wifis.add(sensorDataSet.wifiName!!)
        //TODO bluetooth
        locations.add(sensorDataSet.location!!)
        activities.add(sensorDataSet.activity.toString())
        geolocations.add(LatLng(sensorDataSet.gps!!.latitude, sensorDataSet.gps!!.longitude))
        ambientSound.add(sensorDataSet.ambientSound!!)
        if (sensorDataSet.screenState)
            screenState.add(1)
        else
            screenState.add(0)
        steps.add(sensorDataSet.totalStepsToday!!.toDouble())
        this.ambientLight.addAll(ambientLight)
        this.proximity.addAll(proximity)
        this.pressure.addAll(pressure)
        this.accelerometerData.addAll(acc)
        this.gyroscopData.addAll(gyroscope)
        this.orientationData.addAll(orientation)
        this.magnetData.addAll(mag)
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