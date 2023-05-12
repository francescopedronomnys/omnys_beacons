//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

package com.omnys.ble.beacons.omnys_beacons.data

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

enum class MonitoringState {
    EnterOrInside, ExitOrOutside, Unknown;

    class Adapter {
        @FromJson
        fun fromJson(json: String): MonitoringState =
                MonitoringState.valueOf(json.capitalize())

        @ToJson
        fun toJson(value: MonitoringState): String =
                value.toString().decapitalize()
    }
}