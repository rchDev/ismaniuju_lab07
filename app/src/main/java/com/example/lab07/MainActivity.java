package com.example.lab07;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.WorkManager;


public class MainActivity extends AppCompatActivity {

    private boolean isBatteryReceiverRegistered = false;
    private final BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                return;
            }
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int percent = (int) ((level / (float) scale) * 100);
            Toast.makeText(context, "Battery level: " + percent, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        PackageManager pm = this.getPackageManager();
        ComponentName component = new ComponentName(this, BatteryStateReceiver.class);

        SwitchCompat batterySwitch = findViewById(R.id.sw_battery_level);
        batterySwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                );
                registerBatteryInfoReceiver();
            } else {
                unregisterBatteryInfoReceiver();
                pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );
                WorkManager.getInstance(this)
                        .cancelUniqueWork(BatteryReminderWorker.TAG);
            }
        });
    }

    private void registerBatteryInfoReceiver() {
        if (isBatteryReceiverRegistered) {
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);

        registerReceiver(batteryInfoReceiver, filter);

        isBatteryReceiverRegistered = true;
    }

    private void unregisterBatteryInfoReceiver() {
        if (!isBatteryReceiverRegistered) {
            return;
        }
        unregisterReceiver(batteryInfoReceiver);
        isBatteryReceiverRegistered = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        SwitchCompat batterySwitch = findViewById(R.id.sw_battery_level);
        if (batterySwitch.isChecked()) {
            registerBatteryInfoReceiver();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterBatteryInfoReceiver();
    }
}