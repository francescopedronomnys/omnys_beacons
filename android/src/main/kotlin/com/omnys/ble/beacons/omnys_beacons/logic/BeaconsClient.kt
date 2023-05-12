//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

package com.omnys.ble.beacons.omnys_beacons.logic

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.util.Log
import com.omnys.ble.beacons.omnys_beacons.channel.DataRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import org.altbeacon.beacon.*
import org.altbeacon.beacon.logging.LogManager
import org.altbeacon.beacon.logging.Loggers
import java.util.*
import org.altbeacon.beacon.BeaconParser;
import android.bluetooth.BluetoothAdapter
import com.omnys.ble.beacons.omnys_beacons.OmnysBeaconsPlugin
import com.omnys.ble.beacons.omnys_beacons.data.BeaconModel
import com.omnys.ble.beacons.omnys_beacons.data.MonitoringState
import com.omnys.ble.beacons.omnys_beacons.data.Permission
import com.omnys.ble.beacons.omnys_beacons.data.RegionModel
import com.omnys.ble.beacons.omnys_beacons.data.Result
import com.omnys.ble.beacons.omnys_beacons.data.Settings
import kotlinx.coroutines.launch


class BeaconsClient(private val permissionClient: PermissionClient) : BeaconConsumer, RangeNotifier, MonitorNotifier {

    companion object {
        private const val Tag = "beacons client"

        @SuppressLint("StaticFieldLeak")
        private var beaconManager: BeaconManager? = null
        private var sharedMonitor: SharedMonitor? = null

        fun init(application: Application, callback: OmnysBeaconsPlugin.BackgroundMonitoringCallback) {
            beaconManager = BeaconManager.getInstanceForApplication(application)

            // Add parsing support for iBeacon
            // https://beaconlayout.wordpress.com/
            beaconManager!!.beaconParsers.clear()
            beaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
/*            beaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout("x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15"))
            beaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"))
            beaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v"))*/

            sharedMonitor = SharedMonitor(application, callback)
        }
    }

    private var context: Context? = null
    private var isServiceConnected = false
    private var isPaused = false

    private val requests: ArrayList<Operation> = ArrayList()


    fun bind(context: Context) {
        this.context = context
        beaconManager!!.bind(this)
    }

    fun unbind() {
        beaconManager!!.removeRangeNotifier(this)
        beaconManager!!.removeMonitorNotifier(sharedMonitor!!)
        beaconManager!!.unbind(this)
        context = null
        isServiceConnected = false
    }


    // Beacons api

    fun configure(settings: Settings) {
        when (settings.logs) {
            Settings.Logs.Empty -> {
                LogManager.setVerboseLoggingEnabled(false)
                LogManager.setLogger(Loggers.empty())
            }
            Settings.Logs.Info -> {
                LogManager.setVerboseLoggingEnabled(false)
                LogManager.setLogger(Loggers.infoLogger())
            }
            Settings.Logs.Warning -> {
                LogManager.setVerboseLoggingEnabled(false)
                LogManager.setLogger(Loggers.warningLogger())
            }
            Settings.Logs.Verbose -> {
                LogManager.setVerboseLoggingEnabled(true)
                LogManager.setLogger(Loggers.verboseLogger())
            }
        }
    }

    fun addBackgroundMonitoringListener(listener: SharedMonitor.BackgroundListener) {
        Log.d(Tag, "addBackgroundMonitoringListener")
        sharedMonitor!!.addBackgroundListener(listener)
    }

    fun removeBackgroundMonitoringListener(listener: SharedMonitor.BackgroundListener) {
        Log.d(Tag, "removeBackgroundMonitoringListener")
        sharedMonitor!!.removeBackgroundListener(listener)
    }
    var mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    @SuppressLint("MissingPermission")
    fun addRequest(request: Operation, permission: Permission) {
        try {
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                context!!.startActivity(enableBtIntent)
            }
            request.region.initFrameworkValue()
        } catch (e: Exception) {
            request.callback!!(Result.failure(Result.Error.Type.Runtime, request.region, e.message))
            return
        }

        requests.add(request)

