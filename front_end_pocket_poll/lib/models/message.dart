import 'dart:convert';

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

  static Message fromJSON(Map<String, dynamic> json) {
    final notification = json["notification"];
    final data = json["data"];
    if (notification != null && data != null) {
      Map<String, dynamic> metadata = jsonDecode(data['metadata']);
      if (metadata != null) {
        return new Message(
            title: notification['title'],
            body: notification['body'],
            action: metadata['action'],
            payload: metadata['payload']);
      } else {
        throw new Exception("Bad message format.");
      }
    } else {
      throw new Exception("Bad message format.");
    }
  }
}