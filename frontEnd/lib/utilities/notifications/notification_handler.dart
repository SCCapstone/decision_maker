import 'package:frontEnd/models/message.dart';
import 'package:rxdart/rxdart.dart';

class NotificationHandler {
  NotificationHandler._internal();

  static final NotificationHandler instance = NotificationHandler._internal();

  final BehaviorSubject<Message> notificationStream =
      BehaviorSubject<Message>();

  Stream<Message> get notificationsStream {
    return notificationStream;
  }

  void addNotification(Message message) {
    notificationStream.sink.add(message);
  }

  void dispose() {
    notificationStream?.close();
  }
}
