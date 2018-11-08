package de.leo.smartTrigger.datacollector.datacollection.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.*

/**
 * Created by Leo on 30.01.2018
 */
class MotionSensors(context: Context) {

    internal val GYRO = true
    internal val ACC = true
    internal val ROT = true
    internal val MAG = true


    internal var mSensorManager: SensorManager
    internal var mSensorAcc: Sensor?
    internal var mSensorGyro: Sensor?
    internal var mSensorRotation: Sensor?
    internal var mSensorMagnetometer: Sensor?
    private var accData: ArrayDeque<Pair<Long, FloatArray>> = ArrayDeque(INITIAL_NUMBER_OF_VALUES)
    private var rotData: ArrayDeque<Pair<Long, FloatArray>> = ArrayDeque(INITIAL_NUMBER_OF_VALUES)
    private var magData: ArrayDeque<Pair<Long, FloatArray>> = ArrayDeque(INITIAL_NUMBER_OF_VALUES)
    private var gyroData: ArrayDeque<Pair<Long, FloatArray>> = ArrayDeque(INITIAL_NUMBER_OF_VALUES)
    internal val myListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER   -> accData.addLast(
                    Pair(System.currentTimeMillis(), event.values.copyOf()))
                Sensor.TYPE_GYROSCOPE       -> gyroData.addLast(
                    Pair(System.currentTimeMillis(), event.values.copyOf()))
                Sensor.TYPE_ROTATION_VECTOR -> rotData.addLast(
                    Pair(System.currentTimeMillis(), event.values.copyOf()))
                Sensor.TYPE_MAGNETIC_FIELD  -> magData.addLast(
                    Pair(System.currentTimeMillis(), event.values.copyOf()))
                else                        -> {
                }
            }
        }
    }

    fun readAccData(): ArrayDeque<Pair<Long, FloatArray>> {
        val tmp = accData
        accData = ArrayDeque(INITIAL_NUMBER_OF_VALUES)
        return tmp
    }

    fun readRotData(): ArrayDeque<Pair<Long, FloatArray>> {
        val tmp = rotData
        rotData = ArrayDeque(INITIAL_NUMBER_OF_VALUES)
        return tmp
    }

    fun readGyroData(): ArrayDeque<Pair<Long, FloatArray>> {
        val tmp = gyroData
        gyroData = ArrayDeque(INITIAL_NUMBER_OF_VALUES)
        return tmp
    }

    fun readMagData(): ArrayDeque<Pair<Long, FloatArray>> {
        val tmp = magData
        magData = ArrayDeque(INITIAL_NUMBER_OF_VALUES)
        return tmp
    }


    init {
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensorMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (GYRO) mSensorManager.registerListener(myListener, mSensorGyro, SensorManager
            .SENSOR_DELAY_GAME)
        if (ACC) mSensorManager.registerListener(myListener, mSensorAcc, SensorManager
            .SENSOR_DELAY_GAME)
        if (ROT) mSensorManager.registerListener(myListener, mSensorRotation, SensorManager
            .SENSOR_DELAY_GAME)
        if (MAG) mSensorManager.registerListener(myListener, mSensorMagnetometer, SensorManager
            .SENSOR_DELAY_GAME)
    }

    fun stopMotionSensor() {
        mSensorManager.unregisterListener(myListener)
    }

    companion object {
        //260 with lots of margin
        private val INITIAL_NUMBER_OF_VALUES = 300

    }

}
