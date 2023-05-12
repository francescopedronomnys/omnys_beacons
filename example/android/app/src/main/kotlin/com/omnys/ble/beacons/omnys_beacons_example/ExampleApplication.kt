package com.omnys.ble.beacons.omnys_beacons_example

import android.content.Intent
import com.omnys.ble.beacons.omnys_beacons.OmnysBeaconsPlugin
import com.omnys.ble.beacons.omnys_beacons.data.BackgroundMonitoringEvent
import io.flutter.app.FlutterApplication

class ExampleApplication :  FlutterApplication() {
    override fun onCreate() {
        super.onCreate()
        OmnysBeaconsPlugin.init(this, object : OmnysBeaconsPlugin.BackgroundMonitoringCallback {
            override fun onBackgroundMonitoringEvent(event: BackgroundMonitoringEvent): Boolean {
                val intent = Intent(this@ExampleApplication, MainActivity::class.java)
                startActivity(intent)
                return true
            }
        })
    }
}