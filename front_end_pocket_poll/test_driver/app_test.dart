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
      var usernameField = find.byValueKey("login_page:username_input");
      await driver.tap(usernameField);
      await driver.enterText('testingUser1!');

      var passwordField = find.byValueKey("login_page:password_input");
      await driver.tap(passwordField);
      await driver.enterText('testingUser1!');

      var signInOrUpButton = find.byValueKey("login_page:sign_in_button");
      await driver.tap(signInOrUpButton);

      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
    });

    test('create_category', () async {
      // load the categories home page
      var drawerOpenButton = find.byTooltip("Open navigation menu");
      await driver.tap(drawerOpenButton);
      var categoryButton = find.byValueKey("groups_home:my_categories_button");
      await driver.tap(categoryButton);
      await driver.waitFor(find.byValueKey("categories_home:scaffold"));

      // now, create a category
      var categoryCreateButton =
          find.byValueKey("categories_home:add_category_button");
      driver.tap(categoryCreateButton);
      await driver.waitFor(find.byValueKey("categories_create:scaffold"));
      // enter the category name
      var categoryNameField =
          find.byValueKey("categories_create:category_name_input");
      await driver.tap(categoryNameField);
      await driver.enterText("Example Category");
      // enter a choice
      var choiceNameField = find.byValueKey("choice_row:choice_name_input:1");
      await driver.tap(choiceNameField);
      await driver.enterText("Example Choice");
      // save the category
      var categorySaveButton = find.byValueKey("categories_create:save_button");
      await driver.tap(categorySaveButton);
      await driver.waitFor(find.byValueKey("categories_home:scaffold"));
    });

    test('edit_category', () async {
      var categoryEditButton = find.byTooltip("Edit Category");
      driver.tap(categoryEditButton);
      await driver.waitFor(find.byValueKey("categories_edit:scaffold"));
      // edit the category name
      var categoryNameField =
          find.byValueKey("categories_edit:category_name_input");
      await driver.tap(categoryNameField);
      await driver.enterText("Example Category New");
      // edit a choice
      var choiceNameField = find.byValueKey("choice_row:choice_name_input:1");
      await driver.tap(choiceNameField);
      await driver.enterText("Example Choice New");
      // save the category
      var categorySaveButton = find.byValueKey("categories_edit:save_button");
      await driver.tap(categorySaveButton);
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("categories_home:scaffold"));
    });

    test('delete_category', () async {
      var categoryDeleteButton = find.byTooltip("Delete Category");
      driver.tap(categoryDeleteButton);

      var categoryDeleteConfirmButton = find.text("Yes");
      await driver.tap(categoryDeleteConfirmButton);
      await driver.waitFor(find.byValueKey("categories_home:scaffold"));
      // go back to groups home
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
    });
  });
}
