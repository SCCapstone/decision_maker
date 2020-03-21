import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/groups_widgets/group_page.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/message.dart';

import 'notification_handler.dart';

class NotificationService {
  static final String leaveGroupAction = "leaveGroupAction";
  static final String newGroupAction = "newGroup";
  static final String eventUpdatedAction = "eventUpdated";

  final FirebaseMessaging firebaseMessaging = FirebaseMessaging();
  bool initialized = false;
  BuildContext context;
  Function refreshGroupsHome;

  NotificationService._internal();

  static final NotificationService instance = NotificationService._internal();

  void start(BuildContext context, Function refreshFunction) {
    this.context = context;
    this.refreshGroupsHome = refreshFunction;
    if (!initialized) {
      initializeNotifications(context);
      initialized = true;
    }
  }

  void initializeNotifications(BuildContext context) {
    Future<String> token = firebaseMessaging.getToken();
    UsersManager.registerPushEndpoint(token);

    firebaseMessaging.configure(
        onMessage: _onMessage, onLaunch: _onLaunch, onResume: _onResume);
    firebaseMessaging.requestNotificationPermissions(
        const IosNotificationSettings(sound: true, badge: true, alert: true));
  }

  Future<void> _onMessage(Map<String, dynamic> message) async {
    final notification = message['notification'];
    if (notification != null) {
      final data = message['data'];
      if (data != null) {
        Map<String, dynamic> metadata = jsonDecode(data['metadata']);
        Message message = new Message(
            title: notification['title'],
            body: notification['body'],
            action: metadata['action'],
            payload: metadata['payload']);
        NotificationHandler.instance.addNotification(message);
      }
    }
    return null;
  }

  Future<void> _onLaunch(Map<String, dynamic> message) {
    Message notification;
    final notificationData = message['notification'];
    if (notificationData != null) {
      final data = message['data'];
      if (data != null) {
        Map<String, dynamic> metadata = jsonDecode(data['metadata']);
        notification = new Message(
            title: notificationData['title'],
            body: notificationData['body'],
            action: metadata['action'],
            payload: metadata['payload']);
      }
    }
    if (notification != null) {
      if (notification.action == newGroupAction) {
        // take the user straight to the group they were added to if they click the notification
        Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => GroupPage(
                    groupId: notification.payload[GroupsManager.GROUP_ID],
                    groupName: "TODO",
                  )),
        ).then((val) {
          this.refreshGroupsHome();
        });
      }
    }
    return null;
  }

  Future<void> _onResume(Map<String, dynamic> message) {
    print("onResume $message");
    return null;
  }
}
