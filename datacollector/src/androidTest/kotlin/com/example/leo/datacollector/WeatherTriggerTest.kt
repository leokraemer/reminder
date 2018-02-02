package com.example.leo.datacollector

import android.location.Location
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.jitai.WeatherTrigger
import com.example.leo.datacollector.models.SensorDataSet
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherTriggerTest {
    companion object {
        const val goodWeather = """{"coord":{"lon":139,"lat":35},
"sys":{"country":"JP","sunrise":1369769524,"sunset":1369821049},
"weather":[{"id":804,"main":"clouds","description":"overcast clouds","icon":"04n"}],
"main":{"temp":289.5,"humidity":89,"pressure":1013,"temp_min":287.04,"temp_max":292.04},
"wind":{"speed":7.31,"deg":187.002},
"rain":{"3h":0},
"clouds":{"all":92},
"dt":1369824698,
"id":1851632,
"name":"Shuzenji",
"cod":200}"""
        const val badWeather = """{"coord":{"lon":139,"lat":35},
"sys":{"country":"JP","sunrise":1369769524,"sunset":1369821049},
"weather":[{"id":781,"main":"tornado ","description":"overcast clouds","icon":"04n"}],
"main":{"temp":289.5,"humidity":89,"pressure":1013,"temp_min":287.04,"temp_max":292.04},
"wind":{"speed":7.31,"deg":187.002},
"rain":{"3h":0},
"clouds":{"all":92},
"dt":1369824698,
"id":1851632,
"name":"Shuzenji",
"cod":200}"""
    }

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<EntryActivity>(EntryActivity::class.java)
    lateinit var sensorDataSet : SensorDataSet
    lateinit var db : JitaiDatabase

    @Before
    fun setup() {
        getTargetContext().deleteDatabase(JitaiDatabase.NAME);
        db = JitaiDatabase.getInstance(activityRule.activity)
        db.enterWeather(goodWeather, 1L)
        db.enterWeather(badWeather, 2L)
        sensorDataSet = SensorDataSet(System.currentTimeMillis(), "testWeather")
        sensorDataSet.gps = Location("")
    }

    @After
    fun tearDown(){
        db.close()
    }

    @Test
    fun simpleWeatherTriggerTest() {
        val weatherTrigger = WeatherTrigger(activityRule.activity, 1L)
        sensorDataSet.weather = 1L
        Assert.assertTrue(weatherTrigger.check(sensorDataSet))
        sensorDataSet.weather = 2L
        Assert.assertFalse(weatherTrigger.check(sensorDataSet))
    }

    @Test
    fun updateWeatherTriggerTest() {
        val weatherTrigger = WeatherTrigger(activityRule.activity, 1L)
        sensorDataSet.weather = 1L
        Assert.assertTrue(weatherTrigger.check(sensorDataSet))
        sensorDataSet.weather = 2L
        weatherTrigger.update(sensorDataSet)
        Assert.assertTrue(weatherTrigger.check(sensorDataSet))
    }
}