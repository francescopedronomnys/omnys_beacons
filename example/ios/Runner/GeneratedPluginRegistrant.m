//
//  Generated file. Do not edit.
//

#import "GeneratedPluginRegistrant.h"

#if __has_include(<beacons/BeaconsPlugin.h>)
#import <beacons/BeaconsPlugin.h>
#else
@import beacons;
#endif

#if __has_include(<flutter_local_notifications/FlutterLocalNotificationsPlugin.h>)
#import <flutter_local_notifications/FlutterLocalNotificationsPlugin.h>
#else
@import flutter_local_notifications;
#endif

#if __has_include(<streams_channel/StreamsChannelPlugin.h>)
#import <streams_channel/StreamsChannelPlugin.h>
#else
@import streams_channel;
#endif

@implementation GeneratedPluginRegistrant

+ (void)registerWithRegistry:(NSObject<FlutterPluginRegistry>*)registry {
  [BeaconsPlugin registerWithRegistrar:[registry registrarForPlugin:@"BeaconsPlugin"]];
  [FlutterLocalNotificationsPlugin registerWithRegistrar:[registry registrarForPlugin:@"FlutterLocalNotificationsPlugin"]];
  [StreamsChannelPlugin registerWithRegistrar:[registry registrarForPlugin:@"StreamsChannelPlugin"]];
}

@end
