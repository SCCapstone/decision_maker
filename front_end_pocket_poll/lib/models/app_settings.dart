import 'package:front_end_pocket_poll/imports/users_manager.dart';

class AppSettings {
  bool muted;
  bool darkTheme;
  int groupSort;
  int categorySort;
  int defaultConsiderDuration;
  int defaultVotingDuration;

  AppSettings(
      {this.muted,
      this.darkTheme,
      this.groupSort,
      this.categorySort,
      this.defaultConsiderDuration,
      this.defaultVotingDuration});

  AppSettings.debug(
      this.muted,
      this.darkTheme,
      this.groupSort,
      this.categorySort,
      this.defaultConsiderDuration,
      this.defaultVotingDuration);

  factory AppSettings.fromJson(Map<String, dynamic> json) {
    return AppSettings(
        muted: json[UsersManager.APP_SETTINGS_MUTED],
        darkTheme: json[UsersManager.APP_SETTINGS_DARK_THEME],
        groupSort: json[UsersManager.APP_SETTINGS_GROUP_SORT],
        categorySort: json[UsersManager.APP_SETTINGS_CATEGORY_SORT],
        defaultConsiderDuration: json[UsersManager.DEFAULT_CONSIDER_DURATION],
        defaultVotingDuration: json[UsersManager.DEFAULT_VOTING_DURATION]);
  }

  Map asMap() {
    return {
      UsersManager.APP_SETTINGS_GROUP_SORT: groupSort,
      UsersManager.APP_SETTINGS_CATEGORY_SORT: categorySort,
      UsersManager.APP_SETTINGS_MUTED: muted,
      UsersManager.APP_SETTINGS_DARK_THEME: darkTheme,
      UsersManager.DEFAULT_CONSIDER_DURATION: defaultConsiderDuration,
      UsersManager.DEFAULT_VOTING_DURATION: defaultVotingDuration
    };
  }

  @override
  String toString() {
    return "Muted: $muted DarkTheme: $darkTheme GroupSort: "
        "$groupSort CategorySort: $categorySort ConsdierDuration: $defaultConsiderDuration"
        " VotingDuration: $defaultVotingDuration";
  }
}
