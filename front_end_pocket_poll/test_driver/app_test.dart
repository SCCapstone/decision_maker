import 'dart:math';

import 'package:flutter_driver/flutter_driver.dart';
import 'package:test/test.dart';

void main() {
  group('Pocket Poll Integration Test', () {
    /*
      Note that if any test fails it is suggested to go and delete any categories/groups made as
      they will not be automatically deleted (and there is an upper limit on # of categories that can be made).
      
      Also note that to use the app again you need to re-install the app, since the flutter driver plugin disables 
      the keyboard.
     */
    final String primaryTesterUsername = "testingUser1!";
    final String primaryTesterPassword = "testingUser1!";
    final String primaryTestingUserDisplayName = "Testing";
    final String favoritesUser = "testingUser2!";
    final String otherUser = "testingUser3!";
    final Random rng = new Random();
    final int maxCategoryName = 100000;
    final int maxChoiceName = 10000;
    final int maxEventName = 10000;

    String getRandomCategoryName() {
      return rng.nextInt(maxCategoryName).toString();
    }

    String getRandomChoiceName() {
      return rng.nextInt(maxChoiceName).toString();
    }

    String getRandomEventName() {
      return rng.nextInt(maxEventName).toString();
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
      await driver.enterText(primaryTesterUsername);

      var passwordField = find.byValueKey("login_page:password_input");
      await driver.tap(passwordField);
      await driver.enterText(primaryTesterPassword);

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
      await driver.enterText(primaryTestingUserDisplayName);
      await driver.tap(saveButton);

      // open the favorites page
      var favoritesPageButton =
          find.byValueKey("user_settings:favorites_button");
      await driver.tap(favoritesPageButton);
      await driver.waitFor(find.byValueKey("favorites_page:scaffold"));

      // enter a user
      var usernameInput = find.byValueKey("favorites_page:username_input");
      await driver.tap(usernameInput);
      await driver.enterText(favoritesUser);
      var addUserButton = find.byValueKey("favorites_page:add_user_button");
      await driver.tap(addUserButton);
      await driver.waitFor(find.byValueKey("user_row:$favoritesUser:delete"));

      // remove the user
      var deleteUserButton = find.byValueKey("user_row:$favoritesUser:delete");
      await driver.tap(deleteUserButton);

      // add user back
      await driver.tap(usernameInput);
      await driver.enterText(favoritesUser);
      await driver.tap(addUserButton);
      await driver.waitFor(find.byValueKey("user_row:$favoritesUser:delete"));

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
      var addFavoriteButton = find.byValueKey("user_row:$favoritesUser:add");
      await driver.tap(addFavoriteButton);
      await driver.tap(showFavoritesButton);
      await driver.waitFor(find.byValueKey("user_row:$favoritesUser:delete"));

      // add a member not in favorites
      var memberInput = find.byValueKey("members_page:member_input");
      await driver.tap(memberInput);
      await driver.enterText(otherUser);
      var addMemberButton = find.byValueKey("members_page:add_member_button");
      await driver.tap(addMemberButton);
      await driver.waitFor(find.byValueKey("user_row:$otherUser:delete"));
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("groups_create:scaffold"));

      // now save the group
      var saveButton = find.byValueKey("groups_create:save_button");
      await driver.tap(saveButton);
      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
    });

    test('create_basic_event', () async {
      var group = find.byValueKey("group_row:0");
      await driver.tap(group);
      await driver.waitFor(find.byValueKey("group_page:scaffold"));

      // enter the create event page
      var createEventButton = find.byValueKey("group_page:create_event_button");
      await driver.tap(createEventButton);
      await driver.waitFor(find.byValueKey("event_create:scaffold"));

      // enter an event name (save it for verifying later it was created)
      String eventName = getRandomEventName();
      var eventNameInput = find.byValueKey("event_create:event_name_input");
      await driver.tap(eventNameInput);
      await driver.enterText(eventName);

      // select a category
      var addCategoryButton =
          find.byValueKey("event_create:add_category_button");
      await driver.tap(addCategoryButton);

      // wait for categories to load
      await driver
          .waitFor(find.byValueKey("category_popup_single:category_container"));

      // always select first category
      var categoryCheckBox = find.byValueKey("category_row:checkbox:0");
      await driver.tap(categoryCheckBox);
      var doneButton = find.byValueKey("category_popup_single:done_button");
      await driver.tap(doneButton);
      await driver.waitFor(find.byValueKey("event_create:scaffold"));

      // enter the start time (date is already set for current day by default)
      var hourInput = find.byValueKey("event_create:hour_input");
      await driver.tap(hourInput);
      await driver.enterText(" "); // space automatically picks next hour
      var minuteInput = find.byValueKey("event_create:minute_input");
      await driver.tap(minuteInput);
      await driver.enterText(" "); // space automatically picks current minute

      // enter a consider time
      var considerInput = find.byValueKey("event_create:consider_input");
      await driver.tap(considerInput);
      await driver.enterText("2");

      // enter a vote time
      var voteInput = find.byValueKey("event_create:vote_input");
      await driver.tap(voteInput);
      await driver.enterText("2");

      // save the event
      var saveEventButton = find.byValueKey("event_create:save_event_button");
      await driver.tap(saveEventButton);
      await driver.waitFor(find.byValueKey("group_page:scaffold"));

      // make sure event is there in right stage
      await driver.waitFor(find.byValueKey("event_card_consider:$eventName"));
    });

    test('create_event_skip_consider', () async {
      // enter the create event page
      var createEventButton = find.byValueKey("group_page:create_event_button");
      await driver.tap(createEventButton);
      await driver.waitFor(find.byValueKey("event_create:scaffold"));

      // enter an event name (save it for verifying later it was created)
      String eventName = getRandomEventName();
      var eventNameInput = find.byValueKey("event_create:event_name_input");
      await driver.tap(eventNameInput);
      await driver.enterText(eventName);

      // select a category
      var addCategoryButton =
          find.byValueKey("event_create:add_category_button");
      await driver.tap(addCategoryButton);

      // wait for categories to load
      await driver
          .waitFor(find.byValueKey("category_popup_single:category_container"));

      // always select first category
      var categoryCheckBox = find.byValueKey("category_row:checkbox:0");
      await driver.tap(categoryCheckBox);
      var doneButton = find.byValueKey("category_popup_single:done_button");
      await driver.tap(doneButton);
      await driver.waitFor(find.byValueKey("event_create:scaffold"));

      // enter the start time (date is already set for current day by default)
      var hourInput = find.byValueKey("event_create:hour_input");
      await driver.tap(hourInput);
      await driver.enterText(" "); // space automatically picks next hour
      var minuteInput = find.byValueKey("event_create:minute_input");
      await driver.tap(minuteInput);
      await driver.enterText(" "); // space automatically picks current minute

      // SKIP the consider time
      var skipConsiderButton =
          find.byValueKey("event_create:skip_consider_button");
      await driver.tap(skipConsiderButton);

      // enter a vote time
      var voteInput = find.byValueKey("event_create:vote_input");
      await driver.tap(voteInput);
      await driver.enterText("2");

      // save the event
      var saveEventButton = find.byValueKey("event_create:save_event_button");
      await driver.tap(saveEventButton);
      await driver.waitFor(find.byValueKey("group_page:scaffold"));

      // make sure event is there in right stage
      await driver.waitFor(find.byValueKey("event_card_voting:$eventName"));
    });

    test('create_event_skip_vote', () async {
      // enter the create event page
      var createEventButton = find.byValueKey("group_page:create_event_button");
      await driver.tap(createEventButton);
      await driver.waitFor(find.byValueKey("event_create:scaffold"));

      // enter an event name (save it for verifying later it was created)
      String eventName = getRandomEventName();
      var eventNameInput = find.byValueKey("event_create:event_name_input");
      await driver.tap(eventNameInput);
      await driver.enterText(eventName);

      // select a category
      var addCategoryButton =
          find.byValueKey("event_create:add_category_button");
      await driver.tap(addCategoryButton);

      // wait for categories to load
      await driver
          .waitFor(find.byValueKey("category_popup_single:category_container"));

      // always select first category
      var categoryCheckBox = find.byValueKey("category_row:checkbox:0");
      await driver.tap(categoryCheckBox);
      var doneButton = find.byValueKey("category_popup_single:done_button");
      await driver.tap(doneButton);
      await driver.waitFor(find.byValueKey("event_create:scaffold"));

      // enter the start time (date is already set for current day by default)
      var hourInput = find.byValueKey("event_create:hour_input");
      await driver.tap(hourInput);
      await driver.enterText(" "); // space automatically picks next hour
      var minuteInput = find.byValueKey("event_create:minute_input");
      await driver.tap(minuteInput);
      await driver.enterText(" "); // space automatically picks current minute

      // enter a consider time
      var considerInput = find.byValueKey("event_create:consider_input");
      await driver.tap(considerInput);
      await driver.enterText("2");

      // SKIP the vote time
      var skipVoteButton = find.byValueKey("event_create:skip_vote_button");
      await driver.tap(skipVoteButton);

      // save the event
      var saveEventButton = find.byValueKey("event_create:save_event_button");
      await driver.tap(saveEventButton);
      await driver.waitFor(find.byValueKey("group_page:scaffold"));

      // make sure event is there in right stage
      await driver.waitFor(find.byValueKey("event_card_consider:$eventName"));
    });

    test('create_event_skip_both', () async {
      // enter the create event page
      var createEventButton = find.byValueKey("group_page:create_event_button");
      await driver.tap(createEventButton);
      await driver.waitFor(find.byValueKey("event_create:scaffold"));

      // enter an event name (save it for verifying later it was created)
      String eventName = getRandomEventName();
      var eventNameInput = find.byValueKey("event_create:event_name_input");
      await driver.tap(eventNameInput);
      await driver.enterText(eventName);

      // select a category
      var addCategoryButton =
          find.byValueKey("event_create:add_category_button");
      await driver.tap(addCategoryButton);

      // wait for categories to load
      await driver
          .waitFor(find.byValueKey("category_popup_single:category_container"));

      // always select first category
      var categoryCheckBox = find.byValueKey("category_row:checkbox:0");
      await driver.tap(categoryCheckBox);
      var doneButton = find.byValueKey("category_popup_single:done_button");
      await driver.tap(doneButton);
      await driver.waitFor(find.byValueKey("event_create:scaffold"));

      // enter the start time (date is already set for current day by default)
      var hourInput = find.byValueKey("event_create:hour_input");
      await driver.tap(hourInput);
      await driver.enterText(" "); // space automatically picks next hour
      var minuteInput = find.byValueKey("event_create:minute_input");
      await driver.tap(minuteInput);
      await driver.enterText(" "); // space automatically picks current minute

      // SKIP the consider time
      var skipConsiderButton =
          find.byValueKey("event_create:skip_consider_button");
      await driver.tap(skipConsiderButton);

      // SKIP the vote time
      var skipVoteButton = find.byValueKey("event_create:skip_vote_button");
      await driver.tap(skipVoteButton);

      // save the event
      var saveEventButton = find.byValueKey("event_create:save_event_button");
      await driver.tap(saveEventButton);
      await driver.waitFor(find.byValueKey("group_page:scaffold"));

      // make sure event is there in right stage
      await driver.waitFor(find.byValueKey("event_card_occurring:$eventName"));
    });

    test('edit_group', () async {
      var groupSettingsButton =
          find.byValueKey("group_page:group_settings_button");
      await driver.tap(groupSettingsButton);
      await driver.waitFor(find.byValueKey("group_settings:scaffold"));

      // edit the name
      var groupNameInput = find.byValueKey("group_settings:group_name_input");
      await driver.tap(groupNameInput);
      await driver.enterText("New Group Name");

      // edit the consider time
      var considerInput = find.byValueKey("group_settings:conider_input");
      await driver.tap(considerInput);
      await driver.enterText("2");

      // edit the vote time
      var voteInput = find.byValueKey("group_settings:vote_input");
      await driver.tap(voteInput);
      await driver.enterText("2");

      // remove a selected category
      var addCategoriesButton =
          find.byValueKey("group_settings:add_categories_button");
      await driver.tap(addCategoriesButton);
      await driver.waitFor(find.byValueKey("group_categories:scaffold"));

      // pick the first category that is not a group category (i.e is owned by testing user)
      var categoryCheckbox =
          find.byValueKey("category_row_group:checkbox:false:0");
      await driver.tap(categoryCheckbox);
      await driver.tap(find.pageBack());

      // add a selected category
      await driver.tap(addCategoriesButton);
      await driver.waitFor(find.byValueKey("group_categories:scaffold"));

      // pick the first category that is not a group category (i.e is owned by testing user)
      categoryCheckbox = find.byValueKey("category_row_group:checkbox:false:0");
      await driver.tap(categoryCheckbox);
      await driver.tap(find.pageBack());

      // remove a member
      var addMembersButton =
          find.byValueKey("group_settings:add_members_button");
      await driver.tap(addMembersButton);
      await driver.waitFor(find.byValueKey("members_page:scaffold"));
      var deleteMemberIcon = find.byValueKey("user_row:$otherUser:delete");
      await driver.tap(deleteMemberIcon);
      await driver.tap(find.pageBack());

      // add a member
      await driver.tap(addMembersButton);
      await driver.waitFor(find.byValueKey("members_page:scaffold"));
      var memberInput = find.byValueKey("members_page:member_input");
      await driver.tap(memberInput);
      await driver.enterText(otherUser);
      var addMemberButton = find.byValueKey("members_page:add_member_button");
      await driver.tap(addMemberButton);
      await driver.waitFor(find.byValueKey("user_row:$otherUser:delete"));
      await driver.tap(find.pageBack());

      // save the group
      var saveGroupButton = find.byValueKey("group_settings:save_button");
      await driver.tap(saveGroupButton);

      // go back to home page
      await driver.tap(find.pageBack());
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
    });

    test('log_out', () async {
      var drawerOpenButton = find.byTooltip("Open navigation menu");
      await driver.tap(drawerOpenButton);
      var categoryButton = find.byValueKey("groups_home:log_out_button");
      await driver.tap(categoryButton);
      await driver.waitFor(find.byValueKey("login_page:scaffold"));
    });

    // Log in as second user to test leaving group

    test('leave_group', () async {
      var usernameField = find.byValueKey("login_page:username_input");
      await driver.tap(usernameField);
      await driver.enterText(favoritesUser);

      var passwordField = find.byValueKey("login_page:password_input");
      await driver.tap(passwordField);
      await driver.enterText(favoritesUser);

      var signInOrUpButton = find.byValueKey("login_page:sign_in_button");
      await driver.tap(signInOrUpButton);

      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
      // select a group
      var group = find.byValueKey("group_row:0");
      await driver.tap(group);
      await driver.waitFor(find.byValueKey("group_page:scaffold"));

      // go to group settings page
      var groupSettingsButton =
          find.byValueKey("group_page:group_settings_button");
      await driver.tap(groupSettingsButton);
      await driver.waitFor(find.byValueKey("group_settings:scaffold"));
      // leave the group

      var leaveGroupButton =
          find.byValueKey("group_settings:delete_group_button");
      await driver.tap(leaveGroupButton);
      var leaveConfirm = find.byValueKey("group_settings:leave_confirm");
      await driver.tap(leaveConfirm);
      await driver.waitFor(find.byValueKey("groups_home:scaffold"));

      // log out for next tests
      var drawerOpenButton = find.byTooltip("Open navigation menu");
      await driver.tap(drawerOpenButton);
      var categoryButton = find.byValueKey("groups_home:log_out_button");
      await driver.tap(categoryButton);
      await driver.waitFor(find.byValueKey("login_page:scaffold"));
    });

    test('delete_group', () async {
      var usernameField = find.byValueKey("login_page:username_input");
      await driver.tap(usernameField);
      await driver.enterText(primaryTesterUsername);

      var passwordField = find.byValueKey("login_page:password_input");
      await driver.tap(passwordField);
      await driver.enterText(primaryTesterPassword);

      var signInOrUpButton = find.byValueKey("login_page:sign_in_button");
      await driver.tap(signInOrUpButton);

      await driver.waitFor(find.byValueKey("groups_home:scaffold"));

      // select a group
      var group = find.byValueKey("group_row:0");
      await driver.tap(group);
      await driver.waitFor(find.byValueKey("group_page:scaffold"));

      // go to group settings page
      var groupSettingsButton =
          find.byValueKey("group_page:group_settings_button");
      await driver.tap(groupSettingsButton);
      await driver.waitFor(find.byValueKey("group_settings:scaffold"));

      // delete the group
      var deleteGroupButton =
          find.byValueKey("group_settings:delete_group_button");
      await driver.tap(deleteGroupButton);
      var deleteConfirm = find.byValueKey("group_settings:delete_confirm");
      await driver.tap(deleteConfirm);
      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
    });

    test('delete_category', () async {
      // load the categories home page
      var drawerOpenButton = find.byTooltip("Open navigation menu");
      await driver.tap(drawerOpenButton);
      var categoryButton = find.byValueKey("groups_home:my_categories_button");
      await driver.tap(categoryButton);
      await driver.waitFor(find.byValueKey("categories_home:scaffold"));

      // delete the category (always the first one in the list)
      var categoryDeleteButton =
          find.byValueKey("categories_list_item:category_delete_button:0");
      driver.tap(categoryDeleteButton);

      // always delete the first category, more might exist if previous tests failed
      var categoryDeleteConfirmButton = find
          .byValueKey("categories_list_item:category_delete_button_confirm:0");
      await driver.tap(categoryDeleteConfirmButton);
      await driver.waitFor(find.byValueKey("categories_home:scaffold"));

      // go back to groups home
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
    });

    test('reset_favorites', () async {
      // load the user settings page
      var drawerOpenButton = find.byTooltip("Open navigation menu");
      await driver.tap(drawerOpenButton);
      var categoryButton = find.byValueKey("groups_home:user_settings_button");
      await driver.tap(categoryButton);
      await driver.waitFor(find.byValueKey("user_settings:scaffold"));

      // open the favorites page
      var favoritesPageButton =
          find.byValueKey("user_settings:favorites_button");
      await driver.tap(favoritesPageButton);
      await driver.waitFor(find.byValueKey("favorites_page:scaffold"));

      // remove the user
      var deleteUserButton = find.byValueKey("user_row:$favoritesUser:delete");
      await driver.tap(deleteUserButton);

      // go back to the groups home page
      await driver.tap(find.pageBack());
      await driver.tap(find.pageBack());
      await driver.waitFor(find.byValueKey("groups_home:scaffold"));
    });
  });
}
