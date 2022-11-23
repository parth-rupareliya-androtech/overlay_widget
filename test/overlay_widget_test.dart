import 'package:flutter_test/flutter_test.dart';
import 'package:overlay_widget/overlay_widget.dart';
import 'package:overlay_widget/overlay_widget_platform_interface.dart';
import 'package:overlay_widget/overlay_widget_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockOverlayWidgetPlatform
    with MockPlatformInterfaceMixin
    implements OverlayWidgetPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final OverlayWidgetPlatform initialPlatform = OverlayWidgetPlatform.instance;

  test('$MethodChannelOverlayWidget is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelOverlayWidget>());
  });

  test('getPlatformVersion', () async {
    OverlayWidget overlayWidgetPlugin = OverlayWidget();
    MockOverlayWidgetPlatform fakePlatform = MockOverlayWidgetPlatform();
    OverlayWidgetPlatform.instance = fakePlatform;

    expect(await overlayWidgetPlugin.getPlatformVersion(), '42');
  });
}
