package com.example.lab07;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class BatteryMonitoringService extends Service {

    private static final String CHANNEL_ID = "battery_monitor_channel";
    private static final int NOTIFICATION_ID = 101;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_LOW.equals(action)) {
                // Start periodic WorkManager task
                PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest
                        .Builder(BatteryReminderWorker.class, 15, TimeUnit.MINUTES)
                        .build();

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        BatteryReminderWorker.TAG,
                        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                        reminderRequest
                );
            } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
                WorkManager.getInstance(context)
                        .cancelUniqueWork(BatteryReminderWorker.TAG);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery Monitoring")
                .setContentText("Monitoring battery state in background")
                .setSmallIcon(R.mipmap.ic_launcher) // your app icon
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        registerReceiver(batteryReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver); // Avoid leaks
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Battery Monitoring",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
