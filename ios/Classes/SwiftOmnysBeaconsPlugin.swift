import Flutter
import UIKit

public class SwiftOmnysBeaconsPlugin: NSObject, FlutterPlugin, UIApplicationDelegate {
/*   public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "omnys_beacons", binaryMessenger: registrar.messenger())
    let instance = SwiftOmnysBeaconsPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  } */

    internal let registrar: FlutterPluginRegistrar
    private let locationClient = LocationClient()
    private let channel: Channel

    init(registrar: FlutterPluginRegistrar) {
      self.registrar = registrar
      self.channel = Channel(locationClient: locationClient)
      super.init()

      registrar.addApplicationDelegate(self)
      channel.register(on: self)
    }

    public static func register(with registrar: FlutterPluginRegistrar) {
      _ = SwiftOmnysBeaconsPlugin(registrar: registrar)
    }

    // UIApplicationDelegate

    public func applicationDidBecomeActive(_ application: UIApplication) {
      locationClient.resume()
    }

    public func applicationWillResignActive(_ application: UIApplication) {
      locationClient.pause()
    }
}
