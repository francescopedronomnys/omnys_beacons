//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

part of omnys_beacons;

Future<dynamic> _invokeChannelMethod(
    String tag, MethodChannel channel, String method,
    [dynamic arguments]) async {
  _log('invoke ${channel.name}->$method $arguments', tag: tag);
  String? data;
  try {
    data = await channel.invokeMethod(method, arguments);
  } catch (exception, stack) {
    FlutterError.reportError(FlutterErrorDetails(
      exception: exception,
      stack: stack,
      library: 'beacons',
      context:
          DiagnosticsNode.message('while invoking ${channel.name}/$method'),
    ));
  }

  _log(data ?? 'finished without data', tag: tag);
  return data;
}
