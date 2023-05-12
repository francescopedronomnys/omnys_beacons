import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'omnys_beacons_platform_interface.dart';

/// An implementation of [OmnysBeaconsPlatform] that uses method channels.
class MethodChannelOmnysBeacons extends OmnysBeaconsPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('omnys_beacons');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
