#import "OmnysBeaconsPlugin.h"
#if __has_include(<omnys_beacons/omnys_beacons-Swift.h>)
#import <omnys_beacons/omnys_beacons-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "omnys_beacons-Swift.h"
#endif

@implementation OmnysBeaconsPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftOmnysBeaconsPlugin registerWithRegistrar:registrar];
}
@end
