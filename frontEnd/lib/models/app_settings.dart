import 'package:frontEnd/imports/users_manager.dart';

class AppSettings {
  final int muted;
  final int darkTheme;
  final int groupSort;

  AppSettings(
      {this.muted,
       this.darkTheme,
       this.groupSort});

  AppSettings.debug(this.muted, this.darkTheme, this.groupSort);

  factory AppSettings.fromJson(Map<String, dynamic> json) {
    return AppSettings(
        muted: int.parse(json[UsersManager.APP_SETTINGS_MUTED]),
        darkTheme: int.parse(json[UsersManager.APP_SETTINGS_DARK_THEME]),
        groupSort: int.parse(json[UsersManager.APP_SETTINGS_GROUP_SORT]));
  }

  @override
  String toString() {
    return "Muted: $muted DarkTheme: $darkTheme GroupSort: "
        "$groupSort";
  }
}