import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'omnys_beacons_method_channel.dart';

abstract class OmnysBeaconsPlatform extends PlatformInterface {
  /// Constructs a OmnysBeaconsPlatform.
  OmnysBeaconsPlatform() : super(token: _token);

  static final Object _token = Object();

  static OmnysBeaconsPlatform _instance = MethodChannelOmnysBeacons();

  /// The default instance of [OmnysBeaconsPlatform] to use.
  ///
  /// Defaults to [MethodChannelOmnysBeacons].
  static OmnysBeaconsPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [OmnysBeaconsPlatform] when
  /// they register themselves.
  static set instance(OmnysBeaconsPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
