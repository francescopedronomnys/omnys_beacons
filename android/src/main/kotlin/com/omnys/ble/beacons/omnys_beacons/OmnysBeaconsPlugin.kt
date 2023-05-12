package com.omnys.ble.beacons.omnys_beacons

import android.app.Application
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.omnys.ble.beacons.omnys_beacons.channel.Channels
import com.omnys.ble.beacons.omnys_beacons.data.BackgroundMonitoringEvent
import com.omnys.ble.beacons.omnys_beacons.logic.BeaconsClient
import com.omnys.ble.beacons.omnys_beacons.logic.PermissionClient
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter;


/** OmnysBeaconsPlugin */
class OmnysBeaconsPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private val permissionClient = PermissionClient()
    private val beaconClient = BeaconsClient(permissionClient)
    private val channels = Channels(permissionClient, beaconClient)

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channels.register(flutterPluginBinding)
        beaconClient.bind(flutterPluginBinding.applicationContext)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {

    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channels.unregister(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d("OmnysBeaconPlugin", "onAttachedToActivity")
        bind(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        unbind()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        bind(binding)
    }

    override fun onDetachedFromActivity() {
        unbind()
    }

    private fun bind(binding: ActivityPluginBinding) {
        binding.addRequestPermissionsResultListener(permissionClient.listener)
        beaconClient.bind(binding.activity)
        permissionClient.bind(binding.activity)
        val lifecycle: Lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                beaconClient.resume()
            }

            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                beaconClient.pause()

            }
        })
    }

    private fun unbind() {
        beaconClient.unbind()
        permissionClient.unbind()
    }

    companion object {

        fun init(application: Application, callback: BackgroundMonitoringCallback) {
            BeaconsClient.init(application, callback)
        }
    }

    object Intents {
        const val PermissionRequestId = 92749
    }

    interface BackgroundMonitoringCallback {

        /**
         * Callback on background monitoring events
         *
         * @return true if background mode will end with this event, for instance if an activity has been started.
         * Otherwise return false to continue receiving background events on the current callback
         */
        fun onBackgroundMonitoringEvent(event: BackgroundMonitoringEvent): Boolean
    }
}
