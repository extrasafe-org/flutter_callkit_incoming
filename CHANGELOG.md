## 2.5.3

### Plugin changes:

- breaking: bumps the minimum versions for Flutter and Dart SDKs to 3.29.0 and 3.7.0 respectively;
- adds the `.fvmrc` config file that specifies the development Flutter version – 3.29.2;
- updates Android project build settings:
  - removes the deprecated `jcenter` repository and replaces it with `mavenCentral`;
  - breaking: bumps the minimum Android SDK version from 16 to 26;
  - bumps the compile and target Android SDK versions from 34 to 36;
  - bumps the Java SDK version from 17 to 21;
  - bumps the Kotlin version from 1.8.0 to 2.1.10;
  - bumps the gradle version from 7.1.2 to 8.9.0 and AGP version from 7.4 to 8.11.1.
- updates the Android manifest file by adding missing foreground service permission types for camera and microphone;
- allows to specify local asset for display in the notifications (Android only);
- fixes an issue where plugin would not use the default system ringtone for calls (Android only);
- updates call vibration pattern (Android only);
- refactors the Android project codebase:
  - adds missing permission checks to `OngoingNotificationService`;
  - removes no longer necessary Android version checks;
  - reformats the code;
- breaking: comments out code related to configuring the iOS audio session. It is now the responsibility of the caller to configure audio session if any changes are necessary, which can be done by either:
  - creating an `AppDelegate` extension that conforms to the `CallkitIncomingAppDelegate` protocol and providing the `didActivateAudioSession` and `didDeactivateAudioSession` methods (iOS native), or
  - listening for `actionCallToggleAudioSession` events and doing configuration in event callbacks.
- adds the `NotificationParams::content` field that can be used to set text content that's displayed in the ongoing call notification (Android only);
- adds the `FlutterCallkitIncoming::canUseFullScreenIntent` method to determine whether the device can show full screen notifications in the locked state (Android only). This value can be used to determine whether to show a dialog and subsequently call `FlutterCallkitIncoming::requestFullIntentPermission` to open the settings activity.
- renames the `FlutterCallkitIncoming::requestFullIntentPermission` to `FlutterCallkitIncoming::openFullScreenNotificationsSettings` to better reflect its purpose and changes method return type to `Future<void>` from `Future<dynamic>`.

### Example project changes:

- updates Android project build settings:
  - breaking: bumps the minimum Android SDK version from 19 to 26;
  - bumps the compile and target Android SDK versions from 34 to 36;
  - sets the Java SDK version from 21;
  - sets the Kotlin version from 2.1.10;
  - bumps the gradle version from 7.1.2 to 8.9.0 and AGP version from 7.4 to 8.11.1;
  - migrates the now deprecated imperative application of Flutter's Gradle plugins;
  - removes the deprecated `package` value in the `manifest` root tag from manifests.

## 2.5.2
* Add notification calling for Android `callingNotification`, thank @ebsangam https://github.com/hiennguyen92/flutter_callkit_incoming/pull/662
* Add `logoUrl` properties (inside android prop) 
* Fixed issue DMTF IOS, thank @minn-ee https://github.com/hiennguyen92/flutter_callkit_incoming/issues/577
* Fixed issue duplicate missing notification Android
* Fixed some bugs.

## 2.5.1
* Fix issue security Android, thanks @datpt11 https://github.com/hiennguyen92/flutter_callkit_incoming/issues/651

## 2.5.0
* update jvmToolchain(17) for Android

## 2.0.4+2
* add func `requestFullIntentPermission` (Android 14+) thank @Spyspyspy https://github.com/hiennguyen92/flutter_callkit_incoming/pull/584
* set Notification call style (Android) thank @AAkira https://github.com/hiennguyen92/flutter_callkit_incoming/pull/553
* Many other issues
    1. add prop `accepted` in activeCalls (iOS) thank @vasilich6107

## 2.0.4+1
* Removed `Telecom Framework` (Android)

## 2.0.4
* Removed `Telecom Framework` (Android)
* Fixed hide notification for action `CallBack` (Android)

## 2.0.3
* Fixed linked func `hideCallkitIncoming`

## 2.0.2+2
* Fixed linked func `hideCallkitIncoming`

## 2.0.2+1
* Fixed linked func `hideCallkitIncoming`

## 2.0.2
* Add func `hideCallkitIncoming` clear the incoming notification/ring (after accept/decline/timeout)
* Add props `isShowFullLockedScreen` on Android
* Fixed example/Fixed update android 14

## 2.0.1+2
* Add Action for onDecline
* Add Action for onEnd
* add android props `isShowCallID`

## 2.0.1+1
* Add Callback AVAudioSession for WebRTC setup
* Fix issue no audio for using WebRTC

## 2.0.1-dev.2
* Add Action for onAccept

## 2.0.1-dev.1
* Add AVAudioSession Appdelegate(iOS)

## 2.0.1-dev
* Add AVAudioSession Appdelegate(iOS)

## 2.0.1

* Fixed some bugs.
* `Android` using Telecom Framework
* Add `silenceEvents`
* Add `normalHandle` props https://github.com/hiennguyen92/flutter_callkit_incoming/pull/403
* Android add `textColor` props https://github.com/hiennguyen92/flutter_callkit_incoming/pull/398
* Android invisible avatar for default https://github.com/hiennguyen92/flutter_callkit_incoming/pull/393
* Add Method for call API when accept/decline/end/timeout

