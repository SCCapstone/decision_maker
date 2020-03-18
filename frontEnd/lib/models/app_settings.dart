import 'package:frontEnd/imports/users_manager.dart';

class AppSettings {
  bool muted;
  bool darkTheme;
  int groupSort;
  int categorySort;

  AppSettings({this.muted, this.darkTheme, this.groupSort, this.categorySort});

  AppSettings.debug(
      this.muted, this.darkTheme, this.groupSort, this.categorySort);

  factory AppSettings.fromJson(Map<String, dynamic> json) {
    bool darkVal = json[UsersManager.APP_SETTINGS_DARK_THEME];
    bool mute = json[UsersManager.APP_SETTINGS_MUTED];
    return AppSettings(
        muted: mute,
        darkTheme: darkVal,
        groupSort: json[UsersManager.APP_SETTINGS_GROUP_SORT],
        categorySort: json[UsersManager.APP_SETTINGS_CATEGORY_SORT]);
  }

  @override
  String toString() {
    return "Muted: $muted DarkTheme: $darkTheme GroupSort: "
        "$groupSort CategorySort: $categorySort";
  }
}
