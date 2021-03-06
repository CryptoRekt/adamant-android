package im.adamant.android.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import dagger.android.AndroidInjection;
import im.adamant.android.R;
import im.adamant.android.helpers.LoggerHelper;
import im.adamant.android.helpers.NotificationHelper;
import im.adamant.android.ui.BaseActivity;
import im.adamant.android.ui.SplashScreen;

import static im.adamant.android.Constants.ADAMANT_DEFAULT_NOTIFICATION_CHANNEL_ID;

public class AdamantFirebaseMessagingService extends FirebaseMessagingService {
    private static final int NOTIFICATION_ID = 123445;

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (BaseActivity.isActivityInForeground()){
            LoggerHelper.d("FCM", "IGNORE PUSH-NOTIFICATION ID: " + remoteMessage.getMessageId());
        } else {
            showNotification(remoteMessage);
        }
    }

    private void showNotification(RemoteMessage remoteMessage) {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = getString(R.string.adamant_default_notification_channel);
//            channelId = NotificationHelper.createNotificationChannel(ADAMANT_DEFAULT_NOTIFICATION_CHANNEL_ID, channelName, this);

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            NotificationChannel chan = new NotificationChannel(ADAMANT_DEFAULT_NOTIFICATION_CHANNEL_ID,
                    channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.RED);
            chan.enableLights(true);
            chan.enableVibration(true);
            chan.setVibrationPattern(NotificationHelper.VIBRATE_PATTERN);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            chan.setSound(NotificationHelper.SOUND_URI, attributes);

            NotificationManager service = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            if (service != null) {
                service.createNotificationChannel(chan);
                channelId = ADAMANT_DEFAULT_NOTIFICATION_CHANNEL_ID;
            }
        }

        String title = getString(R.string.adamant_default_notification_channel);
        String text = getString(R.string.default_notification_message);

        Intent notificationIntent = new Intent(this.getApplicationContext(), SplashScreen.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent intent = PendingIntent.getActivity(this.getApplicationContext(), 0,
                notificationIntent, 0);

        Notification notification = NotificationHelper.buildMessageNotification(channelId, this, title, text);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        notification.contentIntent = intent;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

//        int id = (int)(remoteMessage.getSentTime() / 1000);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
