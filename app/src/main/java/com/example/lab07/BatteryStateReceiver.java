package com.example.lab07;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class BatteryStateReceiver extends BroadcastReceiver {
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
}
