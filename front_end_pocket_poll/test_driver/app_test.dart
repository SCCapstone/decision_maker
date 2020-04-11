import 'dart:math';

import 'package:flutter_driver/flutter_driver.dart';
import 'package:test/test.dart';

void main() {
  group('Pocket Poll Integration Test', () {
    // First, define the Finders and use them to locate widgets from the
    // test suite. Note: the Strings provided to the `byValueKey` method must
    // be the same as the Strings we used for the Keys in step 1.
    final String displayName = "Testing";
    final String testingUser2 = "testingUser2!";
    final String testingUser3 = "testingUser3!";
    final _random = new Random();
    final int maxCategoryName = 100000;
    final int maxChoiceName = 10000;

    String getRandomCategoryName() {
      return _random.nextInt(maxCategoryName).toString();
    }

    String getRandomChoiceName() {
      return _random.nextInt(maxChoiceName).toString();
    }

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
      await driver.enterText(getRandomCategoryName());
      // enter a choice
      var choiceNameField = find.byValueKey("choice_row:choice_name_input:1");
      await driver.tap(choiceNameField);
      await driver.enterText(getRandomChoiceName());
      // save the category
      var categorySaveButton = find.byValueKey("categories_create:save_button");
      await driver.tap(categorySaveButton);
      await driver.waitFor(find.byValueKey("categories_home:scaffold"));
    });

    test('edit_category', () async {
      // could be more categories if other tests failed, just always edit first one because that should always be there
      var categoryEditButton =
          find.byValueKey("categories_list_item:category_edit_button:0");
      driver.tap(categoryEditButton);
      await driver.waitFor(find.byValueKey("categories_edit:scaffold"));
      // edit the category name
      var categoryNameField =
          find.byValueKey("categories_edit:category_name_input");
      await driver.tap(categoryNameField);
      await driver.enterText(getRandomCategoryName());
      // edit a choice
      var choiceNameField = find.byValueKey("choice_row:choice_name_input:1");
      await driver.tap(choiceNameField);
      await driver.enterText(getRandomChoiceName());
      // save the category
      var categorySaveButton = find.byValueKey("categories_edit:save_button");
      await driver.tap(categorySaveButton);
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("categories_home:scaffold"));
      // go back to groups home
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
    });

    test('save_user_settings', () async {
      // load the user settings page
      var drawerOpenButton = find.byTooltip("Open navigation menu");
      await driver.tap(drawerOpenButton);
      var categoryButton = find.byValueKey("groups_home:user_settings_button");
      await driver.tap(categoryButton);
      await driver.waitFor(find.byValueKey("user_settings:scaffold"));

      // change the display name
      var displayNameField = find.byValueKey("user_settings:displayName_input");
      await driver.tap(displayNameField);
      await driver.enterText("New DisplayName");
      // change to light theme
      var darkThemeSwitch = find.byValueKey("user_settings:dark_theme_switch");
      await driver.tap(darkThemeSwitch);
      // save the changes
      var saveButton = find.byValueKey("user_settings:save_button");
      await driver.tap(saveButton);
      // change the display name back.
      await driver.tap(displayNameField);
      await driver.enterText(displayName);
      await driver.tap(saveButton);
      // open the favorites page
      var favoritesPageButton =
          find.byValueKey("user_settings:favorites_button");
      await driver.tap(favoritesPageButton);
      await driver.waitFor(find.byValueKey("favorites_page:scaffold"));

      // enter a user
      var usernameInput = find.byValueKey("favorites_page:username_input");
      await driver.tap(usernameInput);
      await driver.enterText(testingUser2);
      var addUserButton = find.byValueKey("favorites_page:add_user_button");
      await driver.tap(addUserButton);
      await driver.waitFor(find.byValueKey("user_row:$testingUser2:delete"));
      // remove the user
      var deleteUserButton = find.byValueKey("user_row:$testingUser2:delete");
      await driver.tap(deleteUserButton);
      // add user back
      await driver.tap(usernameInput);
      await driver.enterText(testingUser2);
      await driver.tap(addUserButton);
      await driver.waitFor(find.byValueKey("user_row:$testingUser2:delete"));
      // go back to the groups home page
      await driver.tap(find.pageBack());
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
    });

    test('create_group', () async {
      var createGroupButton = find.byValueKey("groups_home:new_group_button");
      await driver.tap(createGroupButton);
      await driver.waitFor(find.byValueKey("groups_create:scaffold"));
      // enter a group name
      var groupNameInput = find.byValueKey("groups_create:group_name_input");
      await driver.tap(groupNameInput);
      await driver.enterText("Test Group");
      // enter a default consider duration
      var considerInput = find.byValueKey("groups_create:consider_input");
      await driver.tap(considerInput);
      await driver.enterText("1");
      // enter a default voting duration
      var voteInput = find.byValueKey("groups_create:vote_input");
      await driver.tap(voteInput);
      await driver.enterText("1");
      // add a category
      var addCategoryButton =
          find.byValueKey("groups_create:add_categories_button");
      await driver.tap(addCategoryButton);
      await driver.waitFor(find.byValueKey("category_pick:scaffold"));
      //always select the first category for the group
      var categoryCheckBox = find.byValueKey("category_row:checkbox:0");
      await driver.tap(categoryCheckBox);
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("groups_create:scaffold"));
      // enter the members page
      var membersPageButton =
          find.byValueKey("groups_create:add_members_button");
      await driver.tap(membersPageButton);
      await driver.waitFor(find.byValueKey("members_page:scaffold"));
      // add a member from favorites
      var showFavoritesButton =
          find.byValueKey("members_page:show_favorites_button");
      await driver.tap(showFavoritesButton);
      var addFavoriteButton = find.byValueKey("user_row:$testingUser2:add");
      await driver.tap(addFavoriteButton);
      await driver.tap(showFavoritesButton);
      await driver.waitFor(find.byValueKey("user_row:$testingUser2:delete"));
      // add a member not in favorites
      var memberInput = find.byValueKey("members_page:member_input");
      await driver.tap(memberInput);
      await driver.enterText(testingUser3);
      var addMemberButton = find.byValueKey("members_page:add_member_button");
      await driver.tap(addMemberButton);
      await driver.waitFor(find.byValueKey("user_row:$testingUser3:delete"));
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("groups_create:scaffold"));
      // now save the group
      var saveButton = find.byValueKey("groups_create:save_button");
      await driver.tap(saveButton);
      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
    });

    test('create_event', () async {});

    // TODO remove the favorites at the end

    // TODO edit group

    // TODO logout

    // TODO leave group?

//    test('delete_category', () async {
//      // load the categories home page
//      var drawerOpenButton = find.byTooltip("Open navigation menu");
//      await driver.tap(drawerOpenButton);
//      var categoryButton = find.byValueKey("groups_home:my_categories_button");
//      await driver.tap(categoryButton);
//      await driver.waitFor(find.byValueKey("categories_home:scaffold"));
//      // delete the category
//      var categoryDeleteButton = find.byTooltip("Delete Category");
//      driver.tap(categoryDeleteButton);
//      // always delete the first category, more might exist if previous tests failed
//      var categoryDeleteConfirmButton = find.text("categories_list_item:category_edit_button_confirm:0");
//      await driver.tap(categoryDeleteConfirmButton);
//      await driver.waitFor(find.byValueKey("categories_home:scaffold"));
//      // go back to groups home
//      await driver.tap(find.pageBack());
//      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
//    });
  });
}
