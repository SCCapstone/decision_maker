class Favorite {
  final String username;
  final String icon;
  final String displayName;

  Favorite({this.username, this.displayName, this.icon});

  Favorite.debug(this.username, this.displayName, this.icon);

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    return other is Favorite && this.username == other.username;
  }

  @override
  int get hashCode {
    return username.hashCode;
  }

  @override
  String toString() {
    return "Username: $username DisplayName: $displayName Icon: $icon";
  }
}
