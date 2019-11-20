package net.lastowski.eucworld.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.widget.RemoteViews;

import net.lastowski.eucworld.BluetoothLeService;
import net.lastowski.eucworld.LivemapService;
import net.lastowski.eucworld.LoggingService;
import net.lastowski.eucworld.MainActivity;
import net.lastowski.eucworld.PebbleService;
import net.lastowski.eucworld.SpeechService;
import net.lastowski.eucworld.WheelData;
import net.lastowski.eucworld.R;

public class NotificationUtil {

    private static final int notificationId = 369369;
    private static final String notificationChannel = "net.lastowski.eucworld.notification_channel";

    private static NotificationUtil instance = null;
    private static NotificationCompat.Builder builder;

    private static NotificationManager notificationManager;
    private static int connectionState = BluetoothLeService.STATE_DISCONNECTED;
    private static int notificationMessageId = R.string.disconnected;
    private static int battery = 0;
    private static double distance = 0;
    private static int temperature = 0;

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case Constants.ACTION_BLUETOOTH_CONNECTION_STATE:
                    connectionState = intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, BluetoothLeService.STATE_DISCONNECTED);
                    switch (connectionState) {
                        case BluetoothLeService.STATE_CONNECTED:
                            notificationMessageId = R.string.connected;
                            break;
                        case BluetoothLeService.STATE_DISCONNECTED:
                            notificationMessageId = R.string.disconnected;
                            break;
                        case BluetoothLeService.STATE_CONNECTING:
                            if (intent.hasExtra(Constants.INTENT_EXTRA_BLE_AUTO_CONNECT))
                                notificationMessageId = R.string.searching;
                            else
                                notificationMessageId = R.string.connecting;
                            break;
                    }
                    updateNotification(context);
                    break;
                case Constants.ACTION_WHEEL_DATA_AVAILABLE:
                    int b = WheelData.getInstance().getBatteryLevel();
                    int t = WheelData.getInstance().getTemperature();
                    double d = (double) Math.round(WheelData.getInstance().getDistanceDouble() * 10) / 10;
                    if (battery != b || distance != d || temperature != t) {
                        battery = b;
                        temperature = t;
                        distance = d;
                        updateNotification(context);
                    }
                    break;
                case Constants.ACTION_PEBBLE_SERVICE_TOGGLED:
                case Constants.ACTION_LOGGING_SERVICE_TOGGLED:
                case Constants.ACTION_SPEECH_SERVICE_TOGGLED:
                case Constants.ACTION_LIVEMAP_SERVICE_TOGGLED:
                    updateNotification(context);
                    break;
            }
        }
    };

    public NotificationUtil(Context context) {
        if (instance == null) {
            context.registerReceiver(messageReceiver, makeIntentFilter());
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            builder = new NotificationCompat.Builder(context, notificationChannel);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = context.getResources().getString(R.string.app_name);
                NotificationChannel serviceChannel = new NotificationChannel(notificationChannel, name, NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(serviceChannel);
            }
            instance = this;
        }
    }

    public static int getNotificationId() { return notificationId; }

    public static Notification getNotification(Context context) {
        if (instance == null)
            instance = new NotificationUtil(context);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        RemoteViews notificationView = new RemoteViews(context.getPackageName(), R.layout.notification_base);

        PendingIntent pendingConnectionIntent = PendingIntent.getBroadcast(context, 0, new Intent(Constants.NOTIFICATION_BUTTON_CONNECTION), 0);
        PendingIntent pendingWatchIntent = PendingIntent.getBroadcast(context, 0, new Intent(Constants.NOTIFICATION_BUTTON_WATCH), 0);
        PendingIntent pendingLoggingIntent = PendingIntent.getBroadcast(context, 0, new Intent(Constants.NOTIFICATION_BUTTON_LOGGING), 0);
        PendingIntent pendingSpeechIntent = PendingIntent.getBroadcast(context, 0, new Intent(Constants.NOTIFICATION_BUTTON_SPEECH), 0);
        PendingIntent pendingLivemapIntent = PendingIntent.getBroadcast(context, 0, new Intent(Constants.NOTIFICATION_BUTTON_LIVEMAP), 0);

        notificationView.setOnClickPendingIntent(R.id.ib_connection, pendingConnectionIntent);
        notificationView.setOnClickPendingIntent(R.id.ib_watch, pendingWatchIntent);
        notificationView.setOnClickPendingIntent(R.id.ib_logging, pendingLoggingIntent);
        notificationView.setOnClickPendingIntent(R.id.ib_speech, pendingSpeechIntent);
        notificationView.setOnClickPendingIntent(R.id.ib_livemap, pendingLivemapIntent);

        switch (connectionState) {
            case BluetoothLeService.STATE_CONNECTING:
                notificationView.setImageViewResource(R.id.ib_connection, R.drawable.anim_wheel_icon_notification);
                break;
            case BluetoothLeService.STATE_CONNECTED:
                notificationView.setImageViewResource(R.id.ib_connection, R.drawable.ic_wheel_active_notification_24px);
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                notificationView.setImageViewResource(R.id.ib_connection, R.drawable.ic_circle_notification_24px);
                break;
        }

        notificationView.setTextViewText(R.id.text_title, context.getString(R.string.app_name));
        String title = context.getString(notificationMessageId);

        if (connectionState == BluetoothLeService.STATE_CONNECTED || (distance + temperature + battery) > 0) {
            notificationView.setTextViewText(R.id.text_message, context.getString(R.string.notification_text, battery, temperature, distance));
        }

        notificationView.setTextViewText(R.id.text_title, title);

        if (PebbleService.isInstanceCreated())
            notificationView.setImageViewResource(R.id.ib_watch, R.drawable.ic_watch_active_notification_24px);
        else
            notificationView.setImageViewResource(R.id.ib_watch, R.drawable.ic_watch_notification_24px);

        if (LoggingService.isInstanceCreated())
            notificationView.setImageViewResource(R.id.ib_logging, R.drawable.ic_csv_active_notification_24px);
        else
            notificationView.setImageViewResource(R.id.ib_logging, R.drawable.ic_csv_notification_24px);

        if (SpeechService.isInstanceCreated())
            notificationView.setImageViewResource(R.id.ib_speech, R.drawable.ic_speech_active_notification_24px);
        else
            notificationView.setImageViewResource(R.id.ib_speech, R.drawable.ic_speech_notification_24px);

        if (LivemapService.isInstanceCreated())
            notificationView.setImageViewResource(R.id.ib_livemap, R.drawable.ic_route_active_notification_24px);
        else
            notificationView.setImageViewResource(R.id.ib_livemap, R.drawable.ic_route_notification_24px);

        return builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setContent(notificationView)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void updateNotification(Context context) {
        if (notificationManager != null)
            notificationManager.notify(getNotificationId(), getNotification(context));
    }

    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(messageReceiver);
    }

    private IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_LOGGING_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_PEBBLE_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_SPEECH_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_LIVEMAP_SERVICE_TOGGLED);
        return intentFilter;
    }

    public static class notificationButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Constants.NOTIFICATION_BUTTON_CONNECTION.equals(action))
                context.sendBroadcast(new Intent(Constants.ACTION_REQUEST_CONNECTION_TOGGLE));
            else
            if (Constants.NOTIFICATION_BUTTON_WATCH.equals(action)) {
                Intent pebbleServiceIntent = new Intent(context.getApplicationContext(), PebbleService.class);
                if (PebbleService.isInstanceCreated())
                    context.stopService(pebbleServiceIntent);
                else
                    context.startService(pebbleServiceIntent);
            }
            else
            if (Constants.NOTIFICATION_BUTTON_LOGGING.equals(action)) {
                Intent loggingServiceIntent = new Intent(context.getApplicationContext(), LoggingService.class);
                if (LoggingService.isInstanceCreated())
                    context.stopService(loggingServiceIntent);
                else
                    context.startService(loggingServiceIntent);
            }
            else
            if (Constants.NOTIFICATION_BUTTON_SPEECH.equals(action)) {
                Intent speechServiceIntent = new Intent(context.getApplicationContext(), SpeechService.class);
                if (SpeechService.isInstanceCreated())
                    context.stopService(speechServiceIntent);
                else
                    context.startService(speechServiceIntent);
            }
            else
            if (Constants.NOTIFICATION_BUTTON_LIVEMAP.equals(action)) {
                Intent livemapServiceIntent = new Intent(context.getApplicationContext(), LivemapService.class);
                if (LivemapService.isInstanceCreated())
                    context.stopService(livemapServiceIntent);
                else
                if (!LivemapService.getApiKey().isEmpty())
                    context.startService(livemapServiceIntent);
            }
        }
    }
}
