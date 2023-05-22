//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

package com.omnys.ble.beacons.omnys_beacons.logic

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.omnys.ble.beacons.omnys_beacons.OmnysBeaconsPlugin
import com.omnys.ble.beacons.omnys_beacons.channel.Channels
import com.omnys.ble.beacons.omnys_beacons.channel.Codec
import com.omnys.ble.beacons.omnys_beacons.data.BackgroundMonitoringEvent
import com.omnys.ble.beacons.omnys_beacons.data.MonitoringState
import com.omnys.ble.beacons.omnys_beacons.data.RegionModel
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.loader.FlutterLoader
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterCallbackInformation
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region
import org.altbeacon.beacon.startup.BootstrapNotifier
import org.altbeacon.beacon.startup.RegionBootstrap
import java.util.UUID


class SharedMonitor(
    private val application: Application,
    private val flutterLoader: FlutterLoader,
    private val callback: OmnysBeaconsPlugin.BackgroundMonitoringCallback
) : MonitorNotifier, BootstrapNotifier, MethodChannel.MethodCallHandler {

    private val backgroundEvents = ArrayList<BackgroundMonitoringEvent>()
    private val backgroundListeners = ArrayList<BackgroundListener>()
    private var backgroundCallback: Long? = null
    private val regionBootstrap = RegionBootstrap(this, ArrayList())
    private var isBackgroundCallbackProcessed = false

    private var foregroundNotifier: MonitorNotifier? = null

    private var mBackgroundChannel: MethodChannel? = null

    init {
        initBackgroundChannel()

        // When monitor is running in background, it might not detect right monitoring event
        // probably happen only on android 8+ with the 15min scan delay
        // however by add/removing a random region, it triggers a monitoring scan directly
        // making background monitoring detection much faster
        // todo: discuss this with android-beacon-library's author
        val fakeRegion =
            Region(UUID.randomUUID().toString(), Identifier.fromUuid(UUID.randomUUID()), null, null)
        regionBootstrap.addRegion(fakeRegion)
        regionBootstrap.removeRegion(fakeRegion)
    }

    fun configure() {
        initBackgroundChannel()
    }

    private fun initBackgroundChannel() {
        if (mBackgroundChannel != null) {
            return;
        }
        if (sBackgroundFlutterEngine == null) {
            sBackgroundFlutterEngine = FlutterEngine(applicationContext)

            val callbackHandle = applicationContext.getSharedPreferences(
                Channels.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE
            ).getLong(Channels.CALLBACK_DISPATCHER_HANDLE_KEY, 0)
            if (callbackHandle == 0L) {
                Log.e(Tag, "No callback registered")
                return;
            }

            val callbackInfo = FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)
            if (callbackInfo == null) {
                Log.e(Tag, "Failed to find callback")
                return;

            }
            Log.i(Tag, "Starting SharedMonitor backgroundchannel...")

            val args = DartExecutor.DartCallback(
                applicationContext.assets, flutterLoader.findAppBundlePath(), callbackInfo
            )
            sBackgroundFlutterEngine!!.dartExecutor.executeDartCallback(args)
        }

        mBackgroundChannel = MethodChannel(
            sBackgroundFlutterEngine!!.dartExecutor.binaryMessenger,
            "omnys_beacons/background_dispatcher"
        )
        mBackgroundChannel!!.setMethodCallHandler(this)
    }

    fun attachForegroundNotifier(notifier: MonitorNotifier) {
        Log.d(Tag, "attach foreground notifier")
        this.foregroundNotifier = notifier

        // foreground notifier being attached means background logic is already processed
        // or not needed anymore
        isBackgroundCallbackProcessed = true
    }

    fun detachForegroundNotifier(notifier: MonitorNotifier) {
        Log.d(Tag, "detach foreground notifier")
        check(this.foregroundNotifier == notifier)
        this.foregroundNotifier = null
    }

    fun start(region: RegionModel) {
        regionBootstrap.addRegion(region.frameworkValue)
    }

    fun stop(region: RegionModel) {
        regionBootstrap.removeRegion(region.frameworkValue)
    }

    fun addBackgroundListener(listener: BackgroundListener) {
        backgroundListeners.add(listener)

        if (backgroundEvents.isNotEmpty()) {
            backgroundEvents.forEach { listener.callback(it) }
            backgroundEvents.clear()
        }
    }

    fun removeBackgroundListener(listener: BackgroundListener) {
        backgroundListeners.remove(listener)
    }

    fun registerBackgroundCallback(backgroundCallbackHandle: Long) {
        backgroundCallback = backgroundCallbackHandle
    }

    private fun notifyBackground(event: BackgroundMonitoringEvent) {
        Log.d(Tag, "notify background: ${event.type} / ${event.state}")

        if (!isBackgroundCallbackProcessed) {
            isBackgroundCallbackProcessed = callback.onBackgroundMonitoringEvent(event)
        }

        if (backgroundListeners.isNotEmpty()) {
            backgroundListeners.forEach { it.callback(event) }
        } else {
            backgroundEvents.add(event)
        }
    }

    override fun getApplicationContext(): Context {
        return application
    }

    override fun didDetermineStateForRegion(state: Int, region: Region) {
        Log.d(Tag, "didDetermineStateForRegion: ${region.uniqueId} [$state]")
        //TODO added by omnys to get a initial notification when app starts
        /* when(state) {
             MonitorNotifier.INSIDE -> didEnterRegion(region)
             MonitorNotifier.OUTSIDE -> didExitRegion(region)
         }*/
    }

    override fun didEnterRegion(region: Region) {
        Log.d(Tag, "didEnterRegion: ${region.uniqueId}")

        if (foregroundNotifier != null) {
            foregroundNotifier!!.didEnterRegion(region)
        } else {
            notifyBackground(
                BackgroundMonitoringEvent(
                    "didEnterRegion", RegionModel.parse(region), MonitoringState.EnterOrInside
                )
            )
        }

        //Background callback notifier
        backgroundCallback?.let { backgroundCallbackHandle ->
            val args = listOf<Any>(
                backgroundCallbackHandle, Codec.encodeBackgroundMonitoringEvent(
                    BackgroundMonitoringEvent(
                        "didEnterRegion", RegionModel.parse(region), MonitoringState.EnterOrInside
                    )
                )
            );
            Handler(Looper.getMainLooper()).post {
                mBackgroundChannel?.invokeMethod("didEnterRegion", args) ?: Log.d(
                    Tag, "Cannot invoke didEnterRegion for $region"
                )
            }
        }
    }

    override fun didExitRegion(region: Region) {
        Log.d(Tag, "didExitRegion: ${region.uniqueId}")

        if (foregroundNotifier != null) {
            foregroundNotifier!!.didExitRegion(region)
        } else {
            notifyBackground(
                BackgroundMonitoringEvent(
                    "didExitRegion", RegionModel.parse(region), MonitoringState.ExitOrOutside
                )
            )
        }

        //Background callback notifier
        backgroundCallback?.let { backgroundCallbackHandle ->
            val args = listOf<Any>(
                backgroundCallbackHandle, Codec.encodeBackgroundMonitoringEvent(
                    BackgroundMonitoringEvent(
                        "didExitRegion", RegionModel.parse(region), MonitoringState.ExitOrOutside
                    )
                )
            );
            Handler(Looper.getMainLooper()).post {
                mBackgroundChannel?.invokeMethod("didExitRegion", args) ?: Log.d(
                    Tag, "Cannot invoke didExitRegion for $region"
                )
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        //TODO here we receive callbacks from omnys_beacons/background_dispatcher
    }

    class BackgroundListener(val callback: (BackgroundMonitoringEvent) -> Unit)

    companion object {

        private const val Tag = "beacons monitoring"

        @JvmStatic
        private var sBackgroundFlutterEngine: FlutterEngine? = null
    }
}