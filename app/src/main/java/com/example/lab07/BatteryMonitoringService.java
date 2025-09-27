package com.example.lab07;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.Nullable;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class BatteryMonitoringService extends Service {

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
        // Register dynamic receiver
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
        return null; // Not a bound service
    }
}
