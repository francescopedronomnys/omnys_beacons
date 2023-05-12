//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

package com.omnys.ble.beacons.omnys_beacons.data

class BackgroundMonitoringEvent(
        val type: String,
        val region: RegionModel,
        val state: MonitoringState
)