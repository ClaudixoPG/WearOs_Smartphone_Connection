package com.randomadjective.uactivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

public final class PhoneSessionInfo {

    public static final class Snapshot {
        public final String phoneModel;
        public final double batteryLevel;
        public final double temperatureC;

        public Snapshot(String phoneModel, double batteryLevel, double temperatureC) {
            this.phoneModel = phoneModel;
            this.batteryLevel = batteryLevel;
            this.temperatureC = temperatureC;
        }
    }

    private PhoneSessionInfo() { }

    public static Snapshot read(Context context) {
        Intent batteryIntent = context.registerReceiver(
                null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        );

        int level = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        int temp = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) : -1;

        double batteryLevel = (level >= 0 && scale > 0) ? (level * 100.0 / scale) : -1.0;
        double temperatureC = (temp >= 0) ? (temp / 10.0) : -1.0;
        String phoneModel = Build.MODEL != null ? Build.MODEL : "UnknownPhone";

        return new Snapshot(phoneModel, batteryLevel, temperatureC);
    }
}