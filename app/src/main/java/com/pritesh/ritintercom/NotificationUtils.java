package com.pritesh.ritintercom;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.util.DebugUtils;

import com.pritesh.ritintercom.activities.HomeActivity;
import com.pritesh.ritintercom.data.ChatData;
import com.pritesh.ritintercom.data.DeviceData;
import com.pritesh.ritintercom.utils.DialogUtils;

/**
 * Created by Sourabh on 7/4/2017.
 */
public class NotificationUtils {

    static HomeActivity homeActivity;

    public static final int NOTIFICATION_ID = 1;

    public static final String ACTION_1 = "action_1";

    static DeviceData chatDevice;


    public static void displayNotification(HomeActivity ha, Context context, ChatData chat, DeviceData cd) {

        homeActivity = ha;
        chatDevice = cd;
        Intent action1Intent = new Intent(context, NotificationActionService.class)
                .setAction(ACTION_1);

        PendingIntent action1PendingIntent = PendingIntent.getService(context, 0,
                action1Intent, PendingIntent.FLAG_ONE_SHOT);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(chat.getSentBy())
                        .setContentText(chat.getMessage())
                        .setSound(alarmSound)
                        .addAction(new NotificationCompat.Action(R.drawable.ic_launcher,
                                "Action 1", action1PendingIntent));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    public static class NotificationActionService extends IntentService {
        public NotificationActionService() {
            super(NotificationActionService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String action = intent.getAction();
            if (ACTION_1.equals(action)) {

                DialogUtils.openChatActivity(homeActivity, chatDevice);


                // TODO: handle action 1.
                // If you want to cancel the notification: NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
            }
        }
    }

}