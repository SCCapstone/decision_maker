import 'dart:convert';

import 'package:flutter/material.dart';

@immutable
class Message {
  final String title;
  final String body;
  final String action;
  final Map<String, dynamic> payload;

  const Message(
      {@required this.title,
      @required this.body,
      @required this.action,
      @required this.payload});

  @override
  String toString() {
    return "Title: $title Body: $body Action: $action Metdata $payload";
  }

  static Message fromJSON(Map<String, dynamic> json) {
    Map<String, dynamic> metadata;

    final dynamic data = json["data"];
    if (data != null) {
      metadata = jsonDecode(data["metadata"]);
    } else {
      metadata = jsonDecode(json["metadata"]);
    }

    if (metadata != null) {
      dynamic notification = json["notification"];
      if (notification == null) {
        notification = json["aps"];
        if (notification != null) {
          notification = notification["alert"];
        } else {
          notification = {"title": null, "action": null};
        }
      }

      return new Message(
          title: notification['title'],
          body: notification['body'],
          action: metadata['action'],
          payload: metadata['payload']);
    } else {
      throw new Exception("Bad message format.");
    }
  }
}
