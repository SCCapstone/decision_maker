import 'dart:async';
import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_consider.dart';
import 'package:frontEnd/events_widgets/event_details_occurring.dart';
import 'package:frontEnd/events_widgets/event_details_voting.dart';
import 'package:frontEnd/groups_widgets/group_page.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/message.dart';

class NotificationService {
  static final String removedFromGroupAction = "removedFromGroup";
  static final String addedToGroupAction = "addedToGroup";
  static final String eventCreatedAction = "eventCreated";
  static final String eventChosenAction = "eventChosen";
  static final String eventVotingAction = "eventVoting";
  static final StreamController<Message> messageBroadcaster =
      new StreamController.broadcast();

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

  void closeStream() {
    messageBroadcaster.close();
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
    print("onMessage $message");
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
        messageBroadcaster.sink.add(message);
      }
    }
    return null;
  }

  Future<void> _onLaunch(Map<String, dynamic> message) async {
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
      if (notification.action == addedToGroupAction) {
        // take the user straight to the group they were added to if they click the notification
        Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => GroupPage(
                    groupId: notification.payload[GroupsManager.GROUP_ID],
                    groupName: notification.payload[GroupsManager.GROUP_NAME],
                  )),
        ).then((val) {
          this.refreshGroupsHome();
        });
      } else if (notification.action == eventCreatedAction) {
        Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => GroupPage(
                    groupId: notification.payload[GroupsManager.GROUP_ID],
                    groupName: notification.payload[GroupsManager.GROUP_NAME],
                  )),
        ).then((val) {
          this.refreshGroupsHome();
        });
      } else if (notification.action == eventVotingAction) {
        Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => GroupPage(
                    groupId: notification.payload[GroupsManager.GROUP_ID],
                    groupName: notification.payload[GroupsManager.GROUP_NAME],
                  )),
        ).then((val) {
          this.refreshGroupsHome();
        });
      } else if (notification.action == eventChosenAction) {
        Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => GroupPage(
                    groupId: notification.payload[GroupsManager.GROUP_ID],
                    groupName: notification.payload[GroupsManager.GROUP_NAME],
                  )),
        ).then((val) {
          this.refreshGroupsHome();
        });
      }
    }
    return null;
  }

  Future<void> _onResume(Map<String, dynamic> message) {
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
        messageBroadcaster.sink.add(message);
      }
    }
    return null;
  }
}
