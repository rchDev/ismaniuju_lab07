package com.example.lab07;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BatteryStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BATTERY_LOW.equals(action)) {

        } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {

        }
    }
}
