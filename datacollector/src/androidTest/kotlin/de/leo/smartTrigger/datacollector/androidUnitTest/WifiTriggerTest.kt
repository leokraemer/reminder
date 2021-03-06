package de.leo.smartTrigger.datacollector.androidUnitTest

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.datacollection.models.WifiInfo
import de.leo.smartTrigger.datacollector.jitai.MyWifiGeofence
import de.leo.smartTrigger.datacollector.jitai.WifiTrigger
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class WifiTriggerTest {

    val BSSID = "00:00:00:00"
    val RSSI = 10
    val SSID = "a Network"
    val IP = "127.0.0.1"
    val NETWORKID = 5

    val highRssiThreshold = 50

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
    }


    @Test
    fun testWifiTrigger() {
        //default rssi
        val trigger = WifiTrigger(MyWifiGeofence(bssid = BSSID))
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.wifiInformation = listOf(WifiInfo(BSSID, RSSI, SSID, IP, NETWORKID))
        Assert.assertTrue(trigger.check(context, sensorData))
    }

    @Test
    fun testWifiTriggerRSSI() {
        val trigger = WifiTrigger(MyWifiGeofence(bssid = BSSID, rssi = highRssiThreshold))
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        //lower than threshold rssi
        sensorData.wifiInformation = listOf(WifiInfo(BSSID, highRssiThreshold - 1, SSID, IP,
                                                     NETWORKID))
        Assert.assertFalse(trigger.check(context, sensorData))
        //higher than threshold rssi
        sensorData.wifiInformation = listOf(WifiInfo(BSSID, highRssiThreshold, SSID, IP,
                                                     NETWORKID))
        Assert.assertTrue(trigger.check(context, sensorData))
    }
}