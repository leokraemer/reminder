package de.leo.smartTrigger.datacollector.jitai

import android.content.Context
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.datacollection.models.Weather
import java.util.concurrent.TimeUnit

/**
 * Created by Leo on 11.01.2018.
 */
class WeatherTrigger : Trigger {
    val weather: Weather

    override fun reset(sensorData: SensorDataSet) {
        //noop
    }

    constructor(weather: Weather) {
        this.weather = weather
    }

    constructor(context: Context, weather_id: Long) {
        this.weather = JitaiDatabase.getInstance(context).getWeather(weather_id)!!
    }

    override fun check(context: Context, sensorData: SensorDataSet): Boolean {
        //represents any weather
        if (weather.currentCondition.weatherId == -1)
            return true
        return compare(weather.currentCondition.weatherId,
                       JitaiDatabase.getInstance(context)
                           .getWeather(sensorData.weather!!)!!
                           .currentCondition.weatherId)
    }

    fun compare(thisWeatherId: Int, otherWeatherId: Int): Boolean {
        //bad weather as baseline -> other must also be bad
        if (thisWeatherId < 800)
            return otherWeatherId < 800
        //good weather -> other must also be good
        if (thisWeatherId >= 800)
            return otherWeatherId >= 800
        return false
    }

    override fun toString(): String {
        var weatherCondition = "jedem Wetter"
        if (-1 < weather.currentCondition.weatherId && weather.currentCondition.weatherId < 800)
            weatherCondition = "schlechtem Wetter"
        //good weather -> other must also be good
        if (weather.currentCondition.weatherId >= 800)
            weatherCondition = "gutem Wetter"
        return "Bei $weatherCondition"
    }

    //wants to be checked again immediately
    override fun nextUpdate(): Long = 0

}