        GlobalScope.launch(Dispatchers.Main) {
            val result = permissionClient.request(permission)
            if (result !== PermissionClient.PermissionResult.Granted) {
                request.callback!!(result.result)
                return@launch
            }

            if (requests.count { request === it } == 0) {
                return@launch
            }

            startRequest(request)
        }
    }

    fun removeRequest(request: Operation) {
        val index = requests.indexOfFirst { request === it }
        if (index == -1) return

        stopRequest(request)
        requests.removeAt(index)
    }

    suspend fun startMonitoring(request: DataRequest): Result {
        val operation = Operation(Operation.Kind.Monitoring, request.region, request.inBackground, null)
        requests.add(operation)

        val result = permissionClient.request(request.permission)
        if (result !== PermissionClient.PermissionResult.Granted) {
            return result.result
        }

        if (requests.count { operation === it } == 0) {
            return Result.success(null)
        }

        startRequest(operation)
        return Result.success(null)
    }

    fun stopMonitoring(region: RegionModel) {
        val toRemove = requests.filter { it.kind == Operation.Kind.Monitoring && it.region.identifier == region.identifier }
        if (toRemove.isEmpty()) return

        toRemove.forEach { stopRequest(it) }
        requests.removeAll(toRemove)
    }


    // Lifecycle api

    fun resume() {
        isPaused = false
        sharedMonitor!!.attachForegroundNotifier(this)
        beaconManager!!.backgroundMode = false

        requests.filter { !it.isRunning }
                .forEach { startRequest(it) }
    }

    fun pause() {
        isPaused = true
        sharedMonitor!!.detachForegroundNotifier(this)
        beaconManager!!.backgroundMode = true

        requests.filter { it.isRunning && !it.inBackground }
                .forEach { stopRequest(it) }
    }


    // Internals

    private fun startRequest(request: Operation) {
        if (!isServiceConnected) return

        if (requests.count { it.region.identifier == request.region.identifier && it.kind == request.kind && it.isRunning } == 0) {
            Log.d(Tag, "start ${request.kind} (inBackground:${request.inBackground}) for region: ${request.region.identifier}")

            when (request.kind) {
                Operation.Kind.Ranging -> beaconManager!!.startRangingBeaconsInRegion(request.region.frameworkValue)
                Operation.Kind.Monitoring -> sharedMonitor!!.start(request.region)
            }
        }

        request.isRunning = true
    }

    private fun stopRequest(request: Operation) {
        request.isRunning = false
        if (!isServiceConnected) return

        if (requests.count { it.region.identifier == request.region.identifier && it.kind == request.kind && it.isRunning } == 0) {
            Log.d(Tag, "stop ${request.kind} (inBackground:${request.inBackground}) for region: ${request.region.identifier}")
            when (request.kind) {
                Operation.Kind.Ranging -> beaconManager!!.stopRangingBeaconsInRegion(request.region.frameworkValue)
                Operation.Kind.Monitoring -> sharedMonitor!!.stop(request.region)
            }
        }
    }


    // RangeNotifier

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>, region: Region) {
        requests.filter { it.callback != null }
                .filter { it.kind == Operation.Kind.Ranging && it.region.identifier == region.uniqueId }
                .forEach { it.callback!!(Result.success(beacons.map { BeaconModel.parse(it) }, RegionModel.parse(region))) }
    }


    // MonitoringNotifier

    override fun didDetermineStateForRegion(state: Int, region: Region) {

    }

    override fun didEnterRegion(region: Region) {
        Log.d(Tag, "didEnterRegion: ${region.uniqueId}")
        requests.filter { it.callback != null }
                .filter { it.kind == Operation.Kind.Monitoring && it.region.identifier == region.uniqueId }
                .forEach { it.callback!!(Result.success(MonitoringState.EnterOrInside, RegionModel.parse(region))) }
    }

    override fun didExitRegion(region: Region) {
        Log.d(Tag, "didExitRegion: ${region.uniqueId}")
        requests.filter { it.callback != null }
                .filter { it.kind == Operation.Kind.Monitoring && it.region.identifier == region.uniqueId }
                .forEach { it.callback!!(Result.success(MonitoringState.ExitOrOutside, RegionModel.parse(region))) }
    }


    // BeaconsConsumer

    override fun getApplicationContext(): Context {
        return context!!.applicationContext
    }

    override fun unbindService(p0: ServiceConnection) {
        return context!!.unbindService(p0)
    }

    override fun bindService(p0: Intent?, p1: ServiceConnection, p2: Int): Boolean {
        return context!!.bindService(p0, p1, p2)
    }

    override fun onBeaconServiceConnect() {
        isServiceConnected = true
        beaconManager!!.addRangeNotifier(this)
        beaconManager!!.addMonitorNotifier(sharedMonitor!!);

        requests
                .filter { !it.isRunning && (!isPaused || it.inBackground) }
                .forEach { startRequest(it) }
    }

    class Operation(
        val kind: Kind,
        val region: RegionModel,
        val inBackground: Boolean,
        val callback: ((Result) -> Unit)?
    ) {
        var isRunning: Boolean = false

        enum class Kind {
            Ranging, Monitoring
        }
    }
}