import 'package:flutter/material.dart';

@immutable
class Message {
  final String title;
  final String body;
  final String action;
  final Map<String, dynamic> payload;

  const Message({
    @required this.title,
    @required this.body,
    @required this.action,
    @required this.payload
  });

  @override
  String toString() {
    return "Title: $title Body: $body Action: $action Metdata $payload";
  }
}
