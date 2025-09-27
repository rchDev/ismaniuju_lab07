package com.example.lab07;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    private boolean isBatteryReceiverRegistered = false;
    private boolean isBatteryReminderWorkerRunning = false;
    private final BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int percent = (int) ((level / (float) scale) * 100);
                showNotification("Battery Reminder", "Battery level: " + percent);
            } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        requestNotificationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        showNotification("Battery Reminder", "Permission granted! Try again.");
                    } else {
                        Toast.makeText(this,
                                "Notification permission denied",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        WorkManager.getInstance(getApplicationContext())
                .getWorkInfosForUniqueWorkLiveData(BatteryReminderWorker.TAG)
                .observe(this, workInfos -> {
                    if (workInfos == null || workInfos.isEmpty()) {
                        return;
                    }
                    WorkInfo.State state = workInfos.get(0).getState();
                    boolean isActive = (state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING);
                    runOnUiThread(() -> {
                        SwitchCompat batterySwitch = findViewById(R.id.sw_battery_level);
                        batterySwitch.setChecked(isActive);
                        this.isBatteryReminderWorkerRunning = isActive;
                    });
                });

        SwitchCompat batterySwitch = findViewById(R.id.sw_battery_level);
        if (!isBatteryReminderWorkerRunning && getBatteryPercentage() <= 15 && batterySwitch.isChecked()) {
            PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest
                    .Builder(BatteryReminderWorker.class, 15, TimeUnit.MINUTES)
                    .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    BatteryReminderWorker.TAG,
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                    reminderRequest
            );
        }

        batterySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                registerBatteryInfoReceiver();
                if (!isBatteryReminderWorkerRunning && getBatteryPercentage() <= 15) {
                    PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest
                            .Builder(BatteryReminderWorker.class, 15, TimeUnit.MINUTES)
                            .build();

                    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                            BatteryReminderWorker.TAG,
                            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                            reminderRequest
                    );
                }
            } else {
                unregisterBatteryInfoReceiver();
                WorkManager.getInstance(this)
                        .cancelUniqueWork(BatteryReminderWorker.TAG);
            }
        });
    }

    private float getBatteryPercentage() {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, iFilter);

        int level = -1;
        int scale = -1;
        if (batteryStatus != null) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        }

        return level * 100 / (float) scale;
    }

    private void registerBatteryInfoReceiver() {
        if (isBatteryReceiverRegistered) return;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        registerReceiver(batteryInfoReceiver, filter);
        isBatteryReceiverRegistered = true;
    }

    private void unregisterBatteryInfoReceiver() {
        if (!isBatteryReceiverRegistered) return;
        unregisterReceiver(batteryInfoReceiver);
        isBatteryReceiverRegistered = false;
    }

    private void showNotification(String title, String message) {
        Context context = getApplicationContext();
        String channelId = "battery_reminder_channel";

        // Create notification channel for Android 8+
        NotificationChannel channel = new NotificationChannel(
                channelId,
                "Battery Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        // Open MainActivity when tapped
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        boolean canPost = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            canPost = ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }

        if (canPost) {
            NotificationManagerCompat.from(context)
                    .notify((int) System.currentTimeMillis(), builder.build());
        } else {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}