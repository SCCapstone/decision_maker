class User {
  final String username;
  final String firstName;
  final String lastName;
  final bool darkTheme;
  final bool muted;
  final Map<String, dynamic> groups;
  final Map<String, dynamic> categories;

  User(
      {this.username,
      this.firstName,
      this.lastName,
      this.darkTheme,
      this.muted,
      this.groups,
      this.categories});

  User.debug(this.username, this.firstName, this.lastName, this.darkTheme,
      this.muted, this.groups, this.categories);

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
        username: json['Username'],
        firstName: json['FirstName'],
        lastName: json['LastName'],
        darkTheme: json['AppSetting_DarkTheme'],
        muted: json['AppSetting_Muted'],
        groups: json['Groups'],
        categories: json['Categories']);
  }

  @override
  String toString() {
    return "Username: $username FirstName: $firstName LastName: "
        "$lastName DarkTheme: $darkTheme Muted: $muted Groups: $groups "
        "Categories: $categories";
  }
}