## 2.0.0+2

* Fixed some bugs.
* Support request permission for Android 13+ `requestNotificationPermission`

## 2.0.0+1

* Fixed some bugs.
* Add `landscape` for tablet
* Fix issue head-up for redmi / xiaomi devices

## 2.0.0

* Fixed some bugs.
* Adapt flutter_lints and use lowerCamelCase to Event enum
* Rename properties 
        `textMisssedCall` -> `subtitle`,
        `textCallback` -> `callbackText`,
        `isShowMissedCallNotification` -> `showNotification`,
* Move inside properties `missedCallNotification {showNotification, isShowCallback, subtitle, callbackText}`
* Add setCallConnected option iOS `await FlutterCallkitIncoming.setCallConnected(this._currentUuid)`
* Add hold option iOS
* Add mute call option iOS
* Many other issues
    1. Thank @ryojiro
    https://github.com/hiennguyen92/flutter_callkit_incoming/pull/263
    https://github.com/hiennguyen92/flutter_callkit_incoming/pull/264
    https://github.com/hiennguyen92/flutter_callkit_incoming/pull/262
    2. Many Thank @mouEsam
    https://github.com/hiennguyen92/flutter_callkit_incoming/pull/227
    3. ...


## 1.0.3+3

* Update README.md
* Fixed some bugs.

## 1.0.3+2

* REMOVED

## 1.0.3+1

* Dart class models instead using dynamic types and Maps (thank @icodelifee - https://github.com/hiennguyen92/flutter_callkit_incoming/pull/180)
* Allow to call from native Android (thank @fabiocody - https://github.com/hiennguyen92/flutter_callkit_incoming/pull/185)
* Add android notification channel name `incomingCallNotificationChannelName` `missedCallNotificationChannelName` (thank @AAkira - https://github.com/hiennguyen92/flutter_callkit_incoming/pull/177)
* Adding the feature to change template of notification to small `isCustomSmallExNotification` (thank @anocean2 - https://github.com/hiennguyen92/flutter_callkit_incoming/pull/196)
* Fixed ringtone sound not playing in Release mode on Android (thank @mschudt - https://github.com/hiennguyen92/flutter_callkit_incoming/pull/204)
* Fixed some bugs.

## 1.0.3

* REMOVED

## 1.0.2+2

* Fix notification Android 12
* Fix sound notification
* Support `backgroundUrl` using path assets
* Fixed some bugs.

## 1.0.2+1

* Issue no audio when Accept(iOS)
* Duplicate sound notification(Android)
* Support Flutter 3
* Fixed some bugs.

## 1.0.2

* Fixed issue open app(terminated/background state - Android).
* Completed Example  
* Fixed some bugs.

## 1.0.1+8

* Add props `isShowMissedCallNotification` using show Missed call notification(Android)
* Fixed issue decline(terminated/background state - there will be about 3 seconds to call the api before the app is closed.)
* Fixed some bugs.

## 1.0.1+7

* Fixed issue open app(terminated/background state).
* Fixed some bugs.

## 1.0.1+6

* Add props for text
* Fixed issue open app(terminated/background state).
* Fixed some bugs.

## 1.0.1+5

* Update custom miss call notification
* Fixed issue open app(terminated/background state).

## 1.0.1+4

* add `showMissCallNotification` only for Android, using show miss call notification 
* Fixed some bugs.

## 1.0.1+3

* add props `isShowCallback` using show Callback for miss call(Android)
* public props data call for Object-C/Swift
* Example using FCM(Android)
* Fixed some bugs.

## 1.0.1+2

* Fixed issue default ringtone(Android)
* Fixed issue vibration(Android)
* Fixed issue sound play type ringtone volumn system(Android)
* Fixed flow incomming screen(Android)
* Fixed some bugs.

## 1.0.1+1

* Switch using Service for Ringtone(Android)
* Fixed issue vibration(Android)
* Add `getDevicePushTokenVoIP()` feature
* Fixed some bugs.

## 1.0.1

* Pustkit and VoIP setup instructions (PUSHKIT.md)
* Callback from Recent History IOS
* Using System ringtone for default
* Fixed func `endAllCalls()` Android
* Bugs Android 12.
* Fixed some bugs.

## 1.0.0+8

* Share func call from native(iOS)

## 1.0.0+7

* Add custom `headers` using for avatar/background image (only for Android)

## 1.0.0+6

* Fixed func `activeCalls()` Get active calls

## 1.0.0+5

* Fixed endCall
* Bugs Targeting Android 12 (Android).
* Bugs `audio session` (iOS)
* Fixed some bugs.

## 1.0.0+4

* Update README.md.
* Add func `activeCalls()` Get active calls
* Remove notification when click action `Call back` (Android).
* Bugs `no activation of the audio session` (iOS)
* Fixed some bugs.

## 1.0.0+3

* Update README.md.
* Add props android `isShowLogo`.
* Fixed some bugs.

## 1.0.0+2

* Update README.md.
* Update documentation.

## 1.0.0+1

* Update README.md.
* Fixed some bugs.

## 1.0.0

* Initial release.
