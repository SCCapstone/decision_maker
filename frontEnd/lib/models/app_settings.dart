import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/utilities/utilities.dart';

class AppSettings {
  bool muted;
  bool darkTheme;
  int groupSort;

  AppSettings({this.muted, this.darkTheme, this.groupSort});

  AppSettings.debug(this.muted, this.darkTheme, this.groupSort);

  factory AppSettings.fromJson(Map<String, dynamic> json) {
    bool darkVal =
        intToBool(int.parse(json[UsersManager.APP_SETTINGS_DARK_THEME]));
    bool mute = intToBool(int.parse(json[UsersManager.APP_SETTINGS_MUTED]));
    return AppSettings(
        muted: mute,
        darkTheme: darkVal,
        groupSort: int.parse(json[UsersManager.APP_SETTINGS_GROUP_SORT]));
  }

  @override
  String toString() {
    return "Muted: $muted DarkTheme: $darkTheme GroupSort: "
        "$groupSort";
  }
}
