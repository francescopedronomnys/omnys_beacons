//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

package com.omnys.ble.beacons.omnys_beacons.channel


import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import com.omnys.ble.beacons.omnys_beacons.StreamsChannel
import com.omnys.ble.beacons.omnys_beacons.data.Configuration
import com.omnys.ble.beacons.omnys_beacons.data.Permission
import com.omnys.ble.beacons.omnys_beacons.data.RegionModel
import com.omnys.ble.beacons.omnys_beacons.logic.BeaconsClient
import com.omnys.ble.beacons.omnys_beacons.logic.PermissionClient
import com.omnys.ble.beacons.omnys_beacons.logic.SharedMonitor
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class Channels(
    private val permissionClient: PermissionClient, private val beaconsClient: BeaconsClient
) : MethodChannel.MethodCallHandler {
    private var context : Context? = null;
    private var methodChannel: MethodChannel? = null
    private var rangingChannel: StreamsChannel? = null

    fun register(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext;
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "omnys_beacons")
        methodChannel!!.setMethodCallHandler(this)

        val monitoringChannel =
            StreamsChannel(flutterPluginBinding.binaryMessenger, "omnys_beacons/monitoring")
        monitoringChannel.setStreamHandlerFactory {
            Handler(
                beaconsClient, BeaconsClient.Operation.Kind.Monitoring
            )
        }

        val backgroundMonitoringChannel = StreamsChannel(
            flutterPluginBinding.binaryMessenger, "omnys_beacons/backgroundMonitoring"
        )
        backgroundMonitoringChannel.setStreamHandlerFactory {
            BackgroundMonitoringHandler(
                beaconsClient
            )
        }
    }

    fun unregister(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel?.setMethodCallHandler(null)
        rangingChannel?.setStreamHandlerFactory(null)

        context = null;
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result): Unit {
        Log.d("Channels", "Method ${call.method} called")
        when (call.method) {
            "checkStatus" -> checkStatus(Codec.decodeStatusRequest(call.arguments), result)
            "requestPermission" -> requestPermission(Codec.decodePermission(call.arguments), result)
            "configure" -> configure(Codec.decodeConfiguration(call.arguments), result)
            "startMonitoring" -> startMonitoring(Codec.decodeDataRequest(call.arguments), result)
            "stopMonitoring" -> stopMonitoring(Codec.decodeRegion(call.arguments), result)
            "registerBackgroundCallback" -> registerBackgroundCallback(Codec.decodeRegisterBackgroundCallback(call.arguments), result)
            else -> result.notImplemented()
        }
    }

    private fun checkStatus(request: StatusRequest, result: MethodChannel.Result) {
        result.success(permissionClient.check(request.permission).result)
    }

    private fun requestPermission(permission: Permission, result: MethodChannel.Result) {
        GlobalScope.launch(Dispatchers.Main) {
            result.success(permissionClient.check(permission).result)
        }
    }

    private fun configure(configuration: Configuration, result: MethodChannel.Result) {
        // save callback handle on storage
        context!!.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            .edit()
            .putLong(CALLBACK_DISPATCHER_HANDLE_KEY, configuration.callbackHandle)
            .apply()

        beaconsClient.configure(configuration.settings)
        result.success(null)
    }

    private fun startMonitoring(request: DataRequest, result: MethodChannel.Result) {
        GlobalScope.launch(Dispatchers.Main) {
            result.success(beaconsClient.startMonitoring(request))
        }
    }

    private fun stopMonitoring(region: RegionModel, result: MethodChannel.Result) {
        beaconsClient.stopMonitoring(region)
        result.success(null)
    }

    private fun registerBackgroundCallback(backgroundCallbackHandle: Long, result: MethodChannel.Result) {
        beaconsClient.registerBackgroundCallback(backgroundCallbackHandle)
        result.success(null)
    }

    class Handler(
        private val beaconsClient: BeaconsClient, private val kind: BeaconsClient.Operation.Kind
    ) : EventChannel.StreamHandler {

        private val uiThreadHandler: android.os.Handler = android.os.Handler(
            Looper.getMainLooper()
        )
        private var request: BeaconsClient.Operation? = null

        override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink) {
            val dataRequest = Codec.decodeDataRequest(arguments)
            request = BeaconsClient.Operation(
                kind, dataRequest.region, dataRequest.inBackground
            ) { result ->
                uiThreadHandler.post { eventSink.success(Codec.encodeResult(result)) }
            }
            beaconsClient.addRequest(request!!, dataRequest.permission)
        }

        override fun onCancel(arguments: Any?) {
            beaconsClient.removeRequest(request!!)
            request = null
        }
    }

    class BackgroundMonitoringHandler(private val beaconsClient: BeaconsClient) :
        EventChannel.StreamHandler {

        private var listener: SharedMonitor.BackgroundListener? = null

        override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink) {
            listener = SharedMonitor.BackgroundListener { result ->
                eventSink.success(Codec.encodeBackgroundMonitoringEvent(result))
            }
            beaconsClient.addBackgroundMonitoringListener(listener!!)
        }

        override fun onCancel(arguments: Any?) {
            beaconsClient.removeBackgroundMonitoringListener(listener!!)
            listener = null
        }
    }

    companion object {
        @JvmStatic
        private val TAG = "GeofencingPlugin"

        @JvmStatic
        val SHARED_PREFERENCES_KEY = "omnys_beacons_plugin_cache"

        @JvmStatic
        val CALLBACK_HANDLE_KEY = "callback_handle"

        @JvmStatic
        val CALLBACK_DISPATCHER_HANDLE_KEY = "callback_dispatch_handler"
    }
}