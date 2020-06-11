package com.decisionMaker.pocketPoll;

import android.os.Bundle;
import io.flutter.app.FlutterActivity;
import io.flutter.plugins.GeneratedPluginRegistrant;
import android.app.NotificationManager;
import android.content.Context;

public class MainActivity extends FlutterActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    GeneratedPluginRegistrant.registerWith(this);
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Removes sotifications on the notifcation tray
    cancelAllNotifications();
  }

  private void cancelAllNotifications() {
    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.cancelAll();
  }
}
