package de.leo.smartTrigger.datacollector.datacollection.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.*

/**
 * Created by Yunlong on 4/23/2016.
 */
class MyEnvironmentSensor(private val context: Context) : SensorEventListener {
    private var mSensorManager: SensorManager? = null
    private var mLight: Sensor? = null
    private var mAmbientTemperature: Sensor? = null
    private var mAmbientPressure: Sensor? = null
    private var mRelativeHumidity: Sensor? = null
    private var mProximity: Sensor? = null
    private var light = ArrayDeque<Pair<Long, Float>>()
    private var temperature = ArrayDeque<Pair<Long, Float>>()
    private var pressure = ArrayDeque<Pair<Long, Float>>()
    private var humidity = ArrayDeque<Pair<Long, Float>>()
    private var proximity = ArrayDeque<Pair<Long, Float>>()

    init {
        initSensor()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT               -> light.addLast(Pair(System.currentTimeMillis(),
                                                                  event.values[0]))
            Sensor.TYPE_AMBIENT_TEMPERATURE -> temperature.addLast(Pair(System.currentTimeMillis(),
                                                                        event.values[0]))
            Sensor.TYPE_PRESSURE            -> pressure.addLast(Pair(System.currentTimeMillis(),
                                                                     event.values[0]))
            Sensor.TYPE_RELATIVE_HUMIDITY   -> humidity.addLast(Pair(System.currentTimeMillis(),
                                                                     event.values[0]))
            Sensor.TYPE_PROXIMITY           -> proximity.addLast(Pair(System.currentTimeMillis(),
                                                                      event.values[0]))
            else                            -> {
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    fun readLightData(): ArrayDeque<Pair<Long, Float>> {
        val tmp = light
        light = ArrayDeque(NUMBER_OF_VALUES)
        return tmp
    }

    fun readTemperatureData(): ArrayDeque<Pair<Long, Float>> {
        val tmp = temperature
        temperature = ArrayDeque(NUMBER_OF_VALUES)
        return tmp
    }

    fun readPressureData(): ArrayDeque<Pair<Long, Float>> {
        val tmp = pressure
        pressure = ArrayDeque(NUMBER_OF_VALUES)
        return tmp
    }

    fun readHumidityData(): ArrayDeque<Pair<Long, Float>> {
        val tmp = humidity
        humidity = ArrayDeque(NUMBER_OF_VALUES)
        return tmp
    }

    fun readProximityData(): ArrayDeque<Pair<Long, Float>> {
        val tmp = proximity
        proximity = ArrayDeque(NUMBER_OF_VALUES)
        return tmp
    }


    fun initSensor() {
        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLight = mSensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT)
        mAmbientTemperature = mSensorManager!!.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        mAmbientPressure = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE)
        mRelativeHumidity = mSensorManager!!.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        mProximity = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        registerListener()
    }

    fun registerListener() {
        mSensorManager!!.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this, mAmbientPressure, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this, mAmbientTemperature, SensorManager
            .SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this,
                                          mRelativeHumidity,
                                          SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopEnvironmentSensor() {
        mSensorManager!!.unregisterListener(this)
    }

    companion object {

        private val NUMBER_OF_VALUES = 100
    }
}
