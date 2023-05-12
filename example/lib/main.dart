import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:omnys_beacons/omnys_beacons.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  MyApp({super.key}) {
    OmnysBeacons.loggingEnabled = true;
  }

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final log = List<String>.empty(growable: true);

  @override
  void initState() {
    super.initState();

    OmnysBeacons.configure(const BeaconsSettings(
      android: BeaconsSettingsAndroid(
        logs: BeaconsSettingsAndroidLogs.verbose,
      ),
    ));

    OmnysBeacons.backgroundMonitoringEvents().listen((result) {
      print(
          "BackgroundMonitoring result: ${result.region.identifier} ${result.state}");
      /*final DateFormat dateFormatter = DateFormat('yyyy-MM-dd');
      final DateFormat timeFormatter = DateFormat('HH:mm:ss.SSS');
      final now = DateTime.now();
      setState(() {
        log.add(
            "${dateFormatter.format(now)} ${timeFormatter.format(now)} - monitoring event: $result");
      });*/
    });

    OmnysBeacons.monitoring(
      region: BeaconRegion(
          identifier: "BlueUp", ids: ["ACFD065E-C3C0-11E3-9BBE-1A514932AC01"]),
      inBackground: true,
      permission: const LocationPermission(
        ios: LocationPermissionIOS.always,
        android: LocationPermissionAndroid.fine,
      ),
    ).listen((result) {
      print("Monitoring result: $result");
      final DateFormat dateFormatter = DateFormat('yyyy-MM-dd');
      final DateFormat timeFormatter = DateFormat('HH:mm:ss.SSS');
      final now = DateTime.now();
      setState(() {
        log.add(
            "${dateFormatter.format(now)} ${timeFormatter.format(now)} - monitoring event: $result");
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text(log.join("\n")),
        ),
      ),
    );
  }
}
