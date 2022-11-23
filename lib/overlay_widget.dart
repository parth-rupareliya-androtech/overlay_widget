import 'dart:async';
import 'dart:developer';

import 'package:flutter/services.dart';

class OverlayWidget {
  OverlayWidget._();

  static final StreamController _controller = StreamController();
  static const MethodChannel _channel = MethodChannel("x-slayer/overlay_channel");
  static const MethodChannel _overlayChannel = MethodChannel("x-slayer/overlay");
  static const BasicMessageChannel _overlayMessageChannel = BasicMessageChannel("x-slayer/overlay_messenger", JSONMessageCodec());

  static Future<void> showOverlay({
    int height = -1,
    int width = -1,
    OverlayAlignment alignment = OverlayAlignment.center,
    NotificationVisibility visibility = NotificationVisibility.visibilitySecret,
    OverlayFlag flag = OverlayFlag.defaultFlag,
    String overlayTitle = "overlay activated",
    String? overlayContent,
    bool enableDrag = false,
    PositionGravity positionGravity = PositionGravity.none,
  }) async {
    await _channel.invokeMethod(
      'showOverlay',
      {
        "height": height,
        "width": width,
        "alignment": alignment.name,
        "flag": flag.name,
        "overlayTitle": overlayTitle,
        "overlayContent": overlayContent,
        "enableDrag": enableDrag,
        "notificationVisibility": visibility.name,
        "positionGravity": positionGravity.name,
      },
    );
  }

  /// Check if overlay permission is granted
  static Future<bool> isPermissionGranted() async {
    try {
      return await _channel.invokeMethod<bool>('checkPermission') ?? false;
    } on PlatformException catch (error) {
      log("$error");
      return Future.value(false);
    }
  }

  /// Request overlay permission
  /// it will open the overlay settings page and return `true` once the permission granted.
  static Future<bool?> requestPermission() async {
    try {
      return await _channel.invokeMethod<bool?>('requestPermission');
    } on PlatformException catch (error) {
      log("Error requestPermission: $error");
      rethrow;
    }
  }

  /// Closes overlay if open
  static Future<bool?> closeOverlay() async {
    final bool? res = await _channel.invokeMethod('closeOverlay');
    return res;
  }

  /// Broadcast data to and from overlay app
  static Future shareData(dynamic data) async {
    return await _overlayMessageChannel.send(data);
  }

  /// Streams message shared between overlay and main app
  static Stream<dynamic> get overlayListener {
    _overlayMessageChannel.setMessageHandler((message) async {
      _controller.add(message);
      return message;
    });
    return _controller.stream;
  }

  /// Update the overlay flag while the overlay in action
  static Future<bool?> updateFlag(OverlayFlag flag) async {
    final bool? res = await _overlayChannel.invokeMethod<bool?>('updateFlag', {'flag': flag.name});
    return res;
  }

  /// Update the overlay size in the screen
  static Future<bool?> resizeOverlay(int width, int height) async {
    final bool? res = await _overlayChannel.invokeMethod<bool?>(
      'resizeOverlay',
      {
        'width': width,
        'height': height,
      },
    );
    return res;
  }

  /// Check if the current overlay is active
  static Future<bool> isActive() async {
    final bool? res = await _channel.invokeMethod<bool?>('isOverlayActive');
    return res ?? false;
  }

  /// Dispose overlay stream
  static void disposeOverlayListener() {
    _controller.close();
  }
}

/// Placement of overlay within the screen.
enum OverlayAlignment { topLeft, topCenter, topRight, centerLeft, center, centerRight, bottomLeft, bottomCenter, bottomRight }

/// Type of dragging behavior for the overlay.
enum PositionGravity {
  /// The `PositionGravity.none` will allow the overlay to positioned anywhere on the screen.
  none,

  /// The `PositionGravity.right` will allow the overlay to stick on the right side of the screen.
  right,

  /// The `PositionGravity.left` will allow the overlay to stick on the left side of the screen.
  left,

  /// The `PositionGravity.auto` will allow the overlay to stick either on the left or right side of the screen depending on the overlay position.
  auto,
}

enum OverlayFlag {
  /// Window flag: this window can never receive touch events.
  /// Useful if you want to display click-through overlay
  @Deprecated('Use "clickThrough" instead.')
  flagNotTouchable,

  /// Window flag: this window won't ever get key input focus
  /// so the user can not send key or other button events to it.
  @Deprecated('Use "defaultFlag" instead.')
  flagNotFocusable,

  /// Window flag: allow any pointer events outside of the window to be sent to the windows behind it.
  /// Useful when you want to use fields that show keyboards.
  @Deprecated('Use "focusPointer" instead.')
  flagNotTouchModal,

  /// Window flag: this window can never receive touch events.
  /// Useful if you want to display click-through overlay
  clickThrough,

  /// Window flag: this window won't ever get key input focus
  /// so the user can not send key or other button events to it.
  defaultFlag,

  /// Window flag: allow any pointer events outside of the window to be sent to the windows behind it.
  /// Useful when you want to use fields that show keyboards.
  focusPointer,
}

/// The level of detail displayed in notifications on the lock screen.
enum NotificationVisibility {
  /// Show this notification in its entirety on all lockscreens.
  visibilityPublic,

  /// Do not reveal any part of this notification on a secure lockscreen.
  visibilitySecret,

  /// Show this notification on all lockscreens, but conceal sensitive or private information on secure lockscreens.
  visibilityPrivate
}
