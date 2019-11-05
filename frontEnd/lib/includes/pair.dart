class Pair {
  final String key;
  final String value;

  Pair({this.key, this.value});

  factory Pair.fromJson(Map<String, dynamic> json) {
    return Pair(
      key: json['key'],
      value: json['value'],
    );
  }
}