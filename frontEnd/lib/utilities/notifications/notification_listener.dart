import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:frontEnd/groups_widgets/groups_home.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/models/message.dart';

import 'notification_handler.dart';
import 'notification_service.dart';

class NotificationListener{
  Function refreshList;
  Function returnHome;
  Stream<Message> notificationStream;
  NotificationService notificationService;
  BuildContext context;

  NotificationListener(){
    setUpListener();
  }

  void setUpListener(){
    NotificationService.instance.start();
    notificationStream = NotificationHandler.instance.notificationsStream;
    notificationStream.listen((message) {
      print("home listener");
      /*
        For simplicity's sake, only the group home will show any of the toasts indicating a new notification.
        Other widgets down the tree of the app will refresh depending on the action (such as a new event).
       */
      if (message.action == NotificationService.leaveGroupAction) {
        String groupId = message.payload[GroupsManager.GROUP_ID];
        if (Globals.currentGroup != null &&
            Globals.currentGroup.groupId == groupId) {
          // somewhere in the app the user is in the group they were kicked out of, so bring them back to the home apge
          Navigator.pushAndRemoveUntil(
              context,
              new MaterialPageRoute(
                  builder: (BuildContext context) => GroupsHome()),
                  (Route<dynamic> route) => false);
        }
      } else if (message.action == NotificationService.newGroupAction) {
        Fluttertoast.showToast(
            msg: "${message.title}\n${message.body}",
            toastLength: Toast.LENGTH_LONG,
            gravity: ToastGravity.CENTER);
        if (ModalRoute.of(context).isCurrent) {
          // only refresh if this widget is visible
          refreshList();
        }
      } else {
        // event updates
        Fluttertoast.showToast(
            msg: "${message.title}\n${message.body}",
            toastLength: Toast.LENGTH_LONG,
            gravity: ToastGravity.CENTER);
      }
    });
  }

}