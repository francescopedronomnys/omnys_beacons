//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

package com.omnys.ble.beacons.omnys_beacons.channel

import com.omnys.ble.beacons.omnys_beacons.data.RegionModel
import com.omnys.ble.beacons.omnys_beacons.data.Permission

class DataRequest(
        val region: RegionModel,
        val permission: Permission,
        val inBackground: Boolean
)