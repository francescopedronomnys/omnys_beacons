//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

package com.omnys.ble.beacons.omnys_beacons.channel

import com.omnys.ble.beacons.omnys_beacons.data.BackgroundMonitoringEvent
import com.omnys.ble.beacons.omnys_beacons.data.MonitoringState
import com.omnys.ble.beacons.omnys_beacons.data.Permission
import com.omnys.ble.beacons.omnys_beacons.data.RegionModel
import com.omnys.ble.beacons.omnys_beacons.data.Result
import com.omnys.ble.beacons.omnys_beacons.data.Settings
import com.squareup.moshi.Moshi

object Codec {

    private val moshi: Moshi = Moshi.Builder()
            .add(Permission.Adapter())
            .add(MonitoringState.Adapter())
            .add(Settings.Logs.Adapter())
            .build()

    fun encodeResult(result: Result): String =
            moshi.adapter(Result::class.java).toJson(result)

    fun encodeBackgroundMonitoringEvent(event: BackgroundMonitoringEvent): String =
            moshi.adapter(BackgroundMonitoringEvent::class.java).toJson(event)

    fun decodePermission(arguments: Any?): Permission =
            Permission.Adapter().fromJson(arguments!! as String)

    fun decodeDataRequest(arguments: Any?): DataRequest =
            moshi.adapter(DataRequest::class.java).fromJson(arguments!! as String)!!

    fun decodeStatusRequest(arguments: Any?): StatusRequest =
            moshi.adapter(StatusRequest::class.java).fromJson(arguments!! as String)!!

    fun decodeSettings(arguments: Any?): Settings =
            moshi.adapter(Settings::class.java).fromJson(arguments!! as String)!!

    fun decodeRegion(arguments: Any?): RegionModel =
            moshi.adapter(RegionModel::class.java).fromJson(arguments!! as String)!!

}
