//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

part of omnys_beacons;

class _Channels {
  static const String _loggingTag = 'beacons';

  static const MethodChannel _channel = MethodChannel('omnys_beacons');

  static final StreamsChannel _rangingChannel =
      StreamsChannel('omnys_beacons/ranging');

  static final StreamsChannel _monitoringChannel =
      StreamsChannel('omnys_beacons/monitoring');

  static final StreamsChannel _backgroundMonitoringChannel =
      StreamsChannel('omnys_beacons/backgroundMonitoring');

  Future<BeaconsResult> checkStatus(_StatusRequest request) async {
    final response = await _invokeChannelMethod(
      _loggingTag,
      _channel,
      'checkStatus',
      _Codec.encodeStatusRequest(request),
    );
    return _Codec.decodeResult(response);
  }

  Future<BeaconsResult> requestPermission(LocationPermission permission) async {
    final response = await _invokeChannelMethod(
      _loggingTag,
      _channel,
      'requestPermission',
      _Codec.encodePermission(permission),
    );
    return _Codec.decodeResult(response);
  }

  Future<void> configure(BeaconsSettings settings) async {
    await _invokeChannelMethod(
      _loggingTag,
      _channel,
      'configure',
      _Codec.encodeSettings(settings),
    );
    return;
  }

  Future<BeaconsResult> startMonitoring(_DataRequest request) async {
    final response = await _invokeChannelMethod(
      _loggingTag,
      _channel,
      'startMonitoring',
      _Codec.encodeDataRequest(request),
    );
    return _Codec.decodeResult(response);
  }

  Future<void> stopMonitoring(BeaconRegion region) async {
    await _invokeChannelMethod(
      _loggingTag,
      _channel,
      'stopMonitoring',
      _Codec.encodeRegion(region),
    );
    return;
  }

  Stream<RangingResult> ranging(_DataRequest request) {
    final String json = _Codec.encodeDataRequest(request);
    _log(json, tag: 'ranging');
    return _rangingChannel.receiveBroadcastStream(json).map((data) {
      _log(data, tag: 'ranging');
      return _Codec.decodeRangingResult(data);
    });
  }

  Stream<MonitoringResult> monitoring(_DataRequest request) {
    final String json = _Codec.encodeDataRequest(request);
    _log(json, tag: 'monitoring');
    return _monitoringChannel.receiveBroadcastStream(json).map((data) {
      _log(data, tag: 'monitoring');
      return _Codec.decodeMonitoringResult(data);
    });
  }

  Stream<BackgroundMonitoringEvent> backgroundMonitoringEvents() {
    _log('add listener', tag: 'backgroundMonitoringEvents');
    return _backgroundMonitoringChannel.receiveBroadcastStream().map((data) {
      _log(data, tag: 'backgroundMonitoringEvents');
      return _Codec.decodeBackgroundMonitoringEvent(data);
    });
  }
}
