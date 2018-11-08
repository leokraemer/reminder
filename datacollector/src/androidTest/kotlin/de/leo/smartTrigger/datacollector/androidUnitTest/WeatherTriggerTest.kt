package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import android.location.Location
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.jitai.WeatherTrigger
import junit.framework.Assert
import org.junit.After
import org.junit.Before
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

    lateinit var context : Context
    lateinit var sensorDataSet : SensorDataSet
    lateinit var db : JitaiDatabase

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        db = JitaiDatabase.getInstance(context)
        db.close()
        context.deleteDatabase(JitaiDatabase.NAME)
        db = JitaiDatabase.getInstance(context)
        db.enterWeather(goodWeather, 1L)
        db.enterWeather(badWeather, 2L)
        sensorDataSet = SensorDataSet(System.currentTimeMillis(),
                                                                                             "testWeather")
        sensorDataSet.gps = Location("")
    }

    @After
    fun tearDown(){
        db.close()
    }

    @Test
    fun simpleWeatherTriggerTest() {
        val weatherTrigger = WeatherTrigger( context, 1L)
        sensorDataSet.weather = 1L
        Assert.assertTrue(weatherTrigger.check(context, sensorDataSet))
        sensorDataSet.weather = 2L
        Assert.assertFalse(weatherTrigger.check(context, sensorDataSet))
    }

    @Test
    fun updateWeatherTriggerTest() {
        var weatherTrigger = WeatherTrigger( context,1L)
        sensorDataSet.weather = 1L
        Assert.assertTrue(weatherTrigger.check(context, sensorDataSet))
        sensorDataSet.weather = 2L
        weatherTrigger = WeatherTrigger( context,2L)
        Assert.assertTrue(weatherTrigger.check(context, sensorDataSet))
    }
}