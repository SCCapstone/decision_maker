// Imports the Flutter Driver API.
import 'package:flutter_driver/flutter_driver.dart';
import 'package:test/test.dart';

void main() {
  group('Flutter Driver demo', () {
    // First, define the Finders and use them to locate widgets from the
    // test suite. Note: the Strings provided to the `byValueKey` method must
    // be the same as the Strings we used for the Keys in step 1.
    final keyFinder = find.byValueKey("username");

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

    test('user login', () async {
      var usernameField = find.byValueKey("username");
      await driver.tap(usernameField);
      await driver.enterText('testingUser1!');

      var passwordField = find.byValueKey("password");
      await driver.tap(passwordField);
      await driver.enterText('testingUser1!');

      var signInOrUpButton = find.byValueKey("signInOrUp");
      await driver.tap(signInOrUpButton);

      await driver.waitFor(find.text("Pocket Poll"));
    });
  });
}
