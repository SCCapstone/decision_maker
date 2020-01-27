// Imports the Flutter Driver API.
import 'package:flutter_driver/flutter_driver.dart';
import 'package:test/test.dart';

void main() {
  group('Flutter Driver demo', () {
    // First, define the Finders and use them to locate widgets from the
    // test suite. Note: the Strings provided to the `byValueKey` method must
    // be the same as the Strings we used for the Keys in step 1.
    final keyFinder = find.byValueKey("blah");

    FlutterDriver driver;

    setUpAll(() async {
      driver = await FlutterDriver.connect();
    });

    tearDownAll(() async {
      if (driver != null) {
        await driver.close();
      }
    });

    test('check flutter driver health', () async {
      Health health = await driver.checkHealth();
      print(health.status);
    });

    test('finds a widget using a Key', () async {
      // Find the blah widget using the testKey.
      expect(await driver.getText(keyFinder), "Categories");
    });
  });
}