package de.leo.fingerprint.datacollector

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import de.leo.fingerprint.datacollector.jitai.ActivityTrigger
import de.leo.fingerprint.datacollector.datacollection.models.SensorDataSet
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.DetectedActivity.IN_VEHICLE
import com.google.android.gms.location.DetectedActivity.ON_FOOT
import de.leo.fingerprint.datacollector.datacollection.models.WifiInfo
import de.leo.fingerprint.datacollector.jitai.WifiTrigger
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


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
        val trigger = WifiTrigger(WifiInfo(BSSID, RSSI, SSID, IP, NETWORKID))
        val sensorData = SensorDataSet(System.currentTimeMillis(), "test")
        Assert.assertFalse(trigger.check(context, sensorData))
        sensorData.wifiInformation = listOf(WifiInfo(BSSID, RSSI, SSID, IP, NETWORKID))
        Assert.assertTrue(trigger.check(context, sensorData))
    }

    @Test
    fun testWifiTriggerRSSI() {
        val trigger = WifiTrigger(WifiInfo(BSSID, RSSI, SSID, IP, NETWORKID), highRssiThreshold)
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