import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:overlay_widget/overlay_widget_method_channel.dart';

void main() {
  MethodChannelOverlayWidget platform = MethodChannelOverlayWidget();
  const MethodChannel channel = MethodChannel('overlay_widget');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
